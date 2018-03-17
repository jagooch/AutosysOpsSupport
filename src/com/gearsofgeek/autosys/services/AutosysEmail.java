/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.gearsofgeek.autosys.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.activation.FileDataSource;
import javax.activation.DataHandler;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.SimpleLayout;

/**
 *
 * @author jgooch
 */
public class AutosysEmail {
	private String email_from;
    private String email_to; 
    private String email_cc; 
    private String email_host;
    private Integer email_port;
    private String email_subject;
    private String email_body; //body of the email
    private String email_user;//authentication username
    private String email_pwd; //authentication password
    private Boolean email_auth; //disable or enable authenticating to the SMPT gateway
    private Boolean email_debug; //disable or enable debug mode of the SMTP library
    private String email_attachment; //fully qualified path to the email attachment file
    private String authConfigFilePath;//holds the path to optional authentication config file

    private static org.apache.log4j.Logger logger;//logger to manager all logging needs :)
    private static Level defaultLoggingLevel = Level.INFO;
    
    //Options members
    private static final String CONFIGFILEPATH = "c";
    private static final String LOGGINGLEVEL = "l";
    private static final String AUTHFILEPATH = "a"; //option whose argument hold the path to the authorization  file
    private static final String VARIABLES = "v";
    
    private String variables;
    private static final String HELP = "h";
    HashMap <String,String> hmVars; //stores command line keypairs for creating custom mail text

    //private static final String LOG4JCONFIGFILEPATH = "l";
    private OptionParser parser; //command line parser
    private OptionSet options; //list of recognizable ( configured )options found on the command line.
    
   
    
    public  AutosysEmail( String[] args ) {
    	
    	
    }
    
    public boolean initialize(String[] args) {
        if ( initializeLogging() == false ) {
            logger.error("Failed to initializeLogging ");
            return false;
        }
        else if ( initializeLocals() == false ) {
            logger.error("Failed to initializeLocals");
            return false;
        }
        else if ( initializeCLI( args ) == false ) {
            logger.error("Failed to initializeOptions");
            return false;
        }
        else if ( initializeConfig( args ) == false ) {
            logger.error("Failed to initializeConfig");
            return false;
        }
        else {
            logger.info("Initialization Completed Successfully.");
            return true;
        }
    }

    private boolean initializeLocals() {
        return true;
    }

    private boolean initializeLogging() {
        logger = org.apache.log4j.Logger.getLogger("AutosysEmail");
    	SimpleLayout layout = new SimpleLayout();
        logger.addAppender( new ConsoleAppender(layout)  );
        logger.setLevel( defaultLoggingLevel  );
        logger.info( String.format( "Basic Logging successfully intialized with logging level %s", logger.getLevel().toString() ) );
        return true;
    }

    private boolean initializeCLI( String[] args ) {
        parser = new OptionParser() {
            {
                accepts( AUTHFILEPATH , "(optional)Path to the Authentication file that holds username/password.").withRequiredArg().describedAs("Authentication File.").ofType(String.class);
                accepts( CONFIGFILEPATH, "Config File Path" ).withRequiredArg().describedAs( "configFilePath" ).ofType( String.class );
                accepts( HELP, "Request a print out of help.");
                accepts( VARIABLES, "List of variables to use in the messages.").withRequiredArg().describedAs("variable list").ofType(String.class)   ;
                accepts( LOGGINGLEVEL, "Minimum level of event to log."  ).withRequiredArg().describedAs( "Logging Level.").ofType(String.class )  ;
            }
        };
        options = parser.parse( args );
        logger.info( String.format("Options initialized successfully." ) );
        return true;

    }

