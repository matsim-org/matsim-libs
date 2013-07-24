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

public class CrissCross {
//	private static final String MODE = "walk2d";
	private static final String MODE = "car";
	private static int nid = 0;
	private static int lid = 0;

	private static Id id0_s;
	private static Id id0_t;
	private static Id id1_s;
	private static Id id1_t;

	public static void main(String [] args) {
		String scDir = "/Users/laemmel/devel/crisscross/";
		String inputDir = scDir + "/input/";

		Config c = ConfigUtils.createConfig();

		Scenario sc = ScenarioUtils.createScenario(c);

		createAndSaveEnvironment(inputDir);

		createNetwork(sc,inputDir);

		createPop(sc,inputDir);

		c.network().setTimeVariantNetwork(true);
		
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

			if (i > 20 && i < 30) {
				incr1 = 1;
			}else if (i >= 30 && i < 50){
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
				Link l = net.getLinks().get(id0_s);
				ActivityImpl act = (ActivityImpl) pb.createActivityFromLinkId("h", l.getId());
				act.setCoord(new CoordImpl(-12,y));
				act.setEndTime(i);
				plan.addActivity(act);
				Leg leg = pb.createLeg(MODE);
				plan.addLeg(leg);
				Link l1 = net.getLinks().get(id0_t);
				Activity act2 = pb.createActivityFromLinkId("h", l1.getId());
//				act2.setEndTime(Double.POSITIVE_INFINITY);
				plan.addActivity(act2);
				plan.setScore(0.);
				pers.addPlan(plan);
			}
			if (i > 20 && i < 30) {
				incr2 = 1;
			}else if (i >= 30 && i < 50){
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
				Link l = net.getLinks().get(id1_s);
				ActivityImpl act = (ActivityImpl) pb.createActivityFromLinkId("h", l.getId());
				act.setCoord(new CoordImpl(x,12));
				act.setEndTime(i);
				plan.addActivity(act);
				Leg leg = pb.createLeg(MODE);
				plan.addLeg(leg);
				Link l1 = net.getLinks().get(id1_t);
				Activity act2 = pb.createActivityFromLinkId("h", l1.getId());
//				act2.setEndTime(Double.POSITIVE_INFINITY);
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


	private static Id createLink(double x0, double y0, double x1, double y1,
			NetworkFactoryImpl nf,NetworkImpl net, Scenario sc, double lanes, double flowCap) {

		CoordImpl c0 = new CoordImpl(x0, y0);
		NodeImpl n0 = (NodeImpl) net.getNearestNode(c0);
		if (n0 == null || c0.calcDistance(n0.getCoord()) > 0) {
			n0 = nf.createNode(new IdImpl(nid++), c0);
			sc.getNetwork().addNode(n0);
		}

		CoordImpl c1 = new CoordImpl(x1, y1);
		NodeImpl n1 = (NodeImpl) net.getNearestNode(c1);
		if (n1 == null || c1.calcDistance(n1.getCoord()) > 0) {
			n1 = nf.createNode(new IdImpl(nid++), c1);
			sc.getNetwork().addNode(n1);
		}
		Link l0 = nf.createLink(new IdImpl(lid++), n0, n1, (NetworkImpl) sc.getNetwork(), c1.calcDistance(c0), 1.34, flowCap, lanes);
		sc.getNetwork().addLink(l0);

		Link l1 = nf.createLink(new IdImpl(lid++), n1, n0, (NetworkImpl) sc.getNetwork(), c1.calcDistance(c0), 1.34, flowCap, lanes);
		sc.getNetwork().addLink(l1);
		return l0.getId();
	}

	
	private static Id createLink(double d, double e, double f, double g,
			NetworkFactoryImpl nf, NetworkImpl network, Scenario sc) {
		
		return createLink(d,e,f,g,nf,network,sc,5.4*0.26*5,5*1.33);
	}
	
	private static void createNetwork(Scenario sc, String dir) {

		NetworkFactoryImpl nf = new NetworkFactoryImpl(sc.getNetwork());
		id0_s = createLink(-12.5,0.,-7.5,0.,nf,(NetworkImpl)sc.getNetwork(),sc);
		createLink(-7.5,0.,-2.5,0,nf,(NetworkImpl)sc.getNetwork(),sc);
		createLink(-2.5,0.,2.5,0,nf,(NetworkImpl)sc.getNetwork(),sc);
		createLink(2.5,0.,12.5,0,nf,(NetworkImpl)sc.getNetwork(),sc);
		createLink(12.5,0.,17.5,0,nf,(NetworkImpl)sc.getNetwork(),sc);
		createLink(17.5,0.,22.5,0,nf,(NetworkImpl)sc.getNetwork(),sc);
		id0_t = createLink(22.5,0.,27.5,0,nf,(NetworkImpl)sc.getNetwork(),sc);

		id1_s = createLink(0,12.5,0,7.5,nf,(NetworkImpl)sc.getNetwork(),sc);
		createLink(0,7.5,0,2.5,nf,(NetworkImpl)sc.getNetwork(),sc);
		createLink(0,2.5,0,-2.5,nf,(NetworkImpl)sc.getNetwork(),sc);
		createLink(0,-2.5,0,-15,nf,(NetworkImpl)sc.getNetwork(),sc);
		createLink(0,-15,5,-15,nf,(NetworkImpl)sc.getNetwork(),sc);
		createLink(5,-15,7.5,-15,nf,(NetworkImpl)sc.getNetwork(),sc,5.4*0.26*1.5,1.5*1.33);
		createLink(7.5,-15,15,-15,nf,(NetworkImpl)sc.getNetwork(),sc);
		createLink(15,-15,15,-2.5,nf,(NetworkImpl)sc.getNetwork(),sc);
		createLink(15,-2.5,15,2.5,nf,(NetworkImpl)sc.getNetwork(),sc);
		createLink(15,2.5,15,7.5,nf,(NetworkImpl)sc.getNetwork(),sc);
		id1_t = createLink(15,7.5,15,12.5,nf,(NetworkImpl)sc.getNetwork(),sc);

		String networkOutputFile = dir+"/network.xml";
		((NetworkImpl)sc.getNetwork()).setEffectiveCellSize(0.26);
		((NetworkImpl)sc.getNetwork()).setEffectiveLaneWidth(0.71);
		((NetworkImpl)sc.getNetwork()).setCapacityPeriod(1);
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

		Coordinate c12 = new Coordinate(17.5,-2.5);
		Coordinate c13 = new Coordinate(27.5,-2.5);
		LineString ls4 = geofac.createLineString(new Coordinate[]{c12,c13});
		GisDebugger.addGeometry(ls4);

		Coordinate c14 = new Coordinate(17.5,2.5);
		Coordinate c15 = new Coordinate(27.5,2.5);
		LineString ls5 = geofac.createLineString(new Coordinate[]{c14,c15});
		GisDebugger.addGeometry(ls5);

		Coordinate c16 = new Coordinate(12.5,-2.5);
		Coordinate c17 = new Coordinate(12.5,-12.5);
		Coordinate c18 = new Coordinate(2.5,-12.5);
		LineString ls6 = geofac.createLineString(new Coordinate[]{c16,c17,c18});
		GisDebugger.addGeometry(ls6);

		Coordinate c19 = new Coordinate(-2.5,-12.5);
		Coordinate c20 = new Coordinate(-2.5,-17.5);
		Coordinate c21 = new Coordinate(17.5,-17.5);
		Coordinate c22 = new Coordinate(17.5,-2.5);
		LineString ls7 = geofac.createLineString(new Coordinate[]{c19,c20,c21,c22});
		GisDebugger.addGeometry(ls7);

		Coordinate c23 = new Coordinate(12.5,2.5);
		Coordinate c24 = new Coordinate(12.5,12.5);
		LineString ls8 = geofac.createLineString(new Coordinate[]{c23,c24});
		GisDebugger.addGeometry(ls8);

		Coordinate c25 = new Coordinate(17.5,2.5);
		Coordinate c26 = new Coordinate(17.5,12.5);
		LineString ls9 = geofac.createLineString(new Coordinate[]{c25,c26});
		GisDebugger.addGeometry(ls9);

		Coordinate c27 = new Coordinate(5,-12.5);
		Coordinate c28 = new Coordinate(5,-14.5);
		Coordinate c29 = new Coordinate(7.5,-14.5);
		Coordinate c30 = new Coordinate(7.5,-12.5);
		LineString ls10 = geofac.createLineString(new Coordinate[]{c27,c28,c29,c30});
		GisDebugger.addGeometry(ls10);
		
		Coordinate c31 = new Coordinate(5,-17.5);
		Coordinate c32 = new Coordinate(5,-16);
		Coordinate c33 = new Coordinate(7.5,-16);
		Coordinate c34 = new Coordinate(7.5,-17.5);
		LineString ls11 = geofac.createLineString(new Coordinate[]{c31,c32,c33,c34});
		GisDebugger.addGeometry(ls11);
		
		GisDebugger.dump(dir + "/floorplan.shp");

	}
}
