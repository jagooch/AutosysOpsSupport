package com.psi.autosys.services;


import java.sql.*;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

//logging libraries
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

//io stuff
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;

public class Autosys_JSQL {
	private String url;
	private Connection con;
	private String serverName;
	private String portNumber;
	private String databaseName; //name of the database
	private String queryText; //SQL command to be executed
	private Boolean hasExitCode; //true if the proc has an exit or OUT variable
	private Boolean hasResultSet; //true if the proc returns a resultset
	private static String selectMethod = "cursor";
	private static Logger logger = Logger.getLogger( "com.psi.autosys.services.Autosys_JSQL" ); //class level event logger
	private static Level defaultLoggingLevel = Level.INFO;
	private static String defaultConfigFilePath = "autosys_jsql.properties";
	private static boolean integratedauthentication;
	//Command line parameters members
	private static String CONFIGFILEPATH = "c";
	private static String LOGGINGLEVEL = "l";
    private static final String HELP = "h";
    private OptionParser parser; //command line parser
    private OptionSet options; //list of recognizable ( configured )options found on the command line.

	public Autosys_JSQL(String[] args) {
		logger.addAppender(new ConsoleAppender( new SimpleLayout()));//set up for basic logging
		logger.setLevel(defaultLoggingLevel);
	    logger.info( String.format( "Basic Logging successfully intitialized with logging level %s", logger.getLevel().toString() ) );
	}
	
	

	public boolean initialize( String[] args ) {
		if ( initializeCLI(args) == false ) {
			logger.error( "Failed to initialize command line parameters." );
			return false;
		}
		else if ( initializeConfig() == false  ) {
			logger.error( "Failed to initialize application configuration"  );
			return false;
		}
		return true;
	}
	
	private boolean initializeConfig() {
		File configFile = null;
		Properties props = new Properties();
		if ( options.has(HELP) == true ) {
			return true;
		}
		if ( options.has( CONFIGFILEPATH )  ) {
			configFile = new File( (String) options.valueOf(CONFIGFILEPATH) ) ;
		} 
		else {
			configFile = new File( defaultConfigFilePath   );
		}
		logger.debug( String.format( "Configuration file set to %s.", configFile.getPath()  ) );
		
		//check in config file exist
		if ( configFile.exists() == false  ) {
			logger.error( String.format( "Configuration file %s not found.", configFile.getPath()) );
			return false;
		}
	
		
		try {
			props.loadFromXML(new FileInputStream( configFile  )  );
		} catch (InvalidPropertiesFormatException e) {
			logger.error( String.format( "InvalidPropertiesFormatException encountered while loading configuration file. Reason: %s", e.getMessage()) );
			return false;
		} catch (FileNotFoundException e) {
			logger.error( String.format( "FileNotFoundException encountered while loading configuration file. Reason: %s", e.getMessage()) );
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			logger.error( String.format( "IPException encountered while loading configuration file. Reason: %s", e.getMessage()) );
			return false;
		}

		
		if ( ( serverName = (String) props.getProperty("servername", null ) ) == null ) {
			logger.error( "Servername value not set in configuration file.");
			return false;
		}
		else if ( ( portNumber = (String) props.getProperty("port", null ) ) == null ) {
			logger.error( "Servername value not set in configuration file.");
			return false;
		}
		else if ( ( databaseName = (String) props.getProperty("databasename", null ) ) == null ) {
			logger.error( "Servername value not set in configuration file.");
			return false;
		}
		else if ( ( queryText = (String) props.getProperty("query", null ) ) == null ) {
			logger.error( "Servername value not set in configuration file.");
			return false;
		}

		integratedauthentication = Boolean.valueOf( props.getProperty("", "true" ) );
		hasExitCode = Boolean.valueOf( props.getProperty( "hasexitcode", "false" ) );
		hasResultSet = Boolean.valueOf( props.getProperty( "hasResultSet", "false" ) );
		
		return true;
	}


	private boolean printHelp() {
        try {
            parser.printHelpOn(System.out);
            return true;
        } catch (IOException ex) {
            logger.error( String.format( "IOException while  printing out help. Error: %s", ex.getMessage() )  );
            return false;
        }
	}


