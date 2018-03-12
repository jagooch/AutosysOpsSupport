package com.psi.autosys.project.moveit;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.InvalidPropertiesFormatException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;




/**
 *
 * @author jgooch
 */
public class MoveITStage {
    private String srcRootFolderPath; //path to start the search at, should be one level above the desired path
    private String dstRootFolderPath; //path to build dir structure for destination files

    private File srcRootFolder; //root source folder
    private File dstRootFolder; //root destination folder
    private File srcProgramFolder;
//    private File dstprogramFolder;

    private static Logger logger = Logger.getLogger( MoveITStage.class.getName() );


    private static String defaultConfigFilePath = "autosys_ift.properties.xml";
    private static String configFilePath; //path to the configuration file
    private static Level defaultLoggingLevel = Level.INFO ;
    private Level loggingLevel; //log4j logging level
    private OptionParser parser;
    private OptionSet options;
//    private String year;
//    private String archive;
    private FileInputStream fis; //stores the properties file input stream
    private SimpleDateFormat df;
    private Calendar cal;
    private String programName; //name of the state program to process files for

    private Properties props; //holds values from properties config file
    private Boolean recurse;
    private String srcPathFilter; //filter out directories on the source
    private String fileNameFilter; //filter for the file names


    private Pattern srcPathPattern; //filter for the filepath
    private Pattern fileNamePattern; //filter for the file names

//    private String[] fileExtensions;
    private static String CONFIGFILEPATH = "c";
    private static String LOGGINGLEVEL = "l";
    private static String PROGRAMNAME = "p";
    private static String HELP = "h";

    private String archiveFileParentPath;
	private int fileStageCount;
	private int fileArchiveCount;

    //Constructor
    public MoveITStage( String[] args ) {
    	fileStageCount = 0;
    	fileArchiveCount = 0;
    }
    

    //calculates what the destination File path should be for the current file
    private File getDestinationFile( File  srcFile, File dstRootFolder ) {
        //Get the source file path
        String srcFileParentPath = srcFile.getParent();
        //Get the source file name
        String srcFileName = srcFile.getName();
        logger.debug( String.format("Calculating destination path for %s to place into %s", srcFile.getPath(), dstRootFolder.getPath()   ));
        //Calclulate the destination file path

        String tmpFileParentPath = srcFileParentPath.replace( srcRootFolderPath, "" );
        logger.debug( String.format( "TmpFilePath is now %s after removing %s from the path." ,tmpFileParentPath,  srcRootFolderPath  ) );

        tmpFileParentPath = tmpFileParentPath.replace( programName, "" ) ;
        logger.debug( String.format( "TmpFilePath is now %s after removing %s from the path." , tmpFileParentPath, programName ) );
        logger.debug( String.format(  "Temporary file path is %s and index of \\ is %s",tmpFileParentPath, tmpFileParentPath.indexOf("\\") )    );

        //remove starting and trailing backslashes
        while( tmpFileParentPath.indexOf("\\") == 0 ) {
            tmpFileParentPath = tmpFileParentPath.substring( 1);
        }

        while( tmpFileParentPath.indexOf("\\") == tmpFileParentPath.length() -1 ) {
            tmpFileParentPath = tmpFileParentPath.substring( 1);
        }

        //remove starting and trailing forward slashes
        while( tmpFileParentPath.indexOf("/") == 0 ) {
            tmpFileParentPath = tmpFileParentPath.substring( 0, tmpFileParentPath.length() -1  );
        }

        while( tmpFileParentPath.indexOf("/") == tmpFileParentPath.length() -1 ) {
            tmpFileParentPath = tmpFileParentPath.substring( 0, tmpFileParentPath.length() -1  );
        }

        logger.debug( String.format(  "Temporary file path is %s after removing starting \\'s.",tmpFileParentPath ) );

        String[] parts;

        if (  tmpFileParentPath.indexOf("\\") != -1   ) {
            parts = tmpFileParentPath.split( "\\\\" );
        }
        else {
            parts = tmpFileParentPath.split("/");
        }

        if ( parts.length != 2 ) {
            logger.error( String.format( "Path %s appears to be in the incorrect format. ", tmpFileParentPath  ));
            exitWithFailure(options);
        }

        String entity = parts[0];
        String direction = parts[1];
        List<String> lstParts = Arrays.asList( ( new String[]  { dstRootFolder.getPath(), programName, direction   }  )  )  ;
        String dstFileParentPath = join(lstParts, "/");

        //Calculate the archive Path, note that this sets a global variable.
        List<String> archivePathParts = Arrays.asList( ( new String[]  { dstRootFolder.getPath(), programName, "archive"   }  )  )  ;
        archiveFileParentPath = join( archivePathParts  , "/");

        //now calculate the new file name
        String basename =  srcFileName.substring(0, srcFileName.lastIndexOf( "." ) );
        String extension = srcFileName.substring( srcFileName.lastIndexOf( "."  ) + 1 );
        //Calendar now = Calendar.getInstance();
        //SimpleDateFormat formatter = new SimpleDateFormat("E yyyy.MM.dd 'at' hh:mm:ss a zzz");
        cal = Calendar.getInstance();
        String timestamp = df.format( cal.getTime() );
        logger.info( String.format( "basename:%s entity:%s timestamp:%s  extension:%s", basename, entity, timestamp, extension ));

//      old file name format  String newFileName = String.format( "%s-%s-%s.%s", basename, entity, timestamp, extension );
        String newFileName = String.format( "%s.%s.%s.%s", timestamp, entity,basename, extension );
        logger.debug( String.format( "New file parent directory is %s\nNew file name is %s.\n", dstFileParentPath, newFileName   ));
        return new File(  dstFileParentPath + "/" + newFileName   );
    }

