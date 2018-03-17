package com.gearsofgeek.autosys.services;


//import core java packages
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Enumeration;
import java.util.InvalidPropertiesFormatException;
import java.util.Iterator;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

//import org.apache.log4j.ConsoleAppender;
//import org.apache.log4j.Level;
import org.apache.log4j.*;

import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FTPFile;
import com.enterprisedt.net.ftp.FileTransferClient;
import com.sshtools.j2ssh.SftpClient;
import com.sshtools.j2ssh.SshClient;
import com.sshtools.j2ssh.authentication.AuthenticationProtocolState;
import com.sshtools.j2ssh.authentication.PasswordAuthenticationClient;
import com.sshtools.j2ssh.sftp.FileAttributes;
import com.sshtools.j2ssh.transport.IgnoreHostKeyVerification;

public class Autosys_Ftp {

    //configuration items
    private	String host; //host name or IP address of the remote server
    private	String user; //user name to log into the remote server
    private	String password; //password to log into the remote server
    private	String path; //working directory for the remote server
    private	String fsUser; //local resource user name
    private	String fsPassword; //local resource password
    private	String fsPath; //local working directory
    private Pattern includeMask; //regular expression for files to be included in copy operation
    private Pattern excludeMask;//regular expression for files to be excluded from the copy operation
    private int port; //TCP port for remote connection 
//    private	SshClient sftp; //SFTP client
    private FileTransferClient ftp; //FTP Client
    //private org.apache.commons.net.ftp.FTPClient ftp;
    
    private boolean secure; //whether the connection is secure or not ( true/false )
    private boolean overwrite; //whether to overwrite existing files or not
    private actions action; //upload or download
//    private	int returnCode = 0; //exit code to return to the Operating System

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger("Autosys_Ftp");//logger to manager all logging needs :)
//    private String log4jConfig; //path to the log44j configuration file


    private OptionParser parser; //command line parser
    private OptionSet options; //list of recognizable ( configured )options found on the command line.

    //File name filters to control which files are processed 
    private FileFilter includeFilter; 

    //define Command Line Arguments using string constants
    private String[] pArgs;
    
    private static final String USER = "u"; //ftp username
    private static final String PASSWORD = "p"; //ftp  password
    private static final String PATH = "path"; // ftp path
    private static final String FS_USER = "fsuser"; // local user
    private static final String FS_PASSWORD = "fspassword"; //local password
    private static final String FS_PATH = "fspath"; //local path   
    private static final String PORT = "port"; //ftp port
    private static final String ACTION = "z"; //action to be completed
    private static final String OVERWRITE = "o"; //overwrite local/remote files
    private static final String INCLUDE_MASK = "inc"; //incoming file name inclusion mask
    private static final String EXCLUDE_MASK = "exc"; //incoming file name exlusion mask
    private static final String HOST = "host"; //ftp hostname or Ip address
    private static enum actions { UPLOAD, DOWNLOAD, UNKNOWN, FILEWATCH, DELETE };
    private static final String SECURE = "s"; //secure file transfer

    private static final String CONFIG_FILE_PATH = "c";  //path to the application config file
//    private static final String LOG4J = "l";
    private static final String LOGGINGLEVEL = "l";
    private static final Level  defaultLoggingLevel = Level.INFO;
//    private Level loggingLevel;
    private static final String HELP = "h";

    
    
    
    //encryption/decryption variables
    private static int keyLength = 256;
    private String defaultKey;
    private static final String NEWKEY = "nk";
    private static final String DECRYPT = "dc";
    private static final String ENCRYPT = "ec";

    //Watch File process variables
    private Boolean watchfileFound;//sentinel that indicates if the file was found or not
    private long watchfileStart;//start time in milliseconds
    private long watchfileNow; //current time in milli seconds
    private double watchfileDuration; //duration time in minutes
    private int watchfileInterval; //interval between checks in seconds
    private long watchfileTimeout; //timeout in minutes
    DecimalFormat df = new DecimalFormat("#.##");


    //Credentials path option
    private static final String AUTH_FILE_PATH = "a"; 
    
    
    //statistics variables
    private static int filesProcessedCount = 0;
    
	
    //Create the ftp object
    public Autosys_Ftp( String[] args) {
    	pArgs = args;
    }

    //Initialize the object
    public boolean initialize( String[] args) {
        pArgs = args;

    	if ( initLogging() == false ) {
				System.out.println( String.format( "Failed to initialize the logging facility.", "" ));
                return false;
            }
            else if  ( initSecurity() == false) {
        		logger.error(String.format( "Failed to initialize security layer.", ""  ));
                return false;
            }
            else if ( initLocals() == false ) {
            	logger.error(String.format( "Failed to initialize local variables."  ));
                return false;
            }
            else if ( initCLI() == false ) {
    			logger.error(String.format( "Failed to initialize command line interface.", ""  ));
                return false;
            }
            else if ( ( options.has(NEWKEY) ) || ( options.has( ENCRYPT) ) || options.has( DECRYPT ) || options.has( HELP)  ) {
        		logger.info(String.format( "Initialization for security operation completed successfully.", ""  ));
                return true;
               
            }
            else if (  initConfig() == false ) {
    			logger.error(String.format( "Failed to initialize application configuration.", ""  ));
                    return false;
            }
            else if ( initFilters() == false ) {
    			logger.error(String.format( "Failed to initialize application filters ", ""  ));
                    return false;
            }
            else if ( decryptPasswords() == false ){
    			logger.error(String.format( "Failed to decrypt passwords.", ""  ));
                    return false;
            }
            else {
                return true;
            }
    }

