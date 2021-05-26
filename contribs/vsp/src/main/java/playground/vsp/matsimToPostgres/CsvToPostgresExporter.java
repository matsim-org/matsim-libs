package playground.vsp.matsimToPostgres;

import org.apache.log4j.Logger;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class CsvToPostgresExporter {
    private final Logger log = Logger.getLogger(CsvToPostgresExporter.class);
    private final Connection conn;
    private final String csvFile;
    private final String tableName;
    private final String runID;
    private String overwrite; // should be final!

    private final String schema = "matsim_output";

    private final String[] stringValues = {"person", "trip_id", "mode", "start_link", "end_link", "access_stop_id", "egress_stop_id", "transit_line", "transit_route", "trip_id", "main_mode", "longest_distance_mode", "modes", "start_activity_type", "end_activity_type", "start_facility_id", "start_link", "end_facility_id", "end_link", "first_pt_boarding_stop", "last_pt_egress_stop"};
    private final String[] longValues = {"trip_number"};
    private final String[] floatValues = {"traveled_distance", "euclidean_distance", "start_x", "start_y", "end_x", "end_y", "distance"};
    private final String[] timeValues = {"dep_time", "trav_time", "wait_time"};

    Map<Integer, String> indexToColumnTypes = new HashMap<>();
    Map<Integer, String> indexToColumnNames = new HashMap<>();


    CsvToPostgresExporter(Connection conn, String csvFile, String runID, String overwrite){
        this.conn = conn;
        this.csvFile = csvFile;
        this.tableName = getTableName(csvFile);
        this.runID = runID;
        this.overwrite = overwrite;
    }

    public void export(String runID) throws IOException {

        this.overwrite = "overwriteExistingRunId"; // just for testing cases

        // Check if run_id exists
        if (checkIfSchemaExists()) {
            // Check if the table already exists
            if (checkIfTableExists()) {
                // Check if the run id column already exists
                if (checkIfRunIdColumnExists()) {
                    // Check if the run id already exists
                    if (checkIfRunIdExists()) {
                        if (overwrite.equals("failIfRunIdExists")) {
                            log.error("This Run ID already exists in " + tableName);
                        } else if (overwrite.equals("overwriteExistingRunId")) {
                            String deleteRows = "DELETE FROM " + schema + "." + tableName + " WHERE run_id='" + runID + "';";
                            sqlExecute(conn, deleteRows);
                            log.info("All columns in " + schema + "." + tableName + " with the Run ID: " + runID + " was deleted!");
                        } else {
                            log.error("The overwriteRun settings in the postgresExporterExampleConfig.xml are wrong!");
                        }
                    }
                }
            }
        } else {
            log.error("The schema: " + schema  + " doesn't exist!");
        }
        
        // set database and the table name
        String tableName = getTableName(csvFile);
        
        // analyze csv File
        analyzeFile();

        // create table if not exist
        createTableIfNotExists();

        // write insert statement
        String insertStatement = writeInsertStatement();

        // insert rows
        insertRows(insertStatement);

        log.info("Finished generate Databse for " + tableName);

    }

    private boolean checkIfTableExists() {
        String tableExistsStatement = "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_schema = '" + schema + "' AND table_name = '" + tableName + "');";
        boolean tableExists = false;

        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(tableExistsStatement);
            while(rs.next()) {
                if (rs.getObject("exists", Boolean.class)) {
                    tableExists = true;
                }
            }
        } catch (SQLException ignored) {

        }
        return tableExists;
    }

    private boolean checkIfSchemaExists() {
        String shemaExistsStatement = "SELECT schema_name FROM information_schema.schemata WHERE schema_name = '" + schema + "';";
        boolean shemaExists = false;

        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(shemaExistsStatement);
            while(rs.next()) {
                if (rs.getString("schema_name").equals(schema)) {
                    shemaExists = true;
                }
            }
        } catch (SQLException ignored) {

        }
        return shemaExists;
    }

    private boolean checkIfRunIdColumnExists() {
        String runIdColumnExistsStatement = "SELECT column_name FROM information_schema.columns WHERE table_name = '" + tableName + "'";
        boolean runIdColumnExists = false;

        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(runIdColumnExistsStatement);
            while(rs.next()) {
                if (rs.getString("column_name").equals("run_id")) {
                    runIdColumnExists = true;
                }
            }
        } catch (SQLException ignored) {

        }
        return runIdColumnExists;
    }

    private boolean checkIfRunIdExists() {
        String runIdExistsStatement = "SELECT run_id FROM " + schema + "." + tableName + ";";
        boolean runIdExists = false;

        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(runIdExistsStatement);
            while(rs.next()) {
                if (rs.getString("run_id").equals(runID)) {
                    runIdExists = true;
                    break;
                }
            }
        } catch (SQLException ignored) {

        }
        return runIdExists;
    }

    private void insertRows(String insertStatement) {
        sqlExecute(conn, insertStatement);
    }

    private void analyzeFile() throws IOException {
        // Input stream for the input .gz file
        GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(csvFile));
        BufferedReader br = new BufferedReader(new InputStreamReader(gzip));

        // Read the first row with the column names and split them at the ";"
        String columns = br.readLine();
        String[] columnsArray = columns.split(";", -1);

        // The first datatype for the run_id ist set because the run_id
        // is not included in the csv file
        indexToColumnTypes.put(0, "VARCHAR");
        indexToColumnNames.put(0, "run_id");

        // Analyze the datatypes for each column and store the type in the filledColumns Array
        for (int i = 0; i < columnsArray.length; i++) {
            String name = columnsArray[i];
            indexToColumnNames.put(i+1, name);
            if (Arrays.asList(stringValues).contains(name)) {
                indexToColumnTypes.put(i+1, "VARCHAR");
            } else if (Arrays.asList(longValues).contains(columnsArray[i])) {
                indexToColumnTypes.put(i+1, "BIGINT");
            } else if (Arrays.asList(floatValues).contains(name)) {
                indexToColumnTypes.put(i+1, "DOUBLE PRECISION");
            } else if (Arrays.asList(timeValues).contains(name)) {
                indexToColumnTypes.put(i+1, "TIME");
            } else {
                indexToColumnTypes.put(i+1, "VARCHAR");
                log.warn("Wasn't found in default column names: " + name);
            }
        }

        br.close();
    }


    private void createTableIfNotExists(){
        StringBuilder sqlCreateTable = new StringBuilder("CREATE TABLE IF NOT EXISTS " + schema + "." + tableName + " (id serial PRIMARY KEY");

        for (var indexToColumnName: indexToColumnNames.entrySet()){
            sqlCreateTable.append(", ").append(indexToColumnName.getValue()).append(" ").append(indexToColumnTypes.get(indexToColumnName.getKey()));
        }
        sqlCreateTable.append(");");

        String executeStatement = sqlCreateTable.toString().replace("TIME", "BIGINT");

        sqlExecute(conn, executeStatement);

    }


    private String writeInsertStatement() throws IOException{
        StringBuilder insertStatement = new StringBuilder("INSERT INTO " + schema + "." + tableName + " ");

        // Input stream for the input .gz file
        GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(csvFile));
        BufferedReader br = new BufferedReader(new InputStreamReader(gzip));

        String columnList = ("(" + String.join(", ", indexToColumnNames.values()) + ")").replace("TIME", "BIGINT");
        insertStatement.append(columnList).append(" VALUES ");


        // Reads all rows, find out the datytype with the values stored in the
        // filledColumns and add quotes (string and time) or not
        String row;
        boolean skipFirstRow = true;
        while ((row = br.readLine()) != null) {

            if (skipFirstRow){
                skipFirstRow = false;

            } else {
                StringBuilder appendInsertStatement = new StringBuilder("(");
                row = runID + ";" + row;
                String[] values = row.split(";", -1);

                for (int i = 0; i < values.length; i++) {
                    if (indexToColumnTypes.get(i).equals("VARCHAR")) {
                        appendInsertStatement.append("'").append(values[i]).append("'");

                    } else if (indexToColumnTypes.get(i).equals("TIME")) {
                        appendInsertStatement.append(convertTimeStringToSeconds(values[i]));

                    } else {
                        appendInsertStatement.append(values[i]);
                    }

                    if (i < values.length - 1) {
                        appendInsertStatement.append(", ");
                    }
                }
                insertStatement.append(appendInsertStatement).append("), ");
            }
        }

        // Format the sql statement
        insertStatement = new StringBuilder(insertStatement.substring(0, insertStatement.length() - 2));
        insertStatement.append(";");

        br.close();
        return insertStatement.toString();
    }

    private String getTableName(String csvFile){
        // The table name is automatic the a part from the input file
        String tableName = csvFile.split("/")[csvFile.split("/").length - 1].split("\\.")[csvFile.split("/")[csvFile.split("/").length - 1].split("\\.").length - 3];
        log.info("Tablename: " + tableName);
        return tableName;
    }

    private Long convertTimeStringToSeconds(String timeString){
        String[] splitTime =  timeString.split(":");
        return Integer.parseInt(splitTime[0]) * 3600L + Integer.parseInt(splitTime[1]) * 60L + Integer.parseInt(splitTime[2]);

    }

    private void sqlExecute(Connection conn, String sql) {
        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.execute();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