    private boolean initCLI( String[] params) {
         parser = new OptionParser();
         parser.accepts(CONFIGFILEPATH, "(optional) Path to the configuration file").withRequiredArg().ofType(String.class);
         parser.accepts(LOGGINGLEVEL, "(optional ) Log4j Logging level").withRequiredArg().ofType(String.class);
         parser.accepts( PROGRAMNAME, "(required) Name of the project. Example: GA\\NH").withRequiredArg().ofType(String.class);
         parser.accepts( HELP, "(optional) Prints out the help screen." );
         options = parser.parse(params);

         
         if ( options.has( HELP  )  ) {
        	 return true;
         }
         
         if ( options.has(CONFIGFILEPATH) == false ) {
             logger.info( String.format( "No configfile path given. Using default path %s.", defaultConfigFilePath ));
             configFilePath = defaultConfigFilePath;
         }
         else {
             configFilePath = (String)options.valueOf(CONFIGFILEPATH);
             logger.info( String.format( "Using configfile %s .", configFilePath ));
         }

         //set the logging level
         if ( options.has(LOGGINGLEVEL) == false ) {
             logger.info( String.format( "No logging level given. Using default level of INFO.", "" ));
             loggingLevel = defaultLoggingLevel;
         }
         else {
             loggingLevel =  Level.toLevel( (String) options.valueOf(LOGGINGLEVEL) );
         }
         logger.setLevel(loggingLevel);
         logger.info( String.format( "Logging level set to %s.", loggingLevel.toString() ));

         if ( options.has( PROGRAMNAME )  == false   ) {
            logger.error( String.format( "Missing program name argument - %s .", PROGRAMNAME  )  );
            try {
                parser.printHelpOn(System.err);
            } catch (IOException ex) {
                logger.error( String.format("IOException while printing help. Reason: %s", ex.getMessage() ) );
                exitWithFailure(options);
            }
            return false;
         }
         else {
            programName = (String )options.valueOf(PROGRAMNAME );
            logger.info( String.format( "Progam name set to %s.", programName ));
           
         }
         //if there are invalid/missing arguments, print help and exit with failure
         if ( ( options.nonOptionArguments().size() != 0 ) ) {
            try {
            	logger.error( String.format( "%s invalid option detected.",  options.nonOptionArguments().size()  )  );
            	for ( Iterator<?> nit =  options.nonOptionArguments().iterator();nit.hasNext(); ) {
            		String tmpOption = ( String ) nit.next();
            		logger.info( String.format( "Nonoption %s detected.", tmpOption ) ) ;
            		return false;
            		//            		options.nonOptionArguments().
            	}
                parser.printHelpOn(System.err);
            } catch (IOException ex) {
              logger.error( String.format( "IOException while printing help. Reason: %s", ex ) );
              exitWithFailure(options);
            }
            return false;
        }

         //if the user specifically asks for help, print help and exit with success
         if( options.has( HELP ) ) {
            try {
                parser.printHelpOn(System.err);
            } catch (IOException ex) {
              logger.error( String.format( "IOException while printing help. Reason: %s", ex ) );
              exitWithFailure(options);
            }
            exitWithSuccess(options);
         }

         return true;
    }

