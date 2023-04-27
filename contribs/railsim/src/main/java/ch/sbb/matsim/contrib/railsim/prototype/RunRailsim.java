package ch.sbb.matsim.contrib.railsim.prototype;

import ch.sbb.matsim.contrib.railsim.prototype.prepare.AdjustNetworkToSchedule;
import ch.sbb.matsim.contrib.railsim.prototype.prepare.SplitTransitLinks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;

/**
 * @author Ihab Kaddoura
 */
public final class RunRailsim {
	private static final Logger log = LogManager.getLogger(RunRailsim.class);

	private RunRailsim() {
	}

	public static void main(String[] args) {
		log.info("Arguments:");
		for (String arg : args) {
			log.info(arg);
		}
		log.info("---");

		Config config = prepareConfig(args);
		Scenario scenario = prepareScenario(config);
		Controler controler = prepareControler(scenario);

		controler.run();
	}

	public static Config prepareConfig(String[] args) {
		Config config = ConfigUtils.loadConfig(args);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		config.qsim().setUsingFastCapacityUpdate(false);
		config.qsim().setTimeStepSize(1.);
		config.qsim().setLinkDynamics(LinkDynamics.PassingQ);
		config.qsim().setNumberOfThreads(1);

		// Storage capacities are handled via signals.
		// To avoid any additional restrictions, the storage capacity is set to a very large value.
		if (config.qsim().getStorageCapFactor() < 9999999.) {
			log.warn("Storage capacities are handled via signals. To avoid any additional restrictions, the storage capacity factor is set to 9999999.");
			config.qsim().setStorageCapFactor(9999999.);
		}

		if (config.qsim().getFlowCapFactor() != 1.0) {
			log.warn("The signals need at least one time step to react to the movements of transit vehicles. To enforce this, the flow capacity factor needs to be set to 1.0.");
			config.qsim().setFlowCapFactor(1.0);
		}

		return config;
	}

	public static Scenario prepareScenario(Config config) {
		Scenario scenario = ScenarioUtils.loadScenario(config);
		double timeStepSize = config.qsim().getTimeStepSize();

		// check links
		for (Link link : scenario.getNetwork().getLinks().values()) {

			if (link.getCapacity() > 3600 / timeStepSize) {
				log.warn("The signals need at least one time step to react to the movements of transit vehicles. Link capacities should be set to a maximum of 3600 vehicles per hour.");
				link.setCapacity(3600. / timeStepSize);
			}

			if (RailsimUtils.getOppositeDirectionLink(link, scenario.getNetwork()) != null && RailsimUtils.getTrainCapacity(link) > 1) {

				throw new RuntimeException("In the current version, one direction tracks only work for capacities = 1. " + "For capacities larger than 1, we have to think about the train disposition strategies to avoid deadlocks.");
			}

			for (Link outLink : link.getFromNode().getOutLinks().values()) {
				if (outLink == link) {
					// same link
				} else {
					// another link
					if (outLink.getFromNode() == link.getFromNode() && outLink.getToNode() == link.getToNode()) {
						// overlaying link
						throw new RuntimeException("Overlaying links with identical from and to node: " + outLink.getId() + " and " + link.getId() + ". " + "Please use separate nodes if you want to run trains on separate links, otherwise we have many potentially conflicting links " + "which increases the computation time. Aborting...");
					}
				}
			}

		}

		for (VehicleType vehicleType : scenario.getTransitVehicles().getVehicleTypes().values()) {
			if (vehicleType.getPcuEquivalents() != 1.0) {
				log.warn("The transit vehicle types are required to have a pcu equivalent of 1.0.");
				vehicleType.setPcuEquivalents(1.0);
			}
		}

		return scenario;
	}

	public static Controler prepareControler(Scenario scenario) {

		RailsimConfigGroup railsimConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(), RailsimConfigGroup.class);

		if (railsimConfigGroup.isAdjustNetworkToSchedule()) {
			AdjustNetworkToSchedule adjust = new AdjustNetworkToSchedule(scenario);
			adjust.run();
		}

		if (railsimConfigGroup.isSplitLinks()) {
			SplitTransitLinks splitTransitLinks = new SplitTransitLinks(scenario);
			splitTransitLinks.run(railsimConfigGroup.getSplitLinksLength());
		}

		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {

				// train statistics used in different classes
				bind(TrainStatistics.class).asEagerSingleton();
				addEventHandlerBinding().to(TrainStatistics.class);

				// train expansion along several links
				bind(SpatialTrainDimension.class).asEagerSingleton();
				addEventHandlerBinding().to(SpatialTrainDimension.class);

				// adaptive signal controler
				bind(AdaptiveTrainSignalsControler.class).asEagerSingleton();
				addMobsimListenerBinding().to(AdaptiveTrainSignalsControler.class);
				addEventHandlerBinding().to(AdaptiveTrainSignalsControler.class);
				addControlerListenerBinding().to(AdaptiveTrainSignalsControler.class);

				// visualization output
				bind(LinkUsageVisualizer.class).asEagerSingleton();
				addEventHandlerBinding().to(LinkUsageVisualizer.class);
				addControlerListenerBinding().to(LinkUsageVisualizer.class);

				// for train acceleration / deceleration dynamics
				bind(RailsimLinkSpeedCalculatorImpl.class).asEagerSingleton();
				bind(RailsimLinkSpeedCalculator.class).to(RailsimLinkSpeedCalculatorImpl.class);
				addEventHandlerBinding().to(RailsimLinkSpeedCalculatorImpl.class);

			}
		});

		controler.addOverridingQSimModule(new RailsimSignalsQSimModule());

		return controler;
	}

}