    private boolean beginDecrypt() {
        String decryptedString = null;
        // prompt user for encryption key
        System.out.print("Enter decryption key: ");

        //  open up standard input
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        String dKey = null;

        //  read the username from the command-line; need to use try/catch with the
        //  readLine() method
        try {
             dKey = br.readLine();
        }
        catch (IOException ioe) {
            logger.error( String.format( "IO error trying to the key. Error: %s",  ioe.getMessage()) );
            return false;
        }
        if ( dKey == null || dKey.length() == 0 ) {
            dKey = defaultKey;
        }
        System.out.print("Enter the text to decrypt.");
        String dText = null;

        //  read the username from the command-line; need to use try/catch with the
        //  readLine() method
        try {
            dText = br.readLine();
        } 
        catch (IOException ioe) {
            logger.error( String.format("IO error trying to the key. Error %s", ioe.getMessage() ) ) ;
            return false;
        }

        try {
            decryptedString = new String( decryptString(dKey, dText) );
        }
        catch ( Exception e ) {
            logger.error( String.format( "Failed to decrypt password. Not in hex format? Value=%s",  dText ));
            return false;
            
        }
        System.out.println( "The dencrypted text is " + decryptedString ) ;
        return true;
    }

    private boolean beginEncrypt() {

        // prompt user for encryption key
        System.out.print("Enter encryption key: ");

        //  open up standard input
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        String eKey = null;

        //  read the username from the command-line; need to use try/catch with the
        //  readLine() method
        try {
         eKey = br.readLine();
        }
        catch (IOException ioe) {
         logger.error( String.format("IO error trying to the key. Error: %s", ioe.getMessage()) );
         return false;
        }
        
        if( eKey == null || eKey.length() == 0) {
            eKey = defaultKey;
        }
        
        
        System.out.print("Enter the text to encrypt.");
        String eText = null;

        //  read the username from the command-line; need to use try/catch with the
        //  readLine() method
        try {
            eText = br.readLine();
        }
        catch (IOException ioe) {
         logger.error( String.format( "IO error trying to the key. Error: %s" ), ioe   );
         System.out.println("IO error trying to the key.");
         return false;
        }
        
        String encryptedText = asHex( encryptString(eKey, eText) );
        System.out.println( String.format( "The encrypted text is %s", encryptedText ) );
        return true;
    }

//        decrypt passwords
    private boolean decryptPasswords() {
    		if ( options.has( HELP ) == true ) {
    			return true;
    		}
            if ( password != null && password.length() > 0 ) {
                    byte[] passwordBytes = decryptString( defaultKey, password  );
                    password = new String( passwordBytes );
//                    logger.debug( String.format("Username: %s Decrypted Password is %s", user, password  ));
            }

            if ( fsPassword != null && fsPassword.length() > 0 ) {
                    byte[] passwordBytes = decryptString( defaultKey, password  );
                    fsPassword = new String( passwordBytes );
            }
            return true;
    }

    //Initialize file name filters.
    private boolean initFilters() {
        includeFilter = new FileFilter(){
                Matcher matcher;
                public boolean accept(File file) {
                        //see if the file should be included
                        matcher = includeMask.matcher( file.getName());
                        logger.debug("Include Mask is  " + includeMask.pattern() );
                        if ( matcher.find()) {
                                logger.debug(String.format("File %s matched include mask %s", file.getName(), includeMask.pattern() ));
                                return true;
                        }else {
                                logger.debug(String.format("File %s did not match include mask %s", file.getName(), includeMask.pattern() ));
                                return false;

                        }
                }
        };
        return true;
    }

