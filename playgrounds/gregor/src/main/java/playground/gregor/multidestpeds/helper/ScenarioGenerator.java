package playground.gregor.multidestpeds.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;

import playground.gregor.multidestpeds.io.Mat2XYVxVyEvents;
import playground.gregor.sim2d_v2.config.Sim2DConfigGroup;
import playground.gregor.sim2d_v2.helper.gisdebug.GisDebugger;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;

public class ScenarioGenerator {

	private static final String MODE = "walkPrioQ";

	private static int nodeId = 0;
	private static int linkId = 0;

	public static void main(String [] args) {
		String inputMat = "/Users/laemmel/svn/shared-svn/projects/120multiDestPeds/experimental_data/Dez2010/joined/gr90.mat";
		//		String inputMat = "/Users/laemmel/svn/shared-svn/projects/120multiDestPeds/experimental_data/Dez2010/simulated/gr90_vo.mat";
		String scDir = "/Users/laemmel/devel/gr90/";
		String inputDir = scDir + "/input";

		Config c = ConfigUtils.createConfig();
		c.vspExperimental().setMatsimGlobalTimeFormat("HH:mm:ss.ss");

		Scenario sc = ScenarioUtils.createScenario(c);


		QSimConfigGroup qsim = new QSimConfigGroup();
		qsim.setEndTime(300);
				qsim.setTimeStepSize(1./25.);
		c.addModule("qsim", qsim);

		Sim2DConfigGroup s2d = new Sim2DConfigGroup();
		s2d.setFloorShapeFile(inputDir +"/floorplan.shp");

		s2d.setEnableCircularAgentInterActionModule("false");
		s2d.setEnableCollisionPredictionAgentInteractionModule("false");
		s2d.setEnableCollisionPredictionEnvironmentForceModule("false");
		s2d.setEnableDrivingForceModule("false");
		s2d.setEnableEnvironmentForceModule("false");
		s2d.setEnablePathForceModule("false");
		s2d.setEnableVelocityObstacleModule("true");
		s2d.setEnablePhysicalEnvironmentForceModule("false");


		String shpFile = s2d.getFloorShapeFile();
		ShapeFileReader r = new ShapeFileReader();
		r.readFileAndInitialize(shpFile);

		c.addModule("sim2d", s2d);

		createNetwork(sc,inputDir,r.getFeatureSet());

		createPop(sc,inputDir, inputMat);

		c.controler().setLastIteration(0);
		c.controler().setOutputDirectory(scDir + "output/");
		if (MODE.equals("walkPrioQ")) {
			c.controler().setMobsim("prioQ");
		} else {
			c.controler().setMobsim("hybridQ2D");
		}



		c.strategy().setMaxAgentPlanMemorySize(3);

		c.strategy().addParam("maxAgentPlanMemorySize", "3");
		c.strategy().addParam("Module_1", "ReRoute");
		c.strategy().addParam("ModuleProbability_1", "0.1");
		c.strategy().addParam("Module_2", "ChangeExpBeta");
		c.strategy().addParam("ModuleProbability_2", "0.9");

		c.network().setTimeVariantNetwork(true);

		new ConfigWriter(c).write(inputDir + "/config.xml");

	}