        //TODO Break out the exception handling to make is easier to id the source of problems
    private boolean initializeConfig(String[] pArgs ) {
    	
    	//Check for problems with the CLI parameters
    	//check if the user just wants to print out the help data
    	if ( options.has(HELP)) {
        	printHelp();
        	return true;
        }
    	else if ( options.has(CONFIGFILEPATH) == false ){
        	printHelp();
        	return false;
        } 


    	
    	if ( options.has( LOGGINGLEVEL) == true ) {
    		logger.info( "Logging level switch detected. Changing the logging level. " ); 
    		logger.setLevel(Level.toLevel( (String ) options.valueOf(LOGGINGLEVEL)));
    		logger.info( String.format( "Logging level set to %s." , logger.getLevel().toString() ) ); 
    		
    	}
    	
    	//load values from the command line
        authConfigFilePath = (String ) options.valueOf(  AUTHFILEPATH );
        if ( options.has(VARIABLES)) {
        	variables = (String ) options.valueOf(VARIABLES);
        	if ( variables == null )  {
        		logger.error( String.format( "No parameter given for variables.","" ) );
        		printHelp();
        		return false;
        	}
        	else if ( variables.length() == 0 ) {
        		logger.error( String.format( "No parameter given for variables.","" ) );
        		printHelp();
        		return false;
        	}
        	else if ( parseVariables(  variables  )  == false ) {
                logger.error( String.format( "Failed to parse email variables", "" ) );
                return false;
            }
        	
        }


    	Properties props = new Properties();
    	logger.debug("Loading config file " +  options.argumentOf( CONFIGFILEPATH ) ) ;
        File configFile = new File( (String ) options.argumentOf( CONFIGFILEPATH )  );
        if ( configFile.exists() == false ) {
            logger.error( String.format("Config file %s does not exist.", options.argumentOf( CONFIGFILEPATH ) ) );
            return false;
        }
        else {
            try {
                    props.loadFromXML( new FileInputStream( configFile ) );
                    logger.debug( String.format( "Configuration successfully loaded from configFile %s.", configFile )  );
            } 
            catch (FileNotFoundException e) {
                    logger.error( String.format("Configuration initialization failed. %s", e.getMessage()));
                    return false;
            }
            catch (IOException e ) {
                    logger.error( String.format("Configuration initialization failed. IOException: %s", e.getMessage() ) );
                    return false;
            }
            catch ( Exception e) {
                    logger.error( String.format("Configuration initialization failed. Exception: %s", e.getMessage() ) );
                    return false;
            }

            try {
//					loop through config file properties
                    Enumeration<?> e = props.propertyNames();
                    while ( e.hasMoreElements()) {
                            String elem = ( String )e.nextElement();
                            logger.debug( elem + "=" + props.getProperty(elem)) ;
                    }

                    if ( ( email_from = props.getProperty("email_from") ) == null ) {
                            logger.error("email_from is null");
                            return false;
                    }
                    else if ( (email_to = props.getProperty("email_to" ) ) == null  )  {
                            logger.error("email_to is null");
                            return false;
                    }
                    else if ( ( email_subject = props.getProperty("email_subject" )  )  == null  )  {
                            logger.error("email_subject is null");
                            return false;
                    }
                    else if ( ( email_body = props.getProperty( "email_body" )  )  == null  )  {
                            logger.error("email_body is null");
                            return false;
                    }
                    email_auth = Boolean.valueOf( (String ) props.getProperty(  "email_auth" , "false" ) );
                    if ( email_auth == true  ) {
                    	//check if the authentication file was specified
                        if ( authConfigFilePath != null ) {
                            if ( loadAuthData( authConfigFilePath ) == false  ) {
                                logger.error( String.format( "Failed to load credentials from Auth Config file %s.", authConfigFilePath  ));
                                return false;
                            }
                        }
                        //if there was not an authentication configuration file, try to load credentials from config file
                        else {
                        	
                            if ( ( email_user = (String )  props.getProperty("user") )  == null ) {
                                logger.error( String.format( "Failed to load Email user from application config file %s.", configFile.getPath()  ));
                                return false;

                            }
                            else if ( ( email_pwd = (String )  props.getProperty("password") )  == null ) {
                                logger.error( String.format( "Failed to load Email user from application config file %s.", configFile.getPath()  ));
                                return false;

                            }
                            if ( ( email_host = (String )  props.getProperty("host") )  == null ) {
                                logger.error( String.format( "Failed to load Email user from application config file %s.", configFile.getPath()  ));
                                return false;
                            }
                        }
                    }
                    else {
                		if( ( email_host = (String ) props.getProperty( "host", null ) ) == null ) {
                			logger.error( String.format( "host tag required to send emails. Please check configuration file.", "" ) );
                			return false;
                		}	
                		else if ( email_host.length() == 0 )  {
                			logger.error( String.format( "email_host not specified. Please check configuration file.", "" ) );
                			return false;
                		}
                    	
                    }
                    email_port = Integer.parseInt( props.getProperty("email_port", "25" ) );
                    email_cc = props.getProperty( "email_cc", null );
                    email_debug = Boolean.valueOf( props.getProperty("email_debug", "false")   );
                    email_attachment = props.getProperty("email_attachment", null );

                    logger.debug("Configuration initialized successfully." );
                    return true;  
            }
            catch (Exception e) {
                logger.error( String.format("Configuration initialization failed. %s",  e.getMessage() ) ) ;
                return false;
            }
        }
    }

