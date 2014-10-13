package playground.wdoering.multidestpeds.helper;


import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
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
import org.matsim.core.utils.geometry.geotools.MGC;

import playground.wdoering.oldstufffromgregor.GisDebugger;
import playground.wdoering.oldstufffromgregor.Sim2DConfigGroup;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

public class Bottleneck
{


	private static int persId = 0;
	

	public static void main(String [] args)
	{
		if (args.length != 0)
		{
			System.err.println("No output path given! ( Bottleneck.java /path/to/output/ )");
			System.exit(0);
		}
		else
		{
			//get path
			//				String scDir = args[0];
			String scDir = "C:/temp/bottleneck/";
//			String scDir = "/Users/laemmel/devel/bottleneck/";


			//Bottleneck length, width and distance between waiting area and bottleneck
			double length = 4.0;
			double width = 2.5;
			double distance = 3;
			double waitingAreaWidth = 7;
			int persons = 175;
			
			int model = 2; //0=social force model, 1=collision prediction, 2=velocity obstacle
			
			String[] modelName = {"sf","cp","vo"};
			
			String subdir = modelName[model] +  "/p"+ persons +"-w" + width;
			
			

			//set input directory
			String inputDir = scDir + "/" + subdir + "/input/";
			
			new File(inputDir).mkdirs();
			
			//create config
			Config c = ConfigUtils.createConfig();
			Scenario scenario = ScenarioUtils.createScenario(c);

			//create (and save) environment
			createAndSaveEnvironment(inputDir,length, distance, width, waitingAreaWidth);

			//create nodes and links (network)
			List<Link> links = createNetwork(scenario, inputDir, distance, length, width, waitingAreaWidth);

			//create population
			createPop(scenario, inputDir, links, length, width, waitingAreaWidth, persons);
			//
			c.controler().setLastIteration(0);
			c.controler().setOutputDirectory(scDir + subdir +  "/output/");
			c.controler().setMobsim("hybridQ2D");

			c.strategy().setMaxAgentPlanMemorySize(3);

			c.strategy().addParam("maxAgentPlanMemorySize", "3");
			c.strategy().addParam("Module_1", "ReRoute");
			c.strategy().addParam("ModuleProbability_1", "0.1");
			c.strategy().addParam("Module_2", "ChangeExpBeta");
			c.strategy().addParam("ModuleProbability_2", "0.9");
			//
			Sim2DConfigGroup s2d = new Sim2DConfigGroup();
			
			
			if (model==1)
			{
			
				s2d.setEnableCircularAgentInterActionModule("false");
				s2d.setEnableCollisionPredictionAgentInteractionModule("true");
				s2d.setEnableCollisionPredictionEnvironmentForceModule("true");
				s2d.setEnableDrivingForceModule("true");
				s2d.setEnableEnvironmentForceModule("true");
				s2d.setEnablePathForceModule("true");
				s2d.setEnablePhysicalEnvironmentForceModule("false");
				s2d.setEnableVelocityObstacleModule("false");
				
			}
			else if (model==2)
			{
				s2d.setEnableCircularAgentInterActionModule("false");
				s2d.setEnableCollisionPredictionAgentInteractionModule("false");
				s2d.setEnableCollisionPredictionEnvironmentForceModule("false");
				s2d.setEnableDrivingForceModule("false");
				s2d.setEnableEnvironmentForceModule("false");
				s2d.setEnablePathForceModule("false");
				s2d.setEnablePhysicalEnvironmentForceModule("false");
				s2d.setEnableVelocityObstacleModule("true");
			}
				
			
//			s2d.setFloorShapeFile(inputDir +"/bottleneck" + (int)width + "_" + (int)length +  ".shp");
			s2d.setFloorShapeFile(inputDir +"/floorplan.shp");
			c.addModule(s2d);
			new ConfigWriter(c).write(inputDir + "/config.xml");
			
			System.out.println("");
			System.err.println("DONE");
			System.out.println("");
			
		}


	}
	private static Person createPerson(PopulationFactory populationFactory, CoordImpl coordImpl, Link clink, List<Link> links, double time, Scenario sc)
	{
		//create person
		Person pers = populationFactory.createPerson(Id.create("g"+Integer.toString(persId ++), Person.class));

		//set plan
		Plan plan = populationFactory.createPlan();

		//set activity
		ActivityImpl act = (ActivityImpl) populationFactory.createActivityFromLinkId("h", clink.getId());
		act.setCoord(coordImpl);
		act.setEndTime(time);

		//add activity
		plan.addActivity(act);

		//set leg
		Leg leg = populationFactory.createLeg("walk2d");

		//add leg
		plan.addLeg(leg);

		//get last link
		Link l1 = links.get(links.size()-2);

		//set final activity
		Activity act2 = populationFactory.createActivityFromLinkId("h", l1.getId());
		act2.setEndTime(0);
		plan.addActivity(act2);
		plan.setScore(0.);

		//add to plan
		pers.addPlan(plan);

		return pers;
	}