    //Exits  the application with success code
    private static void exitWithSuccess(OptionSet options) {
        System.exit(0);
    }

    /*
    //Exits the application with warning code
    private static void exitWithWarning(OptionSet options) {
        System.exit(1);
    }

*/
    //Exits  the application with failure code
    private static void exitWithFailure(OptionSet options) {
        System.exit(2);
    }

    private boolean initConfig() {

    	
    	if ( options.has( HELP ) == true ) {
    		return true;
    	}
    	
    	try {
          fis = new FileInputStream( configFilePath);
        } catch (FileNotFoundException ex) {
            logger.error( String.format( "FileNotFoundException: Failed to open application configuration file %s. Reason: %s", configFilePath, ex.getMessage()  )  ) ;
            return false;
        }
       catch ( Exception ex ) {
            logger.error( String.format( "General Exception: Failed to open application configuration file %s. Reason:", configFilePath, ex.getMessage()  )  ) ;
            return false;
       }

        props = new Properties();
        try {
            props.loadFromXML(fis);
        }
        catch (InvalidPropertiesFormatException ex) {
            logger.error( String.format( "InvalidPropertiesFormatException: Failed to read in properties file. Invalid Properties. Reason: %s" ,ex.getMessage() ) );
            return false;
        }
        catch (IOException ex) {
            logger.error( String.format( "IOException Failed to read in properties file. Reason: %s" ,ex.getMessage() ) );
            return false;
        }
        recurse = Boolean.valueOf(props.getProperty("recurse", "true"  ));
        logger.debug( String.format( "The value of recurse is %s", Boolean.toString(recurse) ));
        srcRootFolderPath = props.getProperty("srcRootFolderPath");
        dstRootFolderPath = props.getProperty("dstRootFolderPath");
        srcRootFolder = new File(srcRootFolderPath );
        dstRootFolder = new File( dstRootFolderPath );
        fileNameFilter = props.getProperty("fileNameFilter", null);
        srcPathFilter = props.getProperty( "srcPathFilter", null );


        fileNamePattern = Pattern.compile(fileNameFilter, Pattern.CASE_INSENSITIVE );
        srcPathPattern = Pattern.compile( srcPathFilter, Pattern.CASE_INSENSITIVE);


        //Check that the required folders exits
        if ( srcRootFolder.exists() == false  ) {
            logger.error( String.format( "Source Root folder %s does not exist.", srcRootFolder.getPath() ) );
            return false;
        }
        else if ( dstRootFolder.exists() == false ) {
            logger.error( String.format( "Destination Root folder %s does not exist.", dstRootFolder.getPath() ) );
            return false;
        }

        return true;
    }

    private boolean initLocals() {
        df = new SimpleDateFormat("yyyyMMdd_kkmm");
//        cal = Calendar.getInstance();
        logger.info(String.format("Succesfully initialized local variables.", "") );
        return true;
    }

