/**
 * 
 */
package playground.dgrether.signalsystems.laemmer.testsinglecrossing;

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
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlDataFactory;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.model.DefaultPlanbasedSignalSystemController;
import org.matsim.contrib.signals.utils.SignalUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import playground.dgrether.DgPaths;
import playground.dgrether.signalsystems.laemmer.model.LaemmerSignalController;

/**
 * @author dgrether
 */
public class SingleCrossingScenario {
	
	private static final Logger log = Logger.getLogger(SingleCrossingScenario.class);
	
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
	private Id<Link> linkN2N3Id;
	private Id<Link> linkN3CId;
	private Id<Link> linkCS3Id;
	private Id<Link> linkS3S2Id;
	private Id<Link> linkS2S1Id;
	private Id<Link> linkN1N2Id;
	private Id<Link> linkS1S2Id;
	private Id<Link> linkS2S3Id;
	private Id<Link> linkS3CId;
	private Id<Link> linkCN3Id;
	private Id<Link> linkN3N2Id;
	private Id<Link> linkN2N1Id;
	private Id<Link> linkE1E2Id;
	private Id<Link> linkE2E3Id;
	private Id<Link> linkE3CId;
	private Id<Link> linkCW3Id;
	private Id<Link> linkW3W2Id;
	private Id<Link> linkW2W1Id;
	private Id<Link> linkW1W2Id;
	private Id<Link> linkW2W3Id;
	private Id<Link> linkW3CId;
	private Id<Link> linkCE3Id;
	private Id<Link> linkE3E2Id;
	private Id<Link> linkE2E1Id;
	private Id<SignalSystem> systemId;
	private Id<SignalGroup> sS;
	private Id<SignalGroup> sN;
	private Id<SignalGroup> sW;
	private Id<SignalGroup> sE;
	
	
	private void createIds(Scenario scenario){
		this.linkN1N2Id = Id.create("N1N2", Link.class);
		this.linkN2N3Id = Id.create("N2N3", Link.class);
		this.linkN3CId = Id.create("N3C", Link.class);
		this.linkCS3Id = Id.create("CS3", Link.class);
		this.linkS3S2Id = Id.create("S3S2", Link.class);
		this.linkS2S1Id = Id.create("S2S1", Link.class);

		this.linkS1S2Id = Id.create("S1S2", Link.class);
		this.linkS2S3Id = Id.create("S2S3", Link.class);
		this.linkS3CId = Id.create("S3C", Link.class);
		this.linkCN3Id = Id.create("CN3", Link.class);
		this.linkN3N2Id = Id.create("N3N2", Link.class);
		this.linkN2N1Id = Id.create("N2N1", Link.class);
		
		this.linkE1E2Id = Id.create("E1E2", Link.class);
		this.linkE2E3Id = Id.create("E2E3", Link.class);
		this.linkE3CId = Id.create("E3C", Link.class);
		this.linkCW3Id = Id.create("CW3", Link.class);
		this.linkW3W2Id = Id.create("W3W2", Link.class);
		this.linkW2W1Id = Id.create("W2W1", Link.class);

		this.linkW1W2Id = Id.create("W1W2", Link.class);
		this.linkW2W3Id = Id.create("W2W3", Link.class);
		this.linkW3CId = Id.create("W3C", Link.class);
		this.linkCE3Id = Id.create("CE3", Link.class);
		this.linkE3E2Id = Id.create("E3E2", Link.class);
		this.linkE2E1Id = Id.create("E2E1", Link.class);
		
		this.sE = Id.create("sE", SignalGroup.class);
		this.sW = Id.create("sW", SignalGroup.class);
		this.sN = Id.create("sN", SignalGroup.class);
		this.sS = Id.create("sS", SignalGroup.class);
		this.systemId = Id.create("C", SignalSystem.class);
	}

	
	
