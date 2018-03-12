/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.psi.autosys.project.moveit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.Iterator;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

import com.stdnet.moveit.api.Client;
import com.stdnet.moveit.api.MOVEitFileInfo;

/**
 *
 * @author jgooch
 */
public class MoveITSync {
    //moveit connection settings
    private Boolean ignoreCertProbs; //moveit setting to ignore invalid certs or not
    private Boolean secure; //moveit security level ( true secure, false - nonsecure
    private Client cli; //movetit client connection
    private String host; //moveit hostname/ip
    private String username; //moveit username
    private String password; //moveit password
    private String port; //network port to communicate to moveit on
    private String srcRoot; //root path to the local location of the letter files
    private String dstRoot; //root path to the NMSN destination folder
    private String pathfilter; //filter moveit paths from processing
    private String fileNameFilter; //filter filenames from processing

    private Properties props; //java properties object
    private FileInputStream fis; //file input stream 

    private Pattern pathPattern;
    private Pattern fileNamePattern;
    private Matcher matcher;
    private String configFilePath;
    private static Logger logger = Logger.getLogger( MoveITSync.class.getName() );
    private static Level defaultLoggingLevel = Level.DEBUG;
    
    OptionParser parser; //reads in and interprets the command line options
    OptionSet options; //set of comand line options that were passed in
    private static String CONFIGFILEPATH = "c"; //sets the switch for the configfile path option
    private static String LOGLEVEL = "l";//sets the switch for the log4j logging level option
    private static String HELP = "h";//sets the switch for the log4j logging level option
    private File configFile; //represents the physical configuration file for this application
    private Boolean recurse; //recurse subdirectories or not
    private Boolean deleteEnabled;

    
    //Post-run Report Options
    private Integer rptFileCount = 0;
    private long rptByteCount = 0;
    
    
    
   
    //nothing to do here
    public MoveITSync( String[] args) {
    	
    	    	
    	
    }
    
    
    private boolean initCLI( String[] params ) {
         parser = new OptionParser();
         parser.accepts( CONFIGFILEPATH, "Location of the appliation configuration file").withRequiredArg().ofType(String.class);
         parser.accepts( LOGLEVEL, "Log4j Logging Level").withRequiredArg().ofType(String.class);
         parser.accepts( HELP, "Print out help text." );
         options = parser.parse(params);

         
         if ( options.has(HELP) ) {
        	 return true;
         }
         
         
         if ( options.has(CONFIGFILEPATH) == false ) {
             configFile = new File( "moveitxfer.properties.xml" );
             logger.debug("Using default config file - autosys_moveitsync.properties" );
         }
         else {
             configFile = new File( String.valueOf( options.valueOf(CONFIGFILEPATH) ) );
         }

         if ( options.has(LOGLEVEL) == false ) {

			logger.setLevel( defaultLoggingLevel );
         }
         else {
            logger.setLevel( Level.toLevel( (String) ( options.valueOf(LOGLEVEL) ) ) );
         }


         //check if the configuration file exists or not
         if ( configFile.exists() == false ) {
            logger.error( String.format( "Invalid configuration file. File %s does not exist.", configFile.getPath()   ) ) ;
            return false;
         }

         return true;
    }

    private boolean initConfig() {

    	if ( options.has(HELP)) {
    		return true;
    	}
       try {
              fis = new FileInputStream( configFile.getPath() );
        } catch (FileNotFoundException ex) {
            logger.error( String.format( "Failed to open application configuration file %s. Reason:", configFilePath, ex.getMessage()  )  ) ;
        }
 
        props = new Properties();
        try {
            props.loadFromXML(fis);
        }
        catch (InvalidPropertiesFormatException ex) {
            logger.error( String.format( "Failed to read in properties file. Invalid Properties. Reason: %s" ,ex.getMessage() ) );
        }
        catch (IOException ex) {
            logger.error( String.format( "Failed to read in properties file. Reason: %s" ,ex.getMessage() ) );
        }

        host = props.getProperty("host");
        username = props.getProperty("username");
        password = props.getProperty("password");
        secure = Boolean.valueOf( props.getProperty("secure", "true") );
        ignoreCertProbs = Boolean.valueOf( props.getProperty("ignoreCertProbs", "true" ) );
        srcRoot = props.getProperty("srcRoot");
        dstRoot = props.getProperty("dstRoot");
        fileNameFilter = props.getProperty("fileNameFilter", ".*");
        pathfilter = props.getProperty("pathFilter", ".*");
        recurse = Boolean.valueOf( props.getProperty("recurse",  "true" ) );
        deleteEnabled = Boolean.valueOf( props.getProperty("deleteEnabled", "true"));

        //output settings for debugging purposes.
        logger.debug(  String.format( "host=%s", host   ) );
        logger.debug(  String.format( "username=%s", username   ) );
        logger.debug(  String.format( "password=%s", password   ) );
        logger.debug(  String.format( "secure=%s", secure   ) );
        logger.debug(  String.format( "ignoreCertProbs=%s", ignoreCertProbs   ) );
        logger.debug(  String.format( "srcRoot=%s", srcRoot   ) );
        logger.debug(  String.format( "dstRoot=%s", dstRoot   ) );
        logger.debug(  String.format( "fileNameFilter=%s", fileNameFilter   ) );
        logger.debug(  String.format( "pathfilter=%s", pathfilter   ) );

        cli = new Client();
        cli.secure(secure);
        cli.ignoreCertProbs(ignoreCertProbs);
        fileNamePattern = Pattern.compile( fileNameFilter );
        pathPattern = Pattern.compile(pathfilter);
        return true;
    }

