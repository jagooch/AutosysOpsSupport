/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.psi.autosys.services;



//import Opt-simple
import java.io.IOException;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

public class Autosys_Delay {

    private int delayTime; //the amount to delay in seconds


//    private static org.apache.log4j.Logger logger;//logger to manager all logging needs :)
    private static Logger logger = org.apache.log4j.Logger.getLogger("com.psi.autosys.services.Autosys_Delay");

    //Options members
    private static final String DELAYTIME = "t";
    private static final String HELP = "help";
    private static final String LOGGINGLEVEL = "l";

    //Options member vars
    private OptionParser parser; //command line parser
    private OptionSet options; //list of recognizable ( configured )options found on the command line.

    
    //logging variables
    private Level loggingLevel;
    private static Level defaultLoggingLevel = Level.INFO;

    //empty constructor
    public Autosys_Delay( String[] args ) {
    	logger = Logger.getLogger(this.getClass().getName()) ;
    	logger.setLevel(Level.DEBUG);
    	logger.addAppender( new ConsoleAppender( new SimpleLayout()));
    	
    	
    }
    

    private boolean delay( int delayTime  ) {
        try {
            Thread.sleep( delayTime * 1000 );
            return true;
        }
        catch (InterruptedException ie) {
            logger.error( String.format( "System failed to delay(sleep). Reason: %s", ie.getMessage() ) );
            return false;
        }
    }

    public boolean initialize(String[] args) {


        if ( initializeLocals() == false ) {
            logger.error("Failed to initializeLocals");
            return false;
        }
        else if ( initializeLogging() == false ) {
            logger.error("Failed to initializeLogging");
            return false;
        }
        else if ( initializeOptions( args ) == false ) {
            logger.error("Failed to initializeOptions");
            return false;
        }
        else if ( initializeConfig(args) == false ) {
            logger.error("Failed to initializeConfig");
            return false;
        }
        else {
            logger.info( "Application successfully initialized.");
            return true;
        }
    }

    private boolean initializeLocals() {
        logger.debug( "Logger successfully intialized."  );
        return true;
    }

    private boolean initializeLogging() {
        //logger.setLevel( org.apache.log4j.Level.DEBUG );
        SimpleLayout layout = new SimpleLayout();
        logger.addAppender( new ConsoleAppender(layout)  );
        logger.setLevel((Level) Level.INFO);
        logger.debug( "Logging successfully intialized."  );

        return true;
    }


    private boolean initializeOptions( String[] args ) {
        parser = new OptionParser() {
            {
                accepts( DELAYTIME, "Delay time in seconds" ).withRequiredArg().describedAs( "seconds" ).ofType( String.class );
                accepts( LOGGINGLEVEL, "Level of events to log." ).withRequiredArg().describedAs( "LOGGING LEVEL" ).ofType( String.class );
                accepts( HELP, "Request a print out of help.");
            }
        };
        options = parser.parse( args );
        logger.debug( String.format("Options initialized successfully." ) );

        return true;
    }

    public boolean initializeConfig(String[] pArgs ) {
        if( pArgs.length == 0 ) {
                logger.error( String.format("Configuration initialization failed. Parameter count=%d", pArgs.length));

            try {
                    parser.printHelpOn(System.out);
                    return false;
                } catch (IOException ex) {
                    logger.error( String.format("IOException encountered while printing help. Reason: %s", ex.getMessage() ) );
                    return false;
                }
        }
        else if ( options.has( HELP  )    ) {
            try {
                parser.printHelpOn(System.out);
                return true;
            } catch (IOException ex) {
                    logger.error( String.format("IOException encountered while printing help. Reason: %s", ex.getMessage() ) );
                return false;
            }

        }
        
        if ( options.has(DELAYTIME)) {
            delayTime = Integer.valueOf( options.argumentOf(DELAYTIME) );
        }
        else {
        	logger.error( String.format( "Missing the mandatory delaytime option.", "" ) );
        	return false;
        }
      
        
        //check if the logging config file path was specified
        if ( options.has(LOGGINGLEVEL) ) {
        	loggingLevel = Level.toLevel( (String)  options.valueOf(LOGGINGLEVEL) );
        }
        else {
        	loggingLevel = defaultLoggingLevel;
        }
        logger.setLevel(loggingLevel);
        return true;
    }
    
    //runs the application
    public boolean run() {
    	//if the user has request the help output, there is nothing else to do
    	if( options.has( HELP  ) ) {
    		return true;
    	} 
    	//run the delay method
    	if( delay( delayTime) == true ) {
    		logger.debug( String.format( "Successfully delayed for %s seconds", delayTime  )  );
			return true;
    	}
    	else {
    		logger.error( String.format("Failed to delay for %s seconds", delayTime  )  );
    		return false; 
    	}
    }
    


}
