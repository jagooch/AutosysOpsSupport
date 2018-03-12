/*********************************************************************
* Runs upgrade.sql script for each release, plus any additional 
*   data manipulation scripts needed to complete the release. 
*
* Master_upgrade.sql is a script that will be run once per release. 
*   For each release this script will be the one script that will 
*   be run to perform database changes. Operation order: 
*     1) call the upgrade.sql script, 
*     2) call any data manipulation scripts required for the release 
*        (i.e. data cleanup, data initialization, etc). 
*        
*   These changes were traditionally performed by hand or with 
*   the SQL Analyzer at PSI. This script will be kept under 
*   version control, but it is expected that the required data 
*   changes will change drastically from release to release, so 
*   it is expected that aside from step one (1) this script 
*   will be rewritten each day.
*********************************************************************/
