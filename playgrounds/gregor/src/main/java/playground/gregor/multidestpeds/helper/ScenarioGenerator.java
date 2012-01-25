package playground.gregor.multidestpeds.helper;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;

import playground.gregor.multidestpeds.io.Mat2XYVxVyEvents;
import playground.gregor.sim2d_v2.config.Sim2DConfigGroup;

import com.vividsolutions.jts.geom.Coordinate;

public class ScenarioGenerator {
	
	
	private static int nodeId = 0;
	private static int linkId = 0;
	
	public static void main(String [] args) {
		String inputMat = "/Users/laemmel/svn/shared-svn/projects/120multiDestPeds/experimental_data/Dez2010/joined/gr90.mat";
		String scDir = "/Users/laemmel/devel/gr90/";
		String inputDir = scDir + "/input";
		
		Config c = ConfigUtils.createConfig();
		c.vspExperimental().setMatsimGlobalTimeFormat("HH:mm:ss.ss");

		Scenario sc = ScenarioUtils.createScenario(c);
		
		createNetwork(sc,inputDir);
		
		createPop(sc,inputDir, inputMat);
		
		c.controler().setLastIteration(0);
		c.controler().setOutputDirectory(scDir + "output/");
		c.controler().setMobsim("hybridQ2D");

		c.strategy().setMaxAgentPlanMemorySize(3);

		c.strategy().addParam("maxAgentPlanMemorySize", "3");
		c.strategy().addParam("Module_1", "ReRoute");
		c.strategy().addParam("ModuleProbability_1", "0.1");
		c.strategy().addParam("Module_2", "ChangeExpBeta");
		c.strategy().addParam("ModuleProbability_2", "0.9");
		
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


		c.addModule("sim2d", s2d);
		new ConfigWriter(c).write(inputDir + "/config.xml");
		
	}

	private static void createPop(Scenario sc, String inputDir, String inputMat) {
		new Mat2XYVxVyEvents(sc, inputDir, inputMat).run();
		
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

		createLeftToRight(sc,dir);
		createTopToBottom(sc,dir);
		
		

		
		String networkOutputFile = dir+"/network.xml";
		((NetworkImpl)sc.getNetwork()).setEffectiveCellSize(0.26);
		((NetworkImpl)sc.getNetwork()).setEffectiveLaneWidth(0.71);
		new NetworkWriter(sc.getNetwork()).write(networkOutputFile);
		sc.getConfig().network().setInputFile(networkOutputFile);
	}

	private static void createLeftToRight(Scenario sc, String dir) {
		List<Link> links = new ArrayList<Link>();

		NetworkFactoryImpl nf = new NetworkFactoryImpl(sc.getNetwork());

		
		List<NodeImpl> nodes = new ArrayList<NodeImpl>();

		List<Coordinate> coords = new ArrayList<Coordinate>();
		for (double x = -13; x <= 17; x += 0.5) {
			coords.add(new Coordinate(x,-2.));	
			if (x == 0) {
				x += 5;
			}
		}
		for (Coordinate coord : coords) {
			Id nid = new IdImpl(nodeId++);
			CoordImpl c = new CoordImpl(coord.x,coord.y);
			NodeImpl n = nf.createNode(nid, c);
			nodes.add(n);
			sc.getNetwork().addNode(n);
		}

		
		for (int i = 0; i < nodes.size()-1; i++) {
			NodeImpl n0 = nodes.get(i);
			NodeImpl n1 = nodes.get(i+1);
			Id lid = new IdImpl(linkId++);

			Coordinate c0 = MGC.coord2Coordinate(n0.getCoord());
			Coordinate c1 = MGC.coord2Coordinate(n1.getCoord());
			Link l = nf.createLink(lid, n0, n1, (NetworkImpl) sc.getNetwork(), c0.distance(c1), 1.34, 1, 1);
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
	
	private static void createTopToBottom(Scenario sc, String dir) {
		List<Link> links = new ArrayList<Link>();

		NetworkFactoryImpl nf = new NetworkFactoryImpl(sc.getNetwork());

		
		List<NodeImpl> nodes = new ArrayList<NodeImpl>();

		List<Coordinate> coords = new ArrayList<Coordinate>();
		for (double y = 10; y >= -7; y -= 0.5) {
			coords.add(new Coordinate(2,y));	
			if (y == 0.5) {
				y -= 5.5;
			}
		}
		
		for (Coordinate coord : coords) {
			Id nid = new IdImpl(nodeId++);
			CoordImpl c = new CoordImpl(coord.x,coord.y);
			NodeImpl n = nf.createNode(nid, c);
			nodes.add(n);
			sc.getNetwork().addNode(n);
		}

		
		for (int i = 0; i < nodes.size()-1; i++) {
			NodeImpl n0 = nodes.get(i);
			NodeImpl n1 = nodes.get(i+1);
			Id lid = new IdImpl(linkId++);

			Coordinate c0 = MGC.coord2Coordinate(n0.getCoord());
			Coordinate c1 = MGC.coord2Coordinate(n1.getCoord());
			Link l = nf.createLink(lid, n0, n1, (NetworkImpl) sc.getNetwork(), c0.distance(c1), 1.34, 1, 1);
			sc.getNetwork().addLink(l);
			links.add(l);
		}
		
		
	}

}
