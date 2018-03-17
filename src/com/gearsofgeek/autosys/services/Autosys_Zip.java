package com.gearsofgeek.autosys.services;

//import com.sun.xml.internal.ws.util.StringUtils;
import java.util.zip.*;
import java.util.*;
import java.io.*;

//File and string manipulators
import org.apache.commons.lang.StringUtils;
import org.apache.commons.io.*;


//import joptsimple library
import joptsimple.OptionParser;
import joptsimple.OptionSet;

//import Log4j Logging
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

//random
import java.net.InetAddress;
import java.util.regex.Pattern;


//java generics
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;


/**
 *
 * @author jgooch
 */


public class Autosys_Zip {
    
    //zip related variables
    private ZipFile zip;
    private ZipFile zipFile;
    private String zipFilePath;
    private String[] sourceItems; //holds a list of source folder paths to include in the zip process.
    private ZipOutputStream zipOut;
    private boolean recurse;
    private boolean cleanfiles;
    private boolean overwrite;
    private ArrayList<File> fileList;
    private ArrayList<String> addedFiles;
    private  Collection<File> sourceFiles;
    
    //CLI Parameters
    private static enum Action { CREATE, EXTRACT, ADD, UPDATE  };
    private Action action;
    
    private String[] pArgs; //copy of the command line arguments
    //zip related arguments
    /*
    private static final String EXTRACT = "x"; //extract files from zip file
    private static final String ADD = "a"; // append files to the zip file
    private static final String TARGETFILE = "f"; // zip file to input or output files from
    private static final String SOURCEFILES = "s"; // list of files to include in the zip file
    private static final String DESTINATION = "d"; // destination folder for extracted files
    private static final String INCLUDE_MASK = "inc"; //incoming file name inclusion mask
    private static final String EXCLUDE_MASK = "exc"; //incoming file name exlusion mask
	*/
    
    private static final String RECURSE ="r"; //recurse subdirectories or not
    private static final String LOGGINGLEVEL = "l"; //event level to log
    private static final String HELP = "help"; //print help text
    private static final String ACTION = "a"; //create a new zip file
    private static final String SOURCEITEMS = "s"; // root folder for the file compression process 
    private static final String ZIPFILE = "z"; // zip file for extracted files
    private static final String DESTINATIONFOLDER = "d"; // destination folder for extracted files
    private static final String CLEANFILES = "c"; //clean up sources files.
    private static final String OVERWRITE = "o"; //overwrite existing files
    

    //logging members
    private static Logger logger = Logger.getLogger("Autosys_Zip");
    private static Level defaultLoggingLevel = Level.DEBUG;
    private static Level loggingLevel;

    //Option Parsing items
    private OptionParser parser; //command line parser
    private OptionSet options; //list of recognizable ( configured )options found on the command line.

    
    
    /**
     * @param args the command line arguments
     */
    
    //contructor with args
    public Autosys_Zip( String [] arguments ) {
        pArgs = arguments;
        SimpleLayout layout = new SimpleLayout();
        logger.addAppender( new ConsoleAppender(layout)      );
        logger.setLevel( defaultLoggingLevel   );
        logger.info( String.format(  "Logger Successfully Initialized to event logging level %s", logger.getLevel().toString() ) );
    	logger.debug(String.format(  "%s object created.", this.getClass() ) );    	
    }

    public Autosys_Zip() {
        SimpleLayout layout = new SimpleLayout();
        logger.addAppender( new ConsoleAppender(layout)      );
        logger.setLevel( defaultLoggingLevel );
        logger.info( String.format(  "Logger Successfully Initialized to event logging level %s", logger.getLevel().toString() ) );
    	logger.debug(String.format(  "%s object created.", this.getClass() ) );    	
    }

    private boolean initConfig() {
    	sourceItems =  ( (String) options.valueOf( SOURCEITEMS ) ).split(";"); 
    	return true;
    }

