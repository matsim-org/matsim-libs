package contrib.baseline.calibration.location_choice;

import com.google.inject.name.Names;
import contrib.baseline.IVTBaselineScoringFunctionFactory;
import contrib.baseline.counts.*;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceInitializer;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.*;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.pt.PtConstants;
import playground.ivt.replanning.BlackListedTimeAllocationMutatorConfigGroup;
import playground.ivt.replanning.BlackListedTimeAllocationMutatorStrategyModule;

import java.io.File;

public class RunScenario {
    public static void main(String[] args) {
        final String configFile = args[0];

        // This allows to get a log file containing the log messages happening
        // before controler init.
        OutputDirectoryLogging.catchLogEntries();

        // It is suggested to use the config created by playground/boescpa/baseline/ConfigCreator.java.
        final Config config = ConfigUtils.loadConfig(configFile,
                new BlackListedTimeAllocationMutatorConfigGroup(),
                new DestinationChoiceConfigGroup());

        // This is currently needed for location choice: initializing
        // the location choice writes K-values files to the output directory, which:
        // - fails if the directory does not exist
        // - makes the controler crash latter if the unsafe setOverwriteFiles( true )
        // is not called.
        // This ensures that we get safety with location choice working as expected,
        // before we sort this out and definitely kick out setOverwriteFiles.
        createEmptyDirectoryOrFailIfExists(config.controler().getOutputDirectory());

        final Scenario scenario = ScenarioUtils.loadScenario(config);
        final Controler controler = new Controler(scenario);

        controler.getConfig().controler().setOverwriteFileSetting(
                OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

        initializeLocationChoice(controler);

        // We use a time allocation mutator that allows to exclude certain activities.
        controler.addOverridingModule(new BlackListedTimeAllocationMutatorStrategyModule());
        // We use a specific scoring function, that uses individual preferences for activity durations.
        controler.setScoringFunctionFactory(
                new IVTBaselineScoringFunctionFactory(controler.getScenario(),
                        new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE)));

        // Add PT-Counts creator:
        /*controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                this.addControlerListenerBinding().to(CountsIVTBaseline.class);
                this.bind(PTLinkCountsEventHandler.class);
                bind(String.class)
                        .annotatedWith(Names.named("pathToPTLinksToMonitor"))
                        .toInstance(pathToPTLinksMonitorList);
                this.bind(PTStationCountsEventHandler.class);
                bind(String.class)
                        .annotatedWith(Names.named("pathToPTStationsToMonitor"))
                        .toInstance(pathToPTStationsMonitorList);
                this.bind(StreetLinkDailyCountsEventHandler.class);
                bind(String.class)
                        .annotatedWith(Names.named("pathToStreetLinksDailyToMonitor"))
                        .toInstance(pathToStreetLinksDailyToMonitor);
                this.bind(StreetLinkHourlyCountsEventHandler.class);
                bind(String.class)
                        .annotatedWith(Names.named("pathToStreetLinksHourlyToMonitor"))
                        .toInstance(pathToStreetLinksHourlyToMonitor);
            }
        });*/

        controler.run();
    }

    private static void initializeLocationChoice(MatsimServices controler) {
        Scenario scenario = controler.getScenario();
        DestinationChoiceBestResponseContext lcContext =
                new DestinationChoiceBestResponseContext(scenario);
        lcContext.init();
        controler.addControlerListener(new DestinationChoiceInitializer(lcContext));
    }

    private static void createEmptyDirectoryOrFailIfExists(String directory) {
        File file = new File( directory +"/" );
        if ( file.exists() && file.list().length > 0 ) {
            throw new UncheckedIOException( "Directory "+directory+" exists and is not empty!" );
        }
        file.mkdirs();
    }
}
