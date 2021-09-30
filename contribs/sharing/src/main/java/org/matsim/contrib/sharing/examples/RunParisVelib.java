package org.matsim.contrib.sharing.examples;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.matsim.contrib.sharing.run.SharingConfigGroup;
import org.matsim.contrib.sharing.run.SharingModule;
import org.matsim.contrib.sharing.run.SharingServiceConfigGroup;
import org.matsim.contrib.sharing.run.SharingServiceConfigGroup.ServiceScheme;
import org.matsim.contrib.sharing.service.SharingUtils;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;

public class RunParisVelib {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.build();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"));

		// We need to add the sharing config group
		SharingConfigGroup sharingConfig = new SharingConfigGroup();
		config.addModule(sharingConfig);

		// We need to define a service ...
		SharingServiceConfigGroup serviceConfig = new SharingServiceConfigGroup();
		sharingConfig.addService(serviceConfig);

		// ... with a service id. The respective mode will be "sharing:velib".
		serviceConfig.setId("velib");

		// ... with freefloating characteristics
		serviceConfig.setMaximumAccessEgressDistance(100000);
		serviceConfig.setServiceScheme(ServiceScheme.StationBased);
		serviceConfig.setServiceAreaShapeFile(null);

		// ... with a number of available vehicles and their initial locations
		serviceConfig.setServiceInputFile("velib_service.xml.gz");

		// ... and, we need to define the underlying mode, here "bike".
		serviceConfig.setMode("bike");

		// Finally, we need to make sure that the service mode (sharing:velib) is
		// considered in mode choice.
		List<String> modes = new ArrayList<>(Arrays.asList(config.subtourModeChoice().getModes()));
		modes.add(SharingUtils.getServiceMode(serviceConfig));
		config.subtourModeChoice().setModes(modes.toArray(new String[modes.size()]));

		// We need to add interaction activity types to scoring
		ActivityParams pickupParams = new ActivityParams(SharingUtils.PICKUP_ACTIVITY);
		pickupParams.setScoringThisActivityAtAll(false);
		config.planCalcScore().addActivityParams(pickupParams);

		ActivityParams dropoffParams = new ActivityParams(SharingUtils.DROPOFF_ACTIVITY);
		dropoffParams.setScoringThisActivityAtAll(false);
		config.planCalcScore().addActivityParams(dropoffParams);

		ActivityParams bookingParams = new ActivityParams(SharingUtils.BOOKING_ACTIVITY);
		bookingParams.setScoringThisActivityAtAll(false);
		config.planCalcScore().addActivityParams(bookingParams);

		{
			// Remove some standard eqasim config groups
			config.getModules().remove("DiscreteModeChoice");
			config.getModules().remove("eqasim");
			config.getModules().remove("eqasim:calibration");

			// Replace some standard eqasim settings

			for (StrategySettings strategy : config.strategy().getStrategySettings()) {
				if (strategy.getStrategyName().equals("DiscreteModeChoice")) {
					strategy.setStrategyName("SubtourModeChoice");
				} else if (strategy.getStrategyName().equals("KeepLastSelected")) {
					strategy.setStrategyName("ChangeExpBeta");
				}
			}

			config.strategy().setPlanSelectorForRemoval("WorstPlanSelector");
			config.transit().setUseTransit(true);
			config.transit().setUsingTransitInMobsim(true);
		}

		// Write out all events (DEBUG)
		config.controler().setWriteEventsInterval(1);
		config.controler().setWritePlansInterval(1);
		config.controler().setLastIteration(10);

		// Set up controller (no specific settings needed for scenario)
		Controler controller = new Controler(config);

		// Does not really "override" anything
		controller.addOverridingModule(new SharingModule());

		// Enable QSim components
		controller.configureQSimComponents(SharingUtils.configureQSim(sharingConfig));

		controller.run();
	}
}
