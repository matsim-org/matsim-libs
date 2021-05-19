package playground.vsp.matsimToPostgres;

import org.matsim.core.config.Config;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Arrays;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

public class PostgresExporter {


    Config config;

    PostgresExporter(Config config){
        this.config = config;
    }

    // Added default constructor
    public PostgresExporter() {
    }

    public static void main(String[] args) throws IOException {
        new PostgresExporter().export("/Users/friedrichvolkers/m1_1.output_legs.csv.gz");
    }

    public void export(String csvPath) throws IOException {

        String overwriteRun = new PostgresExporterConfigGroup().getOverwriteRun().toString();
        String runID = "007"; // String runID = config.controler().getRunId();

        String[] stringValues = {"person", "trip_id", "mode", "start_link", "end_link", "access_stop_id", "egress_stop_id", "transit_line", "transit_route", "trip_id", "main_mode", "longest_distance_mode", "modes", "start_activity_type", "end_activity_type", "start_facility_id", "start_link", "end_facility_id", "end_link", "first_pt_boarding_stop", "last_pt_egress_stop"};
        String[] longValues = {"trip_number"};
        String[] floatValues = {"traveled_distance", "euclidean_distance", "start_x", "start_y", "end_x", "end_y", "distance"};
        String[] timeValues = {"dep_time", "trav_time", "wait_time"};

        // Set the database and the table name
        // The table name is automatic the a part from the input file
        String databaseName = "testitest_schema";
        String tableName = csvPath.split("/")[csvPath.split("/").length - 1].split("\\.")[csvPath.split("/")[csvPath.split("/").length - 1].split("\\.").length - 3];

        // the run_id is already defined becuase the run_id is not included in the .csv file
        StringBuilder sqlCreateTable = new StringBuilder("CREATE TABLE IF NOT EXISTS " + databaseName + "." + tableName + " (id serial PRIMARY KEY, run_id VARCHAR"); // id serial PRIMARAY KEY,
        StringBuilder columnList = new StringBuilder("(run_id");
        StringBuilder insertStatement = new StringBuilder("INSERT INTO " + databaseName + "." + tableName + " ");

        // Input stream for the input .gz file
        GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(csvPath));
        BufferedReader br = new BufferedReader(new InputStreamReader(gzip));

        // Read the first row with the column names and split them at the ";"
        String columns = br.readLine();
        String[] columnsArray = columns.split(";", -1);

        // The filledColumns Array stores for each column the datatype
        // the first datatype is for the run_id becuase the run_id
        // is not included in the csv file
        String[] filledColumns = new String[columnsArray.length + 1];
        filledColumns[0] = "VARCHAR"; // datatype for runID

        // Analyze the datatypes for each column and store the type in the filledColumns Array
        for (int i = 0; i < columnsArray.length; i++) {
            String name = columnsArray[i];
            if (Arrays.asList(stringValues).contains(name)) {
                sqlCreateTable.append(", ").append(name).append(" VARCHAR");
                columnList.append(", ").append(name);
                filledColumns[i + 1] = "VARCHAR";
            } else if (Arrays.asList(longValues).contains(name)) {
                sqlCreateTable.append(", ").append(name).append(" LONG");
                columnList.append(", ").append(name);
                filledColumns[i + 1] = "LONG";
            } else if (Arrays.asList(floatValues).contains(name)) {
                sqlCreateTable.append(", ").append(name).append(" FLOAT");
                columnList.append(", ").append(name);
                filledColumns[i + 1] = "FLOAT";
            } else if (Arrays.asList(timeValues).contains(name)) {
                sqlCreateTable.append(", ").append(name).append(" BIGINT");
                columnList.append(", ").append(name);
                filledColumns[i + 1] = "TIME";
            } else {
                sqlCreateTable.append(", ").append(name).append(" VARCHAR");
                columnList.append(", ").append(name);
                filledColumns[i + 1] = "VARCHAR";
                System.out.println("Wasn't found in defualt column names: " + name);
            }
        }

        columnList.append(")");
        insertStatement.append(columnList).append(" VALUES ");

        // Reads all rows, find out the datytype with the values stored in the
        // filledColumns and add quotes (string and time) or not
        String row;
        while ((row = br.readLine()) != null) {
            StringBuilder appendInsertStatement = new StringBuilder("(");
            row = runID + ";" + row;
            String[] values = row.split(";", -1);
            for (int i = 0; i < values.length; i++) {
                if (i == values.length - 1) {
                    if (filledColumns[i].equals("VARCHAR")) {
                        appendInsertStatement.append("'").append(values[i]).append("'");
                    } else if (filledColumns[i].equals("TIME")) {
                        String[] splitTime =  values[i].split(":");
                        long result = Integer.parseInt(splitTime[0]) * 3600L + Integer.parseInt(splitTime[1]) * 60L + Integer.parseInt(splitTime[2]);
                        appendInsertStatement.append(result);
                    } else {
                        appendInsertStatement.append(values[i]);
                    }
                } else {
                    if (filledColumns[i].equals("VARCHAR")) {
                        appendInsertStatement.append("'").append(values[i]).append("', ");
                    } else if (filledColumns[i].equals("TIME")) {
                        String[] splitTime =  values[i].split(":");
                        long result = Integer.parseInt(splitTime[0]) * 3600L + Integer.parseInt(splitTime[1]) * 60L + Integer.parseInt(splitTime[2]);
                        appendInsertStatement.append(result).append(", ");
                    } else {
                        appendInsertStatement.append(values[i]).append(", ");
                    }
                }
            }
            insertStatement.append(appendInsertStatement).append("), ");
        }

        // Format the  sqlstatement
        sqlCreateTable.append(");");
        insertStatement = new StringBuilder(insertStatement.substring(0, insertStatement.length() - 2));
        insertStatement.append(";");

        // Execute the sql statement
        databaseConnection(sqlCreateTable.toString());
        databaseConnection(insertStatement.toString());

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

    public void databaseConnection(String sqlStatement){
        String url = "jdbc:postgresql://wedekind-matsim-analysis.c7lku8hegspn.eu-central-1.rds.amazonaws.com/testitest";
        Properties props = new Properties();
        props.setProperty("user","postgres");
        props.setProperty("password","dWS8GFklQZltTFwZmdvz");

        try {
            Connection conn = DriverManager.getConnection(url, props);
            sqlExecute(conn, sqlStatement);

        } catch (SQLException e) {
            System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void sqlExecute(Connection conn, String sql) throws SQLException {

        if (conn != null) {
            System.out.println("Connected to the database!");
        } else {
            System.out.println("Failed to make connection!");
        }

        assert conn != null;
        PreparedStatement statement = conn.prepareStatement(sql);
        statement.execute();
        conn.commit();
        conn.close();

    }
}
