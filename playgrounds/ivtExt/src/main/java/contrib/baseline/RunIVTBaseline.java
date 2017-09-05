package contrib.baseline;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.PtConstants;

import com.google.inject.name.Names;

import contrib.baseline.counts.CountsIVTBaseline;
import contrib.baseline.counts.PTLinkCountsEventHandler;
import contrib.baseline.counts.PTStationCountsEventHandler;
import contrib.baseline.counts.StreetLinkDailyCountsEventHandler;
import contrib.baseline.counts.StreetLinkHourlyCountsEventHandler;
import playground.ivt.replanning.BlackListedTimeAllocationMutatorConfigGroup;
import playground.ivt.replanning.BlackListedTimeAllocationMutatorStrategyModule;

/**
 * Basic main for the ivt baseline scenarios.
 *
 * Based on playground/ivt/teaching/RunZurichScenario.java by thibautd
 *
 * @author boescpa
 */
public class RunIVTBaseline {

    public static void main(String[] args) {
		final String configFile = args[0];
		final String pathToPTLinksMonitorList = args[1];
		final String pathToPTStationsMonitorList = args[2];
		final String pathToStreetLinksDailyToMonitor = args[3];
		final String pathToStreetLinksHourlyToMonitor = args[4];

		// This allows to get a log file containing the log messages happening
		// before controler init.
		OutputDirectoryLogging.catchLogEntries();

		// It is suggested to use the config created by playground/boescpa/baseline/ConfigCreator.java.
		final Config config = ConfigUtils.loadConfig(configFile, new BlackListedTimeAllocationMutatorConfigGroup());
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists);
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		final Controler controler = new Controler(scenario);

		// We use a time allocation mutator that allows to exclude certain activities.
		controler.addOverridingModule(new BlackListedTimeAllocationMutatorStrategyModule());
		// We use a specific scoring function, that uses individual preferences for activity durations.
		controler.setScoringFunctionFactory(
				new IVTBaselineScoringFunctionFactory(controler.getScenario(),
						new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE)));

		// Add PT-Counts creator:
		controler.addOverridingModule(new AbstractModule() {
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
		});

		controler.run();
    }
}
