package contrib.baseline;

import contrib.baseline.counts.*;
import com.google.inject.name.Names;
import contrib.baseline.lib.F2LConfigGroup;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.*;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.algorithms.WorldConnectLocations;
import org.matsim.pt.PtConstants;
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
		/*final String pathToStreetLinksDailyToMonitor = args[3];
		final String pathToStreetLinksHourlyToMonitor = args[4];*/

		// This allows to get a log file containing the log messages happening
		// before controler init.
		OutputDirectoryLogging.catchLogEntries();

		// It is suggested to use the config created by playground/boescpa/baseline/ConfigCreator.java.
		final Config config = ConfigUtils.loadConfig(configFile, new BlackListedTimeAllocationMutatorConfigGroup(), new F2LConfigGroup());
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists);
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		final Controler controler = new Controler(scenario);

		connectFacilitiesWithNetwork(controler);

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
				/*this.bind(StreetLinkDailyCountsEventHandler.class);
				bind(String.class)
						.annotatedWith(Names.named("pathToStreetLinksDailyToMonitor"))
						.toInstance(pathToStreetLinksDailyToMonitor);
				this.bind(StreetLinkHourlyCountsEventHandler.class);
				bind(String.class)
						.annotatedWith(Names.named("pathToStreetLinksHourlyToMonitor"))
						.toInstance(pathToStreetLinksHourlyToMonitor);*/
			}
		});

		controler.run();
    }

    public static void connectFacilitiesWithNetwork(MatsimServices controler) {
        ActivityFacilities facilities = controler.getScenario().getActivityFacilities();
        Network network = controler.getScenario().getNetwork();
        WorldConnectLocations wcl = new WorldConnectLocations(controler.getConfig());
        wcl.connectFacilitiesWithLinks(facilities, network);
    }
}
