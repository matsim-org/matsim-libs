package contrib.baseline.calibration.location_choice;

import com.google.inject.name.Names;
import contrib.baseline.IVTBaselineScoringFunctionFactory;
import contrib.baseline.counts.*;
import contrib.baseline.preparation.IVTConfigCreator;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceInitializer;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.*;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.pt.PtConstants;
import playground.ivt.replanning.BlackListedTimeAllocationMutatorConfigGroup;
import playground.ivt.replanning.BlackListedTimeAllocationMutatorStrategyModule;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class LCScenarioRunner {
    public static void main(String[] args) {}

    public Map<String, Double> runScenario123(String name, int iteration, int percentage, String[] purposes, List<Double> epsilons) {
        Config config = ConfigUtils.loadConfig("config.xml");
        Scenario scenario = ScenarioUtils.loadScenario(config);

        config.controler().setOutputDirectory("simulations/output_" + name + "_" + String.valueOf(iteration));
        config.controler().setLastIteration(0);

        Controler controler = new Controler(scenario);

        final DistanceAggregator aggregator = new DistanceAggregator(scenario.getNetwork());

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addEventHandlerBinding().toInstance(aggregator);
            }
        });

        controler.run();
        return aggregator.getMeanDistances();
    }

    public Map<String, Double> runScenario(String name, int iteration, int percentage, int cores, String[] purposes, List<Double> epsilons) {
        // Set up configuraton

        OutputDirectoryLogging.catchLogEntries();

        Config config = ConfigUtils.createConfig();

        new IVTConfigCreator().makeConfigIVT(config, percentage);

        Collection<StrategyConfigGroup.StrategySettings> strategySettings = config.strategy().getStrategySettings();
        config.strategy().clearStrategySettings();

        for (StrategyConfigGroup.StrategySettings s : strategySettings) {
            if (s.getStrategyName().contains("locationchoice")) {
                config.strategy().addStrategySettings(s);
            }
        }

        String[] epsilonStrings = new String[purposes.length];
        for (int i = 0; i < epsilonStrings.length; i++) epsilonStrings[i] = String.valueOf(epsilons.get(i));

        DestinationChoiceConfigGroup lcConfig = (DestinationChoiceConfigGroup) config.getModules().get("locationchoice");
        lcConfig.setFlexibleTypes(String.join(", ", purposes));
        lcConfig.setEpsilonScaleFactors(String.join(", ", epsilonStrings));

        config.controler().setOutputDirectory("simulations/output_" + name + "_" + String.valueOf(iteration));
        config.controler().setLastIteration(0);
        config.global().setNumberOfThreads(cores);
        config.qsim().setNumberOfThreads(cores);

        // Run simulation as usual

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

        // Configure the measurements

        final DistanceAggregator aggregator = new DistanceAggregator(scenario.getNetwork());

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addEventHandlerBinding().toInstance(aggregator);
            }
        });

        controler.run();
        return aggregator.getMeanDistances();
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
