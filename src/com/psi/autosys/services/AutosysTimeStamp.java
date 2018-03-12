package com.psi.autosys.services;

import java.util.Calendar;
import java.text.SimpleDateFormat;



public class AutosysTimeStamp {
	private String dateFormat;
	
	public AutosysTimeStamp(String[] args) {
		
		
	}	
	
	public boolean initialize( String[] args ) {
		if ( args == null   ) {
			return false;
			
		}
		else  if (  args.length != 1 ) {
			return false;
		}
		else {
			dateFormat = args[0];
			
		}
	
		return true;
	} 
	
	
	public boolean run() {
	    SimpleDateFormat sdf = new SimpleDateFormat( dateFormat );
	    Calendar c1 = Calendar.getInstance(); // today
	    System.out.println( String.format( "TIMESTAMP=%s", sdf.format(c1.getTime() ) ) );
	    return true;
	}
	
	
	
	
}
