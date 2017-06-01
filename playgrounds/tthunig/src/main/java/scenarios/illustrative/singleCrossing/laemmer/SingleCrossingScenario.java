/**
 * 
 */
package scenarios.illustrative.singleCrossing.laemmer;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlDataFactory;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.model.DefaultPlanbasedSignalSystemController;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.utils.SignalUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import signals.laemmer.model.LaemmerSignalController;

/**
 * @author dgrether
 * @author tthunig
 */
public class SingleCrossingScenario {

	private static final Logger log = Logger.getLogger(SingleCrossingScenario.class);

	private static final Id<Link> linkN2N3Id = Id.create("N2N3", Link.class);
	private static final Id<Link> linkN3CId = Id.create("N3C", Link.class);
	private static final Id<Link> linkCS3Id = Id.create("CS3", Link.class);
	private static final Id<Link> linkS3S2Id = Id.create("S3S2", Link.class);
	private static final Id<Link> linkS2S1Id = Id.create("S2S1", Link.class);
	private static final Id<Link> linkN1N2Id = Id.create("N1N2", Link.class);
	private static final Id<Link> linkS1S2Id = Id.create("S1S2", Link.class);
	private static final Id<Link> linkS2S3Id = Id.create("S2S3", Link.class);
	private static final Id<Link> linkS3CId = Id.create("S3C", Link.class);
	private static final Id<Link> linkCN3Id = Id.create("CN3", Link.class);
	private static final Id<Link> linkN3N2Id = Id.create("N3N2", Link.class);
	private static final Id<Link> linkN2N1Id = Id.create("N2N1", Link.class);
	private static final Id<Link> linkE1E2Id = Id.create("E1E2", Link.class);
	private static final Id<Link> linkE2E3Id = Id.create("E2E3", Link.class);
	private static final Id<Link> linkE3CId = Id.create("E3C", Link.class);
	private static final Id<Link> linkCW3Id = Id.create("CW3", Link.class);
	private static final Id<Link> linkW3W2Id = Id.create("W3W2", Link.class);
	private static final Id<Link> linkW2W1Id = Id.create("W2W1", Link.class);
	private static final Id<Link> linkW1W2Id = Id.create("W1W2", Link.class);
	private static final Id<Link> linkW2W3Id = Id.create("W2W3", Link.class);
	private static final Id<Link> linkW3CId = Id.create("W3C", Link.class);
	private static final Id<Link> linkCE3Id = Id.create("CE3", Link.class);
	private static final Id<Link> linkE3E2Id = Id.create("E3E2", Link.class);
	private static final Id<Link> linkE2E1Id = Id.create("E2E1", Link.class);
	private static final Id<SignalSystem> systemId = Id.create("C", SignalSystem.class);
	private static final Id<SignalGroup> sS = Id.create("sS", SignalGroup.class);
	private static final Id<SignalGroup> sN = Id.create("sN", SignalGroup.class);
	private static final Id<SignalGroup> sW = Id.create("sW", SignalGroup.class);
	private static final Id<SignalGroup> sE = Id.create("sE", SignalGroup.class);
	
	private final double tCycleSec = 120.0;
	private final double tDesired = 120.0;
	private final double tmax = 180.0;
	private final double tau0 = 5.0;
	private final double maxFlowWEPerHour = 1440.0;
	private final double maxFlowNSPerHour = 180.0;

	private final double runtimeSeconds = 7200.0;
	private final double startTimeSeconds = 0.0;
	private final double endTimeSeconds = startTimeSeconds + runtimeSeconds;
	private int personIdInt = 1;

	public Scenario createScenario(double lambdaWestEast, boolean useFixeTimeControl) {
		Config config = this.createConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(scenario.getConfig()).loadSignalsData());
		