    public boolean initialize(String[] args) {
        if ( initCLI() == false ) {
            logger.error("Failed to initialize the command line.");
            printHelp();
            return false;
        }
        else if ( initLocals() == false ) {
            logger.error("Failed to initialize local variables.");
            return false;
        }
        else if ( initConfig() == false) {
            logger.error("Failed to initialize the configuration.");
            return false;
        }
        else {
            logger.info( "Application initialized successfully.");
            return true;
        }
        
    }
    
    
        //initialize local variables
    private boolean initLocals()	{
        fileList = new ArrayList<File>();
        addedFiles = new ArrayList<String>();
        return true;

    }
    
    //parse the command line, check for help and mandatory parameters
    private boolean initCLI(){
            parser = new OptionParser() {
            {
                accepts( ACTION, "Action to perform.").withRequiredArg().describedAs("Options are - CREATE, EXTRACT, ADD, UPDATE.").ofType(String.class) ;
                accepts( ZIPFILE, "Name of zip file." ).withRequiredArg().describedAs( "Zip File Name" ).ofType( String.class ); 
                accepts( RECURSE, "Search subdirectories for files matching pattern." );
                accepts( HELP, "Print help text." );
                accepts( LOGGINGLEVEL, "Minimum level of event to log" ).withRequiredArg().describedAs("Options are DEBUG,INFO,ERROR,CRITICAL."); 
                accepts( SOURCEITEMS, "Semicolon separated list of pahts to source dirs and files." ).withRequiredArg().describedAs("Fully qualified path.").ofType(String.class);
                accepts( ZIPFILE, "Name of the zip file" ).withRequiredArg().describedAs("Full path/name of zip file.").ofType(String.class);
                accepts( DESTINATIONFOLDER, "Path to the destination folder." ).withRequiredArg().describedAs("Fully qualified path.").ofType(String.class);
                accepts( CLEANFILES, "Option to clean up leftover files." );
                accepts( OVERWRITE , "Option to overwrite existing files." );
                
                //                accepts( EXTRACT, "Extract files from the zip file."  );
//                accepts( ADD, "Add file(s) to the zip file." ); 
//                accepts( SOURCEFILES, "List/Name Pattern of file to append to the zip file." ).withRequiredArg().describedAs( "sourceFiles" ).ofType( String.class );
//                accepts( DESTINATION, "Path to folder where files will be extracted to." ).withRequiredArg().describedAs("destPath").ofType( String.class) ;
            }
        };
        options = parser.parse( pArgs );

        if( options.has( LOGGINGLEVEL )  == true ) {
        	logger.setLevel(Level.toLevel((String) options.valueOf(LOGGINGLEVEL) ) );
        	
        }
        
        
        
        //Now determine which operation has been selected, and make sure required arguments are there.
        if ( options.has( HELP ) == true ) {
        	return true;
        }
        else if ( options.has( ACTION ) == false ) {
        	logger.error( String.format( "%s argument required.", ACTION) );
        	printHelp();
        	return false;
        }
        else if ( options.hasArgument(ACTION) == false ) {
        	logger.error( String.format( "%s requires an Action.", ACTION ) );
        	printHelp();
        	return false;
        }
    		
    		
    		
		String strAction = (String) options.valueOf(ACTION);
		if ( strAction.equalsIgnoreCase("CREATE")  ) {
			action = Action.CREATE ;
		}
		else if ( strAction.equalsIgnoreCase("UPDATE")  ) {
   			action = Action.UPDATE ;
		}
		else if ( strAction.equalsIgnoreCase("ADD")  ) {
   			action = Action.ADD ;
		}
		else if ( strAction.equalsIgnoreCase("EXTRACT")  ) {
   			action = Action.EXTRACT ;
		}
		else {
			logger.error( String.format( "%s is not a recognized action.", strAction ) );
			printHelp();
			return false;
		}
		
		switch ( action  ) {
		case CREATE: 
            if( options.has( ZIPFILE ) == false ) {
                logger.error( "Target Zip File name must be given." );
                return false;
            }
            else if ( options.has( SOURCEITEMS) == false ){
                logger.error( "Source file path/pattern required." );
                return false;
            }
            else if ( options.has( DESTINATIONFOLDER) == false ){
                logger.error( "Destination file path/pattern required." );
                return false;
            }
            break;
		default:
			logger.error( "Requested action not yet implemented."   );
			return false;
        }
   		
        recurse = options.has( RECURSE );
        cleanfiles = options.has( CLEANFILES );
        overwrite = options.has( OVERWRITE );
		return true;
    }
    
    
    
    
    private boolean initFilters() {
        logger.debug("Filters Initialized.");
        return true;
    }

    
    
