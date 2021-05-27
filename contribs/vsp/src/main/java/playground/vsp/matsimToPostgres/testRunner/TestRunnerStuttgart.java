package playground.vsp.matsimToPostgres.testRunner;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.stuttgart.run.RunStuttgart;
import org.matsim.stuttgart.run.StuttgartAnalysisMainModeIdentifier;
import playground.vsp.matsimToPostgres.PostgresExporterConfigGroup;
import playground.vsp.matsimToPostgres.PostgresExporterModule;

public class TestRunnerStuttgart {
    // paths may have to be adjusted...
    private static final String runId = "abc123";
    private static final String configFile = System.getProperty("user.dir") + "/../shared-svn/projects/matsim-stuttgart/stuttgart-v2.0/testConfig.xml";
    private static final String outputDir = System.getProperty("user.dir") + "/../shared-svn/projects/matsim-stuttgart/stuttgart-v2.0/output";
    private static final int lastIteration = 1;

    private static final String dbParamFile = "C:/Users/david/Desktop/dbParam2.xml";
    private static final String viewDirectory = "C:/Users/david/Documents/03_Repositories/matsim-libs/contribs/vsp/src/main/java/playground/vsp/matsimToPostgres/analyzerQueries";
    private static final PostgresExporterConfigGroup.OverwriteRunSettings overwriteRunSettings = PostgresExporterConfigGroup.OverwriteRunSettings.failIfRunIdExists;


    public static void main(String[] args) {

        Config config = RunStuttgart.loadConfig(new String[]{configFile}, setupExporterConfigGroup());
        config.controler().setRunId(runId);
        config.controler().setLastIteration(lastIteration);
        config.controler().setOutputDirectory(outputDir);
        config.controler().setWriteTripsInterval(1);

        Scenario scenario = RunStuttgart.loadScenario(config);

        Controler controler = RunStuttgart.loadControler(scenario);

        // Register the Postgres Exporter Module
        controler.addOverridingModule(new PostgresExporterModule());

        controler.run();

    }

    public static PostgresExporterConfigGroup setupExporterConfigGroup(){

        // Setup PostgresExporterConfigGroup settings
        PostgresExporterConfigGroup exporterConfigGroup = new PostgresExporterConfigGroup();
        exporterConfigGroup.setDbParamFile(dbParamFile);
        exporterConfigGroup.setOverwriteRun(overwriteRunSettings);
        exporterConfigGroup.setAnalyzerQueryDir(viewDirectory);

        return exporterConfigGroup;
    }

}
