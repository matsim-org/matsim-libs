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
import org.matsim.core.gbl.MatsimRandom;
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

public class ScenarioGeneratorII {
	public static void main(String [] args) {
		String scDir = "/Users/laemmel/devel/crossing/";
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

	private static void createPop(Scenario sc, String inputDir) {
		Population pop = sc.getPopulation();
		PopulationFactory pb = pop.getFactory();

		int persId = 0;
		double incr1 = 1;
		double incr2 = 1;
		for (int i = 0; i < 500; i++) {
			
			if (i > 20 && i < 40) {
				incr1 = 1;
			}else if (i >= 40 && i < 80){
				incr1 = 100;
				i+=10;
			}else if (i %7 == 0) {
				double r = MatsimRandom.getRandom().nextDouble();
				if (r < 0.1) {
					incr1 = 1;
				} else if (r < 0.15) {
					incr1 = 1.1;
				} else if (r < 0.25) {
					incr1 = 1.9;
				} else if (r < 0.75) {
						incr1 = 2.5;
 				} else {
 					incr1 = 5;
 				}
			}
			for (double y = -2; y <= 2; y+= incr1) {
				Person pers = pb.createPerson(sc.createId("r"+Integer.toString(persId++)));
				pop.addPerson(pers);
				Plan plan = pb.createPlan();
				NetworkImpl net = (NetworkImpl) sc.getNetwork();
				Link l = net.getLinks().get(new IdImpl(0));
				ActivityImpl act = (ActivityImpl) pb.createActivityFromLinkId("h", l.getId());
				act.setCoord(new CoordImpl(-12,y));
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
			if (i > 20 && i < 40) {
				incr2 = 1;
			}else if (i >= 40 && i < 80){
				incr2 = 100;
			}else if (i %7 == 0) {
				double r = MatsimRandom.getRandom().nextDouble();
				if (r < 0.1) {
					incr2 = 1;
				} else if (r < 0.15) {
					incr2 = 1.1;
				} else if (r < 0.25) {
					incr2 = 1.9;
				} else if (r < 0.75) {
						incr2 = 2.5;
 				} else {
 					incr2 = 5;
 				}
			}
//			if (MatsimRandom.getRandom().nextDouble() > 0.8){
//				incr = (MatsimRandom.getRandom().nextDouble()+0.05)*8;
//			}
			for (double x = -2; x <= 2; x+=incr2 ) {
				Person pers = pb.createPerson(sc.createId("g"+Integer.toString(persId++)));
				pop.addPerson(pers);
				Plan plan = pb.createPlan();
				NetworkImpl net = (NetworkImpl) sc.getNetwork();
				Link l = net.getLinks().get(new IdImpl(6));
				ActivityImpl act = (ActivityImpl) pb.createActivityFromLinkId("h", l.getId());
				act.setCoord(new CoordImpl(x,12));
				act.setEndTime(i);
				plan.addActivity(act);
				Leg leg = pb.createLeg("walk2d");
				plan.addLeg(leg);
				Link l1 = net.getLinks().get(new IdImpl(10));
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

		sc.getConfig().planCalcScore().addActivityParams(pre);
	}


	private static void createNetwork(Scenario sc, String dir) {

		NetworkFactoryImpl nf = new NetworkFactoryImpl(sc.getNetwork());


		Id nid0 = new IdImpl(0);
		CoordImpl c0 = new CoordImpl(-12.5,0);
		NodeImpl n0 = nf.createNode(nid0, c0);

		Id nid0b = new IdImpl(-1);
		CoordImpl c0b = new CoordImpl(-7.5,0);
		NodeImpl n0b = nf.createNode(nid0b, c0b);
		
		Id nid1 = new IdImpl(1);
		CoordImpl c1 = new CoordImpl(-2.5,0);
		NodeImpl n1 = nf.createNode(nid1, c1);

		Id nid2 = new IdImpl(2);
		CoordImpl c2 = new CoordImpl(2.5,0);
		NodeImpl n2 = nf.createNode(nid2, c2);

		Id nid3 = new IdImpl(3);
		CoordImpl c3 = new CoordImpl(12.5,0);
		NodeImpl n3 = nf.createNode(nid3, c3);

		sc.getNetwork().addNode(n0);
		sc.getNetwork().addNode(n0b);
		sc.getNetwork().addNode(n1);
		sc.getNetwork().addNode(n2);
		sc.getNetwork().addNode(n3);

		Id lid0 = new IdImpl(0);
		Link l0 = nf.createLink(lid0, n0, n0b, (NetworkImpl) sc.getNetwork(), 5, 1.34, 1, 1);
		
		Id lid0b = new IdImpl(-1);
		Link l0b = nf.createLink(lid0b, n0b, n1, (NetworkImpl) sc.getNetwork(), 5, 1.34, 1, 1);

		Id lid1 = new IdImpl(1);
		Link l1 = nf.createLink(lid1, n1, n0, (NetworkImpl) sc.getNetwork(), 10, 1.34, 1, 1);

		Id lid2 = new IdImpl(2);
		Link l2 = nf.createLink(lid2, n1, n2, (NetworkImpl) sc.getNetwork(), 5, 1.34, 1, 1);

		Id lid3 = new IdImpl(3);
		Link l3 = nf.createLink(lid3, n2, n1, (NetworkImpl) sc.getNetwork(), 5, 1.34, 1, 1);

		Id lid4 = new IdImpl(4);
		Link l4 = nf.createLink(lid4, n2, n3, (NetworkImpl) sc.getNetwork(), 10, 1.34, 1, 1);

		Id lid5 = new IdImpl(5);
		Link l5 = nf.createLink(lid5, n3, n2, (NetworkImpl) sc.getNetwork(), 10, 1.34, 1, 1);

		sc.getNetwork().addLink(l0);
		sc.getNetwork().addLink(l0b);
		sc.getNetwork().addLink(l1);
		sc.getNetwork().addLink(l2);
		sc.getNetwork().addLink(l3);
		sc.getNetwork().addLink(l4);
		sc.getNetwork().addLink(l5);


		Id nid5 = new IdImpl(5);
		CoordImpl c5 = new CoordImpl(0,12.5);
		NodeImpl n5 = nf.createNode(nid5, c5);
		
		Id nid5b = new IdImpl(-2);
		CoordImpl c5b = new CoordImpl(0,7.5);
		NodeImpl n5b = nf.createNode(nid5b, c5b);

		Id nid6 = new IdImpl(6);
		CoordImpl c6 = new CoordImpl(0,2.5);
		NodeImpl n6 = nf.createNode(nid6, c6);

		Id nid7 = new IdImpl(7);
		CoordImpl c7 = new CoordImpl(0,-2.5);
		NodeImpl n7 = nf.createNode(nid7, c7);

		Id nid8 = new IdImpl(8);
		CoordImpl c8 = new CoordImpl(0,-12.5);
		NodeImpl n8 = nf.createNode(nid8, c8);

		sc.getNetwork().addNode(n5);
		sc.getNetwork().addNode(n5b);
		sc.getNetwork().addNode(n6);
		sc.getNetwork().addNode(n7);
		sc.getNetwork().addNode(n8);

		Id lid6 = new IdImpl(6);
		Link l6 = nf.createLink(lid6, n5, n5b, (NetworkImpl) sc.getNetwork(), 5, 1.34, 1, 1);
		
		Id lid6b = new IdImpl(-2);
		Link l6b = nf.createLink(lid6b, n5b, n6, (NetworkImpl) sc.getNetwork(), 5, 1.34, 1, 1);

		Id lid7 = new IdImpl(7);
		Link l7 = nf.createLink(lid7, n6, n5, (NetworkImpl) sc.getNetwork(), 10, 1.34, 1, 1);

		Id lid8 = new IdImpl(8);
		Link l8 = nf.createLink(lid8, n6, n7, (NetworkImpl) sc.getNetwork(), 5, 1.34, 1, 1);

		Id lid9 = new IdImpl(9);
		Link l9 = nf.createLink(lid9, n7, n6, (NetworkImpl) sc.getNetwork(), 5, 1.34, 1, 1);

		Id lid10 = new IdImpl(10);
		Link l10 = nf.createLink(lid10, n7, n8, (NetworkImpl) sc.getNetwork(), 10, 1.34, 1, 1);

		Id lid11 = new IdImpl(11);
		Link l11 = nf.createLink(lid11, n8, n7, (NetworkImpl) sc.getNetwork(), 10, 1.34, 1, 1);

		sc.getNetwork().addLink(l6);
		sc.getNetwork().addLink(l6b);
		sc.getNetwork().addLink(l7);
		sc.getNetwork().addLink(l8);
		sc.getNetwork().addLink(l9);
		sc.getNetwork().addLink(l10);
		sc.getNetwork().addLink(l11);


		String networkOutputFile = dir+"/network.xml";
		((NetworkImpl)sc.getNetwork()).setEffectiveCellSize(0.26);
		((NetworkImpl)sc.getNetwork()).setEffectiveLaneWidth(0.71);
		new NetworkWriter(sc.getNetwork()).write(networkOutputFile);
		sc.getConfig().network().setInputFile(networkOutputFile);


	}

	private static void createAndSaveEnvironment(String dir) {
		GeometryFactory geofac = new GeometryFactory();

		Coordinate c0 = new Coordinate(-12.5,2.5);
		Coordinate c1 = new Coordinate(-2.5,2.5);
		Coordinate c2 = new Coordinate(-2.5,12.5);
		LineString ls0 = geofac.createLineString(new Coordinate[]{c0,c1,c2});
		GisDebugger.addGeometry(ls0);

		Coordinate c3 = new Coordinate(2.5,12.5);
		Coordinate c4 = new Coordinate(2.5,2.5);
		Coordinate c5 = new Coordinate(12.5,2.5);
		LineString ls1 = geofac.createLineString(new Coordinate[]{c3,c4,c5});
		GisDebugger.addGeometry(ls1);

		Coordinate c6 = new Coordinate(-12.5,-2.5);
		Coordinate c7 = new Coordinate(-2.5,-2.5);
		Coordinate c8 = new Coordinate(-2.5,-12.5);
		LineString ls2 = geofac.createLineString(new Coordinate[]{c6,c7,c8});
		GisDebugger.addGeometry(ls2);

		Coordinate c9 = new Coordinate(2.5,-12.5);
		Coordinate c10 = new Coordinate(2.5,-2.5);
		Coordinate c11 = new Coordinate(12.5,-2.5);
		LineString ls3 = geofac.createLineString(new Coordinate[]{c9,c10,c11});
		GisDebugger.addGeometry(ls3);

		GisDebugger.dump(dir + "/floorplan.shp");

	}
}