    //loads username and password from authentication configuration file
    private boolean loadAuthData(String authConfigFilePath) {
        Properties aprops = new Properties();
        try {
            aprops.loadFromXML(new FileInputStream(authConfigFilePath));
        }
        catch (InvalidPropertiesFormatException ex) {
            logger.error( String.format( "InvalidPropertiesFormatException encountered while loading AuthData properties file %s. Reason: %s.", authConfigFilePath, ex.getMessage()    )  );
            return false;
        }
        catch (IOException ex) {
            logger.error( String.format( "IOException encountered while loading AuthData properties file %s. Reason: %s.", authConfigFilePath, ex.getMessage()    )  );
            return false;
        }

        if ( aprops.containsKey( "password" ) == false )  {
            logger.error( String.format( "Email PWD value  missing from Auth Config file %s", authConfigFilePath ));
            return false;
        }
        else if ( aprops.containsKey("user") == false )  {
            logger.error( String.format( "Email user value  missing from Auth Config file %s", authConfigFilePath ));
            return false;
        }
        else if ( aprops.containsKey("host") == false )  {
            logger.error( String.format( "Email user value  missing from Auth Config file %s", authConfigFilePath ));
            return false;
        }
        
        email_host = aprops.getProperty("host");
        email_user =  aprops.getProperty("user");
        email_pwd =  aprops.getProperty("password");
        //print out options
        
        
        
        return true;
   }

    //parses the -v argument to extract key/value pairs and stores them in a hashmap
    private boolean parseVariables( String vars ) {
        //initialize the variables HashMap
        hmVars = new HashMap<String,String>();
        String[] arVars = vars.split(";");
        for( String pair: arVars ) {
            String[] parts = pair.split("=");
            if ( parts.length != 2 ) {
                logger.error( String.format( "Data format error parsing the variables. source string = %s", pair ) ) ;
                return false;
            }
            else {
                hmVars.put( parts[0].trim(), parts[1].trim() ); //add the keypair to the HashMap
            }
        }
        return true;
    }
    
