/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.gearsofgeek.autosys.system;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.InvalidPropertiesFormatException;
import java.util.Iterator;
import java.util.Properties;
import java.util.Date;
import java.text.SimpleDateFormat;


import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;



/**
 *
 * @author jgooch
 */
public class Autosys_DateGenerator {
   
	//command line parameter members
	private static final String CONFIGFILEPATH = "c";
	private static final String LOGGINGLEVEL = "l";
	private static final String HELP = "h";
	
	
    //parsing section
    private OptionParser parser;
    private OptionSet options;
	
	//logging members
	private static Logger logger = Logger.getLogger("Autosys_DateGenerator");
	private Level loggingLevel;
	private static Level defaultLoggingLevel = Level.INFO;
	
	
	
	//configuration members
	private String configFilePath;
	Properties props; //holds the key/value pairs from the properties file
	
	
	
	//constructor
	public Autosys_DateGenerator( String[] args ) {
		logger.addAppender(new ConsoleAppender( new SimpleLayout()));//set up for basic logging
		logger.setLevel(defaultLoggingLevel);
	    logger.info( String.format( "Basic Logging successfully intitialized with logging level %s", logger.getLevel().toString() ) );
		logger.debug( String.format( "%s object created.", this.getClass() ));

	}
	
	
	public boolean initialize( String[] args) {
		logger.info( String.format( "Initializing Date Generator.", "") );
		if ( initLogging() == false ) {
			System.out.println( "Failed to initialize logging." );
			return false;
		}
		else if ( initCLI(args) == false ) {
			logger.error( "Failed to initialize the command line parameters." );
			return false;
		}
		else if ( initConfig() == false ) {
			logger.error( "Failed to initialize the application configuration." );
			return false;
		}
		else if ( chkEnvironment() == false ) {
			logger.error( "System failed Autosys environment check. Make sure Application is running in an Autosys shell." );
			return false;
		}
		else {
			logger.info( String.format( "Application Successfully initialized.", "") );
			return true;
		}
	}
	
	
    private boolean chkEnvironment() {
		String autoserv = null;
		String autoroot = null;
		String autosys = null;
		String autouser = null;
		
		if ( (  autoserv = System.getenv("AUTOSERV") ) == null ) {
			logger.error( "Environment variable AUTOSERV not set." );
			return false;
		}
		else if ( (  autoroot = System.getenv("AUTOROOT") ) == null ) {
			logger.error( "Environment variable AUTOROOT not set." );
			return false;
		}
		else if ( (  autosys = System.getenv("AUTOSYS") ) == null ) {
			logger.error( "Environment variable AUTOSYS not set." );
			return false;
		}
		else if ( (  autouser = System.getenv("AUTOUSER") ) == null ) {
			logger.error( "Environment variable AUTOUSER not set." );
			return false;
		}
		else {
			return true;
		}
	}


	private boolean initConfig() {
		if ( options.has( HELP ) == true) {
			return true;
		}
    	configFilePath = (String) options.valueOf(CONFIGFILEPATH );
    	if( new File( configFilePath ).exists() == false  ) {
    		logger.error( String.format("Configuration file %s does not exist.", configFilePath));
    		return false;
    		
    	}
    	props = new Properties();
    	//load properties from xml file
    	try {
			props.loadFromXML( new FileInputStream(configFilePath)  );
		} catch (InvalidPropertiesFormatException e) {
			logger.error( String.format( "FormatException encountered. Reason: %s",   e.getMessage() ) );
			return false;
		} catch (FileNotFoundException e) {
			logger.error( String.format( "FileNotFoundExceptionFileNotFoundException encountered. Reason: %s",   e.getMessage()  ) );
			return false;
		} catch (IOException e) {
			logger.error( String.format("IOExceptionIOException encountered. Reason: %s" ,e.getMessage() ) );
			return false;
		}
    	
		Enumeration<?> e = props.propertyNames();
		while ( e.hasMoreElements()  ) {
			String key = (String) e.nextElement();  
			logger.debug( String.format( "key %s=%s", key, props.getProperty(key) ) );
		}
		logger.info( String.format("Application Configuration Successfully Initialized.", ""));
		return true;
    }
		