    private boolean initLocals() {
        return true;
    }

    private boolean initLogger() {
       logger.setLevel(Level.DEBUG);
       logger.addAppender( new ConsoleAppender( new SimpleLayout()));
       return true;
    }


    public boolean initialize( String[] args ) {
        if ( initLogger() == false ) {
            return false;
        }
        else if ( initCLI( args ) == false ) {
            logger.error(String.format( "Failed to initialize the command line interface." , ""  ) );
            return false;
        }
        else if ( initLocals() == false ) {
            logger.error(String.format( "Failed to initialize the Local variables." , ""  ) );
            return false;
        }
        else if ( initConfig() == false ) {
            logger.error(String.format( "Failed to initialize the configuration ." , ""  ) );
            return false;
        }
        else {
            logger.info(String.format( "Successfully initialized the applicatiomn. ", ""  ) );
            return true;
        }
    }

    //Connects to the MoveIT server
    private boolean connect() {
        cli.host(host);
        if ( cli.signon(username, password) == false ) {
            logger.error( "Failed to log in");
            return false;
        }
        else {
            logger.info("Successfully logged in.");
            return true;
        }
    }


    //Starts the file transfer process
    public boolean run() {
        logger.info( "Starting the run function" );
        if ( options.has(HELP) ) {
        	return printHelp();
        }
        
        if ( connect() == true ) {
            logger.info( String.format( "Successfully connected to MoveIT server %s", host ));
            if ( moveFiles( srcRoot, dstRoot ) == true  ) {
            	printReport(); //print out the results
                return true;
            }
            else {
                logger.error( String.format( "Failed to sync files from %s to %s", srcRoot, dstRoot   ) );
                return false;
            }
        }
        else {
            return  false;
        }
    }

    private boolean moveFiles( String srcRoot, String dstRoot )  {
	    logger.info("Finding new files\n");
	    HashMap files = cli.findFilesInFolder( "*", srcRoot  );
	
	    logger.info(String.format( "File count = %s" , files.size()  ));
	
	    if ( (files == null  ) == true ) {
	        return false;
	    }
	    else {
	    	for ( Iterator fit = files.keySet().iterator();fit.hasNext();) {
	            MOVEitFileInfo file = (MOVEitFileInfo)  files.get(fit.next()) ;
	            //make certain the file path matches the pathfilter
	            matcher = pathPattern.matcher( file.path() );
	
	            if ( matcher.matches() == false ) {
	                logger.info(String.format( "File path %s does not match filter %s" , file.path(), pathfilter ) );
	            }
	            else {
	                logger.info(String.format( "File path %s matches the filter %s" , file.path(), pathfilter ) );
	                String srcFilePath = file.path();
	                String dstFilePath =  String.format( "%s"  , file.path().replaceFirst(  srcRoot, dstRoot ) )  ;
	                logger.info( String.format( "The dstFile path is %s\ndstPath" ,  dstFilePath   ) );
	
	                logger.info( String.format( "Downloading %s to %s" , srcFilePath, dstFilePath)  );
	                if ( cli.downloadFileAs( srcFilePath  , dstFilePath ) == false  ) {
	                    logger.error( String.format( "Failed to download %s to  %s " ,  srcFilePath, dstFilePath )  );
	                    return false;
	                }
	                else if ( deleteEnabled == true ) {
	                    if ( cli.deleteFile( srcFilePath  ) == false ){
	                        logger.error( String.format( "Failed to delete source file %s" ,  srcFilePath, dstFilePath )  );
	                        return false;
	                    }
	                }
	                else  {
	                    logger.info(  String.format( "%s successfully moved to %s", file.path(), dstFilePath   )    ) ;
	                }
	            }
	            rptFileCount++;
	        }
	        return true;
		}
    }
    
    private void printReport() {
    	logger.info( String.format( "MovIT Sync Report", "" ));
    	logger.info( String.format( "Files Moved: %d", rptFileCount ));
    }
    
    //print out application help info.
    private boolean printHelp() {
    	try {
			parser.printHelpOn(System.out);
			return true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
	}
}