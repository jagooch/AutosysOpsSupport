package com.psi.autosys;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;
import java.io.*;
import java.util.Enumeration;
import java.util.regex.*;

public class AutosysApplication {
	private static String className;
	private ArrayList<String> lArgs;
	private static File jarFile; 
	
	
	public AutosysApplication( String[] args ) {
		
		/*
		System.out.println(String.format( "Initializing Autosys Application with %d args:", args.length ))  ;
		for ( String parameter : args ) {
			System.out.println( String.format( "%s", parameter  )   );
		}
		System.out.println(String.format( "Converting args to ArrayList", ""))  ;
		*/
		lArgs = new ArrayList<String>(Arrays.asList( args  )); 
//		System.out.println(String.format( "After conversion lArgs looks like:", ""))  ;

		
		try {
			jarFile = new File( com.psi.autosys.AutosysApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI() );
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args)  {
		AutosysApplication aa = new AutosysApplication(args);
		if ( aa.run() == false ) {
			exitWithFail();
		}  
		else {
			exitWithSuccess();
		}
	}
	
	private static void exitWithFail() {
		System.exit(1);
	}
	
	private static void exitWithSuccess() {
		System.exit(0);
	}
	
	private boolean runapp(String className, String[] args) {
		Object obj = null;
		Method init = null;
		Method run = null;;
		Object[] arg = { args };
		try {
			Class<?> c = Class.forName(className);
			Class<?>[] paramTypes = { String[].class  };
			Constructor<?> cons = c.getConstructor( paramTypes  );
			obj = cons.newInstance( arg );
			System.out.println( "I just found the constructor." + cons.getName() );
			System.out.println("I just made a new " + obj.getClass().getName()  );
			init = c.getMethod("initialize", String[].class );
			run = c.getMethod("run" );
		} catch (SecurityException e) {
			System.out.println( String.format( "SecurityException Exception in Runapp.", e.getMessage() ) );
			e.printStackTrace();
			return false;
		} catch (IllegalArgumentException e) {
			System.out.println( String.format( "IllegalArgument Exception in Runapp. Reason: %s", e.getMessage() ) );
			e.printStackTrace();
			return false;
		} catch (ClassNotFoundException e) {
			System.out.println( String.format( "ClassNotFoundException encountered. Class %s not found. Reason: %s",className, e.getMessage()  ) );
			e.printStackTrace();
			return false;
		} catch (NoSuchMethodException e) {
			System.out.println( String.format( "NoSuchMethodException Exception in Runapp. Reason: %s", e.getMessage() ) );
			e.printStackTrace();
			return false;
		}/*
		catch (InstantiationException e) {
			System.out.println( String.format( "InstantiationException Exception in Runapp. Reason: %s", e.getMessage() ) );
			e.printStackTrace();
			return false;
		}
		catch (IllegalAccessException e) {
			System.out.println( String.format( "IllegalAccessException Exception in Runapp. Reason: %s", e.getMessage() ) );
			e.printStackTrace();
			return false;
		}
		catch (InvocationTargetException e) {
			System.out.println( String.format( "InvocationTargetException Exception in Runapp. Reason: %s", e.getMessage() ) );
			e.printStackTrace();
			return false;
		}*/
		catch ( Exception e ) {
			System.out.println( String.format( "General Exception in Runapp. Reason: %s", e.getMessage() ) );
			e.printStackTrace();
			return false;
			
		}

		try {
			/*
			System.out.println( "Preparing to call " + obj.getClass().getName() + " with the following parameters:");
			for ( String parameter : args ) {
				System.out.println( String.format( "%s", parameter  )   );
			}
			*/
			if ( (Boolean ) init.invoke( obj  , new Object[] { args } ) == false ) {
				System.out.println( String.format( "Init method failed. for %s", className));
				return false;
			}
			else if (  (Boolean ) run.invoke( obj ) == false ) {
				System.out.println( String.format( "Run method failed. for %s", className ));
				return false;
			}
		} catch (IllegalArgumentException e) {
			System.out.println( String.format( "IllegalArgument Exception in Runapp. Reason: %s", e.getMessage() ) );
			e.printStackTrace();
			return false;
		} catch (IllegalAccessException e) {
			System.out.println( String.format( "IllegalAccessException Exception in Runapp. Reason: %s", e.getMessage() ) );
			e.printStackTrace();
			return false;
		} catch (InvocationTargetException e) {
			System.out.println( String.format( "InvocationTargetException Exception in Runapp. Reason: %s", e.getMessage() ) );
			e.printStackTrace();
			return false;
		}
		System.out.println( String.format( "Successfully ran %s. Exiting runapp.", obj.getClass().getName()  ) );
		return true;
		
	}
	
	private boolean printHelp( String[] args ) {
		ArrayList<String> classes = new ArrayList<String>();
		JarFile myJarFile = null;
		if( jarFile.isFile() == false ) {
			try {
				myJarFile = new JarFile(  new File( "build/AutosysOpsSupport.jar" ) );
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			} 
		}else {
			try {
				myJarFile = new JarFile(  jarFile  );
			} catch (IOException e) {
				
				e.printStackTrace();
				return false;
			}
		}
		
		Enumeration<JarEntry> jarEntries = myJarFile.entries();
		System.out.println( String.format( "%s jarfile contains %s entries." ,  myJarFile.getName(), myJarFile.size() ) );
		while (jarEntries.hasMoreElements()) {
			JarEntry jarEntry = (JarEntry) jarEntries.nextElement();
			String fileName = jarEntry.getName();
			System.out.println( String.format( "Processing %s", fileName )   );
			Pattern pattern = Pattern.compile("(.*/[a-zA-Z_]+)\\.class$");
			Matcher matcher = pattern.matcher(fileName);
			while ( matcher.find() == true ) {
				System.out.println( String.format( "%s matched. Adding." , matcher.group(1)) );
				classes.add( matcher.group(1));
			}
		}
		System.out.println( String.format( "Found a total of %d classes to print help on.", classes.size() )   );

		Iterator<String> sit = classes.iterator();
		while ( sit.hasNext()) {
			String helpClassName = sit.next().replace("/", ".");
			System.out.println(  String.format( "Processing %s for printHelp.", helpClassName ) );
			System.out.println(  String.format( "Checkif if %s name is the same as %s", helpClassName, this.getClass().getName() ) );
			if ( helpClassName.indexOf( this.getClass().getName() ) == -1 ) {
				System.out.println( String.format( "Now printing help for Class Name %s.", helpClassName ) );
				if ( runapp( helpClassName, new String[]{ "-h"  } ) == false ) {
					System.out.println( String.format( "Failed printing help for %s",  helpClassName) );
					return false;
				}
				else {
					System.out.println( String.format( "Successfully  printed help for %s",  helpClassName) );
				}
			}
			else {
				System.out.println( String.format( "Not printing help for calling class. %s",  helpClassName) );
			}
			
		}
		System.out.println( String.format( "Finished printing help", "") );
		return true;
	}
	
	private boolean run() {
		className = lArgs.get(0)  ; //store the target class list
		lArgs.remove(0); //remote the class name from the argument array
		String[] pArgs = lArgs.toArray( new String[0]  );
		if ( className.equalsIgnoreCase( "CLASSHELP" )  == true ) {
			if ( printHelp( pArgs ) == true ) {
				return true;
			}
			else {
				return false;
			}
		}
		else if ( runapp( className, pArgs ) == true ) {
			return true;
		}
		else {
			return false;
		}
	} 
}