	private boolean initializeCLI( String[] args) {
		parser = new OptionParser() {
			{
				accepts( CONFIGFILEPATH, "Path to the configuration file." ).withRequiredArg().describedAs("Path to config file.").ofType(String.class);
				accepts( LOGGINGLEVEL, "Event Logging level." ).withRequiredArg().describedAs("Minimum level to log.").ofType(String.class);
				accepts( HELP, "Print the help text." );
			}
		};
		//initialize options
		options = parser.parse( args ); 
		//check for mandatory switches
		if ( options.has(HELP) == true ) {
			return true;
		}
		else if ( options.has(CONFIGFILEPATH) == false ) {
			logger.error("Missing mandatory argument - ConfigFile Path.");
			return false;
		}
		return true;
	}

	public boolean run() {
		if ( options.has( HELP ) == true ) {
			return printHelp();
			
		}
//		String connectString = String.format(  "jdbc:sqlserver://%s:%s;databaseName=%s;selectMethod=%s;IntegratedSecurity=true;",serverName,portNumber,databaseName,selectMethod   );


		if ( connect() == false  ) {
			logger.error( String.format( "Failed to connect to database.", "" ));
			return false;
			
		}
 		
		
		


		if ( ( hasExitCode == true ) && (  hasResultSet == false  )  ) {
			return callProcWithExitCode();
			
		}
		else if ( ( hasExitCode == false ) && (  hasResultSet == false  )  ) {
			return callProcWithoutExitCode();
		}
		else {
			logger.info( String.format( "Combination of hasExitCode=%s and hasResultSet=%s not supported", hasExitCode.toString(), hasResultSet.toString() )  );
			return false;
		}
		
	}
	
	private boolean connect() {
		String connectString = String.format(  "jdbc:sqlserver://%s:%s;databaseName=%s;IntegratedSecurity=true;",serverName,portNumber,databaseName  );
		print( "connection String is "  + connectString );
		try {
			con = java.sql.DriverManager.getConnection( connectString  );
			if ( con == null ) {
				System.out.println( " Connection failed!");
			}
			else {
				System.out.println( " COnnection successful");
			}
		}
		catch (SQLException e) {
			logger.error( String.format( "Failed to connect to database using connect string %s. Reason: ", connectString, e.getMessage()));
			e.printStackTrace();
			return false;
		} 
		return true;
	}



	private boolean callProcWithoutExitCode() {
		logger.debug(String.format( "Executing callProcWithoutExitCode()", "" ) );
		CallableStatement stmt = null;
		   try {
			   	  stmt = con.prepareCall(  String.format("%s", queryText ) );
			   	  stmt.executeUpdate();
//			   	  stmt.executeQuery();
			   	  return true;
			   }
			   catch ( SQLException e) {
			      logger.error( String.format( "SQLException Error executing sql statement %s. Reason: %s.", queryText, e.getMessage() ) )  ;
			      return false;
			   }
			   catch ( Exception e ) {
				      logger.error( String.format( "General Error executing sql statement %s. Reason: %s.", queryText, e.getMessage() ) )  ;
				      return false;
			   }
			   finally {
					if ( stmt != null) {
						try {
							
							stmt.close();
						}
						catch (SQLException e) {
							e.printStackTrace();
						}
				    }
			   }
	}



	private boolean callProcWithExitCode() {
		logger.debug(String.format( "Executing callProcWithExitCode()", "" ) );

		CallableStatement stmt = null;
	   try {
		   	  stmt = con.prepareCall(  String.format("%s", queryText ) );
		   	  stmt.registerOutParameter( 1, java.sql.Types.INTEGER );
		   	  stmt.executeUpdate();
		   	  int returnCode = stmt.getInt(1);
		   	  if ( returnCode == 1 ) {
		   		  logger.error( "Stored procedure failed. Return code " + returnCode );
		   		  return false;
		   	  }
		   	  else {
		   		  logger.info( "Stored procedure ran successfully. Return code " + returnCode );
		   	  }
		   	  return true;
		   }
		   catch ( SQLException e) {
		      logger.error( String.format( "SQLException Error executing sql statement %s. Reason: %s.", queryText, e.getMessage() ) )  ;
		      return false;
		   }
		   catch ( Exception e ) {
			      logger.error( String.format( "General Error executing sql statement %s. Reason: %s.", queryText, e.getMessage() ) )  ;
			      return false;
		   }
		   finally {
				if ( stmt != null) {
					try {
						stmt.close();
					}
					catch (SQLException e) {
						e.printStackTrace();
					}
			    }
		   }
	}


	private void print(String text) {
		System.out.println( text );
	}

}