    public boolean run() {
    	if ( options.has( HELP )) {
    		return printHelp();
    	}
    	
    	
    	if ( action == Action.CREATE ) {
        	logger.debug("Action is CREATE zip file.");
        	zipFilePath = (String) options.valueOf(DESTINATIONFOLDER) + "/" + (String) options.valueOf(ZIPFILE) ;
        	if ( create() == true ) {
        		logger.info(String.format( "Successfully created zip file %s", zipFilePath )  );
        		return true;
        	} 
        	else {
        		logger.error(String.format( "Failed to create zip file %s", zipFilePath )  );
        		return false;
        		
        	}
        }
        else if ( action == Action.EXTRACT ) {
        	logger.error( String.format( "%s action not supported.", action ) );
            return false;
            
        }
        else if ( action == Action.ADD)  {
        	logger.error( String.format( "%s action not supported.", action ) );
            return false;
        }
        else if ( action == Action.UPDATE)  {
        	logger.error( String.format( "%s action not supported.", action ) );
            return false;
        }
        else {
        	logger.error( String.format( "%s action not supported.", action ) );
            return false;
        }
    }
    
    
    
    private boolean create() {
    	//get the source directories/files path, destination dir path, and zip file name
    	//String  srcDirPath = ( String ) options.valueOf(SOURCEFOLDER);
    	ArrayList<String> sourceFiles = new ArrayList<String>();
    	String dstDirPath = (String ) options.valueOf(DESTINATIONFOLDER);
    	String zipFileName = ( String ) options.valueOf(ZIPFILE);
    	
    	for ( String item: sourceItems ) {
    		File tmpFile = new File( item  );
    		if ( tmpFile.isDirectory()) {
    	    	//First get a list of source files from the source dir
    	    	 Collection<File> tmpColl = FileUtils.listFiles( tmpFile , null , recurse );
    	    	 for ( Iterator<File> cit = tmpColl.iterator();cit.hasNext() ;) {
    	    		 	sourceFiles.add(  cit.next().getPath()  );
    	    	 }
    		}
    		else {
    			sourceFiles.add( tmpFile.getPath() );
    		}
    	}    	
    	
    	if (  sourceFiles.size() == 0 ) {
    		logger.info( String.format( "%d file found to process. Nothing to do...", sourceFiles.size()  )  );
    		return true;
    	}
    	
    	//remove any duplicates from the list
    	HashSet<String> hashSet  = new HashSet<String>( sourceFiles );
    	
    	ArrayList<String> cleanSourceFiles = new ArrayList<String>(hashSet);
    	
    	
       //now create the zipFile output stream
    	 try {
			zipOut = new ZipOutputStream( new FileOutputStream( dstDirPath + File.separator + zipFileName ) );
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			logger.error( String.format( "FileNotFoundException while creating output zip file. Reason: %s ", e.getMessage() )  );
			e.printStackTrace();
			return false;
		}
		catch ( Exception e  ) {
			logger.error( String.format( "Exception while creating output zip file. Reason: %s ", e.getMessage() )  );
			e.printStackTrace();
			return false;
		}
		
		//Loop through source files and add them to the zip outputstream
        for (Iterator<String> iterator = cleanSourceFiles.iterator(); iterator.hasNext();) {
            File file = new File( (String ) iterator.next() );
            System.out.println("Calling addzip for File = " + file.getAbsolutePath());
            if ( addToZip(file.getPath(), zipOut ) == false ) {
            	logger.error( String.format( "Failed to add file %s to zip file %s", file.getPath(), zipFilePath  ));
            	return false;
            }
        }
 
        try {
			zipOut.flush();
		} catch (IOException e) {
			logger.error( String.format( "Failed to flush the zip outputstream cache. Reason: %s", e.getMessage() ));
			e.printStackTrace();
			return false;
		}
		catch ( Exception e  ) {
			logger.error( String.format( "Exception while flushing output zip file. Reason: %s ", e.getMessage() )  );
			e.printStackTrace();
			return false;
		}
		
        try {
			zipOut.close();
		} catch (IOException e) {
			logger.error( String.format( "Failed to close the zip outputstream cache. Reason: %s", e.getMessage() ));
			e.printStackTrace();
			return false;
		}
		catch ( Exception e  ) {
			logger.error( String.format( "Exception while closing output zip file. Reason: %s ", e.getMessage() )  );
			e.printStackTrace();
			return false;
		}
		
		
        //check if source files should be deleted or not
		if( cleanfiles == true ) {
			if ( deleteFiles(  cleanSourceFiles   ) == false ) {
				logger.error( String.format( "Failed to remove source files", "" )  );
				return false;
			}
			
		}
		
		
    	 return true;
	}