	private static void createPersons(Scenario sc,PopulationFactory pb, Population pop, List<Link> links, int persons, double time, double length, double width, double waitingAreaWidth)
	{

		//DEBUG
		GeometryFactory geofac = new GeometryFactory();

		//double dist = length / persons;

		//get links
		Iterator<Link> it = links.iterator();
		Link currentLink = it.next();
		//		double linkLength = currentLink.getLength();

		//double currPos = 0;

		//approx. persons per row
		double pprApprox = Math.sqrt(persons);
		double gap = Math.abs(Math.floor(pprApprox) - pprApprox + 0.001d);
		
		int personsPerRow = (int)(Math.min(waitingAreaWidth*2,Math.floor(Math.sqrt(persons))));
		
		System.out.println("persons per row: " + personsPerRow);
		

		for (int i = 0; i < persons; i++)
		{

			double step = (((i % personsPerRow) + 0.01f) / personsPerRow);
			double x = (-waitingAreaWidth/2) + ( step * (float)waitingAreaWidth) + 0.45d;
			double y = (Math.floor((i+0.01)/personsPerRow));
			
			//create geometry factory point
			Point p = geofac.createPoint(new Coordinate(x,y));
			GisDebugger.addGeometry(p, ""+i);
			
			//create person
			Person pers = createPerson(pb,new CoordImpl(x,y),currentLink,links,time,sc);

			//add preson to population
			pop.addPerson(pers);
		}

		//		GisDebugger.dump("C:/temp/persons.shp");
	}



