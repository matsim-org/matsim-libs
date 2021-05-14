package playground.vsp.matsimToPostgres;

import org.matsim.core.config.Config;

public class PostgresExporter {
    Config config;

    PostgresExporter(Config config){
        this.config = config;
    }

    public void export(String csvPath){


        // => Friedrich

        // Convert a csv file to a postgres table

        // Check if table exists
        // If not create table with columns and primary key ...

        // Does the table have all columns needed? Does it match the csv?
        // If not, error with comparison

        // Check if run id exists in table?
        // If failIfRunIdExists then exit
        // Otherwise drop all lines with runId

        // Copy instead of insert query
        // https://www.postgresqltutorial.com/import-csv-file-into-posgresql-table/

    }




}
