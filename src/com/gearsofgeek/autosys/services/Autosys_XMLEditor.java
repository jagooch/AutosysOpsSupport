/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


package com.gearsofgeek.autosys.services;

//import Opt-simple
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

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
public class Autosys_XMLEditor {

    String xmlDocPath; // path to the xml document to edit
    String xmltag; //string pattern to find within the xml doc for replacement
    String replace; //string to replace the found string pattern with;
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger("com.psi.autosys.Autosys_Ftp");//logger to manager all logging needs :)
	private static Level defaultLoggingLevel = Level.INFO;

    private Properties prop;
    
    
    //Options Variables
    private OptionParser parser; //command line parser
    private OptionSet options; //list of recognizable ( configured )options found on the command line.

    
    //Options Parameter switches
    private static final String XMLDOCPATH = "x"; //path to the xml document file
    private static final String XMLTAG = "t"; //text that needs to be replaced
    private static final String REPLACE = "r"; // test to replace the original text
    private static final String LOGGINGLEVEL = "l"; //sets the log4j level
    private static final String HELP = "h"; //prints out help
    
    public Autosys_XMLEditor(String[] args) {
		logger.addAppender(new ConsoleAppender( new SimpleLayout()));//set up for basic logging
		logger.setLevel(defaultLoggingLevel);
	    logger.info( String.format( "Basic Logging successfully intitialized with logging level %s", logger.getLevel().toString() ) );
    }
    

    public boolean initialize(String[] args) {
        logger.debug("Starting initialize.");
        if ( initLogging() == false ) {
            logger.error( String.format( "Failed to initialize logging", ""   ) );
            return false;
        }
        else if ( initializeCLI( args ) == false ) {
            logger.error( String.format( "Failed to initialize CLI", ""   ) );
        	return false;
        }
        else if ( initializeLocals() == false ) {
            logger.error( String.format( "Failed to initialize Local Variables", ""   ) );
            return false;
        }
        else {
            logger.info("Application successfully initialized.");
            logger.debug("Exiting initialize.");
            return true;
        }
    }

    private boolean initializeLocals() {
        prop = new Properties();
        return true;
    }
      
    private boolean initializeCLI( String[] args ) {
       	logger.debug("Starting initializeCLI");
           	parser = new OptionParser() {
    		{
	    		accepts( XMLDOCPATH,"XML Document Path").withRequiredArg().describedAs("Path to XML file to edit").ofType(String.class);
	    		accepts( XMLTAG,"XMLTag").withRequiredArg().describedAs("Name of tag whose value will be replaced with the specified one.").ofType(String.class);
	    		accepts( REPLACE,"Replacement text").withRequiredArg().describedAs("Text that replaces the original text").ofType(String.class);
	    		accepts( HELP,"{Print help text.");
	    		accepts( LOGGINGLEVEL,"Set Logging level.").withRequiredArg().describedAs("Sets the minimum level of events to be logged.").ofType(String.class);
	    		
			}
    	};

    	options = parser.parse(args);

    	//check for problems
    	//check if Help is requested
        if ( options.has(HELP) )  {
    		if( printHelp() == true ) {
    			return true;
    		}
    		else {
    			logger.error( "Failed to print application help." );
    			return false;
    		}
    		
    	}
    	//check for mandatory arguments 
    	if ( options.has( XMLDOCPATH) == false   ) {
    		logger.error( String.format( "Missing mandatory argument %s", XMLDOCPATH  ) );
    		printHelp();
    		return false;	
    	}    	
    	if ( options.has( XMLTAG) == false   ) {
    		logger.error( String.format( "Missing mandatory argument %s", XMLTAG  ) );
    		printHelp();
    		return false;	
    	}    	
    	if ( options.has( REPLACE) == false   ) {
    		logger.error( String.format( "Missing mandatory argument %s", REPLACE  ) );
    		printHelp();
    		return false;	
    	}    	

    	//check for optional logging level parameter and set level accordingly
    	if ( options.has(LOGGINGLEVEL) ) {
    		if ( options.hasArgument(LOGGINGLEVEL) == true  ) {
    			logger.setLevel( Level.toLevel(  (String) options.valueOf(LOGGINGLEVEL) ) );
    			
    		}
    		else {
    			logger.debug(String.format( "Missing argument for the logging level parameter.", "" )  );
    			printHelp();
    			return false;
    			
    		}
    			
    	}
    	
    	//load command line arguments into the correct variables.
    	//TODO continue from here
    	xmlDocPath = options.valueOf(XMLDOCPATH).toString();
    	replace = options.valueOf(REPLACE).toString();
    	xmltag = options.valueOf(XMLTAG).toString();
    	
    	
    	
    	logger.info("Parameters successfully initialized.");
        logger.debug("Exiting initializeParameters");
        return true;
    
    }
    
    private boolean printHelp() {
		try {
			parser.printHelpOn(System.out  );
		} catch (IOException e) {
			logger.error( String.format( "Error while printing help text. Reason: %s" , e.getMessage() ) );
		}

		return false;
	}

	private boolean initLogging() {
        //logger.setLevel( org.apache.log4j.Level.DEBUG );
        SimpleLayout layout = new SimpleLayout();
        logger.addAppender( new ConsoleAppender(layout)  );
        logger.setLevel((Level) Level.DEBUG);
        logger.debug("Logger Initialized");
        return true;
    }

    public boolean run()  {
    	if( options.has(HELP) ) {
    		return true;
    	}
        File xmlFile = new File(xmlDocPath);
        
        if ( xmlFile.exists() == false   ) {
            logger.error(  String.format( "%s does not exist."  , xmlDocPath  )  );
            return false;
        }
        else if ( xmlFile.isFile() == false   ) {
            logger.error(  String.format( "%s is not a normal file."  , xmlDocPath  )  );
            return false;
        
        }
        else  {    
            try {
                prop.loadFromXML( new FileInputStream( xmlDocPath ) ); 
                logger.info( String.format( "Properties successfully loaded from %s", xmlDocPath  ) );
            }
            catch ( IOException ex )  {
                logger.error( String.format( "IOException loading the properties file %s. Exception: %s"  , xmlDocPath, ex.getMessage()  )  );
                return false;    
            }

            prop.setProperty(xmltag, replace );
            logger.info( String.format( "XML Tag %s value changed to %s", xmltag, replace  ) );

            try {
                prop.storeToXML(new FileOutputStream(xmlDocPath), "WOW");
                logger.info( String.format( "Updated XML File %s successfully saved.", xmlDocPath  ) );

            } catch (IOException ex) {
                logger.error( String.format( "IOException saving the properties file %s. Exception: %s"  , xmlDocPath, ex.getMessage()  )  );
            }
        }
        return true;

    }
        
    
    
    
    
    

}
