/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.gearsofgeek.autosys.services;

/**
 * original version by Roger Ko
 * @author jgooch
 */

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.*;

//log4j libraries for logging
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

public class j_zip_dir {

	//workaround to make run method work correctly
	private String[] pArgs; 
	
	//logging members
	private static Logger logger = Logger.getLogger( "j_zip_dir"  );
	
	
	
	
	
	private int FileZip(File fl,ZipOutputStream s)throws IOException {
		try {
	        byte[] buf = new byte[1024];
	        String pathname = fl.getPath();
	        String filename = fl.getName();
	        System.out.println (pathname);
	        FileInputStream fis = new FileInputStream(pathname);
	        s.putNextEntry(new ZipEntry(filename));

	        int len;
	        while ((len = fis.read(buf)) > 0) {
	            s.write(buf, 0, len);
	        }
	        fis.close();
	        s.closeEntry();
	        return 0;

	    } catch (Exception e) { System.err.println(e.getMessage()); return 9; }
	}
	public int DirZip(String dir_str, ZipOutputStream s,
			String zipfilename, int flag) throws IOException {
        try {
            File dir = new File (dir_str);
            String[] children = dir.list();
            int ret_v = 0;
            for (int i=0; i<children.length; i++) {
                String filename = children[i];
                if (filename.equals(zipfilename)) {
                	continue;
                }
                File file_path = new File(dir, filename);
                if (!file_path.isDirectory()) {
                    if (flag == 9) {
                    	ret_v = FileZip(file_path, s);
                    	if (ret_v != 0) { break; }
                    }
                    else { boolean isdeleted = file_path.delete(); }
                }
            }
            s.close();
            return ret_v;
	    } catch (Exception e) { System.err.println(e.getMessage()); return 9; }
    }

	public j_zip_dir() {
		
		
	}
	
	public j_zip_dir( String[] args ) {
		pArgs = args;
		SimpleLayout layout = new SimpleLayout();
        logger.addAppender( new ConsoleAppender(layout)      );
        logger.setLevel((Level) Level.DEBUG);
        logger.info( String.format(  "Logger Successfully Initialized to event logging level %s", logger.getLevel().toString() ) );
    	logger.debug(String.format(  "%s object created.", this.getClass() ) );    	
	} 
	
	//place holder function
	public boolean initialize( String[] args ) {
		return true;
		
		
	}
	
	
	public boolean run() {
		return run( pArgs );
		
	}
	
	public boolean run( String[] args ) {
		// TODO Auto-generated method stub
        if (args.length < 2) {
            System.out.println( "Usage: java ZipDir [directory to be zipped] [zip filename] ");
            return true;
        }
        
        
        try {
        	// ------ Error Check ---------
        	int ret_val = 0;
            String dir_str = args[0];
            File dir = new File (dir_str);
            logger.debug( String.format( "Starting dir is %s", dir.getPath() ) );
            String[] children = dir.list();
            if (children == null || children.length == 0) { 
            	logger.info("No children to zip. Exiting.");	
            	return false; 
            }
            logger.debug( String.format( "%s children found to process in  %s",  children.length, dir.getPath() ) );

            int fcnt = 0;
            for (int i=0; i<children.length; i++) {
                String filename = children[i];
                File file_path = new File(dir, filename);
                if (!file_path.isDirectory()) {
                	fcnt = fcnt + 1;
                }
            }
            //if (fcnt == 0) { 
            	logger.info("No dirs  to zip. Exiting.");	
//            	return false;
        	//}

            // ---- Start Processing ------
            String zipfilename = args[1];
            Date now = new Date();
            SimpleDateFormat format =
                new SimpleDateFormat("yyyyMMddHHmmss");
            zipfilename = zipfilename + "_" + format.format(now) + ".zip";
            String zipfilepath = dir_str + zipfilename;
            //System.out.println (zipfilepath);
            ZipOutputStream s = new ZipOutputStream(
                    (OutputStream)new FileOutputStream(zipfilepath));
            j_zip_dir list = new j_zip_dir( );
            //----- Zip files ------------
            ret_val = list.DirZip(dir_str, s, zipfilename, 9);
            //s.finish();

            //----- Delete files ------------
            if ( ret_val != 0) {
            	logger.error("Zip Error!");
                File zip_path = new File(zipfilepath);
                boolean isdeleted = zip_path.delete();
                if (isdeleted) { 
                	logger.info("Zip file deleted!");
                	return true;
            	}
                else { 
                	logger.error("Zip file NOT deleted!");
                	return false;
                }
                
        	}
            ret_val = list.DirZip(dir_str, s, zipfilename, 0);
        } catch (Exception e) { 
        	logger.error( String.format( "Error Zipping Files. Reason %s.", e.getMessage() ) );
        	return false;
        }
        finally {
        }
		return true;
	}
}