	private static void createPop(Scenario scenario, String inputDir, List<Link> links, double length, double width, double waitingAreaWidth, int persons)
	{
		Population population = scenario.getPopulation();
		PopulationFactory populationFactory = population.getFactory();

		//			createPersons(scenario,pb,pop,links,83,0*60);

		createPersons(scenario, populationFactory, population, links, persons, 0*60, length, width, waitingAreaWidth);
//		createPersons(scenario, populationFactory, population, links, 23, 44, length, width, waitingAreaWidth);
//		createPersons(scenario, populationFactory, population, links, 23, 88, length, width, waitingAreaWidth);
//		createPersons(scenario, populationFactory, population, links, 23, 132, length, width, waitingAreaWidth);

		//create plans file
		String outputPopulationFile = inputDir + "/plans.xml";
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork(), 1).write(outputPopulationFile);
		scenario.getConfig().plans().setInputFile(outputPopulationFile);


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
		scenario.getConfig().planCalcScore().addActivityParams(pre);
		//			scenario.getConfig().planCalcScore().addActivityParams(post);
	}





	private static List<Link> createNetwork(Scenario scenario, String dir, double distance, double length, double width, double waitingAreaWidth)
	{

		//create links list
		List<Link> links = new ArrayList<Link>();

		//create network and nodes list
		NetworkFactoryImpl network = new NetworkFactoryImpl(scenario.getNetwork());
		List<NodeImpl> nodes = new ArrayList<NodeImpl>();

		//create nodes
		NodeImpl n01 = createNode(network, 0, 0d, waitingAreaWidth);
		NodeImpl n02 = createNode(network, 1, 0d, 0d);
		NodeImpl n03 = createNode(network, 2, 0d, -distance);
		NodeImpl n04 = createNode(network, 3, 0d, -distance - 10d);
		
//		NodeImpl n02 = createNode(network, 1, 0d, -0.25d);
//		NodeImpl n03 = createNode(network, 2, 0d, -distance - width + 0.25d);
//		NodeImpl n04 = createNode(network, 3, 0d, -distance - width);

		//add nodes to nodes list
		nodes.add(n01); nodes.add(n02); nodes.add(n03); nodes.add(n04);
		scenario.getNetwork().addNode(n01);
		scenario.getNetwork().addNode(n02);
		scenario.getNetwork().addNode(n03);
		scenario.getNetwork().addNode(n04);

		//barrier (point) for counting agents
		Coordinate countingStation = new Coordinate(0d, -distance);

		//create links
		int linkId = 0;
		for (int i = 0; i < nodes.size()-1; i++)
		{
			//get current and next node
			NodeImpl n0 = nodes.get(i);
			NodeImpl n1 = nodes.get(i+1);


			//get coordinates from current and next node
			Coordinate c0 = MGC.coord2Coordinate(n0.getCoord());
			Coordinate c1 = MGC.coord2Coordinate(n1.getCoord());

			Id<Link> lid = Id.create(linkId++, Link.class);
			if (c1.distance(countingStation) == 0)
				System.out.println("Counting station link:" + lid);

			//create link between the current and the next node
			Link l = network.createLink(lid, n0, n1, (NetworkImpl) scenario.getNetwork(), c0.distance(c1), 1.34, 1, 1);
			Id<Link> lid1 = Id.create(linkId++, Link.class);
			Link l1 = network.createLink(lid1, n1, n0, (NetworkImpl) scenario.getNetwork(), c0.distance(c1), 1.34, 1, 1);
			//add to network
			scenario.getNetwork().addLink(l);
			scenario.getNetwork().addLink(l1);

			//add to links
			links.add(l);
			links.add(l1);
		}


		String networkOutputFile = dir+"/network.xml";
		((NetworkImpl)scenario.getNetwork()).setEffectiveCellSize(0.26);
		((NetworkImpl)scenario.getNetwork()).setEffectiveLaneWidth(0.71);
		new NetworkWriter(scenario.getNetwork()).write(networkOutputFile);
		scenario.getConfig().network().setInputFile(networkOutputFile);

		return links;
	}

	private static NodeImpl createNode(NetworkFactoryImpl network, int id, double x, double y)
	{
		Id<Node> nodeID = Id.create(id, Node.class);
		CoordImpl coord = new CoordImpl(x, y);
		NodeImpl n = network.createNode(nodeID, coord);

		return n;
	}


	private static void createAndSaveEnvironment(String dir, double length, double distance, double width, double waitingAreaWidth)
	{

		GeometryFactory geometryFactory = new GeometryFactory();

		//wall coordinates for both left and right side of the bottleneck
		Coordinate[] leftSide = new Coordinate[4];
		Coordinate[] rightSide = new Coordinate[4];

		//left side coordinates
		leftSide[0] = new Coordinate(-waitingAreaWidth/2,waitingAreaWidth+9d);
		leftSide[1] = new Coordinate(-waitingAreaWidth/2,-distance);
		leftSide[2] = new Coordinate(-width/2,-distance);
		leftSide[3] = new Coordinate(-width/2,-distance - length);

		//right side coordinates
		rightSide[0] = new Coordinate(waitingAreaWidth/2,waitingAreaWidth+9d);
		rightSide[1] = new Coordinate(waitingAreaWidth/2,-distance);
		rightSide[2] = new Coordinate(width/2,-distance);
		rightSide[3] = new Coordinate(width/2,-distance - length);

		//the coordinate arrays formulated as line strings
		LineString leftSideLines = geometryFactory.createLineString(leftSide);
		LineString rightSideLines = geometryFactory.createLineString(rightSide);

		//adding them to the debugger
		GisDebugger.addGeometry(leftSideLines);
		GisDebugger.addGeometry(rightSideLines);
//		GisDebugger.dump(dir + "/bottleneck" + (int)width + "_" + (int)length +  ".shp");
		GisDebugger.dump(dir + "/floorplan.shp");
		
	}


}