    //TODO Break out the exception handling to make is easier to id the source of problems
    private boolean initConfig() {
//    	if the user request a help print out, then skip configuring the application
    	if( options.has(HELP )==true ) {
    		return true;
    	}

    	
//    	set the logging level
    	if ( options.has( LOGGINGLEVEL  ) == false  ) {
			logger.setLevel(defaultLoggingLevel);
	    }
	    else {
    		logger.setLevel( Level.toLevel( ( String ) options.valueOf(LOGGINGLEVEL) ) );  
	    }
	    	
	    Properties props = new Properties();
	    if( pArgs.length == 0 ) {
	            try {
	                parser.printHelpOn(System.out);
	            } catch (IOException ex) {
	                logger.error( String.format( "Failed to print out help message. Reason: %s", ex.getMessage() ) ) ;
	                return false;
	            }
	
	            logger.error( String.format("Configuration initialization failed. Parameter count=%d", pArgs.length));
	            return false;
	    }
	    else if (options.has( CONFIG_FILE_PATH  ) ) { 
	        logger.debug("Loading config file " +  options.valueOf( CONFIG_FILE_PATH ) ) ;
	        File configFile = new File( (String )options.valueOf( CONFIG_FILE_PATH )  );
	        File configDir = configFile.getParentFile();
	        logger.debug( String.format( "Files in the directory %s", configDir.getPath() ) );
	        for( String filename : configDir.list() ) {
	        	logger.debug( String.format( "Filename: %s",  filename )   );
	        }
	        
	        logger.info( String.format( "Successfully created object for config file  %s", configFile.getPath() ));
	        if ( ( configFile.exists() ) == false ) {
	                logger.error( String.format("Config file %s does not exist.", configFile.getPath() ) );
	                return false;
	        }
	        else {
	                try {
	                        props.loadFromXML( new FileInputStream( configFile ) );
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
	
//	                try {
	//					loop through config file properties
                        Enumeration<?> e = props.propertyNames();
                        while ( e.hasMoreElements()) {
                                String elem = ( String )e.nextElement();
                                if ( elem.equalsIgnoreCase("password") == false ) {
                                logger.debug( elem + "=" + props.getProperty(elem)) ;
                                }
                        }

	
                        if ( options.has(AUTH_FILE_PATH)  ){ 
                        	logger.info( String.format( "Loading Authentication File %s", options.valueOf( AUTH_FILE_PATH  )));
                        	File authFile = new File( (String ) options.valueOf( AUTH_FILE_PATH ) );
                        	logger.debug( String.format( "Checking the existence of %s" , authFile.getPath()) );
                        		
                        	Properties authProps = new Properties();
 
                        	try {
								authProps.loadFromXML(  new FileInputStream( new File( (String)  options.valueOf( AUTH_FILE_PATH  ) ) ) ) ;
							} catch (InvalidPropertiesFormatException e1) {
								// TODO Auto-generated catch block
								logger.error(String.format("InvalidPropertiesFormatException encountered while opening auth file. Reason: %s", e1.getMessage()));
								e1.printStackTrace();
								return false;
							} catch (FileNotFoundException e1) {
								// TODO Auto-generated catch block
								logger.error(String.format("FileNotFoundException encountered while opening auth file. Reason: %s", e1.getMessage() ));
								e1.printStackTrace();
								return false;
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								logger.error(String.format("IOException encountered while opening auth file. Reason: %s", e1.getMessage()));
								e1.printStackTrace();
								return false;
							}
                        	
                        	if (authProps.size() == 0 ) {
                        		logger.debug( String.format( "Failed to load authorization file %s. Exiting",  (String)  options.valueOf( AUTH_FILE_PATH  ) ) );
                            	return false;
                        	}
                        	else {
                        		logger.debug( String.format( "Successfully loaded authorization file. %s", (String)  options.valueOf( AUTH_FILE_PATH  ) ) );
                        		
                        	}
//                        	Load the host name.
                        	if ( ( host = authProps.getProperty("host") ) == null ) {
	                            logger.error("Host is null");
	                            return false;
	                        }
                        	else {
                        		logger.debug(String.format("Hostname %s loaded from authentication file.", host ) );
                        		
                        	}
                        	
//                        	Load the user name from the authentication file.
	                        if ( (user = authProps.getProperty("user" ) ) == null  )  {
	                                logger.error("ftpUser is null");
	                                return false;
	                        }
	                        else {
	                     		logger.debug(String.format("Username %s successfully loaded from authentication file.", user ) );
	                        }
	                        
//	                        load the password 
	                        if ( ( password = authProps.getProperty("password" ) ) == null  )  {
	                                logger.error("ftpPassword is null");
	                                return false;
	                        }
	                        else {
	                     		logger.debug(String.format("Password successfully loaded from authentication file.", "" ) );
	                        }
                        }
                        else {
//                        	TODO remove this section as authentication file is now required.
                        	/*
                        	if ( ( host = props.getProperty("host") ) == null ) {
	                                logger.error("Host is null");
	                                return false;
	                        }
	                        else if ( (user = props.getProperty("user" ) ) == null  )  {
	                                logger.error("ftpUser is null");
	                                return false;
	                        }
	                        else if ( ( password = props.getProperty("password" ) ) == null  )  {
	                                logger.error("ftpPassword is null");
	                                return false;
	                        }*/
                        	logger.error( String.format( "Authentication file is mandatory but not supplied.", "" ));
                        	return false;
                        }
                        
                        
                        if ( ( path = props.getProperty("path" )  )  == null  )  {
                                logger.error("ftpPath is null");
                                return false;
                        }
                        else if ( ( props.getProperty( "action" )  )  == null  )  {
                                logger.error("action is null");
                                return false;
                        }
                        fsUser = props.getProperty("fsUser" );
                        fsPassword = props.getProperty("fsPassword" );
                        fsPath = props.getProperty("fsPath" );
                        includeMask = Pattern.compile( props.getProperty("includeMask", ".*") );
                        excludeMask = Pattern.compile( props.getProperty("excludeMask", "") );
                        secure = Boolean.valueOf( props.getProperty( "secure" , "false") );	
                        port = Integer.valueOf( props.getProperty("port", ( secure?"22":"21") ) );
                        overwrite = Boolean.valueOf(props.getProperty("overwrite", "false"));

                        if ( props.getProperty( "action" ).toLowerCase().equalsIgnoreCase("upload") ) {
                                action = actions.UPLOAD;
                        } 
                        else if ( props.getProperty( "action" ).toLowerCase().equalsIgnoreCase( "download") ){
                                action = actions.DOWNLOAD;
                        }
                        else if (  props.getProperty( "action" ).toLowerCase().equalsIgnoreCase( "filewatch") ) {
                            action = actions.FILEWATCH;

                            if ( props.getProperty( "interval" ) == null ) {
                                logger.error( "action is WatchFile but Interval is not set."  );
                                return false;
                            }
                            else {
                                watchfileInterval = Integer.parseInt(  props.getProperty( "interval" )   );
                            }
                            
                            if ( props.getProperty( "timeout" ) == null   ) {
                                logger.error( "action is WatchFile but Timeout is not set."  );
                                return false;
                            }
                            else {
                                watchfileTimeout = Integer.parseInt(  props.getProperty( "timeout" )   );
                            }
                        }
                        else if ( props.getProperty( "action" ).toLowerCase().equalsIgnoreCase( "delete") ){
                                action = actions.DELETE;
                        }

                        else {
                                logger.error( String.format("Action Property %s not valid.", props.getProperty( "action" ).toLowerCase()));
                                return false;
                        }

                        logger.debug("Configuration initialized successfully." );
                        return true;
                        
                        
//                }
//                catch (Exception e) {
//                        logger.error( String.format("Configuration initialization failed. Reason: %s", e.getMessage()));
//                        return false;
//                }
	        }
	    }
	    else {
	        try {
	            //check if required options exist
	            if ( options.has(  HOST ) == false ) {
	                return false;
	            }
	            else if ( options.has(  USER  ) == false ) {
	                return false;
	            }
	            else if ( options.has(  PASSWORD ) == false ) {
	                return false;
	            }
	            else if ( options.has(  PATH ) == false ) {
	                return false;
	            }
	            else if ( options.has(  ACTION ) == false ) {
	                return false;
	            }
	
	            host = ( String )options.valueOf(  HOST )  ;
	
	            user =  ( String )options.valueOf( USER ) ;
	
	            password = ( String )options.valueOf( PASSWORD );
	
	            path = ( String )options.valueOf( PATH);
	
	            fsUser = ( String ) options.valueOf("fsuser");
	
	            fsPassword = ( String ) options.valueOf("");
	
	            fsPath = ( String ) options.valueOf("fspath");
	
	            includeMask = Pattern.compile( ( String ) options.valueOf( INCLUDE_MASK ) );
	
	            excludeMask = Pattern.compile( ( String ) options.valueOf( EXCLUDE_MASK  ) );
	
	            port = (Integer ) options.valueOf( PORT ) ;
	
	            secure =  Boolean.valueOf( (String )options.valueOf( SECURE ) );	
	            overwrite = Boolean.valueOf( ( String ) options.valueOf( OVERWRITE ) );
	
	            if ( options.valueOf( ACTION  ) == "upload") {
	                    action = actions.UPLOAD;
	            }
	            else if ( options.valueOf( ACTION ) == "download"  ) {
	                    action = actions.DOWNLOAD;
	            }
	            logger.debug("Configuration initialized successfully." );
	            return true;
	        }
	        catch (Exception e) {
	                logger.error(String.format("Configuration initialization failed. %s", e.getMessage() ));
	                return false;
	        }
	    }
    }		

    //initialize local variables
    private boolean initLocals()	{
        logger.debug( String.format("Locals initialized successfully." ) );
        return true;

    }

    //initialize ( read in and parse ) the command line arguments.
    private boolean initCLI(){
        parser = new OptionParser() {
            {
                accepts( USER, "FTP Username" ).withRequiredArg().describedAs( "username" ).ofType( String.class );
                accepts( PASSWORD, "FTP Password"  ).withRequiredArg().describedAs( "password" ).ofType( String.class );
                accepts( PORT, "(s)FTP Port" ).withRequiredArg().describedAs( "port" ).ofType( String.class ); 
                accepts( PATH, "FTP Folder" ).withRequiredArg().describedAs( "path" ).ofType( String.class ); 
                accepts( FS_USER, "File System User" ).withRequiredArg().describedAs( "fsuser" ).ofType( String.class );
                accepts( FS_PASSWORD , "File System Password" ).withRequiredArg().describedAs( "fspassword" ).ofType( String.class );
                accepts( FS_PATH, "File System Working Directory" ).withRequiredArg().describedAs( "fspath" ).ofType( String.class ); 
                accepts( ACTION, "upload or download" ).withRequiredArg().describedAs("action").ofType(Boolean.class);
                accepts( OVERWRITE, "Overwrite existing files or not." );
                accepts( INCLUDE_MASK, "Pattern of file names to include in operation." ).withRequiredArg().describedAs("include").ofType( String.class );
                accepts( EXCLUDE_MASK, "Pattern of file names to exclude from operation." ).withRequiredArg().describedAs("exclude").ofType(String.class);
                accepts( CONFIG_FILE_PATH, "Path to the configuration file." ).withRequiredArg().describedAs("configFile").ofType( String.class);
                accepts( SECURE, "True or false. Where to use sftp or not." );
                accepts( HOST, "FQDN of the FTP host" ).withRequiredArg().describedAs("host name").ofType(String.class);
                accepts( LOGGINGLEVEL, "Log events at this level or higher." ).withRequiredArg().describedAs( "Minimum level of event to log."  ).ofType(String.class) ;
                accepts( NEWKEY, "Request for new encryption key."  );
                accepts( ENCRYPT, "Request to encrypt a word." );
                accepts( DECRYPT, "Request to decrypt a word.");
                accepts( HELP, "Request a print out of help.");
                accepts( AUTH_FILE_PATH, "Path to the authentication file." ).withRequiredArg().describedAs("Path to the authentication file").ofType(String.class);
            }
        };
        options = parser.parse( pArgs );
     
        
       logger.debug( String.format("CLI initialized successfully." ) );
        return true;
    }
	
	private boolean initSecurity() {
            defaultKey = "7a6adc66be417ed95be31b88a1331bfda5dad57b02b631942847b8791cc0f6fd";
            return true;
		
	}
	
	private boolean initLogging() {
            //logger.setLevel( org.apache.log4j.Level.DEBUG );
            SimpleLayout layout = new SimpleLayout();
            logger.addAppender( new ConsoleAppender(layout)  );
            logger.setLevel((Level) Level.INFO);
            logger.debug("Logger Initialized");
            return true;
        }

        //Beging the process of transferring file
        public boolean run() {
        	if( options.has(HELP) ) {
        		
        		return printHelp();
        	}

            //Perform a secure file transfer over ssh ( SFTP )
            if ( options.has( NEWKEY)) {

                System.out.println( "Your new key is " + asHex( ( requestKey() ) ) );
                return true;
            }
            else if( options.has(ENCRYPT)) {
                beginEncrypt();
                return true;
                //encryptString(key, user)
            }
            else if ( options.has( DECRYPT)) {
            //
                beginDecrypt();
                return true;
            }
            else if( options.has(HELP)) {
                try {
                    parser.printHelpOn(System.out);
                    return true;
                } catch (IOException ex) {
                    logger.error( String.format( "IOException while  printing out help. Error: %s", ex.getMessage() )  );
                    return false;
                }
            }
            else if ( secure == true ) {
                    return runSFTP();
            }
            //Perform an insecure file transfer ( FTP )
            else {
                    return runFTP();
            }
	}

	private boolean printHelp() {
			try {
				parser.printHelpOn(System.out);
				return true;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			
		}

	private boolean runFTP() {
            ftp = new FileTransferClient();
        try {
            ftp = new FileTransferClient();
            ftp.setRemoteHost(host);
            ftp.setUserName(user);
            ftp.setPassword(password);
		} 
            catch (FTPException e) {
			logger.debug( String.format("Failed to set FTP parameter. FTPException %s", e.getMessage() ));
			return false;
		}

		try {
//                    ftp.connect(host);
//                    ftp.login(user, password);
                    ftp.connect() ;
		} catch ( FTPException e) {
			logger.error( String.format( "FTP Connection failure. FTPException %s", e.getMessage()));
			return false;
		} catch (IOException e) {
			logger.error( String.format("FTP Connection failure. IOException %s", e.getMessage()));
			return false;
		}
		
        //set up the local directory
        File localDir = new File( fsPath );
        if ( localDir.exists() == false ) {
                logger.error(String.format("Local directory %s does not exist.", fsPath ));
                return false;
        }
        FTPFile[] list = null;//ftp.listFiles();  
        
        
        /*
        String[] arPath = path.split( "/" );
        try {
            for ( int i=0; i< arPath.length; i++) {
                if ( arPath[i].length() > 0 ) { 
                    logger.debug( String.format( "CD to %s", arPath[i]));
                     ftp.changeDirectory(arPath[i]  );
                }
            }
        } catch ( FTPException e) {
                logger.error( String.format( "FTPException when changing to remote directory %s.  Error: %S" ,  path,  e.getMessage()  ));
                return false;
        } catch (IOException e) {
                logger.error( String.format( "IOException when changing to remote directory %s.  Error: %S" ,  path,  e.getMessage()  ));
                return false;
        }
        */

        
        
        try {
            logger.debug( String.format( "CD to %s", path));
            ftp.changeDirectory( path  );
        } catch ( FTPException e) {
                logger.error( String.format( "FTPException when changing to remote directory %s.  Error: %S" ,  path,  e.getMessage()  ));
                return false;
        } catch (IOException e) {
                logger.error( String.format( "IOException when changing to remote directory %s.  Error: %S" ,  path,  e.getMessage()  ));
                return false;
        } catch ( Exception e ) {
            logger.error( String.format( "General Exception encountered when changing to directory %s. Error: %s", path, e.getMessage() ) ); 
        }
        
        
        //Make sure we made it to the correct directory
        try {
            String curDir = ftp.getRemoteDirectory();
            if ( curDir.equals( path ) == false ) {
                logger.error( String.format("Failed to change to directory %s, current directory is %s", path, curDir ));
                return false;
            }
            logger.debug( String.format("Current directory is %s", curDir ));
            
        }
        catch ( FTPException ex) {
            java.util.logging.Logger.getLogger(Autosys_Ftp.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        catch (IOException ex) {
            java.util.logging.Logger.getLogger(Autosys_Ftp.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } 
 		
		//Determine which operation to perform
		//check if action is download
		if ( action == actions.DOWNLOAD ) {
			//Declare local variables
			FTPFile[] files = null;
			try {
				files = ftp.directoryList();
			} catch (FTPException e) {
				logger.error(String.format("Directory listing failed. FTPException %s.", e.getMessage() ));
				return false;
			} catch (IOException e) {
				logger.error(String.format("Directory listing failed. IOException %s.", e.getMessage() ));
				return false;
			} catch (ParseException e) {
				logger.error(String.format("Directory listing failed. ParseException %s.", e.getMessage() ));
				return false;
			}
	
			for( int i = 0; i< files.length; i++ ) {
				FTPFile file =  files[i];
                                logger.debug(String.format( "Examining Remote file %s", file.getName() ));
                                if ( file.isDir() || file.isLink() ) {
                                    continue;
                                }
				if ( includeMask.matcher(file.getName() ).find() ) {
                                        logger.debug(String.format( "Match on remote file %s.", file.getName()  ));
					try {
                                            
						ftp.downloadFile( localDir.getAbsolutePath() +  File.separatorChar + file.getName() , file.getName() );
                                                logger.info( String.format( "File %s was successfully downloaded to %s",  file.getPath(),  localDir.getAbsolutePath() +  File.separatorChar + file.getName() ) );
						filesProcessedCount++;
					} catch (FTPException e) {
						logger.error( String.format( "File %s download failed. FTPException %s", file.getPath(), e.getMessage()) );
						return false;
					} catch (IOException e) {
						logger.error( String.format( "File %s download failed. IOException %s", file.getPath(), e.getMessage()) );
						return false;
					}
				}
			}
		}
		//check if action is upload
		else if ( action == actions.UPLOAD ) {
			//create list of files to upload
			File[] files = localDir.listFiles( includeFilter );
			for ( int i=0; i<files.length; i++) {
				try {
					ftp.uploadFile( files[i].getAbsolutePath(), files[i].getName() );
                                        logger.info( String.format( "File %s was successfully uploaded to %s",  localDir.getAbsolutePath() + "/" + files[i].getName(), files[i].getPath()   ) );
                                        filesProcessedCount++;

				} catch ( FTPException e) {
					logger.error( String.format( "File %s upload failed. FTPException %s", files[i].getPath(), e.getMessage()) );
					return false;
				} catch (IOException e) {
					logger.error( String.format( "File %s upload failed. IOException %s", files[i].getPath(), e.getMessage()) );
					return false;
				}
			}	
		}
		else {
			logger.error( String.format("Action type %s not understood. ", actions.UNKNOWN ) );
			return false;
		}
		
        // Shut down client
        try {
            ftp.disconnect();
            if ( filesProcessedCount == 0  ) {
                logger.info( "Zero files processed.");
            }
            return true;
        } 
        catch ( FTPException e) {
            logger.error( String.format( "FTP disconnect failed. FTPException %s", e.getMessage()) );
            return false;
        } 
        catch (IOException e) {
            logger.error( String.format( "FTP disconnect failed. IOPException %s", e.getMessage()) );
            return false;
        }
    }

	private boolean runSFTP() {
		SftpClient client;
                SshClient ssh;    
                try {
                    ssh = new SshClient() ;
                    //ssh.connect(host, new  com.sshtools.j2ssh.SshClient().   ConsoleKnownHostsKeyVerification()   );
                    ssh.connect( host, port, new IgnoreHostKeyVerification() );
                    
                    
                    //.connect(host, new  com.sshtools.j2ssh.SshClient().   ConsoleKnownHostsKeyVerification()   );

                    //ssh.connect(host, port, com.sshtools.j2ssh.transport.IgnoreHostKeyVerification );
                    //Authenticate
                    PasswordAuthenticationClient passwordAuthenticationClient = new PasswordAuthenticationClient();
                    passwordAuthenticationClient.setUsername(user);
                    passwordAuthenticationClient.setPassword(password);
                    int result = ssh.authenticate(passwordAuthenticationClient);
                    if(result != AuthenticationProtocolState.COMPLETE){
//			     throw new FTPException("Login to " + host + ":" + port + " " + user + "/" + password + " failed");
                            logger.error(  String.format("Failed to log into  %s as %s", host, user ) );
                            return false;
                    }
                    else {
                            logger.debug( String.format("SSH Login to %s successful." , host) );
                    }
                }
                catch ( ConnectException e) {
                    logger.error( String.format("ConnectException while logging in to host %s on port %d as user %s failed. Reason: %s", host, port, user, e.getMessage()  )  );
                    return false;
		}
		catch (IOException e) {
                    logger.error(String.format("IOException while logging in. Failed to connect to host %s on port %d as user %s. IOException %s",host, port, user, e.getMessage() ) );
                    return false;
		}

                //SSH Connection has been established
                //Create SFTP Connection
                try {
                    client = ssh.openSftpClient();
                    logger.debug( String.format("SFTP Connection to %s successful." , host) );
                                    
                }
                catch( Exception e) {
                    logger.error( String.format("Failed to open SFTP client connection. Error: %s", e.getMessage() ) );
                    return false;
                }
                //SFTP Connection Established

                //Open the SFTP channel
                    //check what type of action is requested
                    if ( action == actions.UPLOAD) {
                            logger.debug( "Upload action selected. ");
                            //Get a list of files to upload
                            File srcDir = new File( fsPath ); 
                            if ( srcDir.exists() == false ) {
                                    logger.error( String.format("fsPath %s does not exist.", fsPath));
                                    return false;

                            }
                            File[] srcFiles = srcDir.listFiles( includeFilter ); 
                            try {
                            //Switch to the destination directory
                                client.cd(path);
                            } catch (IOException ex) {
                                logger.error(String.format("IOException. Failed to Change Remote Directory to %s. Error: %s",path, ex.getMessage() )   );
                                return false;
                            }
                            
                            String remoteDir = client.pwd(); 
                            logger.debug(  String.format( "Successfully changed to the remote %s directory.", remoteDir ) );

                            //Send the files
                            for ( int i=0; i < srcFiles.length; i++){
                                    try {
                                        client.put( srcFiles[i].getAbsolutePath()  );
                                        logger.info(String.format("File %s was successfully uploaded to %s.", srcFiles[i].getAbsolutePath(), host));
                                        filesProcessedCount++;
                                    } 
                                    catch ( IOException e) {
                                        logger.error(String.format( "IOException Failed to upload file %s. Error: %s", srcFiles[i].getAbsolutePath(),  e.getMessage() ) ) ;
                                        //return false;
                                    }
                                    try {
                                        //verify the file was sent ok
                                        FileAttributes rf = client.stat(srcFiles[i].getName());
                                        if ( rf.getSize().longValue() != srcFiles[i].length() ) {
                                            logger.error(String.format("File size discrepancy: Local: %d Remote: %d",  srcFiles[i].length(), rf.getSize().longValue()  ));
                                            return false;
                                        }
                                        else {
                                            logger.info(String.format("File %s was successfully uploaded to %s. Local Size %d bytes Remote Size: %d bytes.", srcFiles[i].getAbsolutePath(), host, srcFiles[i].length(), rf.getSize().longValue()));
                                        }
                                    } catch (IOException ex) {
                                        logger.error(String.format( "IOException. Failed to get attributes for file %s. Error:", srcFiles[i].getName(), ex.getMessage()  ) );
                                        //return false;
                                    }
                            }
                            
                            try {
                                client.quit();
                            } catch ( IOException e) {
                                logger.error( String.format("Error disconnecting SFTP Client. IOException %s", e.getMessage() ));
                                return false;
                            }
                            ssh.disconnect();
                            return true;
                    }
                    else if ( action == actions.DOWNLOAD ){
                        com.sshtools.j2ssh.sftp.SftpFile file; //the remote file 
                        //local directory for the download destination
                        File dstDir = new File( fsPath );
                        if ( dstDir.exists() == false ) {
                                logger.error( String.format( "Specified download directory  %s does not exist.", dstDir.getAbsolutePath()) );
                                return false;

                        }
                        else {
                            java.util.List<com.sshtools.j2ssh.sftp.SftpFile>  files = null;
                            logger.debug( "Download action selected.");
                            try {
                                client.cd(path);
                            } 
                            catch (IOException ex) {
                                logger.error( String.format( "IOException. Failed to cd to remote directory %s. Error: %s", path, ex.getMessage()  ));
                                return false;
                            }



                            try {
                                files = client.ls(path);
                            } catch (IOException ex) {
                                logger.error(String.format( "IOException while listing files in remote directory %s. Error: %s", path, ex.getMessage()));
                                return false;
                            }


                            for (Iterator<com.sshtools.j2ssh.sftp.SftpFile> it = files.iterator(); it.hasNext(); ) {
                                file = it.next();
                                logger.debug( String.format( "Processing remote file %s ", file.getFilename() ) );
                                if ( file.isDirectory() == true) {
                                    logger.debug( String.format( "Remote file %s is directory.", file.getFilename() ) );
                                    //it.remove();
                                    continue;
                                }
                                else if ( includeMask.matcher( file.getFilename() ).find() == false ) {
                                    logger.debug( String.format("File %s did not match %s ", file.getFilename(), includeMask.pattern() ) );
//                                    it.remove();
                                    continue;
                                }
                                else {
                                    logger.debug( String.format("File %s matched %s ", file.getFilename(), includeMask.pattern() ) );
                                    try {
                                            String dstFilePath = dstDir.getAbsolutePath() + File.separator + file.getFilename();
                                            client.get(  file.getAbsolutePath(),  dstFilePath  );
                                            logger.info(  String.format( "File %s successfully downloaded to %s.", file.getAbsolutePath(), dstFilePath  ) );
                                            filesProcessedCount++;  

                                    }
                                    catch ( IOException e) {
                                            logger.error( String.format("IOException. Failed to download remote file %s. Error: %s ", file.getAbsolutePath(), e.getMessage()   ) );
                                            return false;
                                    }
                                }
                            }
                        }
                        try {
                            client.quit();
                        } catch ( IOException e) {
                            logger.error( String.format("Error disconnecting SFTP Client. IOException %s", e.getMessage() ));
                            return false;
                        }
                        try  {
                            ssh.disconnect();     
                        }
                        catch ( Exception e ) {
                            logger.error( String. format("Exception. Error disconnecting from SFTP server. Reason: %s" , e.getMessage() ) );
                            return false;
                        }
                        if ( filesProcessedCount == 0  ) {
                            logger.info( "No files processed.");
                        }

                        return true;
                        
                }
                else if ( action == actions.FILEWATCH   ) {
                        com.sshtools.j2ssh.sftp.SftpFile file; //the remote file 
                            java.util.List<com.sshtools.j2ssh.sftp.SftpFile>  files = null;
                            logger.debug( "File Watch action selected.");
                            try {
                                client.cd(path);
                            } 
                            catch (IOException ex) {
                                logger.error( String.format( "IOException. Failed to cd to remote directory %s. Error: %s", path, ex.getMessage()  ));
                                return false;
                            }

                            watchfileFound = false; //sentinel that indicates if the file was found or not
                            watchfileStart = System.currentTimeMillis(); //start time in milliseconds
                            watchfileNow = System.currentTimeMillis(); //current time in milli seconds
                            watchfileDuration = 0; //start time in seconds

                            //Enter the file watch loop
                            while ( ( watchfileFound == false ) && ( watchfileDuration < watchfileTimeout ) ) {
                                //Sleep this thread
                                try {
                                    Thread.sleep(watchfileInterval * 1000);
                                } catch (InterruptedException ex) {
                                   logger.error("InterruptedException Trying to wait for interval."); 
                                   return false;
                                }
                                //get a file listing
                                try {
                                    files = client.ls(path);
                                } catch (IOException ex) {
                                    logger.error(String.format( "IOException while listing files in remote directory %s. Error: %s", path, ex.getMessage()));
                                    //return false;
                                }

                                for (Iterator<com.sshtools.j2ssh.sftp.SftpFile> it = files.iterator(); it.hasNext(); ) {
                                    file = it.next();
                                    logger.debug( String.format( "Processing remote file %s ", file.getFilename() ) );
                                    if ( file.isDirectory() == true) {
                                        logger.debug( String.format( "Remote file %s is directory.", file.getFilename() ) );
                                        //it.remove();
                                        continue;
                                    }
                                    else if ( includeMask.matcher( file.getFilename() ).find() == false ) {
                                        logger.debug( String.format("File %s did not match %s ", file.getFilename(), includeMask.pattern() ) );
    //                                    it.remove();
                                        continue;
                                    }
                                    else {
                                        logger.debug( String.format("File %s matched %s ", file.getFilename(), includeMask.pattern() ) );
                                        logger.debug( String.format("File %s found!", file.getFilename(), includeMask.pattern() ) );
                                        watchfileFound = true;
                                        filesProcessedCount++;
                                        break;
                                    }
                                }
                                watchfileNow = System.currentTimeMillis();

                                watchfileDuration =  ( (  watchfileNow  - watchfileStart ) / 1000.00 / 60.00 );
                                logger.debug( String.format( "WatchFile duration is %s minutes", df.format(watchfileDuration) )  );

                        }
                        try {
                            client.quit();
                        } catch ( IOException e) {
                            logger.error( String.format("Error disconnecting SFTP Client. IOException %s", e.getMessage() ));
                            return false;
                        }
                        try  {
                            ssh.disconnect();     
                        }
                        catch ( Exception e ) {
                            logger.error( String. format("Exception. Error disconnecting from SFTP server. Reason: %s" , e.getMessage() ) );
                            return false;
                        }
                        if ( filesProcessedCount == 0  ) {
                            logger.info( "No files processed.");
                        }

                    if ( watchfileFound == true ) {

                        return true;
                    }
                    else {
                        return false;
                    }                        
                }
                else if ( action == actions.DELETE ) {
                    logger.debug( "Delete action selected.");
                    com.sshtools.j2ssh.sftp.SftpFile file; //the remote file
                    //local directory for the download destination
                    java.util.List<com.sshtools.j2ssh.sftp.SftpFile>  files = null;
                    try {
                        client.cd(path);
                    }
                    catch (IOException ex) {
                        logger.error( String.format( "IOException. Failed to cd to remote directory %s. Error: %s", path, ex.getMessage()  ));
                        return false;
                    }

                    try {
                        files = client.ls(path);
                    } catch (IOException ex) {
                        logger.error(String.format( "IOException while listing files in remote directory %s. Error: %s", path, ex.getMessage()));
                        return false;
                    }


                    for (Iterator<com.sshtools.j2ssh.sftp.SftpFile> it = files.iterator(); it.hasNext(); ) {
                        file = it.next();
                        logger.debug( String.format( "Processing remote file %s ", file.getFilename() ) );
                        if ( file.isDirectory() == true) {
                            logger.debug( String.format( "Remote file %s is directory.", file.getFilename() ) );
                            continue;
                        }
                        else if ( includeMask.matcher( file.getFilename() ).find() == false ) {
                            logger.debug( String.format("File %s did not match %s ", file.getFilename(), includeMask.pattern() ) );
                            continue;
                        }
                        else {
                            logger.debug( String.format("File %s matched %s ", file.getFilename(), includeMask.pattern() ) );
                            try {
//                                    String dstFilePath = dstDir.getAbsolutePath() + File.separator + file.getFilename();
                                    logger.info(  String.format( "Preparing to delete remote file %s.", file.getAbsolutePath()  ) );
                                    file.delete();
                                    logger.info(  String.format( "Remote File %s successfully deleted.", file.getAbsolutePath()  ) );
                                    filesProcessedCount++;

                            }
                            catch ( IOException e) {
                                    logger.error( String.format("IOException. Failed to delete remote file %s. Error: %s ", file.getAbsolutePath(), e.getMessage()   ) );
                                    return false;
                            }
                        }
                    }
                    try {
                        client.quit();
                    } catch ( IOException e) {
                        logger.error( String.format("Error disconnecting SFTP Client. IOException %s", e.getMessage() ));
                        return false;
                    }
                    try  {
                        ssh.disconnect();
                    }
                    catch ( Exception e ) {
                        logger.error( String. format("Exception. Error disconnecting from SFTP server. Reason: %s" , e.getMessage() ) );
                        return false;
                    }
                    if ( filesProcessedCount == 0  ) {
                        logger.info( "No files processed.");
                    }

                    return true;

                }
                else {
                    logger.error( String.format( "Selected Action %s is invalid. Please check your settings.", action.toString() ) );
                    return false;
                }
	}
	
    private byte[] decryptString( String key, String encryptedString ) {
        Cipher cipher = null;
        byte[] byteKey = new byte[key.length() / 2];

        for (int i = 0; i < byteKey.length; i++) {
            byteKey[i] = (byte) Integer.parseInt(key.substring(2*i, 2*i+2), 16);
        }
        
        byte[] encrypted = new byte[ encryptedString.length() / 2];

        for (int i = 0; i < encrypted.length; i++) {
            encrypted[i] = (byte) Integer.parseInt(encryptedString.substring(2*i, 2*i+2), 16);
        }
        SecretKeySpec skeySpec = new SecretKeySpec(byteKey, "AES");
        
        try {
            cipher = Cipher.getInstance("AES");
        }
        catch (NoSuchAlgorithmException ex) {
            logger.error( "decryptString - NoSuchAlgorithmException error encountered while creating cipher. Reason:" + ex );
            return null;
        
        }
        catch (NoSuchPaddingException ex) {
            logger.error( "decryptString - NoSuchPaddingException error encountered while creating cipher. Reason:" + ex );
            return null;
        }
        
        try {
            //System.out.println("Original string: " +  originalString + " " + asHex(original));
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
        }
        catch (InvalidKeyException ex) {
            logger.error( "decryptString - InvalidKeyException error encountered while initializing cipher. Reason:" + ex );
            return null;
        }
        
        byte[] original;
        
        try {
            original = cipher.doFinal(encrypted);
            return original;
        }
        catch (IllegalBlockSizeException ex) {
            logger.error( "decryptString - IllegalBlockSizeException error encountered while encrypting password. Reason:" + ex );
            return null;
        }
        catch (BadPaddingException ex) {
            logger.error( "decryptString - BadPaddingException error encountered while encrypting password. Reason:" + ex );
            return null;
        }
        
    }
    
        private byte[] encryptString( String key, String unencryptedString ) {
        Cipher cipher = null;
        byte[] byteKey = new byte[key.length() / 2];
        for (int i = 0; i < byteKey.length; i++) {
            byteKey[i] = (byte) Integer.parseInt(key.substring(2*i, 2*i+2), 16);
        }
        SecretKeySpec skeySpec = new SecretKeySpec(byteKey, "AES");
        try {
            // Instantiate the cipher
            cipher = Cipher.getInstance("AES");
        }
        catch (NoSuchAlgorithmException ex) {
            logger.error( String.format( "encryptString - NoSuchAlgorithmException Error encounter while creating cipher. Reason: %s", ex ) );
            return null;
        }
        catch (NoSuchPaddingException ex) {
            logger.error( String.format( "encryptString - NoSuchPaddingException Error encounter while creating cipher. Reason: %s", ex ) );
            return null;
        }
        
        try {
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
        }   
        catch (InvalidKeyException ex) {
            logger.error( String.format( "encryptString - InvalidKeyException Error encounter while initializing cipher. Reason: %s", ex ) );
            return null;
        }
        
        try {
            byte[] encrypted =  cipher.doFinal( unencryptedString.getBytes() );
            return encrypted;
        }
        catch (IllegalBlockSizeException ex) {
            logger.error( String.format( "encryptString - IllegalBlockSizeException Error encounter while encrypting. Reason: %s", ex ) );
            return null;
        }
        catch (BadPaddingException ex) {
            logger.error( String.format( "encryptString - BadPaddingException Error encounter while initializing encrypting. Reason:", ex ) );
            return null;
        }

    }   

    
    public byte[] requestKey() {
        byte[] encryptedKey = generateKey();
        return  encryptedKey;
    }
    //generate an encryption key
    private byte[] generateKey() {
        KeyGenerator kgen;

        try {
            kgen = KeyGenerator.getInstance("AES");
        } 
        catch (NoSuchAlgorithmException ex) {
            System.out.println( "NoSuchAlgorithmException error trying to encrypt password. Reason: %s" + ex.getMessage() );
            return null;
        }

        kgen.init( keyLength ); // 192 and 256 bits may not be available
            
        // Generate the secret key specs.
        SecretKey skey = kgen.generateKey();
        byte[] keyByte = skey.getEncoded();
        return keyByte;
    }
    
    
    private static String asHex (byte buf[]) {
        StringBuffer strbuf = new StringBuffer(buf.length * 2);
        int i;
        for (i = 0; i < buf.length; i++) {
            if (((int) buf[i] & 0xff) < 0x10) {
                strbuf.append("0");
            }
            strbuf.append(Long.toString((int) buf[i] & 0xff, 16));
        }
        return strbuf.toString();
    }
	
}