	private boolean deleteFiles(ArrayList<String> targetFiles) {
		for ( Iterator<String> fit = targetFiles.iterator(); fit.hasNext();){
			String tmpFilePath = (String) fit.next();
			File delFile = new File( tmpFilePath );
			delFile.delete();
			delFile = null;
			System.gc();
			if ( new File( tmpFilePath ).exists() == true )  {
				logger.error( String.format( "Failed to delete source file %s.", tmpFilePath )  );
				return false;
			}
			else {
				logger.debug(String.format( "Successfully deleted source file %s.", tmpFilePath ) );
			}
		}
		return true;
	}

	private boolean createZipFile(String srcFilePath, ZipOutputStream zipOut2 ) {
    	//First make sure there is something to do
    	File srcFolder = new File( srcFilePath );
    	File[] srcFiles = srcFolder.listFiles(); //gets list of  files and folders   
    	//exit if no files or directories are found.
    	if ( srcFiles == null ) {
    		logger.info( String.format( "No files or folders found in folder %s to zip.", srcFolder.getPath() ) );
    		return true;
    	}
    	//if there are items to process, get started
    	else {
    		for ( File srcFile: srcFiles ) {
    			//if the file is a directory, then process that directory
    			if ( srcFile.isDirectory() == true  ) {
    				if ( recurse == true ) {
    					createZipFile( srcFile.getPath(), zipOut2  );
    				}
				}
    			//otherwise add the file to the zip file
    			else if ( ( srcFile.isFile() == true ) )  {
    				if ( new File( zipFilePath ).getName().equalsIgnoreCase(srcFile.getName() ) ) {
    					continue;
    				}
    				
    				logger.debug( String.format( "Preparing to add %s to zip file %s", srcFile.getPath(), zipFilePath  ));
    				if ( addToZip( srcFile.getPath(), zipOut2  ) == false ) {
    					 return false;
    				 }
    				 else {
    					 addedFiles.add( srcFile.getPath());
    					 logger.debug( String.format(  "File %s added to deletion list.", srcFile.getPath() ) );
    					 
    				 }
    			}
    		}
    		return true;
    	}
    }
    	
    	

