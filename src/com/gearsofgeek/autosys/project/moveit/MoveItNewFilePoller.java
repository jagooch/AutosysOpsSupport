/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.gearsofgeek.autosys.project.moveit;


import com.stdnet.moveit.api.*;
import java.util.HashMap;
import java.util.Iterator;



public class MoveItNewFilePoller {

    private Client cli;
    private String host;
    private String username;
    private String password;
    private String port;

    /**
     * @param args the command line arguments
     */
    
    /*
    public static void main(String[] args) {
        MoveItNewFilePoller mp = new MoveItNewFilePoller();
        mp.run();

        // TODO code application logic here
    }


*/
    public MoveItNewFilePoller( String[] args ) {
        cli = new Client();
        host = "10.3.3.239";
        username = "dev";
        password = "testme1!";
        cli.secure(true);
        cli.ignoreCertProbs(true);

    }


    
    public boolean initialize( String[] args ) {
    	return true;
    }
    
    
    //Connects to the MoveIT server
    private boolean connect() {
        cli.host(host);
        if ( cli.signon(username, password) == false ) {
            System.out.println("Failed to log in");
            return false;
        }
        else {
            System.out.println("Successfully logged in.");
            return true;
        }
    }

    private boolean getNewFiles() {
        System.out.println("Finding new files\n");
        HashMap newFiles = cli.findNewFiles("*.*");
        System.out.println(String.format( "New File count = %s" , newFiles.size()  ));

        if ( (newFiles == null  ) == true ) {
            return false;
        }
        else {
            for ( Iterator fit = newFiles.keySet().iterator();fit.hasNext();) {
                String strNewFile = (String) fit.next();
                System.out.println(String.format( "New File name = %s" ,  strNewFile ));
            }
        }


        return true;
    }

    private boolean getFiles() {
        System.out.println("Finding all files...\n");
        HashMap files = cli.findFiles("*.*");
        if ( (files == null  ) == true ) {
            return false;
        }
        else {
            for ( Iterator fit = files.keySet().iterator();fit.hasNext();) {
                String strNewFile = (String) fit.next();
                System.out.println(String.format( "File name = %s" ,  strNewFile ));
            }
        }


        return true;
    }



    private boolean sendMessage() {
        System.out.println( String.format("Entering Sendmessage", "") );
        //MOVEitMessageInfo msg =  cli.composeNewMessage("MoveIT Test", "jgooch@policy-studies.com", "Test message from MoveIT Java API");
        MOVEitMessageInfo msg =  cli.composeNewMessage( "MoveIT Test", "jgooch", "Test message from MoveIT Java API");

        if ( cli.sendMessage(msg) == true ) {
            System.out.println( String.format("Success: Message successfully sent - %s", msg.subject() ) );
            return true;

        }
        else {
            System.out.println( String.format("Failure: Message send failed - %s", cli.statusDescription()  ) );
           
            return false;
        }

    }




    private boolean getNewMessages() {
       HashMap newMessages = cli.listNewMessages();
        if ( ( newMessages == null ) == true )  {
            return false;
        }
        else {
            System.out.println( String.format( "%s new messages found.", newMessages.size() ) );
            for ( Iterator mit = newMessages.keySet().iterator(); mit.hasNext(); ) {
                String message = (String) mit.next();
                System.out.print(String.format( "Found message with subject %s.", message  ) );

            }
            return true;
        }

    }

    //Starts the file transfer process
    public boolean run() {
        System.out.println("Starting the run function");
        if ( 1==1  ) {
            return true;
        }
        
        //Connect to the MoveIt server
        if ( connect() == false ) {
            return false;
        }
        else {

            getNewMessages();
            getNewFiles();
            getFiles();
            sendMessage();
             return true;
        }
    }
    


}