		this.createPopulation(scenario, lambdaWestEast);
		this.createSignals(scenario);
		if (useFixeTimeControl) {
			this.createFixedTimeSignalControl(scenario, lambdaWestEast);
		} else {
			this.createLaemmerSignalControl(scenario);
		}
		return scenario;
	}

	private void createLaemmerSignalControl(Scenario scenario) {
		SignalsData signals = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalControlData control = signals.getSignalControlData();
		SignalControlDataFactory fac = signals.getSignalControlData().getFactory();
		SignalSystemControllerData controller = fac.createSignalSystemControllerData(systemId);
		control.addSignalSystemControllerData(controller);
		controller.setControllerIdentifier(LaemmerSignalController.IDENTIFIER);

	}

	private void createFixedTimeSignalControl(Scenario scenario, double lambdaWestEast) {
		SignalsData signals = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalControlData control = signals.getSignalControlData();
		SignalControlDataFactory fac = signals.getSignalControlData().getFactory();
		SignalSystemControllerData controller = fac.createSignalSystemControllerData(systemId);
		control.addSignalSystemControllerData(controller);
		controller.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);

		SignalPlanData plan = fac.createSignalPlanData(Id.create(systemId, SignalPlan.class));
		plan.setCycleTime((int) this.tCycleSec);
		controller.addSignalPlanData(plan);

		double sumIntergreen = 4.0 * this.tau0;
		double effectiveGreenTime = this.tCycleSec - sumIntergreen;
		double effectiveflowWE = lambdaWestEast * this.maxFlowWEPerHour;
		double sumFlow = (2.0 * this.maxFlowNSPerHour) + (2.0 * effectiveflowWE);
		int greenNS = (int) Math.round(this.maxFlowNSPerHour / sumFlow * effectiveGreenTime);
		int greenEW = (int) Math.round(effectiveflowWE / sumFlow * effectiveGreenTime);
		log.info("greenNS: " + greenNS + " greenWE: " + greenEW);

		int second = 0;
		SignalGroupSettingsData settings = fac.createSignalGroupSettingsData(sN);
		settings.setOnset(second);
		second += greenNS;
		settings.setDropping(second);
		plan.addSignalGroupSettings(settings);
		second = (int) (greenNS + this.tau0);

		settings = fac.createSignalGroupSettingsData(sE);
		settings.setOnset(second);
		second += greenEW;
		settings.setDropping(second);
		plan.addSignalGroupSettings(settings);
		second += this.tau0;

		settings = fac.createSignalGroupSettingsData(sS);
		settings.setOnset(second);
		second += greenNS;
		settings.setDropping(second);
		plan.addSignalGroupSettings(settings);
		second += this.tau0;

		settings = fac.createSignalGroupSettingsData(sW);
		settings.setOnset(second);
		second += greenEW;
		settings.setDropping(second);
		plan.addSignalGroupSettings(settings);

	}

	private void createSignals(Scenario scenario) {
		SignalsData signals = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalSystemsData systems = signals.getSignalSystemsData();
		SignalSystemData system = systems.getFactory().createSignalSystemData(systemId);
		systems.addSignalSystemData(system);
		SignalData signal = systems.getFactory().createSignalData(Id.create(sE, Signal.class));
		signal.setLinkId(linkE3CId);
		system.addSignalData(signal);
		signal = systems.getFactory().createSignalData(Id.create(sW, Signal.class));
		signal.setLinkId(linkW3CId);
		system.addSignalData(signal);
		signal = systems.getFactory().createSignalData(Id.create(sN, Signal.class));
		signal.setLinkId(linkN3CId);
		system.addSignalData(signal);
		signal = systems.getFactory().createSignalData(Id.create(sS, Signal.class));
		signal.setLinkId(linkS3CId);
		system.addSignalData(signal);

		SignalUtils.createAndAddSignalGroups4Signals(signals.getSignalGroupsData(), system);
	}

	private void createPopulation(Scenario scenario, double lambdaWestEast) {
		// N -> S
		List<Id<Link>> routeLinkIds = new ArrayList<Id<Link>>();
		// routeIds.add(linkN1N2Id);
		routeLinkIds.add(linkN2N3Id);
		routeLinkIds.add(linkN3CId);
		routeLinkIds.add(linkCS3Id);
		routeLinkIds.add(linkS3S2Id);
		double frequency = 3600.0 / maxFlowNSPerHour;
		createPersonsPerFrequency(scenario, linkN1N2Id, linkS2S1Id, routeLinkIds, frequency);

		// S -> N
		routeLinkIds = new ArrayList<Id<Link>>();
		routeLinkIds.add(linkS2S3Id);
		routeLinkIds.add(linkS3CId);
		routeLinkIds.add(linkCN3Id);
		routeLinkIds.add(linkN3N2Id);
		createPersonsPerFrequency(scenario, linkS1S2Id, linkN2N1Id, routeLinkIds, frequency);

		// E->W
		frequency = 3600.0 / (maxFlowWEPerHour * lambdaWestEast);
		routeLinkIds = new ArrayList<Id<Link>>();
		routeLinkIds.add(linkE2E3Id);
		routeLinkIds.add(linkE3CId);
		routeLinkIds.add(linkCW3Id);
		routeLinkIds.add(linkW3W2Id);
		createPersonsPerFrequency(scenario, linkE1E2Id, linkW2W1Id, routeLinkIds, frequency);

		// W -> E
		routeLinkIds = new ArrayList<Id<Link>>();
		routeLinkIds.add(linkW2W3Id);
		routeLinkIds.add(linkW3CId);
		routeLinkIds.add(linkCE3Id);
		routeLinkIds.add(linkE3E2Id);
		createPersonsPerFrequency(scenario, linkW1W2Id, linkE2E1Id, routeLinkIds, frequency);
	}

	private void createPersonsPerFrequency(Scenario scenario, Id<Link> startLinkId, Id<Link> endLinkId, List<Id<Link>> routeLinkIds, double frequency) {
		PopulationFactory fac = scenario.getPopulation().getFactory();
		for (double second = startTimeSeconds; second <= endTimeSeconds; second += frequency) {
			Activity startAct = fac.createActivityFromLinkId("dummy", startLinkId);
			Activity endAct = fac.createActivityFromLinkId("dummy", endLinkId);
			Person person = this.createAndAddPerson(scenario);
			Plan plan = fac.createPlan();
			person.addPlan(plan);
			plan.addActivity(startAct);
			startAct.setEndTime(second);
			Leg leg = fac.createLeg("car");
			plan.addLeg(leg);
			NetworkRoute route = new LinkNetworkRouteImpl(startLinkId, endLinkId);
			route.setLinkIds(startLinkId, routeLinkIds, endLinkId);
			leg.setRoute(route);
			plan.addActivity(endAct);
		}
	}

	private Person createAndAddPerson(Scenario scenario) {
		Person person = scenario.getPopulation().getFactory().createPerson(Id.create(personIdInt, Person.class));
		personIdInt++;
		scenario.getPopulation().addPerson(person);
		return person;
	}

	private Config createConfig() {
		String network = "../../../shared-svn/studies/dgrether/laemmer/isolatedcrossingtest/network.xml";
		Config config = ConfigUtils.createConfig();
		config.controler().setOutputDirectory("../../../runs-svn/laemmer/isolatedcrossingtest/");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(0);
		config.network().setInputFile(network);
		config.qsim().setUseLanes(false);
		config.qsim().setUsingFastCapacityUpdate(false);
		ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).setUseSignalSystems(true);
		config.qsim().setEndTime(endTimeSeconds + 2 * 3600);
		config.qsim().setSnapshotStyle(SnapshotStyle.queue);
		config.qsim().setStuckTime(1000.0);
		config.qsim().setNodeOffset(20.0);
		ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).setShowTeleportedAgents(true);
		ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).setDrawNonMovingItems(true);
		{
			ActivityParams dummyAct = new ActivityParams("dummy");
			dummyAct.setTypicalDuration(12 * 3600);
			config.planCalcScore().addActivityParams(dummyAct);
		}
		{
			StrategySettings strat = new StrategySettings();
			strat.setStrategyName(DefaultSelector.KeepLastSelected.toString());
			strat.setWeight(0.9);
			strat.setDisableAfter(config.controler().getLastIteration());
			config.strategy().addStrategySettings(strat);
		}
		return config;
	}

}
