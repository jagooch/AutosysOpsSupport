/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.psi.autosys.project.moveit;
import java.io.FilenameFilter;
import java.io.File;
import java.io.FileInputStream;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Properties;
import org.apache.log4j.*;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.InvalidPropertiesFormatException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;




/**
 *
 * @author jgooch
 */
public class FTImport {
    private String srcPath; //path to folder where the source files are located
    private String dstPath; //path to destination folder where the source files will be copied
    private String arcPath; //path to the archive folder
    private String fileMask; //pattern that files must match to be moved
    private Pattern pattern; //compiled regex pattern
    private Matcher matcher; //regex matcher object
    private Properties props; //java properties object
    private Logger logger = Logger.getLogger(this.getClass().getName()) ;
    
    private static final String CONFIGFILEPATH="c";
    private static final String HELP="h";
    private String configFilePath;
    private OptionSet options;
    private OptionParser parser = new OptionParser() {
    	{
    		accepts( CONFIGFILEPATH , "path to configuration file.");
    		accepts( HELP , "Print out help text.");
    		
    		
    	}
    	
    	
    };

    
    
    public FTImport(String[] args ) {
    	logger = Logger.getLogger(this.getClass().getName()) ;
    	logger.setLevel(Level.DEBUG);
    	logger.addAppender( new ConsoleAppender( new SimpleLayout()));
    	
    }
    

    /**
     * @param args the command line arguments
     */
    
    /*
    public static void main(String[] args) {
        FTImport fti = new FTImport();
        if ( fti.initialize(args) == true ) {
            if ( fti.run() == true ) {
                System.exit( 0 );
                
            }
            else {
                System.exit(1);
            }
        }
           
    }
    */
    private String loglevel;

    public boolean initialize(String[] args) {
        options = parser.parse( args );
        
        if ( options.has(HELP) ) {
        	return true;
        }
        if ( options.has(CONFIGFILEPATH) )  {
            configFilePath = (String )options.valueOf(CONFIGFILEPATH);
        }
        else {
            configFilePath = "ftimport.properties";

        }
        logger = Logger.getLogger( FTImport.class.getName());
        logger.setLevel( Level.DEBUG   );
        FileInputStream fis;

        

        try {
            fis = new FileInputStream(configFilePath);
        } catch (FileNotFoundException ex) {
            logger.error( ex.getMessage() );
            return false;
        }

        props = new Properties();
        try {
            props.loadFromXML(fis);
        }
        catch (InvalidPropertiesFormatException ex) {
            logger.error( ex.getMessage());
            return false;
        }
        catch (IOException ex) {
            logger.error( ex.getMessage());
            return false;
        }


        if ( ( srcPath = props.getProperty("srcPath") ) == null  ) {
            return false;
        }
        else if ( (dstPath =  props.getProperty("dstPath") ) == null ) {
            return false;
        }
        else if ( ( arcPath = props.getProperty("arcPath") )  == null  ) {
            return false;
        }
        else if ( ( fileMask = props.getProperty("fileMask", ".*")  ) == null  ) {
            return false;
        }
        else if ( ( loglevel = props.getProperty("loglevel")  ) != null  ) {
            logger.setLevel( Level.toLevel(loglevel) );
        }

        else if ( ( pattern = Pattern.compile(fileMask) ) == null ) {
            return false;
        }
        return true;



    }

    private File[] listFiles(String srcPath, String fileMask) {
        File dir = new File( srcPath );
        return ( dir.listFiles( new  fileFilter( fileMask ) ) );


    }

    private boolean moveAndArchiveFile(String dstPath, File file) {
        //Create datetimestamp to make sure the filename is unique
        String DATE_FORMAT = "yyyyMMdd_HHmmss";
        SimpleDateFormat sdf =  new SimpleDateFormat(DATE_FORMAT);
        Calendar c1 = Calendar.getInstance(); // today
        System.out.println("Today is " + sdf.format(c1.getTime()));
        String strDate = sdf.format(c1.getTime());


        File dstDir = new File( dstPath );
        if ( dstDir.exists() == false ) {
            if ( dstDir.mkdirs() == false ) {
                return false;
            }
        }
        String basename;
        String extension;
        String newFileName;
        String fileName =  file.getName();
        Integer index = fileName.lastIndexOf(".");
        System.out.println(  String.format("Filename=%s  Index=%d ", fileName, index  )  );

        if ( index == -1 ) {
            basename = fileName;
            newFileName = String.format( "%s-%s", basename, strDate);
        }
        else {

            //String[] name = fileName.split("\\.");
            basename = fileName.substring( 0, index );
            extension =  fileName.substring(index );
            newFileName = String.format( "%s-%s%s", basename, strDate, extension );
        }

        System.out.println( "New file name is " + newFileName + " new File Path is " + dstDir.getAbsolutePath()  );
        File newFile = new File( dstDir, newFileName   );
        if (  file.renameTo( newFile ) == false ) {
            System.out.println("File rename failed." );
            return false;
        }
        System.out.println("After renaming,New File path is " + newFile.getAbsolutePath() + "\n\n\n");
        System.out.println("Archiving file +" + newFile.getAbsolutePath());
        File arcDir = new File( dstDir, "archive" );
        if ( arcDir.exists() == false ) {
            if ( arcDir.mkdirs() ) {
                logger.error("Failed to create archive directory - " + arcDir.getAbsolutePath() );
                return false;
            }
        }
        try {
            copyFile( newFile, new File(arcDir, newFile.getName()));
        } catch (IOException ex) {
            logger.error( ex.getMessage()  );
            return false;
        }
        return true;
    }



    public boolean run() {
    	if ( options.has( HELP ) ) {
    		return printHelp();
    	}
    	System.out.println("Searching " + srcPath + " for files containing " +  fileMask   );
        //See if there are any file to import
        File[] files = listFiles( srcPath, fileMask  );
        if ( files == null ) {
            System.out.println( "No files found." );
            return true;
        }
        else if ( files.length == 0 ) {
            System.out.println( files.length + "  files found." );
            return true;
        }
        else {
            for ( File file: files ) {
                if ( moveAndArchiveFile( dstPath, file   ) == false ) {
                    try {
                        logger.error(String.format("Failed to move and arhive file %s. ", file.getCanonicalPath()));
                    } catch (IOException ex) {
                        logger.error( ex.getMessage()  );
                    }

                }
            }

            return true;
        }

    }

    private boolean printHelp() {
		try {
			parser.printHelpOn(System.out);
			logger.info( "Successfully printed help text." );
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static void copyFile(File source, File dest) throws IOException {
         FileChannel in = null, out = null;
         try {
              in = new FileInputStream(source).getChannel();
              out = new FileOutputStream(dest).getChannel();

              long size = in.size();
              MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, size);

              out.write(buf);

         } finally {
              if (in != null)          in.close();
              if (out != null)     out.close();
         }
    }

	class fileFilter implements FilenameFilter {
	    String filter;
	    public fileFilter( String strFilter ) {
	        filter = strFilter;

	    }


	    public boolean accept(File dir, String name) {
	        Pattern pattern = Pattern.compile(filter);
	        Matcher matcher = pattern.matcher( name );
	        return (matcher.matches() );
	    }
	}
	
	
	
}






