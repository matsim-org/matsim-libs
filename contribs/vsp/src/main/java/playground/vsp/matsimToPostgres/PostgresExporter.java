package playground.vsp.matsimToPostgres;

import org.matsim.core.config.Config;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
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

        //String runID = config.controler().getRunId();

        String tableName = csvPath.split("/")[csvPath.split("/").length - 1].split("\\.")[csvPath.split("/")[csvPath.split("/").length - 1].split("\\.").length - 3];
        String sqlCreateTable = "CREATE TABLE IF NOT EXISTS testitest_schema." + tableName + " (id serial PRIMARY KEY, "; // id serial PRIMARAY KEY,
        String columnList = "(";
        String insertStatement;
        String outputCsv = csvPath.substring(0, csvPath.length() - 3);

        decompressGzip(Paths.get(csvPath), Paths.get(outputCsv));

        GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(csvPath));
        BufferedReader br = new BufferedReader(new InputStreamReader(gzip));

        // get column names
        String columns = br.readLine();
        String firstRow = br.readLine();

        String[] columnsArray = columns.split(";", -1);
        String[] firstRowArray = firstRow.split(";", -1);
        
        String[] types = new String[columnsArray.length];

        // Analysis of the first line with values to define the data types
        for (int i = 0; i < firstRowArray.length; i++) {
            if (i != firstRowArray.length - 1) {  // -1
                if (isInteger(firstRowArray[i])) {
                    sqlCreateTable = sqlCreateTable + columnsArray[i] + " VARCHAR, ";
                    columnList = columnList + columnsArray[i] + ", ";
                    types[i] = "VARCHAR";
                } else if (isFloat(firstRowArray[i])) {
                    sqlCreateTable = sqlCreateTable + columnsArray[i] + " VARCHAR, ";
                    columnList = columnList + columnsArray[i] + ", ";
                    types[i] = "VARCHAR";
                } else {
                    sqlCreateTable = sqlCreateTable + columnsArray[i] + " VARCHAR, ";
                    columnList = columnList + columnsArray[i] + ", ";
                    types[i] = "VARCHAR";
                }
            } else {
                if (isInteger(firstRowArray[i])) {
                    sqlCreateTable = sqlCreateTable + columnsArray[i] + " VARCHAR)";
                    columnList = columnList + columnsArray[i] + ")";
                    types[i] = "VARCHAR";
                } else if (isFloat(firstRowArray[i])) {
                    sqlCreateTable = sqlCreateTable + columnsArray[i] + " VARCHAR)";
                    columnList = columnList + columnsArray[i] + ")";
                    types[i] = "VARCHAR";
                } else {
                    sqlCreateTable = sqlCreateTable + columnsArray[i] + " VARCHAR)";
                    columnList = columnList + columnsArray[i] + ")";
                    types[i] = "VARCHAR";
                }
            }
        }

        insertStatement = "INSERT INTO testitest_schema." + tableName + " " + columnList + " VALUES ";

        String[] values;
        String combinedValues;

        int test = 0;

        do {
            values = firstRow.split(";", -1);
            combinedValues = "";
            test++;
            for (int i = 0; i < values.length; i++) {
                if (i == 0) {
                    if (types[i].equals("VARCHAR")) {
                        combinedValues = combinedValues + "('" + values[i] + "', ";
                    } else {
                        combinedValues = combinedValues + "(" + values[i] + ", ";
                    }
                } else if (i == values.length - 1) {
                    if (types[i].equals("VARCHAR")) {
                        combinedValues = combinedValues + "'" + values[i] + "'), ";
                    } else {
                        combinedValues = combinedValues + "" + values[i] + "), ";
                    }
                } else {
                    if (types[i].equals("VARCHAR")) {
                        combinedValues = combinedValues + "'" + values[i] + "', ";
                    } else {
                        combinedValues = combinedValues + "" + values[i] + ", ";
                    }
                }
            }
            insertStatement = insertStatement + combinedValues;
            if (test == 10) {
                break;
            }
        } while ((firstRow = br.readLine()) != null);

        insertStatement = insertStatement.substring(0, insertStatement.length() - 2);
        insertStatement = insertStatement + ";";

        databaseConnection(sqlCreateTable);
        databaseConnection(insertStatement);

        System.out.println(insertStatement);

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

    public static boolean isFloat(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch(NumberFormatException e){
            return false;
        }
    }

    public static boolean isInteger (String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch(NumberFormatException e){
            return false;
        }
    }

    public static void decompressGzip(Path source, Path target) throws IOException {

        try (GZIPInputStream gis = new GZIPInputStream(
                new FileInputStream(source.toFile()));
             FileOutputStream fos = new FileOutputStream(target.toFile())) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = gis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
        }
    }
}
