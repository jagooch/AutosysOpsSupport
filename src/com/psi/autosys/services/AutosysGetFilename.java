/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.psi.autosys.services;

//import java core libraries
import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;
import java.util.Arrays;
import java.util.Comparator;


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




public class AutosysGetFilename {

    //parameters section
    private static final String PATH = "p"; //
    private static final String NAME = "n"; //pattern of filename to look for
    private static final String HELP = "h"; //print out the help text
    private static final String LOGGINGLEVEL = "l"; //logging level

    
    //parsing section
    private OptionParser parser;
    private OptionSet options;
    
    //logging section
    private static Logger logger = Logger.getLogger("com.psi.autosys.services.AutosysGetFilename"); 
    private String loggingLevel; //logging level ( debug, error, etc )
    private static Level defaultLoggingLevel = Level.INFO;
    
    
    //default constructor
	public AutosysGetFilename(String[] args) {
		logger.addAppender(new ConsoleAppender( new SimpleLayout()));//set up for basic logging
		logger.setLevel(defaultLoggingLevel);
	    logger.info( String.format( "Basic Logging successfully intitialized with logging level %s", logger.getLevel().toString() ) );
	}
        
	
	public boolean initialize(String[] args) {
        if ( initLogging() == false ) {
            return false;
        }
        else if ( initCLI(args) == false ) {
            return false;
        }
        else if ( initCLIConfig() == false )  {
            return false;
        }
        else if ( initLocals() == false ) {
            return false;
        }
        else {
            return true;
        }

    }

    
    
    private boolean initLogging() {
        logger = Logger.getLogger("getFileName.class");//logger to manager all logging needs :)
        //logger.setLevel( org.apache.log4j.Level.DEBUG );
        SimpleLayout layout = new SimpleLayout();
        logger.addAppender( new ConsoleAppender(layout)      );
        logger.setLevel((Level) Level.INFO );
        logger.debug("Logger Initialized");
        return true;
    }

    private boolean initCLI(String[] args) {
        parser = new OptionParser() {
            {
                accepts( PATH, "Application configuration file." ).withRequiredArg().describedAs( "Path to configuration file." ).ofType(String.class) ;
                accepts( LOGGINGLEVEL, "Application configuration file." ).withRequiredArg().describedAs( "Path to configuration file." ).ofType(String.class) ;
                accepts( NAME, "Project configuration file. Lists project, their flows, and their jobs" ).withRequiredArg().describedAs( "Path to Projects File." ).ofType( String.class ); 
                //accepts( GLOBAL_VARIABLE, "Debugging level of application. Default is info." ).withRequiredArg().describedAs( "Logging level." ).ofType( String.class );
                accepts( HELP );
            }
        };
        options = parser.parse( args );
        return true;
    }

    private boolean initCLIConfig() {
        logger.debug("Entering initCLIConfig");
        //check for mandatory parameters
        if ( options.has( HELP )  ) {
            return true;
        }
        else if( options.has( PATH ) == false  ) {
            logger.error( String.format( "Search directory path option missing." ) );
            return false;
        }
        else if ( options.hasArgument(PATH) == false) {
            logger.error( String.format( "Search directory path value missing." ) );
            return false;
        }
        else if ( options.has( NAME ) == false) {
            logger.error( String.format( "File name pattern option missing." ) );
            return false;
        }
        else if ( options.hasArgument( NAME ) == false) {
            logger.error( String.format( "File name pattern value missing." ) );
            return false;
        }
        /*else  if ( options.has(GLOBAL_VARIABLE) == false  ) {
            logger.error( String.format( "Global variable name option missing." ) );
            return false;
        }
        else if ( options.hasArgument(GLOBAL_VARIABLE) == false  ) {
            logger.error( String.format( "Global variable name value missing." ) );
            return false;
        }
        */
        
        //set the logging level if specified 
        if ( options.has(LOGGINGLEVEL) == false ) {
            logger.setLevel( ( Level ) Level.INFO );
        }
        else {
            loggingLevel = options.valueOf(LOGGINGLEVEL).toString().toUpperCase();

            //set the logging level
            if ( loggingLevel.equalsIgnoreCase("DEBUG") ) {
                logger.setLevel( ( Level ) Level.DEBUG );
            } 
            else if (loggingLevel.equalsIgnoreCase("INFO")  ) {
                logger.setLevel( ( Level ) Level.INFO );
            }
            else if (loggingLevel.equalsIgnoreCase("WARNING")  ) {
                logger.setLevel( ( Level ) Level.WARN );
        }
            else if (loggingLevel.equalsIgnoreCase("ERROR")  ) {
                logger.setLevel( ( Level ) Level.ERROR );
            }
            else if (loggingLevel.equalsIgnoreCase("FATAL")  ) {
                logger.setLevel( ( Level ) org.apache.log4j.Level.FATAL );
            }
            else {
                logger.setLevel( ( Level ) Level.INFO );
            }
        }
        
        
        
        
        return true;
    }

    private boolean initLocals() {
        logger.debug("Entering initLocals");
        return true;
    }

    public boolean run() {
    	if( options.has(HELP) ) {
    		return true;
    	}

        logger.debug( "Entering Run" );
        File dir = new File( options.valueOf(PATH).toString()  );
        if ( dir.exists() == false ) {
            return false;
        }
        else {
            File[] files = dir.listFiles( new DirFilter(options.valueOf(NAME).toString() ) )   ;
            
            Arrays.sort(files, new Comparator<File>(){
                public int compare(File f1, File f2)
                {
                    return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
                } });

            
            for ( File testfile : files ) {
                if ( ( testfile.isFile() == true ) ) {
                    logger.debug( "Directory is " + testfile.isDirectory() + " and File is " + testfile.isFile()   );
                    //System.out.print( testfile.getName()  );
                    System.out.println( String.format( "FILENAME=%s", testfile.getName()  ) );
                    return true;
                }
            }
            return false;
        }
        
    }

    class DirFilter implements FilenameFilter {
    	  private Pattern pattern;

    	  public DirFilter(String regex) {
    	    pattern = Pattern.compile(regex);
    	  }

    	  public boolean accept(File dir, String name) {
    	    // Strip path information, search for regex:
    	    return pattern.matcher(new File(name).getName()).matches();
    	  }
    	} 

    

}
