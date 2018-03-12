/*********************************************************************
* Determines the current database version and performs necessary 
*   iterative upgrades to the database to get it to the desired 
*   version.
*
* The upgrade.sql file is a cumulative script that should contain 
*   all necessary database changes for each release. This script 
*   could be run on the initial database setup for an application, 
*   and be able to update the database correctly to any version 
*   of the application. Additionally, the upgrade.sql script 
*   should be able to take parameters from the command line. In 
*   the case that there are no parameters its default behavior 
*   should be to determine the current database version and 
*   automatically upgrade it to the release version. If 
*   parameters are given on the command line, the script should 
*   upgrade from version X to version Y.
*
*********************************************************************/