	private boolean addToZip( String inputFilePath, ZipOutputStream zos) {
	   // Create a buffer for reading the files
	    byte[] buf = new byte[1024];
	    FileInputStream in  = null;
		try {
	        // Compress the file
            in = new FileInputStream( inputFilePath );
 
            // Add ZIP entry to output stream.
            zos.putNextEntry(new ZipEntry( inputFilePath ));
    
            // Transfer bytes from the file to the ZIP file
            int len;
            while ((len = in.read(buf)) > 0) {
            	zos.write(buf, 0, len);
            }
    
            // Complete the entry
            zos.closeEntry();
            in.close();
	    
	        // Complete the ZIP file
            //zipOut2.close();
	        logger.info( String.format( "File %s successfully added to zip file %s.", inputFilePath, zos));
	        return true;
	        
	    } 
		catch( ZipException e ) {
	    	logger.error( String.format( "ZipException encountered. Failed to add %s file to zip file %s. Reason:", inputFilePath, zos, e.getMessage()  ));
	    	e.printStackTrace();
	    	return false;
		}
		
		catch (IOException e) {
	    	logger.error( String.format( "Failed to add %s file to zip file %s. Reason:", inputFilePath, zos, e.getMessage()  ));
	    	e.printStackTrace();
	    	return false;
	    }
	    
	}

	private void filterFiles( File startDir, String[] filePattern ) {
        logger.debug(  String.format("Searching startDir %s for files matching pattern %s", startDir.getAbsolutePath(), filePattern[0] ) );
        //loop through the file patterns to find matching files
        for ( int i=0; i<filePattern.length;i++){
            File[] files =  startDir.listFiles( new GenericFileFilter(filePattern));   
            logger.debug( String.format( "I found %d files in directory %s.", files.length, startDir.getAbsolutePath()));
            for( int x=0;x< files.length; x++ ) { 
                if ( files[x].isDirectory() && recurse == true ) {
                    if ( files[x].exists() ) {
                        logger.debug(  String.format( "%s is a directory. Adding...", files[x].getName() ) );
                        filterFiles( files[x], filePattern );
                    }
                }
                else if( files[x].isFile() ) {
                    if ( files[x].exists() ) {
                        logger.debug(  String.format( "%s is a file.", files[x].getName() ) );
                        fileList.add( files[x] );
                    }
                }
                else {
                    logger.debug(  String.format( "i don't know what %s is or recurse is off. Recurse:%s ", files[x].getName(), recurse ) );

                }

            }
        }
    }
    
    public class GenericFileFilter implements FilenameFilter {
        private TreeSet<String> exts = new TreeSet<String>() ;

        public GenericFileFilter(String[] pattern) {
          Iterator<String> patternList = Arrays.asList(pattern).iterator();
          while (patternList.hasNext()) { 
            exts.add( patternList.next().toLowerCase().trim());
          }
          exts.remove("");
        } 

        public boolean accept(File dir, String name) {
          final Iterator<String> patternList = exts.iterator();
          while (patternList.hasNext()) {
              String regex = wildcardToRegex(patternList.next());
              logger.debug(String.format( "Matching %s against %s",  name, regex ));
              if ( Pattern.matches(regex, name) ) {
                return true;
              }
          }
          return false;
        }
    }
    
        public static String wildcardToRegex(String wildcard){
        StringBuffer s = new StringBuffer(wildcard.length());
        s.append('^');
        for (int i = 0, is = wildcard.length(); i < is; i++) {
            char c = wildcard.charAt(i);
            switch(c) {
                case '*':
                    s.append(".*");
                    break;
                case '?':
                    s.append(".");
                    break;
                    // escape special regexp-characters
                case '(': case ')': case '[': case ']': case '$':
                case '^': case '.': case '{': case '}': case '|':
                case '\\':
                    s.append("\\");
                    s.append(c);
                    break;
                default:
                    s.append(c);
                    break;
            }
        }
        s.append('$');
        return(s.toString());
    }

    private boolean printHelp() {
        try {
            parser.printHelpOn(System.out);
            return true;
        } catch (IOException ex) {
            logger.error( String.format("Failed to print help. Error: %s", ex.getMessage() ) );
            return false;
        }

    }
        
    
}
