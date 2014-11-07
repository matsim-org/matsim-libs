package matsimConnector.scenarioGenerator;

import java.util.HashSet;
import java.util.Set;

import matsimConnector.utility.Constants;
import matsimConnector.utility.Distances;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;

import pedCA.output.Log;
import scenarios.ContextGenerator;

public class ScenarioGenerator {
	private static String inputDir = "C:/Users/Luca/Documents/uni/Dottorato/Juelich/developing_stuff/Test/input";
	private static String outputDir = "C:/Users/Luca/Documents/uni/Dottorato/Juelich/developing_stuff/Test/output";

	private static final Double DOOR_WIDTH = 2.4;
	private static final Double CA_LENGTH = 10.;
	private static final int CA_ROWS = (int)Math.round((DOOR_WIDTH/Constants.CA_CELL_SIDE));
	private static final int CA_COLS = (int)Math.round((CA_LENGTH/Constants.CA_CELL_SIDE));
	private static final int POPULATION_SIZE = 1;
	
	public static void main(String [] args) {
		Config c = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(c);
		
		createNetwork(scenario);
		
		c.network().setInputFile(inputDir + "/network.xml.gz");

		//		c.strategy().addParam("Module_1", "playground.gregor.sim2d_v4.replanning.Sim2DReRoutePlanStrategy");
		c.strategy().addParam("Module_1", "ReRoute");
		c.strategy().addParam("ModuleProbability_1", ".1");
		c.strategy().addParam("ModuleDisableAfterIteration_1", "100");
		c.strategy().addParam("Module_2", "ChangeExpBeta");
		c.strategy().addParam("ModuleProbability_2", ".9");

		c.controler().setOutputDirectory(outputDir);
		c.controler().setLastIteration(200);

		c.plans().setInputFile(inputDir + "/population.xml.gz");

		ActivityParams pre = new ActivityParams("origin");
		// needs to be geq 49, otherwise when running a simulation one gets "java.lang.RuntimeException: zeroUtilityDuration of type pre-evac must be greater than 0.0. Did you forget to specify the typicalDuration?"
		// the reason is the double precision. see also comment in ActivityUtilityParameters.java (gl)
		pre.setTypicalDuration(49); 
		pre.setMinimalDuration(49);
		pre.setClosingTime(49);
		pre.setEarliestEndTime(49);
		pre.setLatestStartTime(49);
		pre.setOpeningTime(49);


		ActivityParams post = new ActivityParams("destination");
		post.setTypicalDuration(49); 
		post.setMinimalDuration(49);
		post.setClosingTime(49);
		post.setEarliestEndTime(49);
		post.setLatestStartTime(49);
		post.setOpeningTime(49);
		scenario.getConfig().planCalcScore().addActivityParams(pre);
		scenario.getConfig().planCalcScore().addActivityParams(post);
		scenario.getConfig().planCalcScore().setLateArrival_utils_hr(0.);
		scenario.getConfig().planCalcScore().setPerforming_utils_hr(0.);


		QSimConfigGroup qsim = scenario.getConfig().qsim();
		qsim.setEndTime(20*60);
		c.controler().setMobsim(Constants.CA_MOBSIM_MODE);
		c.global().setCoordinateSystem("EPSG:3395");
		c.qsim().setEndTime(60*10);

		createCAScenario();
		createPopulation(scenario);

		new ConfigWriter(c).write(inputDir+ "/config.xml");
		new NetworkWriter(scenario.getNetwork()).write(c.network().getInputFile());
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(c.plans().getInputFile());
	}


	private static void createCAScenario() {
		Log.log("CA Scenario generation");
		ContextGenerator.createAndSaveBidCorridorContext(inputDir+"/CAScenario", CA_ROWS, CA_COLS, 2);
	}

