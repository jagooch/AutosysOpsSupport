/*********************************************************************
* Performs a rollback of the database to the previous version. 
* Should only rollback one version.
*
* This script will “rollback” one release of the database. 
*   rollback.sql will be used in the event that a release needs 
*   to be backed out, using this script will prevent having to 
*   restore the database from a tape backup. The rollback.sql 
*   script should be updated by the person who is making changes 
*   to the create.sql scripts and the upgrade.sql script. 
*   Ideally the rollback.sql script will be updated at the same 
*   time as the other changes are made. 
*   
*********************************************************************/
