package playground.vsp.matsimToPostgres;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import playground.vsp.matsimToPostgres.exporters.CsvToPostgresExporter;

import javax.inject.Inject;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MatsimToPostgresControlerListener implements ShutdownListener {
    private final Logger log = Logger.getLogger(MatsimToPostgresControlerListener.class);

    @Inject
    private Config config;

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        final String runID = config.controler().getRunId();
        final String outputDir = config.controler().getOutputDirectory();

        // create db connections
        final PostgresExporterConfigGroup exporterConfigGroup = (PostgresExporterConfigGroup)config.getModules().get(PostgresExporterConfigGroup.GROUP_NAME);
        DBParameters params = new DBParameters(exporterConfigGroup.dbParamFile);
        Connection connection = params.createDBConnection();
        PostgresExporterConfigGroup.OverwriteRunSettings overwriteRunSettings = exporterConfigGroup.getOverwriteRun();

        if (connection != null) {
            log.info("Connected to the database!");
        } else {
            log.error("Failed to make connection!");
        }


        try{
            // import run data

            // import tripsCSV
            String tripsCSVFile = outputDir + "/" + runID + ".output_trips.csv.gz";
            log.info("Start database import for " + runID + ".output_trips.csv.gz");
            CsvToPostgresExporter tripsExporter = new CsvToPostgresExporter(connection, tripsCSVFile, runID, overwriteRunSettings);
            tripsExporter.export(runID);
            log.info("Finish database import for " + runID + ".output_trips.csv.gz");

            // import legsCSV
            String legsCSVFile = outputDir + "/" + runID + ".output_legs.csv.gz";
            log.info("Start database import for " + runID + ".output_legs.csv.gz");
            CsvToPostgresExporter legsExporter = new CsvToPostgresExporter(connection, legsCSVFile, runID, overwriteRunSettings);
            legsExporter.export(runID);
            log.info("Finish database import for " + runID + ".output_legs.csv.gz");

            // import pe
            // create and update views
            log.info("Create (if not exists) and update analyzer (materialized) views...");
            List<String> viewNames = createMissingViews(connection, exporterConfigGroup.getAnalyzerQueryDir());
            updateViews(connection, viewNames);
            log.info("Analyzer completed.");

            assert connection != null;
            connection.close();


        } catch (IOException e) {
            log.error("Csv output files were not found hence cannot export trips and legs to database. A very likely reason is that the value for the tripsWriterInterval is not set correctly.");
            e.printStackTrace();
        } catch (SQLException e){
            log.error("Some sql error. See Stack Trace...");
            e.printStackTrace();
        }

    }


    public List<String> createMissingViews(Connection connection, String analyzerQueryDir) {
        // go through directory
        // create views of sql scripts missing in db

        File dir = new File(analyzerQueryDir);
        String[] extensions = new String[] { "sql" };

        List<String> viewNames = new ArrayList<>();

        try (Stream<Path> walk = Files.walk(Paths.get(analyzerQueryDir))) {

            List<String> scripts = walk.map(Path::toString)
                    .filter(f -> f.endsWith(".sql"))
                    .sorted()
                    .collect(Collectors.toList());

            for (var script: scripts){
                String tableName = matchTableName(script);
                String query = sqlFileToQueryString(tableName, script);
                sqlExecute(connection, query);
                viewNames.add(tableName);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return viewNames;

    }

    private String sqlFileToQueryString(String tableName, String scriptFile) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(scriptFile));

        log.info("Create materialized view: " + tableName);
        String header = "CREATE MATERIALIZED VIEW IF NOT EXISTS " + tableName + " AS (";
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(header);

        String row;
        while ((row = br.readLine()) != null) {
            queryBuilder.append(row).append(" ");
        }

        queryBuilder.append(") WITH NO DATA;");
        return queryBuilder.toString();
    }


    private String matchTableName(String script) {
        var file = new File(script);
        var splitted = file.getName().split("_", 2);
        return splitted[splitted.length - 1].replace(".sql", "");
    }

    private void updateViews(Connection connection, List<String> viewNames) {

        for (var viewName: viewNames){
            log.info("Refresh materialized view: " + viewName);
            String query = "REFRESH MATERIALIZED VIEW " + viewName;
            sqlExecute(connection, query);
        }
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