	private static void createNetwork(Scenario sc) {
		Network net = sc.getNetwork();
		NetworkFactory fac = net.getFactory();
		Node n0 = fac.createNode(Id.create("n0",Node.class), new CoordImpl(-20.,DOOR_WIDTH/2));
		Node n1 = fac.createNode(Id.create("n1",Node.class), new CoordImpl(-10.,DOOR_WIDTH/2));
		Node n2 = fac.createNode(Id.create("n2",Node.class), new CoordImpl(-0.2,DOOR_WIDTH/2));
		Node n5 = fac.createNode(Id.create("n5",Node.class), new CoordImpl(CA_LENGTH+0.,DOOR_WIDTH/2));
		Node n6 = fac.createNode(Id.create("n6",Node.class), new CoordImpl(CA_LENGTH+10.,DOOR_WIDTH/2));
		Node n7 = fac.createNode(Id.create("n7",Node.class), new CoordImpl(CA_LENGTH+20.,DOOR_WIDTH/2));
		
		net.addNode(n0);
		net.addNode(n1);
		net.addNode(n2);
		net.addNode(n5);
		net.addNode(n6);
		net.addNode(n7);
		
		double flow = Constants.FLOPW_CAP_PER_METER_WIDTH * DOOR_WIDTH;
		double lanes = DOOR_WIDTH/0.71;
		
		Link l0 = fac.createLink(Id.create("l0",Link.class), n0, n1);
		Link l1 = fac.createLink(Id.create("l1",Link.class), n1, n2);
		Link l2 = fac.createLink(Id.create("l2",Link.class), n5, n6);
		Link l3 = fac.createLink(Id.create("l3",Link.class), n6, n7);
		
		Link l0Rev = fac.createLink(Id.create("l0Rev",Link.class), n1, n0);
		Link l1Rev = fac.createLink(Id.create("l1Rev",Link.class), n2, n1);
		Link l2Rev = fac.createLink(Id.create("l2Rev",Link.class), n6, n5);
		Link l3Rev = fac.createLink(Id.create("l3Rev",Link.class), n7, n6);
		
		Set<String> modes = new HashSet<String>();
		modes.add("walk");modes.add("car");
		l0.setLength(Distances.EuclideanDistance(l0));
		l1.setLength(Distances.EuclideanDistance(l1));
		l2.setLength(Distances.EuclideanDistance(l2));
		l3.setLength(Distances.EuclideanDistance(l3));
		
		l0Rev.setLength(Distances.EuclideanDistance(l0Rev));
		l1Rev.setLength(Distances.EuclideanDistance(l1Rev));
		l2Rev.setLength(Distances.EuclideanDistance(l2Rev));
		l3Rev.setLength(Distances.EuclideanDistance(l3Rev));
		
		l0.setAllowedModes(modes);
		l1.setAllowedModes(modes);
		l2.setAllowedModes(modes);
		l3.setAllowedModes(modes);

		l0Rev.setAllowedModes(modes);
		l1Rev.setAllowedModes(modes);
		l2Rev.setAllowedModes(modes);
		l3Rev.setAllowedModes(modes);
		
		l0.setFreespeed(Constants.PEDESTRIAN_SPEED);
		l1.setFreespeed(Constants.PEDESTRIAN_SPEED);
		l2.setFreespeed(Constants.PEDESTRIAN_SPEED);
		l3.setFreespeed(Constants.PEDESTRIAN_SPEED);

		l0Rev.setFreespeed(Constants.PEDESTRIAN_SPEED);
		l1Rev.setFreespeed(Constants.PEDESTRIAN_SPEED);
		l2Rev.setFreespeed(Constants.PEDESTRIAN_SPEED);
		l3Rev.setFreespeed(Constants.PEDESTRIAN_SPEED);
		
		l0.setCapacity(flow);
		l1.setCapacity(flow);
		l2.setCapacity(flow);
		l3.setCapacity(flow);

		l0Rev.setCapacity(flow);
		l1Rev.setCapacity(flow);
		l2Rev.setCapacity(flow);
		l3Rev.setCapacity(flow);
		
		l0.setNumberOfLanes(lanes);
		l1.setNumberOfLanes(lanes);
		l2.setNumberOfLanes(lanes);
		l3.setNumberOfLanes(lanes);
		
		l0Rev.setNumberOfLanes(lanes);
		l1Rev.setNumberOfLanes(lanes);
		l2Rev.setNumberOfLanes(lanes);
		l3Rev.setNumberOfLanes(lanes);
		
		net.addLink(l0);
		net.addLink(l1);
		net.addLink(l2);
		net.addLink(l3);
		
		net.addLink(l0Rev);
		net.addLink(l1Rev);
		net.addLink(l2Rev);
		net.addLink(l3Rev);

		((NetworkImpl)net).setCapacityPeriod(1);
		((NetworkImpl)net).setEffectiveCellSize(.26);
		((NetworkImpl)net).setEffectiveLaneWidth(.71);
	}
	
	private static void createPopulation(Scenario sc) {
		Population population = sc.getPopulation();
		population.getPersons().clear();
		PopulationFactory factory = population.getFactory();
		double t = 0;
		for (int i = 0; i < POPULATION_SIZE/2; i++) {
			Person pers = factory.createPerson(Id.create("b"+i,Person.class));
			Plan plan = factory.createPlan();
			pers.addPlan(plan);
			Activity act0;
			act0 = factory.createActivityFromLinkId("origin", Id.create("l0",Link.class));
			act0.setEndTime(t);
			plan.addActivity(act0);
			Leg leg = factory.createLeg("car");
			plan.addLeg(leg);
			Activity act1 = factory.createActivityFromLinkId("destination", Id.create("l3",Link.class));
			plan.addActivity(act1);
			population.addPerson(pers);
		}
		for (int i = POPULATION_SIZE/2; i < POPULATION_SIZE; i++) {
			Person pers = factory.createPerson(Id.create("a"+i,Person.class));
			Plan plan = factory.createPlan();
			pers.addPlan(plan);
			Activity act0;
			act0 = factory.createActivityFromLinkId("origin", Id.create("l3Rev",Link.class));
			act0.setEndTime(t);
			plan.addActivity(act0);
			Leg leg = factory.createLeg("car");
			plan.addLeg(leg);
			Activity act1 = factory.createActivityFromLinkId("destination", Id.create("l0Rev",Link.class));
			plan.addActivity(act1);
			population.addPerson(pers);
		}
	}
}