	private boolean initCLI(String[] args) {
        parser = new OptionParser() {
            {
                accepts( CONFIGFILEPATH, "Application configuration file." ).withRequiredArg().describedAs( "Path to configuration file." ).ofType(String.class) ;
                accepts( LOGGINGLEVEL, "Application configuration file." ).withRequiredArg().describedAs( "Path to configuration file." ).ofType(String.class) ;
                accepts( HELP, "Prints out help text." );
            }
        };
        options = parser.parse( args );
        
        
        //Check if help is requested
        if ( options.has(HELP ) ) {
        	return true;
        }
        
        //Check for mandatory parameters
        if ( options.has( CONFIGFILEPATH) == false  ) {
        	logger.error( String.format( "Option %s required.", CONFIGFILEPATH  ) );
        	printHelp();
        	return false;
        }
        
        //check if a logging level was specified.
        if ( options.has( LOGGINGLEVEL  )  == true )  {
        	logger.setLevel(Level.toLevel(LOGGINGLEVEL) ); 
        	logger.info( String.format( "Logging level set to %s", logger.getLevel().toString() ));
        }
        
        logger.info( String.format( "Command line successfully initialized.", "" ) );
        return true;
 	}

	private boolean initLogging() {
        SimpleLayout layout = new SimpleLayout();
        logger.addAppender( new ConsoleAppender(layout)      );
        logger.setLevel(defaultLoggingLevel );
        logger.debug("Logger Successfully Initialized");
        return true;
	}

	public boolean run() {
		logger.debug( "Entering the Run method");
		//if help was requested, exit immediately as it has already been printed
		if ( options.has(HELP) == true  ) {
			return printHelp();
		}
		else {
			logger.debug(String.format( "Help Option Not Detected. Continuing Run method.","" ));
			//initialize the date value and formatter
			SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");
	        Date now = new Date();

			//set all of the specified date formats
			String[] command = null;
			command = new String[5];
			command[0] = "sendevent";
			command[1] = "-E";
			command[2] = "SET_GLOBAL";
			command[3] = "-G";
			Enumeration<?> e = props.keys();
			while ( e.hasMoreElements() ) {
				String key = ( String ) e.nextElement();
				df.applyPattern(   (String ) props.getProperty(key) );
				command[4] = String.format( "\"%s=%s\"", key,  df.format(now) ) ;
				logger.debug( String.format( "Preparing to execute the following commmand - %s", join( Arrays.asList(command)    , " "   ) ) );
				try {
					Process sendevent = Runtime.getRuntime().exec(command);
					BufferedReader Resultset = new BufferedReader( new InputStreamReader ( sendevent.getInputStream()));
					String line;
			        while ((line = Resultset.readLine()) != null) {
			                logger.info(line);
	                }
		        }
				catch (IOException e1) {
					logger.error( String.format( "IOExceptions Failure starting sendevent. Reason: %s", e1.getMessage()  ) );
					return false;
				}
				//Now print out the Global Variable value for logging purposes
				
				try {
					Process autorep = Runtime.getRuntime().exec(String.format( "autorep -G %s | find \"%s\"", key, key  ) );
					BufferedReader Resultset = new BufferedReader( new InputStreamReader ( autorep.getInputStream()));
					String line;
			        while ((line = Resultset.readLine()) != null) {
			                logger.info(line);
	                }
		        }
				catch (IOException e1) {
					logger.error( String.format( "IOExceptions Failure starting autorep. Reason: %s", e1.getMessage()  ) );
					return false;
				}
				
				
				
			}
		}
		
		/*
		SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");
        Date now = new Date();
        System.out.println( String.format( "TODAY=%s",  df.format(now) ) );
        df.applyPattern("yyyyMMdd");
        System.out.println( String.format( "YYYYMMDD=%s",  df.format(now) ) );
        df.applyPattern("MMdd");
        System.out.println( String.format( "MMDD=%s",  df.format(now) ) );
        df.applyPattern("MMddyyyy");
        System.out.println( String.format( "MMDDYYYY=%s",  df.format(now) ) );
        df.applyPattern("MMddyy");
        System.out.println( String.format( "MMDDYY=%s",  df.format(now) ) );
        df.applyPattern("yyyy");
        System.out.println( String.format( "YYYY=%s",  df.format(now) ) );
		*/
        return true;
    }
    
    
	
	private boolean printHelp() {
		try {
			parser.printHelpOn(System.out);
			return true;
		}
		catch( Exception e ) {
			logger.error( String.format("Failed to print help text. Reason: %s", e.getMessage() ) );
			return false;
		}
		
		
	}
	//joins members of collection into a single,delimited string.
    public static <T>
    String join(final Collection<T> objs, final String delimiter) {
      if (objs == null || objs.isEmpty())
        return "";
      Iterator<T> iter = objs.iterator();
      StringBuffer buffer = new StringBuffer(iter.next().toString());
      while (iter.hasNext())
        buffer.append(delimiter).append(iter.next().toString());
      return buffer.toString();
    }
	

}