    private boolean initLogging() {
       logger.setLevel(Level.DEBUG);
       logger.addAppender( new ConsoleAppender( new SimpleLayout()));
       return true;
    }
    //initialized the application environment
    public boolean initialize(String[] args) {
        if ( initLogging() == false ) {
            System.out.println( String.format("Logger initialization failed.", "" ));
            return false;
        }
        else if ( initCLI(args ) == false ) {
            logger.error( String.format( "Command line parameter initialization failed.", ""));
            return false;
        }
        else if ( initLocals() == false ) {
            logger.error( String.format( "Local variable initialization failed.", ""));
            return false;
        }
        else if ( initConfig() == false  ) {
            logger.error( String.format( "Application configuration initialization failed.", ""));
            return false;
        }
        else {
            logger.info( String.format( "Application initialization completed successfully.", ""));
            return true; //indicates initialization completed successfully
        }
    }


        private boolean moveFile( File sourceFile, File destFile ) throws IOException {
        //Make sure the destination folder path already exists, if not, create it
        File destParent = new File(destFile.getParent() );
        if ( destParent.exists() == false ) {
            logger.info( String.format( "Destination directory structure does not exist. Will create." , destParent.getPath() ));
            if( destParent.mkdirs() == false ) {
                logger.error( String.format( "Failed to create destination directory structure %s.", destParent.getPath() ) );
                return false;
            }
            else {
                logger.info( String.format( "Successfully created destination directory structure %s.", destParent.getPath() ) );
            }
        }
        else {
            logger.info( String.format( "Destination directory structure already exists. No need to create." , destParent.getPath() ));
        }


        if( destFile.exists() ==  false ) {
            logger.info ( String.format( "Destination File %s does not exist. Creating.", destFile.getPath() ));
            if (  destFile.createNewFile() == false ) {
                logger.error( String.format( "Failed to create destination file %s.", destFile.getPath() ) );
                return false;
            }
            else {
                logger.info( String.format( "Successfully created destinastion file %s.", destFile.getPath() ) );
            }
        }
        else {
            logger.info ( String.format( "Destination File %s already exists. Will overewrite it.", destFile.getPath() ));
          }

        FileChannel source = null;
        FileChannel destination = null;
        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
            source.close();
            destination.close();
            System.gc();
            sourceFile.delete();
            logger.info( String.format( "Successfully move %s to %s." , sourceFile.getPath(), destFile.getPath() ));
            return true;
        }
        finally {
            if ( source != null ) {
                source.close();
            }
            if ( destination != null ) {
                    destination.close();
            }
        }
    }




        private boolean copyFile( File sourceFile, File destFile ) throws IOException {
        //Make sure the destination folder path already exists, if not, create it
        File destParent = new File(destFile.getParent() );
        if ( destParent.exists() == false ) {
            logger.debug( String.format( "Destination directory structure does not exist. Will create." , destParent.getPath() ));
            if( destParent.mkdirs() == false ) {
                logger.error( String.format( "Failed to create destination directory structure %s.", destParent.getPath() ) );
                return false;
            }
            else {
                logger.debug( String.format( "Successfully created destination directory structure %s.", destParent.getPath() ) );
            }
        }
        else {
            logger.debug( String.format( "Destination directory structure already exists. No need to create." , destParent.getPath() ));
        }


        if( destFile.exists() ==  false ) {
            logger.debug( String.format( "Destination File %s does not exist. Creating.", destFile.getPath() ));
            if (  destFile.createNewFile() == false ) {
                logger.error( String.format( "Failed to create destination file %s.", destFile.getPath() ) );
                return false;
            }
            else {
                logger.debug( String.format( "Successfully created destinastion file %s.", destFile.getPath() ) );
            }
        }
        else {
            logger.debug( String.format( "Destination File %s already exists. Will overewrite it.", destFile.getPath() ));
          }

        FileChannel source = null;
        FileChannel destination = null;
        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
            source.close();
            destination.close();
            //logger.debug( String.format( "Successfully copied %s to %s." , sourceFile.getPath(), destFile.getPath() ));
            return true;
        }
        finally {
            if ( source != null ) {
                source.close();
            }
            if ( destination != null ) {
                    destination.close();
            }
        }
    }

    //execute the main task of this object
    public boolean run() {
    	
    	if ( options.has( HELP ) == true ) {
    		return printHelp();
    		
    	}
        //Set up local variables
        //Search the source root folder for any files
        logger.debug( String.format( "Searching for files under %s for program %s with directories like:%s , file names like:%s  and with recurse:%s", srcRootFolder.getPath(), programName, srcPathFilter, fileNameFilter,  Boolean.toString( true ) ));

        String srcProgramFolderPath = srcRootFolder + "/" + programName ;
        srcProgramFolder = new File( srcProgramFolderPath );
//        FileFilternew FileFilter
        Collection<?> files = FileUtils.listFiles( srcProgramFolder, null , recurse );

        logger.info( String.format( "Discovered %s files to move to staging area.", files.size() ));
        for ( Iterator<?> fit = files.iterator(); fit.hasNext();) {
            File srcFile = ( File ) fit.next();
            //skip directories
            if ( srcFile.isDirectory() ) {
                logger.debug( String.format( "Skipping file %s as it appears to be a directory"  , srcFile.getPath()));
                continue;
            
            }
            //skip not in correct directory
            else if ( srcPathPattern.matcher(srcFile.getParent()).matches() == false )  {
                logger.debug( String.format( "Skipping file %s as its parent directory path %s doesn't match the pattern %s."  , srcFile.getName()  ,  srcFile.getParent(), srcPathFilter   ));
                continue;
            }
            //skip the filename does not match the specified pattern
            else if ( fileNamePattern.matcher(srcFile.getName() ).matches()  == false )  {
                logger.debug( String.format( "Skipping file %s as its filename doesn't match the pattern %s."  , srcFile.getName()  ,  fileNamePattern.pattern()));
                continue;
            }

            logger.info( String.format( "Processing Input file %s",  srcFile.getPath()  ));
            //Get the calculated destination file path
            File dstFile = getDestinationFile( srcFile, dstRootFolder  );
           
            logger.info( String.format( "Moving Source File %s to Destination File %s", srcFile.getPath(), dstFile.getPath() ) );
           //Now move the files  to the destination directory.

            try {
                if (moveFile(srcFile, dstFile) == false) {
                    logger.error( String.format( "Failed to move file %s to %s. Reason: %s", srcFile.getPath(), dstFile.getPath()  ));
                    return false;
                }
                else {
                    logger.info( String.format( "Successfully moved file %s to %s.", srcFile.getPath(), dstFile.getPath() ));
                	fileStageCount++;
                }

            } catch (IOException ex) {
                logger.error( String.format( "Failed to move file %s to %s. Reason: %s", srcFile.getPath(), dstFile.getPath(), ex.getMessage()     ));
                return false;
            }

            //Now make an archive copy of the file
            logger.info( String.format( "Archiving file %s to %s",  dstFile.getPath(), archiveFileParentPath    ) );
            File archiveFile = new File( archiveFileParentPath + "/" + dstFile.getName()   );
            try {
                if (copyFile( dstFile, archiveFile) == false) {
                    logger.error( String.format( "Failed to archive file %s to %s.", dstFile.getPath(), archiveFile.getPath()  ));
                    return false;
                }
                else {
                    logger.info( String.format( "Successfully archived file %s at %s.", dstFile.getPath(), archiveFile.getPath() ));
                    fileArchiveCount++;
                }

            } catch (IOException ex) {
                logger.error( String.format( "Failed to archive file %s at %s. Reason: %s", dstFile.getPath(), archiveFile.getPath(), ex.getMessage()     ));
                return false;
            }
        }
        logger.info( String.format("File staging completed. Preparing to run report.", "" ));
        //Run a report
        reportStatistics();
       
        return true;
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


	private void reportStatistics() {
    	logger.info( String.format( "%s files staged successfully.", fileStageCount ));
    	logger.info(String.format(  "%s files archived successfully.", fileArchiveCount ));
    	
    	
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