	public Scenario createScenario(double lambdaWestEast, boolean useFixeTimeControl){
		Config config = this.createConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);
		this.createIds(scenario);
		this.createPopulation(scenario, lambdaWestEast);
		this.createSignals(scenario);
		if (useFixeTimeControl){
			this.createFixedTimeSignalControl(scenario, lambdaWestEast);
		}
		else {
			this.createLaemmerSignalControl(scenario);
		}
		return scenario;
	}
	
	private void createLaemmerSignalControl(Scenario scenario){
		SignalsData signals = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalControlData control = signals.getSignalControlData();
		SignalControlDataFactory fac = signals.getSignalControlData().getFactory();
		SignalSystemControllerData controller = fac.createSignalSystemControllerData(systemId);
		control.addSignalSystemControllerData(controller);
		controller.setControllerIdentifier(LaemmerSignalController.IDENTIFIER);
		
	}
	
	
	private void createFixedTimeSignalControl(Scenario scenario, double lambdaWestEast){
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
		double sumFlow = (2.0 * this.maxFlowNSPerHour) + (2.0 * effectiveflowWE) ;
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
		
		Population pop = scenario.getPopulation();
		PopulationFactory fac = pop.getFactory();
		//N -> S
		List<Id<Link>> routeIds = new ArrayList<Id<Link>>();
//		routeIds.add(linkN1N2Id);
		routeIds.add(linkN2N3Id);
		routeIds.add(linkN3CId);
		routeIds.add(linkCS3Id);
		routeIds.add(linkS3S2Id);
		double frequency = 3600.0 / maxFlowNSPerHour;
		for (double second = startTimeSeconds; second <= endTimeSeconds; second += frequency){
			Activity home1 = fac.createActivityFromLinkId("home", linkN1N2Id);
			Activity home2 = fac.createActivityFromLinkId("home", linkS2S1Id);
			Person person = this.createAndAddPerson(scenario);
			Plan plan = fac.createPlan();
			person.addPlan(plan); 
			plan.addActivity(home1);
			home1.setEndTime(second);
			Leg leg = fac.createLeg("car");
			plan.addLeg(leg);
			NetworkRoute route = new LinkNetworkRouteImpl(linkN1N2Id, linkS2S1Id);
			route.setLinkIds(linkN1N2Id, routeIds, linkS2S1Id);
			leg.setRoute(route);
			plan.addActivity(home2);
		}
		
		//S -> N
		routeIds = new ArrayList<Id<Link>>();
		routeIds.add(linkS2S3Id);
		routeIds.add(linkS3CId);
		routeIds.add(linkCN3Id);
		routeIds.add(linkN3N2Id);
		for (double second = startTimeSeconds; second <= endTimeSeconds; second += frequency){
			Activity home1 = fac.createActivityFromLinkId("home", linkS1S2Id);
			Activity home2 = fac.createActivityFromLinkId("home", linkN2N1Id);
			Person person = this.createAndAddPerson(scenario);
			Plan plan = fac.createPlan();
			person.addPlan(plan); 
			plan.addActivity(home1);
			home1.setEndTime(second);
			Leg leg = fac.createLeg("car");
			plan.addLeg(leg);
			NetworkRoute route = new LinkNetworkRouteImpl(linkS1S2Id, linkN2N1Id);
			route.setLinkIds(linkS1S2Id, routeIds, linkN2N1Id);
			leg.setRoute(route);
			plan.addActivity(home2);
		}
		
		

		
		//E->W
		frequency = 3600.0 / (maxFlowWEPerHour * lambdaWestEast);
		routeIds = new ArrayList<Id<Link>>();
		routeIds.add(linkE2E3Id);
		routeIds.add(linkE3CId);
		routeIds.add(linkCW3Id);
		routeIds.add(linkW3W2Id);
		for (double second = startTimeSeconds; second <= endTimeSeconds; second += frequency){
			Activity home1 = fac.createActivityFromLinkId("home", linkE1E2Id);
			Activity home2 = fac.createActivityFromLinkId("home", linkW2W1Id);
			Person person = this.createAndAddPerson(scenario);
			Plan plan = fac.createPlan();
			person.addPlan(plan); 
			plan.addActivity(home1);
			home1.setEndTime(second);
			Leg leg = fac.createLeg("car");
			plan.addLeg(leg);
			NetworkRoute route = new LinkNetworkRouteImpl(linkE1E2Id, linkW2W1Id);
			route.setLinkIds(linkE1E2Id, routeIds, linkW2W1Id);
			leg.setRoute(route);
			plan.addActivity(home2);
		}



		//W -> E
		routeIds = new ArrayList<Id<Link>>();
		routeIds.add(linkW2W3Id);
		routeIds.add(linkW3CId);
		routeIds.add(linkCE3Id);
		routeIds.add(linkE3E2Id);
		for (double second = startTimeSeconds; second <= endTimeSeconds; second += frequency){
			Activity home1 = fac.createActivityFromLinkId("home", linkW1W2Id);
			Activity home2 = fac.createActivityFromLinkId("home", linkE2E1Id);
			Person person = this.createAndAddPerson(scenario);
			Plan plan = fac.createPlan();
			person.addPlan(plan); 
			plan.addActivity(home1);
			home1.setEndTime(second);
			Leg leg = fac.createLeg("car");
			plan.addLeg(leg);
			NetworkRoute route = new LinkNetworkRouteImpl(linkW1W2Id, linkE2E1Id);
			route.setLinkIds(linkW1W2Id, routeIds, linkE2E1Id);
			leg.setRoute(route);
			plan.addActivity(home2);
		}
		
	}
	
	private Person createAndAddPerson(Scenario scenario){
		Person person = scenario.getPopulation().getFactory().createPerson(Id.create(personIdInt, Person.class));
		personIdInt++;
		scenario.getPopulation().addPerson(person);
		return person;
	}

	private Config createConfig(){
		String repos = DgPaths.REPOS;
		String network = repos + "shared-svn/studies/dgrether/laemmer/isolatedcrossingtest/network.xml";
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(network);
		config.scenario().setUseLanes(false);
		ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).setUseSignalSystems(true);
		QSimConfigGroup qsim = config.qsim();
		qsim.setSnapshotStyle( SnapshotStyle.queue ) ;;
		qsim.setStuckTime(1000.0);
		config.qsim().setNodeOffset(20.0);
		ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).setShowTeleportedAgents(true);
		ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).setDrawNonMovingItems(true);
		return config;
	}
	

}
