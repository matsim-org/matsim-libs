package playground.gregor.scenariogen;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
import org.matsim.core.config.groups.SignalSystemsConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.signalsystems.SignalUtils;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.SignalsScenarioWriter;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalControlData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalGroupSettingsData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalPlanData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalSystemControllerData;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupsData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsData;
import org.matsim.signalsystems.model.DefaultPlanbasedSignalSystemController;

import playground.gregor.sim2d_v3.config.Sim2DConfigGroup;
import playground.gregor.sim2d_v3.helper.gisdebug.GisDebugger;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class TrafficLightScenarioGenerator {
	
	private static int persId = 0;

	public static void main(String [] args) {
		String scDir = "/Users/laemmel/devel/trafficlights/";
		String inputDir = scDir + "/input/";



		Config c = ConfigUtils.createConfig();
		c.scenario().setUseSignalSystems(true);

		Scenario sc = ScenarioUtils.createScenario(c);

		createAndSaveEnvironment(inputDir);

		List<Link> links = createNetwork(sc,inputDir);

		createPop(sc,inputDir, links);
		
		
		
		createTrafficLights(sc,inputDir);


		

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
	private static void createTrafficLights(Scenario sc, String inputDir) {
		SignalsData signalsData = sc.getScenarioElement(SignalsData.class);
		createSignalSystemsAndGroups(sc, signalsData);
		createSignalControl(sc, signalsData);
		
		//write to file
		SignalsScenarioWriter signalsWriter = new SignalsScenarioWriter();
		String signalSystemsFile = inputDir + "/signal_systems.xml";
		signalsWriter.setSignalSystemsOutputFilename(signalSystemsFile);
		String signalGroupsFile = inputDir + "/signal_groups.xml";
		signalsWriter.setSignalGroupsOutputFilename(signalGroupsFile);

		String signalControlFile = inputDir + "/signal_control.xml";
		signalsWriter.setSignalControlOutputFilename(signalControlFile);
		signalsWriter.writeSignalsData(signalsData);
		
		
		Config c = sc.getConfig();
		SignalSystemsConfigGroup signalsConfig = c.signalSystems();
		signalsConfig.setSignalSystemFile(signalSystemsFile);
		signalsConfig.setSignalGroupsFile(signalGroupsFile);
		signalsConfig.setSignalControlFile(signalControlFile);
	}
	private static void createSignalControl(Scenario scenario, SignalsData sd) {
		int cycle = 10;
		SignalControlData control = sd.getSignalControlData();
		
		//signal system 3, 4 control
		List<Id> ids = new LinkedList<Id>();
		ids.add(scenario.createId("1"));
		for (Id id : ids){
			SignalSystemControllerData controller = control.getFactory().createSignalSystemControllerData(id);
			control.addSignalSystemControllerData(controller);
			controller.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
			SignalPlanData plan = control.getFactory().createSignalPlanData(scenario.createId("1"));
			controller.addSignalPlanData(plan);
			plan.setCycleTime(cycle);
			plan.setOffset(0);
			SignalGroupSettingsData settings1 = control.getFactory().createSignalGroupSettingsData(scenario.createId("1"));
			plan.addSignalGroupSettings(settings1);
			settings1.setOnset(0);
			settings1.setDropping(2);
		}
	}
	private static void createSignalSystemsAndGroups(Scenario scenario,
			SignalsData signalsData) {
		
		SignalSystemsData systems = signalsData.getSignalSystemsData();
		SignalGroupsData groups = signalsData.getSignalGroupsData();
		
		//signal system 1
		SignalSystemData sys = systems.getFactory().createSignalSystemData(scenario.createId("1"));
		systems.addSignalSystemData(sys);
		SignalData signal = systems.getFactory().createSignalData(scenario.createId("1"));
		sys.addSignalData(signal);
		signal.setLinkId(scenario.createId("2"));
		SignalUtils.createAndAddSignalGroups4Signals(groups, sys);
	}
	private static Person createPerson(PopulationFactory pb, CoordImpl coordImpl, Link from,
			Link to, double time, Scenario sc) {
		Person pers = pb.createPerson(sc.createId("g"+Integer.toString(persId ++)));
		Plan plan = pb.createPlan();
		ActivityImpl act = (ActivityImpl) pb.createActivityFromLinkId("h", from.getId());
		act.setCoord(coordImpl);
		act.setEndTime(time);
		plan.addActivity(act);
		Leg leg = pb.createLeg("walk2d");
		plan.addLeg(leg);
		Activity act2 = pb.createActivityFromLinkId("h", to.getId());
		act2.setEndTime(0);
		plan.addActivity(act2);
		plan.setScore(0.);
		pers.addPlan(plan);
		return pers;
	}


	private static void createPersons(Scenario sc,PopulationFactory pb, Population pop,
			List<Link> links, int persons, double time) {

		//DEBUG
		//		GeometryFactory geofac = new GeometryFactory();

		Link from = links.get(0);
		Link to = links.get(links.size()-2);

		for (int i = 0; i < persons; i+=5) {
			double x = 2.5;
			for (double y = 1; y < 10; y += 2) {
				double rnd = (MatsimRandom.getRandom().nextDouble() - .5);
				Person pers = createPerson(pb,new CoordImpl(x,y+rnd),from,to,time,sc);
				pop.addPerson(pers);
				//			Point p = geofac.createPoint(new Coordinate(x,y));
				//			GisDebugger.addGeometry(p, ""+i);
			}
			
			time += 0.5;

		}
		//		GisDebugger.dump("/Users/laemmel/devel/counter/input/persons.shp");


	}



	private static void createPop(Scenario sc, String inputDir, List<Link> links) {
		Population pop = sc.getPopulation();
		PopulationFactory pb = pop.getFactory();


		createPersons(sc,pb,pop,links,500,0*60);

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





	private static List<Link> createNetwork(Scenario sc, String dir) {

		List<Link> links = new ArrayList<Link>();

		NetworkFactoryImpl nf = new NetworkFactoryImpl(sc.getNetwork());

		int nodeId = 0;
		List<NodeImpl> nodes = new ArrayList<NodeImpl>();

		List<Coordinate> coords = new ArrayList<Coordinate>();
		coords.add(new Coordinate(0,5));
		coords.add(new Coordinate(5,5));
		coords.add(new Coordinate(24.5,5));
		coords.add(new Coordinate(25,5));
		coords.add(new Coordinate(35,5));
		coords.add(new Coordinate(40,5));
		
		for (Coordinate coord : coords) {
			Id nid = new IdImpl(nodeId++);
			CoordImpl c = new CoordImpl(coord.x,coord.y);
			NodeImpl n = nf.createNode(nid, c);
			nodes.add(n);
			sc.getNetwork().addNode(n);
		}

		int linkId = 0;
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


		NodeImpl n0 = nodes.get(nodes.size()-1);
		NodeImpl n1 = nodes.get(0);
		Id lid = new IdImpl(linkId++);
		Coordinate c0 = MGC.coord2Coordinate(n0.getCoord());
		Coordinate c1 = MGC.coord2Coordinate(n1.getCoord());
		Link l = nf.createLink(lid, n0, n1, (NetworkImpl) sc.getNetwork(), c0.distance(c1), 1.34, 1, 1);
		sc.getNetwork().addLink(l);
		links.add(l);
		
		String networkOutputFile = dir+"/network.xml";
		((NetworkImpl)sc.getNetwork()).setEffectiveCellSize(0.26);
		((NetworkImpl)sc.getNetwork()).setEffectiveLaneWidth(0.71);
		new NetworkWriter(sc.getNetwork()).write(networkOutputFile);
		sc.getConfig().network().setInputFile(networkOutputFile);

		return links;
	}


	private static void createAndSaveEnvironment(String dir) {



		GeometryFactory geofac = new GeometryFactory();

		Coordinate[] c1 = new Coordinate[2];
		c1[0] = new Coordinate(0,0);
		c1[1] = new Coordinate(40,0);

		Coordinate[] c2 = new Coordinate[2];
		c2[0] = new Coordinate(0,10);
		c2[1] = new Coordinate(40,10);
		
		LineString ls0 = geofac.createLineString(c1);
		GisDebugger.addGeometry(ls0);

		LineString ls1 = geofac.createLineString(c2);
		GisDebugger.addGeometry(ls1);

		GisDebugger.dump(dir + "/floorplan.shp");

	}

}
