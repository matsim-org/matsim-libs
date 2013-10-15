/**
 * 
 */
package playground.dgrether.signalsystems.laemmer.testsinglecrossing;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.signalsystems.SignalUtils;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalControlData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalControlDataFactory;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalGroupSettingsData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalPlanData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalSystemControllerData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsData;
import org.matsim.signalsystems.model.DefaultPlanbasedSignalSystemController;

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
	private Id linkN2N3Id;
	private Id linkN3CId;
	private Id linkCS3Id;
	private Id linkS3S2Id;
	private Id linkS2S1Id;
	private Id linkN1N2Id;
	private Id linkS1S2Id;
	private Id linkS2S3Id;
	private Id linkS3CId;
	private Id linkCN3Id;
	private Id linkN3N2Id;
	private Id linkN2N1Id;
	private Id linkE1E2Id;
	private Id linkE2E3Id;
	private Id linkE3CId;
	private Id linkCW3Id;
	private Id linkW3W2Id;
	private Id linkW2W1Id;
	private Id linkW1W2Id;
	private Id linkW2W3Id;
	private Id linkW3CId;
	private Id linkCE3Id;
	private Id linkE3E2Id;
	private Id linkE2E1Id;
	private Id systemId;
	private Id sS;
	private Id sN;
	private Id sW;
	private Id sE;
	
	
	private void createIds(Scenario scenario){
		this.linkN1N2Id = scenario.createId("N1N2");
		this.linkN2N3Id = scenario.createId("N2N3");
		this.linkN3CId = scenario.createId("N3C");
		this.linkCS3Id = scenario.createId("CS3");
		this.linkS3S2Id = scenario.createId("S3S2");
		this.linkS2S1Id = scenario.createId("S2S1");

		this.linkS1S2Id = scenario.createId("S1S2");
		this.linkS2S3Id = scenario.createId("S2S3");
		this.linkS3CId = scenario.createId("S3C");
		this.linkCN3Id = scenario.createId("CN3");
		this.linkN3N2Id = scenario.createId("N3N2");
		this.linkN2N1Id = scenario.createId("N2N1");
		
		this.linkE1E2Id = scenario.createId("E1E2");
		this.linkE2E3Id = scenario.createId("E2E3");
		this.linkE3CId = scenario.createId("E3C");
		this.linkCW3Id = scenario.createId("CW3");
		this.linkW3W2Id = scenario.createId("W3W2");
		this.linkW2W1Id = scenario.createId("W2W1");

		this.linkW1W2Id = scenario.createId("W1W2");
		this.linkW2W3Id = scenario.createId("W2W3");
		this.linkW3CId = scenario.createId("W3C");
		this.linkCE3Id = scenario.createId("CE3");
		this.linkE3E2Id = scenario.createId("E3E2");
		this.linkE2E1Id = scenario.createId("E2E1");
		
		this.sE = scenario.createId("sE");
		this.sW = scenario.createId("sW");
		this.sN = scenario.createId("sN");
		this.sS = scenario.createId("sS");
		this.systemId = scenario.createId("C");
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
		
		SignalPlanData plan = fac.createSignalPlanData(systemId);
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
		SignalData signal = systems.getFactory().createSignalData(sE);
		signal.setLinkId(linkE3CId);
		system.addSignalData(signal);
		signal = systems.getFactory().createSignalData(sW);
		signal.setLinkId(linkW3CId);
		system.addSignalData(signal);
		signal = systems.getFactory().createSignalData(sN);
		signal.setLinkId(linkN3CId);
		system.addSignalData(signal);
		signal = systems.getFactory().createSignalData(sS);
		signal.setLinkId(linkS3CId);
		system.addSignalData(signal);
		
		SignalUtils.createAndAddSignalGroups4Signals(signals.getSignalGroupsData(), system);
	}



	private void createPopulation(Scenario scenario, double lambdaWestEast) {
		
		Population pop = scenario.getPopulation();
		PopulationFactory fac = pop.getFactory();
		//N -> S
		List<Id> routeIds = new ArrayList<Id>();
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
		routeIds = new ArrayList<Id>();
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
		routeIds = new ArrayList<Id>();
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
		routeIds = new ArrayList<Id>();
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
		Person person = scenario.getPopulation().getFactory().createPerson(scenario.createId(Integer.toString(personIdInt)));
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
		config.scenario().setUseSignalSystems(true);
		QSimConfigGroup qsim = config.qsim();
		qsim.setSnapshotStyle("queue");
		qsim.setStuckTime(1000.0);
		config.qsim().setNodeOffset(20.0);
		config.otfVis().setShowTeleportedAgents(true);
		config.otfVis().setDrawNonMovingItems(true);
		return config;
	}
	

}
