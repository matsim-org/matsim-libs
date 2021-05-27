package playground.vsp.matsimToPostgres.testRunner;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import playground.vsp.matsimToPostgres.PostgresExporterConfigGroup;
import playground.vsp.matsimToPostgres.PostgresExporterModule;

import java.util.ArrayList;
import java.util.List;


public class TestRunnerBerlin {
    private static final Logger log = Logger.getLogger( TestRunnerBerlin.class );

    public static void main(String[] args) {
        Config config = TestRunnerBerlin.prepareConfig();
        Scenario scenario = TestRunnerBerlin.prepareScenario(config);
        Controler controler = TestRunnerBerlin.prepareControler(scenario);
        controler.run();

    }

    private static Controler prepareControler(Scenario scenario) {
        Controler controler = new Controler(scenario);

        controler.addOverridingModule(new PostgresExporterModule());
        return controler;

    }

    private static Scenario prepareScenario(Config config) {
        return ScenarioUtils.loadScenario(config);

    }


    public static Config prepareConfig(){
        final String configURL = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-1pct/input/berlin-v5.5-1pct.config.xml";
        String[] args = {configURL};
        Config config = ConfigUtils.loadConfig(configURL);
        config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.ignore);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        final long minDuration = 600;
        final long maxDuration = 3600 * 27;
        final long difference = 600;

        // Activities without opening & closing time
        createActivityPatterns("home", minDuration, maxDuration, difference).forEach(params -> config.planCalcScore().addActivityParams(params));
        createActivityPatterns("errands", minDuration, maxDuration, difference).forEach(params -> config.planCalcScore().addActivityParams(params));
        createActivityPatterns("educ_secondary", minDuration, maxDuration, difference).forEach(params -> config.planCalcScore().addActivityParams(params));
        createActivityPatterns("educ_higher", minDuration, maxDuration, difference).forEach(params -> config.planCalcScore().addActivityParams(params));
        createActivityPatterns("other", minDuration, maxDuration, difference).forEach(params -> config.planCalcScore().addActivityParams(params));


        // Activities with opening & closing time
        createActivityPatterns("work", minDuration, maxDuration, difference, 6, 20).forEach(params -> config.planCalcScore().addActivityParams(params));
        createActivityPatterns("business", minDuration, maxDuration, difference, 6, 20).forEach(params -> config.planCalcScore().addActivityParams(params));
        createActivityPatterns("leisure", minDuration, maxDuration, difference, 9, 27).forEach(params -> config.planCalcScore().addActivityParams(params));
        createActivityPatterns("shopping", minDuration, maxDuration, difference, 8, 20).forEach(params -> config.planCalcScore().addActivityParams(params));

        config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("freight").setTypicalDuration(60 * 15));


        // ToDo: As of here relevant...
        config.controler().setRunId("New14573");
        config.controler().setWriteTripsInterval(1);
        config.controler().setLastIteration(1);

        // ToDo: Friedrich to change for his own purposes/ pc structure
        //config.plans().setInputFile("/Users/friedrichvolkers/populationReduced.xml.gz");
        config.plans().setInputFile("C:/Users/david/Desktop/populationReduced.xml.gz");

        ConfigUtils.addOrGetModule(config, PostgresExporterConfigGroup.class);
        PostgresExporterConfigGroup exporterConfigGroup = (PostgresExporterConfigGroup) config.getModules().get(PostgresExporterConfigGroup.GROUP_NAME);
        exporterConfigGroup.setOverwriteRun(PostgresExporterConfigGroup.OverwriteRunSettings.overwriteExistingRunId);

        // ToDo: Friedrich to change for his own purposes/ pc structure
        //exporterConfigGroup.setDbParamFile("C:\\Users\\david\\Documents\\03_Repositories\\matsim-libs\\contribs\\vsp\\src\\main\\java\\playground\\vsp\\matsimToPostgres\\dbParam.xml");
        exporterConfigGroup.setDbParamFile("C:/Users/david/Desktop/dbParam.xml");
        exporterConfigGroup.setAnalyzerQueryDir("C:/Users/david/Documents/03_Repositories/matsim-libs/contribs/vsp/src/main/java/playground/vsp/matsimToPostgres/analyzerQueries");

        return config;
    }

    // Copied from https://github.com/matsim-vsp/mosaik-2/blob/master/src/main/java/org/matsim/mosaik2/Utils.java
    public static List<PlanCalcScoreConfigGroup.ActivityParams> createActivityPatterns(String type, long minDurationInSeconds, long maxDurationInSeconds, long durationDifferenceInSeconds) {

        List<PlanCalcScoreConfigGroup.ActivityParams> result = new ArrayList<>();
        for (long duration = minDurationInSeconds; duration <= maxDurationInSeconds; duration += durationDifferenceInSeconds) {
            final PlanCalcScoreConfigGroup.ActivityParams params = new PlanCalcScoreConfigGroup.ActivityParams(type + "_" + duration + ".0");
            params.setTypicalDuration(duration);
            result.add(params);
        }
        return result;
    }


    public static List<PlanCalcScoreConfigGroup.ActivityParams> createActivityPatterns(String type, long minDurationInSeconds, long maxDurationInSeconds, long durationDifferenceInSeconds, double openingHour, double closingHour) {

        List<PlanCalcScoreConfigGroup.ActivityParams> result = createActivityPatterns(type, minDurationInSeconds, maxDurationInSeconds, durationDifferenceInSeconds);
        for (var activityParams: result){
            activityParams.setOpeningTime(openingHour * 3600.);
            activityParams.setClosingTime(closingHour * 3600.);
        }
        return result;

    }



}