    public boolean run()  {

    	//hack to avoid running the email code if the user just requested a printout of help
    	if ( options.has(HELP)) {
        	return true;
        }
    	
    	Properties props = System.getProperties();
        props.put("mail.smtp.host", email_host);
        props.put("mail.smtp.debug", email_debug.toString() );
        props.put("mail.smtp.auth", email_auth.toString() );
    	if ( email_auth == true ) {
	        props.put("mail.smtp.username", email_user );
	        props.put("mail.smtp.password", email_pwd );
    	}
        props.put( "mail.smtp.port" , email_port  );
        logger.debug( "Ready to connncect to Mail server");
        
        try {

        	Authenticator signon = null;
        	Session session = null;
        	if ( email_auth == true ) {
        		signon = new Authenticator() {
	                public PasswordAuthentication getPasswordAuthentication() {
	                    return new PasswordAuthentication( email_user, email_pwd );
	                }
        		};
        		session = Session.getDefaultInstance(props, signon ); 
        	}
        	else {
        		session = Session.getDefaultInstance(props );
        		
        	}
            session.setDebug(true);

            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress( email_from ));
            if ( ( variables == null ) == false ) {
            	email_body = substituteVars(email_body);
				email_subject = substituteVars(email_subject);
			}
			msg.setSubject( email_subject );
            
 
			if ( email_attachment != null ) {
				logger.debug(String.format( "Attachment option selected. Getting attachment %s", email_attachment   )); 


				//Create the message text
				logger.debug(String.format( "Attaching message body part.", ""  )); 
				MimeBodyPart messagePart = new MimeBodyPart();
				messagePart.setText(email_body);

				
				//Create the file attachment part
				logger.debug(String.format( "Attaching file part.", ""  )); 
				MimeBodyPart attachmentPart = null;
				attachmentPart = new MimeBodyPart();
	            FileDataSource fileDataSource = new FileDataSource(email_attachment) {
	                public String getContentType() {
	                    return "application/octet-stream";
	                }
	           };
           
	           attachmentPart.setDataHandler(new DataHandler(fileDataSource));
	           attachmentPart.setFileName( new File( email_attachment).getName() );
	           
	           Multipart multipart = new MimeMultipart();
	           multipart.addBodyPart(messagePart);
        	   logger.debug( String.format(  "Attaching file %s to email." , email_attachment ));
               multipart.addBodyPart(attachmentPart);
               msg.setContent(multipart);
			}    
			else {
				logger.debug(String.format( "Attachment variable %s is null. Sending single-part message.", email_attachment   )); 
				msg.setText(email_body);
			}
			
           InternetAddress[] to_address =  InternetAddress.parse(email_to,false);
           msg.addRecipients(Message.RecipientType.TO, to_address );
            
           if (  email_cc != null  ) {
            InternetAddress[] cc_address =  InternetAddress.parse(email_cc,false);
            msg.addRecipients(Message.RecipientType.CC, cc_address );
           }	
            Transport tr = session.getTransport("smtp");

            tr.connect( email_host, email_user, email_pwd);
            msg.saveChanges(); // don't forget this
            tr.sendMessage(msg, msg.getAllRecipients());
            tr.close();
            return true;
        } catch (AddressException e) {
            logger.error(  String.format( "AddressException while sending email. Reason: ", e.getMessage()  ) );
            e.printStackTrace();
            return false;
		} catch (MessagingException e) {
            logger.error(  String.format( "MessagingException while sending email. Reason: %s", e.getMessage()  ) );
            e.printStackTrace();
            return false;
		}
        catch (Exception e) {
            logger.error(  String.format( "General Exception while sending email. Reason: ", e.getMessage()  ) );
            e.printStackTrace();
            return false;
        }
    }

    //Replace give text with values from variables
    private String  substituteVars(String text ) {
    	logger.debug(String.format( "Text Before Substitution: %s", text ));
        for ( String key:  hmVars.keySet().toArray( new String[0]) ) {
        	logger.debug(String.format( "Looking for variable $$%s",key   )) ;
        	if ( text.contains( "$$" +  key)  )  {
                text = text.replace( "$$" +  key, hmVars.get(key)   );
            }
        }
    	logger.debug(String.format( "Text After Substitution: %s", text ));
        return text;
    }
    
//TODO Remove Deprecated item after 4/27/09
/*
	private class SMTPAuthenticator extends javax.mail.Authenticator {
        public PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(email_from, email_pwd);
        }
    }
*/
	
	private boolean printHelp() {
		try {
			parser.printHelpOn(System.out);
			return true;
		} catch (IOException ex) {
			logger.error( String.format( "Error printing out help information. Reason. %s", ex.getMessage()  ) );
			return false;
		}

	}
    
}
