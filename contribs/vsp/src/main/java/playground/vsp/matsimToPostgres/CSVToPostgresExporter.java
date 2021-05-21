package playground.vsp.matsimToPostgres;

import org.apache.log4j.Logger;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class CSVToPostgresExporter {
    private final Logger log = Logger.getLogger(CSVToPostgresExporter.class);
    private final Connection conn;
    private final String csvFile;
    private final String databaseName;
    private final String tableName;
    private final String runID;

    private final String schema = "matsim_output";

    private final String[] stringValues = {"person", "trip_id", "mode", "start_link", "end_link", "access_stop_id", "egress_stop_id", "transit_line", "transit_route", "trip_id", "main_mode", "longest_distance_mode", "modes", "start_activity_type", "end_activity_type", "start_facility_id", "start_link", "end_facility_id", "end_link", "first_pt_boarding_stop", "last_pt_egress_stop"};
    private final String[] longValues = {"trip_number"};
    private final String[] floatValues = {"traveled_distance", "euclidean_distance", "start_x", "start_y", "end_x", "end_y", "distance"};
    private final String[] timeValues = {"dep_time", "trav_time", "wait_time"};

    Map<Integer, String> indexToColumnTypes = new HashMap<>();
    Map<Integer, String> indexToColumnNames = new HashMap<>();


    CSVToPostgresExporter(Connection conn, String csvFile, String databaseName, String runID){
        this.conn = conn;
        this.csvFile = csvFile;
        this.tableName = getTableName(csvFile);
        this.databaseName = databaseName;
        this.runID = runID;
    }

    public void export(String databaseName, String runID) throws IOException {
        // set database and the table name
        String tableName = getTableName(csvFile);
        
        // analyze csv File
        analyzeFile(csvFile);

        // create table if not exist
        createTableIfNotExists();

        // write insert statement
        String insertStatement = writeInsertStatement();

        // insert rows
        insertRows(insertStatement);

    }


    private void insertRows(String insertStatement) {
        sqlExecute(conn, insertStatement);
    }

    private void analyzeFile(String csvFile) throws IOException {
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
                indexToColumnTypes.put(i+1, "INTEGER");
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

        sqlExecute(conn, sqlCreateTable.toString());



    }


    private String writeInsertStatement() throws IOException{
        StringBuilder insertStatement = new StringBuilder("INSERT INTO " + schema + "." + tableName + " ");

        StringBuilder columnList = new StringBuilder("(");

        // Input stream for the input .gz file
        GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(csvFile));
        BufferedReader br = new BufferedReader(new InputStreamReader(gzip));

        // ToDo fst element and concat
        for (var columnName: indexToColumnNames.values()){
            columnList.append(", ").append(columnName);
        }

        columnList.append(")");
        insertStatement.append(columnList).append(" VALUES ");


        // ToDo as of snd row
        // Reads all rows, find out the datytype with the values stored in the
        // filledColumns and add quotes (string and time) or not
        String row;
        while ((row = br.readLine()) != null) {
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

                if (i == values.length - 1) {
                    appendInsertStatement.append(", ");
                }
            }
            insertStatement.append(appendInsertStatement).append("), ");
        }

        // Format the sql statement
        insertStatement = new StringBuilder(insertStatement.substring(0, insertStatement.length() - 2));
        insertStatement.append(";");

        br.close();
        return insertStatement.toString();

    }


    private String getTableName(String csvFile){
        // The table name is automatic the a part from the input file
        return csvFile.split("/")[csvFile.split("/").length - 1].split("\\.")[csvFile.split("/")[csvFile.split("/").length - 1].split("\\.").length - 3];
    }

    private Long convertTimeStringToSeconds(String timeString){
        String[] splitTime =  timeString.split(":");
        return Integer.parseInt(splitTime[0]) * 3600L + Integer.parseInt(splitTime[1]) * 60L + Integer.parseInt(splitTime[2]);

    }


    private void sqlExecute(Connection conn, String sql) {

        if (conn != null) {
            System.out.println("Connected to the database!");
        } else {
            System.out.println("Failed to make connection!");
        }

        assert conn != null;

        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.execute();
            conn.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }




    }
}