	private static void createPop(Scenario sc, String inputDir, String inputMat) {
		new Mat2XYVxVyEvents(sc, inputDir, inputMat,MODE).run();

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

	private static void createNetwork(Scenario sc, String dir, Set<Feature> set) {

		createLeftToRight(sc,set);
		createTopToBottom(sc,set);




		String networkOutputFile = dir+"/network.xml";
		((NetworkImpl)sc.getNetwork()).setEffectiveCellSize(0.26);
		((NetworkImpl)sc.getNetwork()).setEffectiveLaneWidth(0.71);
		((NetworkImpl)sc.getNetwork()).setCapacityPeriod(1);
		new NetworkWriter(sc.getNetwork()).write(networkOutputFile);
		sc.getConfig().network().setInputFile(networkOutputFile);
	}

	private static void createLeftToRight(Scenario sc, Set<Feature> set) {
		List<Link> links = new ArrayList<Link>();

		NetworkFactoryImpl nf = new NetworkFactoryImpl(sc.getNetwork());


		List<NodeImpl> nodes = new ArrayList<NodeImpl>();

		List<Coordinate> coords = new ArrayList<Coordinate>();

		coords.add(new Coordinate(-10,-2.));
		coords.add(new Coordinate(-7.5,-2.));
		coords.add(new Coordinate(-5,-2.));
		coords.add(new Coordinate(-2.5,-2.));
		coords.add(new Coordinate(0,-2.));
		coords.add(new Coordinate(2,-2));
		coords.add(new Coordinate(5,-2.));
		coords.add(new Coordinate(7.5,-2.));
		coords.add(new Coordinate(10,-2.));
		coords.add(new Coordinate(12.5,-2.));
		coords.add(new Coordinate(15,-2.));
		coords.add(new Coordinate(17.5,-2.));
		coords.add(new Coordinate(20,-2.));

		for (Coordinate coord : coords) {
			CoordImpl c = new CoordImpl(coord.x,coord.y);
			Node tmp = ((NetworkImpl)sc.getNetwork()).getNearestNode(c);
			NodeImpl n;
			if (tmp != null && c.calcDistance(tmp.getCoord()) <= 0.0000001) {
				n = (NodeImpl) tmp;
			} else {
				Id nid = new IdImpl(nodeId++);
				n= nf.createNode(nid, c);
				sc.getNetwork().addNode(n);
			}
			nodes.add(n);
		}


		for (int i = 0; i < nodes.size()-1; i++) {
			NodeImpl n0 = nodes.get(i);
			NodeImpl n1 = nodes.get(i+1);
			Id lid = new IdImpl(linkId++);

			Coordinate c0 = MGC.coord2Coordinate(n0.getCoord());
			Coordinate c1 = MGC.coord2Coordinate(n1.getCoord());
			double w = 2*estWidth(c0,c1,set);
			double cap  = w * 1.33;
			double lanes = 5.4*0.26 * w;
			double freespeed = 1.34;
			double length = c0.distance(c1); //-freespeed*((QSimConfigGroup)(sc.getConfig().getModule("qsim"))).getTimeStepSize();
			Link l = nf.createLink(lid, n0, n1, (NetworkImpl) sc.getNetwork(), length, freespeed,cap , lanes);
			sc.getNetwork().addLink(l);
			links.add(l);
		}

		//		NodeImpl n0 = nodes.get(nodes.size()-1);
		//		NodeImpl n1 = nodes.get(0);
		//		Id lid = new IdImpl(linkId++);
		//		Coordinate c0 = MGC.coord2Coordinate(n0.getCoord());
		//		Coordinate c1 = MGC.coord2Coordinate(n1.getCoord());
		//		Link l = nf.createLink(lid, n0, n1, (NetworkImpl) sc.getNetwork(), c0.distance(c1), 1.34, 1, 1);
		//		sc.getNetwork().addLink(l);
		//		links.add(l);

	}

	private static void createTopToBottom(Scenario sc, Set<Feature> set) {
		List<Link> links = new ArrayList<Link>();

		NetworkFactoryImpl nf = new NetworkFactoryImpl(sc.getNetwork());


		List<NodeImpl> nodes = new ArrayList<NodeImpl>();

		List<Coordinate> coords = new ArrayList<Coordinate>();
		coords.add(new Coordinate(2,10));
		coords.add(new Coordinate(2,7.5));
		coords.add(new Coordinate(2,4.5));
		coords.add(new Coordinate(2,2));
		coords.add(new Coordinate(2,-.5));
		coords.add(new Coordinate(2,-2));
		coords.add(new Coordinate(2,-5.5));
		coords.add(new Coordinate(2,-8));
		coords.add(new Coordinate(2,-10));
		coords.add(new Coordinate(2,-12.5));
		coords.add(new Coordinate(2,-15));
		coords.add(new Coordinate(2,-17.5));
		coords.add(new Coordinate(2,-20));
		//		for (double y = 10; y >= -7; y -= 0.5) {
		//			coords.add(new Coordinate(2,y));	
		//			if (y == 0) {
		//				y -= 4.5;
		//			}
		//		}

		for (Coordinate coord : coords) {
			CoordImpl c = new CoordImpl(coord.x,coord.y);
			Node tmp = ((NetworkImpl)sc.getNetwork()).getNearestNode(c);
			NodeImpl n;
			if (tmp != null && c.calcDistance(tmp.getCoord()) <= 0.0000001) {
				n = (NodeImpl) tmp;
			} else {
				Id nid = new IdImpl(nodeId++);
				n= nf.createNode(nid, c);
				sc.getNetwork().addNode(n);
			}
			nodes.add(n);
		}


		for (int i = 0; i < nodes.size()-1; i++) {
			NodeImpl n0 = nodes.get(i);
			NodeImpl n1 = nodes.get(i+1);
			Id lid = new IdImpl(linkId++);

			Coordinate c0 = MGC.coord2Coordinate(n0.getCoord());
			Coordinate c1 = MGC.coord2Coordinate(n1.getCoord());

			double w = 2*estWidth(c0,c1,set);
			double cap  = w * 1.33;
			double lanes = 5.4*0.26 * w;
			double freespeed = 1.34;
			double length = c0.distance(c1); //-freespeed*((QSimConfigGroup)(sc.getConfig().getModule("qsim"))).getTimeStepSize();
			Link l = nf.createLink(lid, n0, n1, (NetworkImpl) sc.getNetwork(), length, freespeed,cap , lanes);
			sc.getNetwork().addLink(l);
			links.add(l);
		}


	}

	private static double estWidth(Coordinate c0, Coordinate c1, Set<Feature> set) {
		LineString ls = GisDebugger.geofac.createLineString(new Coordinate[]{c0,c1});
		double minDist = 10;
		for (Feature ft : set) {
			Geometry geo = ft.getDefaultGeometry();
			double dist = ls.distance(geo);
			if (dist < minDist) {
				minDist = dist;
			}
		}
		return Math.max(minDist, 1.4);
	}

}
