package playground.gregor.scenariogen;


import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.gregor.sim2d_v3.config.Sim2DConfigGroup;
import playground.gregor.sim2d_v3.helper.gisdebug.GisDebugger;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class ScenarioGenerator {


	public static void main(String [] args) {
		String scDir = "/Users/laemmel/devel/counter/";
		String inputDir = scDir + "/input/";

		Config c = ConfigUtils.createConfig();

		Scenario sc = ScenarioUtils.createScenario(c);

		createAndSaveEnvironment(inputDir);

		createNetwork(sc,inputDir);

		createPop(sc,inputDir);


		c.controler().setLastIteration(0);
		c.controler().setOutputDirectory(scDir + "output/");
		c.controler().setMobsim("hybridQ2D");

		c.strategy().setMaxAgentPlanMemorySize(3);

		c.strategy().addParam("maxAgentPlanMemorySize", "3");
		c.strategy().addParam("Module_1", "ReRoute");
		c.strategy().addParam("ModuleProbability_1", "0.1");
		c.strategy().addParam("Module_2", "ChangeExpBeta");
		c.strategy().addParam("ModuleProbability_2", "0.9");
		//
		Sim2DConfigGroup s2d = new Sim2DConfigGroup();
		//		s2d.setFloorShapeFile(inputDir +"/bottleneck" + (int)width + "_" + (int)length +  ".shp");
		s2d.setFloorShapeFile(inputDir +"/floorplan.shp");

		s2d.setEnableCircularAgentInterActionModule("false");
		s2d.setEnableCollisionPredictionAgentInteractionModule("false");
		s2d.setEnableCollisionPredictionEnvironmentForceModule("false");
		s2d.setEnableDrivingForceModule("false");
		s2d.setEnableEnvironmentForceModule("false");
		s2d.setEnablePathForceModule("false");
		s2d.setEnableVelocityObstacleModule("true");
		s2d.setEnablePhysicalEnvironmentForceModule("false");


		c.addModule(s2d);
		new ConfigWriter(c).write(inputDir + "/config.xml");



	}

	private static void createPopII(Scenario sc, String inputDir) {
		Population pop = sc.getPopulation();
		PopulationFactory pb = pop.getFactory();

		int persId = 0;
		for (int i = 0; i < 20; i++) {

			for (double y = 10; y <= 10; y++) {
				Person pers = pb.createPerson(sc.createId("r"+Integer.toString(persId++)));
				pop.addPerson(pers);
				Plan plan = pb.createPlan();
				NetworkImpl net = (NetworkImpl) sc.getNetwork();
				Link l = net.getLinks().get(new IdImpl(0));
				ActivityImpl act = (ActivityImpl) pb.createActivityFromLinkId("h", l.getId());
				act.setCoord(new CoordImpl(2.5,y));
				act.setEndTime(i);
				plan.addActivity(act);
				Leg leg = pb.createLeg("walk2d");
				plan.addLeg(leg);
				Link l1 = net.getLinks().get(new IdImpl(4));
				Activity act2 = pb.createActivityFromLinkId("h", l1.getId());
				act2.setEndTime(0);
				plan.addActivity(act2);
				plan.setScore(0.);
				pers.addPlan(plan);
			}
			for (double y = 10; y <= 10; y++) {
				Person pers = pb.createPerson(sc.createId("g"+Integer.toString(persId++)));
				pop.addPerson(pers);
				Plan plan = pb.createPlan();
				NetworkImpl net = (NetworkImpl) sc.getNetwork();
				Link l = net.getLinks().get(new IdImpl(5));
				ActivityImpl act = (ActivityImpl) pb.createActivityFromLinkId("h", l.getId());
				act.setCoord(new CoordImpl(47.5,y));
				act.setEndTime(i);
				plan.addActivity(act);
				Leg leg = pb.createLeg("walk2d");
				plan.addLeg(leg);
				Link l1 = net.getLinks().get(new IdImpl(1));
				Activity act2 = pb.createActivityFromLinkId("h", l1.getId());
				act2.setEndTime(0);
				plan.addActivity(act2);
				plan.setScore(0.);
				pers.addPlan(plan);
			}
		}

		String outputPopulationFile = inputDir + "/plans.xml";
		new PopulationWriter(sc.getPopulation(), sc.getNetwork(), 1).write(outputPopulationFile);
		sc.getConfig().plans().setInputFile(outputPopulationFile);


		ActivityParams pre = new ActivityParams("h");
		pre.setTypicalDuration(49); // needs to be geq 49, otherwise when running a simulation one gets "java.lang.RuntimeException: zeroUtilityDuration of type pre-evac must be greater than 0.0. Did you forget to specify the typicalDuration?"
		// the reason is the double precision. see also comment in ActivityUtilityParameters.java (gl)
		pre.setMinimalDuration(49);
		pre.setClosingTime(49);
		pre.setEarliestEndTime(49);
		pre.setLatestStartTime(49);
		pre.setOpeningTime(49);


		//		ActivityParams post = new ActivityParams("post-evac");
		//		post.setTypicalDuration(49); // dito
		//		post.setMinimalDuration(49);
		//		post.setClosingTime(49);
		//		post.setEarliestEndTime(49);
		//		post.setLatestStartTime(49);
		//		post.setOpeningTime(49);
		sc.getConfig().planCalcScore().addActivityParams(pre);
		//		sc.getConfig().planCalcScore().addActivityParams(post);
	}

	private static void createPop(Scenario sc, String inputDir) {
		Population pop = sc.getPopulation();
		PopulationFactory pb = pop.getFactory();

		int persId = 0;
		for (int i = 0; i < 30; i ++) {
			for (double y = 2.5; y <= 17.5; y++) {
				Person pers = pb.createPerson(sc.createId("r"+Integer.toString(persId++)));
				pop.addPerson(pers);
				Plan plan = pb.createPlan();
				NetworkImpl net = (NetworkImpl) sc.getNetwork();
				Link l = net.getLinks().get(new IdImpl(0));
				ActivityImpl act = (ActivityImpl) pb.createActivityFromLinkId("h", l.getId());
				act.setCoord(new CoordImpl(2.5,y));
				act.setEndTime(i);
				plan.addActivity(act);
				Leg leg = pb.createLeg("walk2d");
				plan.addLeg(leg);
				Link l1 = net.getLinks().get(new IdImpl(4));
				Activity act2 = pb.createActivityFromLinkId("h", l1.getId());
				act2.setEndTime(0);
				plan.addActivity(act2);
				plan.setScore(0.);
				pers.addPlan(plan);
			}

			for (double y = 2.5; y <= 17.5; y++) {
				Person pers = pb.createPerson(sc.createId("g"+Integer.toString(persId++)));
				pop.addPerson(pers);
				Plan plan = pb.createPlan();
				NetworkImpl net = (NetworkImpl) sc.getNetwork();
				Link l = net.getLinks().get(new IdImpl(5));
				ActivityImpl act = (ActivityImpl) pb.createActivityFromLinkId("h", l.getId());
				act.setCoord(new CoordImpl(47.5,y));
				act.setEndTime(i);
				plan.addActivity(act);
				Leg leg = pb.createLeg("walk2d");
				plan.addLeg(leg);
				Link l1 = net.getLinks().get(new IdImpl(1));
				Activity act2 = pb.createActivityFromLinkId("h", l1.getId());
				act2.setEndTime(0);
				plan.addActivity(act2);
				plan.setScore(0.);
				pers.addPlan(plan);
			}
		}

		String outputPopulationFile = inputDir + "/plans.xml";
		new PopulationWriter(sc.getPopulation(), sc.getNetwork(), 1).write(outputPopulationFile);
		sc.getConfig().plans().setInputFile(outputPopulationFile);


		ActivityParams pre = new ActivityParams("h");
		pre.setTypicalDuration(49); // needs to be geq 49, otherwise when running a simulation one gets "java.lang.RuntimeException: zeroUtilityDuration of type pre-evac must be greater than 0.0. Did you forget to specify the typicalDuration?"
		// the reason is the double precision. see also comment in ActivityUtilityParameters.java (gl)
		pre.setMinimalDuration(49);
		pre.setClosingTime(49);
		pre.setEarliestEndTime(49);
		pre.setLatestStartTime(49);
		pre.setOpeningTime(49);


		//		ActivityParams post = new ActivityParams("post-evac");
		//		post.setTypicalDuration(49); // dito
		//		post.setMinimalDuration(49);
		//		post.setClosingTime(49);
		//		post.setEarliestEndTime(49);
		//		post.setLatestStartTime(49);
		//		post.setOpeningTime(49);
		sc.getConfig().planCalcScore().addActivityParams(pre);
		//		sc.getConfig().planCalcScore().addActivityParams(post);
	}


	private static void createNetwork(Scenario sc, String dir) {

		NetworkFactoryImpl nf = new NetworkFactoryImpl(sc.getNetwork());


		Id nid0 = new IdImpl(0);
		CoordImpl c0 = new CoordImpl(0,10);
		NodeImpl n0 = nf.createNode(nid0, c0);

		Id nid1 = new IdImpl(1);
		CoordImpl c1 = new CoordImpl(5,10);
		NodeImpl n1 = nf.createNode(nid1, c1);

		Id nid2 = new IdImpl(2);
		CoordImpl c2 = new CoordImpl(45,10);
		NodeImpl n2 = nf.createNode(nid2, c2);

		Id nid3 = new IdImpl(3);
		CoordImpl c3 = new CoordImpl(50,10);
		NodeImpl n3 = nf.createNode(nid3, c3);

		sc.getNetwork().addNode(n0);
		sc.getNetwork().addNode(n1);
		sc.getNetwork().addNode(n2);
		sc.getNetwork().addNode(n3);

		Id lid0 = new IdImpl(0);
		Link l0 = nf.createLink(lid0, n0, n1, (NetworkImpl) sc.getNetwork(), 5, 1.34, 1, 1);

		Id lid1 = new IdImpl(1);
		Link l1 = nf.createLink(lid1, n1, n0, (NetworkImpl) sc.getNetwork(), 5, 1.34, 1, 1);

		Id lid2 = new IdImpl(2);
		Link l2 = nf.createLink(lid2, n1, n2, (NetworkImpl) sc.getNetwork(), 40, 1.34, 1, 1);

		Id lid3 = new IdImpl(3);
		Link l3 = nf.createLink(lid3, n2, n1, (NetworkImpl) sc.getNetwork(), 40, 1.34, 1, 1);

		Id lid4 = new IdImpl(4);
		Link l4 = nf.createLink(lid4, n2, n3, (NetworkImpl) sc.getNetwork(), 5, 1.34, 1, 1);

		Id lid5 = new IdImpl(5);
		Link l5 = nf.createLink(lid5, n3, n2, (NetworkImpl) sc.getNetwork(), 5, 1.34, 1, 1);

		sc.getNetwork().addLink(l0);
		sc.getNetwork().addLink(l1);
		sc.getNetwork().addLink(l2);
		sc.getNetwork().addLink(l3);
		sc.getNetwork().addLink(l4);
		sc.getNetwork().addLink(l5);


		String networkOutputFile = dir+"/network.xml";
		((NetworkImpl)sc.getNetwork()).setEffectiveCellSize(0.26);
		((NetworkImpl)sc.getNetwork()).setEffectiveLaneWidth(0.71);
		new NetworkWriter(sc.getNetwork()).write(networkOutputFile);
		sc.getConfig().network().setInputFile(networkOutputFile);


	}

	private static void createAndSaveEnvironment(String dir) {
		GeometryFactory geofac = new GeometryFactory();

		//hallway 50m length 20m width
		Coordinate c0 = new Coordinate(0,0);
		Coordinate c1 = new Coordinate(50,0);
		LineString ls0 = geofac.createLineString(new Coordinate[]{c0,c1});
		GisDebugger.addGeometry(ls0);

		Coordinate c2 = new Coordinate(0,20);
		Coordinate c3 = new Coordinate(50,20);
		LineString ls1 = geofac.createLineString(new Coordinate[]{c2,c3});
		GisDebugger.addGeometry(ls1);

		GisDebugger.dump(dir + "/floorplan.shp");

	}
}
