package playground.gregor.scenariogen;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;

import playground.gregor.sim2d_v3.config.Sim2DConfigGroup;
import playground.gregor.sim2d_v3.helper.gisdebug.GisDebugger;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class ScenarioGeneratorOCRAdbg {

	private static int persId = 0;

	public static void main(String [] args) {
		String scDir = "/Users/laemmel/devel/OCRA/";
		String inputDir = scDir + "/input/";

		//		double length = 6;
		double a = 12;
		double b = 12.;


		Config c = ConfigUtils.createConfig();

		Scenario sc = ScenarioUtils.createScenario(c);

		createAndSaveEnvironment(inputDir,a,b);

		List<Link> links = createNetwork(sc,inputDir,a,b);

		createPop(sc,inputDir, links,a,b);


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
		s2d.setEnableEnvironmentForceModule("false");
		s2d.setEnableCollisionPredictionAgentInteractionModule("false");
		s2d.setEnableCollisionPredictionEnvironmentForceModule("false");
		s2d.setEnablePathForceModule("false");
		s2d.setEnableDrivingForceModule("false");
		s2d.setEnableVelocityObstacleModule("false");
		s2d.setEnablePhysicalEnvironmentForceModule("false");			

		s2d.setEnableMentalLinkSwitch("false");
		
		

		s2d.setTimeStepSize(""+0.04);
		QSimConfigGroup qsim = new QSimConfigGroup();
		qsim.setEndTime(600);
		//				qsim.setTimeStepSize(1./25.);
		c.addModule(qsim);

		c.addModule(s2d);
		new ConfigWriter(c).write(inputDir + "/config.xml");



	}
	private static Person createPerson(PopulationFactory pb, CoordImpl coordImpl, CoordImpl dest, double time, Scenario sc) {
		Person pers = pb.createPerson(sc.createId("g"+Integer.toString(persId ++)));
		Plan plan = pb.createPlan();
		ActivityImpl act = (ActivityImpl) pb.createActivityFromCoord("h", coordImpl);
		act.setCoord(coordImpl);
		act.setEndTime(time);
		plan.addActivity(act);
		Leg leg = pb.createLeg("walk2d");
		plan.addLeg(leg);
		Activity act2 = pb.createActivityFromCoord("h", dest);
		act2.setEndTime(0);
		plan.addActivity(act2);
		plan.setScore(0.);
		pers.addPlan(plan);
		return pers;
	}


	private static void createPersons(Scenario sc,PopulationFactory pb, Population pop,
			List<Link> links, double a, double b, double time) {

		//DEBUG
		//		GeometryFactory geofac = new GeometryFactory();

		//		
		Person pers = createPerson(pb,new CoordImpl(a,b/2),new CoordImpl(0,b/2),time,sc);
		pop.addPerson(pers);
		pers = createPerson(pb,new CoordImpl(a,b/2+1),new CoordImpl(0,b/2+0.6),time,sc);
		pop.addPerson(pers);
		
		pers = createPerson(pb,new CoordImpl(0,b/2),new CoordImpl(a,b/2),time,sc);
		pop.addPerson(pers);
		pers = createPerson(pb,new CoordImpl(0,b/2+1),new CoordImpl(a,b/2+0.8),time,sc);
		pop.addPerson(pers);
		
		pers = createPerson(pb,new CoordImpl(a/2,b),new CoordImpl(a/2,0),time,sc);
		pop.addPerson(pers);
		pers = createPerson(pb,new CoordImpl(a/2-.5,b),new CoordImpl(a/2,0),time,sc);
		pop.addPerson(pers);
		
		pers = createPerson(pb,new CoordImpl(a/2,0),new CoordImpl(a/2,b),time,sc);
		pop.addPerson(pers);
		pers = createPerson(pb,new CoordImpl(a/2+.5,0),new CoordImpl(a/2,b),time,sc);
		pop.addPerson(pers);
		
//		pers = createPerson(pb,new CoordImpl(a/4,b/2-b/8),new CoordImpl(a-a/4,b/2-b/8),time,sc);
//		pop.addPerson(pers);
//
//		pers = createPerson(pb,new CoordImpl(a-a/4,b/2-b/4),new CoordImpl(a/4,b/2-b/4),time,sc);
//		pop.addPerson(pers);

	}



	private static void createPop(Scenario sc, String inputDir, List<Link> links,double a, double b) {
		Population pop = sc.getPopulation();
		PopulationFactory pb = pop.getFactory();

		//		createPersons(sc,pb,pop,links,23,0);

		//

		//		createPersons(sc,pb,pop,links,70,0*60);
		//		createPersons(sc,pb,pop,links,62,60*60);
		//		createPersons(sc,pb,pop,links,56,120*60);
		//		createPersons(sc,pb,pop,links,45,180*60);
		//		createPersons(sc,pb,pop,links,39,240*60);
		//		createPersons(sc,pb,pop,links,34,300*60);
		//		createPersons(sc,pb,pop,links,28,360*60);
		//		createPersons(sc,pb,pop,links,25,420*60);
		//		createPersons(sc,pb,pop,links,22,480*60);
		//		createPersons(sc,pb,pop,links,20,540*60);
		//		createPersons(sc,pb,pop,links,17,600*60);
		//		createPersons(sc,pb,pop,links,14,660*60);
		//		//		createPersons(sc,pb,pop,links,1,430*60);
		//		//		createPersons(sc,pb,pop,links,1,435*60);
		//		//		createPersons(sc,pb,pop,links,107,440*60);

		createPersons(sc,pb,pop,links,a,b,0*60);
		//				createPersons(sc,pb,pop,links,72,120*60);
		//				createPersons(sc,pb,pop,links,59,180*60);
		//				createPersons(sc,pb,pop,links,49,240*60);
		//				createPersons(sc,pb,pop,links,37,300*60);
		//				createPersons(sc,pb,pop,links,23,360*60);
		//				createPersons(sc,pb,pop,links,18,400*60);
		//				createPersons(sc,pb,pop,links,14,405*60);
		//				createPersons(sc,pb,pop,links,12,410*60);
		//				createPersons(sc,pb,pop,links,10,415*60);
		//				createPersons(sc,pb,pop,links,8,420*60);
		//				createPersons(sc,pb,pop,links,2,425*60);
		//				createPersons(sc,pb,pop,links,1,430*60);
		//				createPersons(sc,pb,pop,links,1,435*60);
		//				createPersons(sc,pb,pop,links,107,440*60);
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





	private static List<Link> createNetwork(Scenario sc, String dir, double a, double b) {

		List<Link> links = new ArrayList<Link>();

		NetworkFactoryImpl nf = new NetworkFactoryImpl(sc.getNetwork());

		int nodeId = 0;
		List<NodeImpl> nodes = new ArrayList<NodeImpl>();

		//		double xm = r;
		//		double ym = 0;
		List<Coordinate> oval = getCenterLineLR(a,b);
		for (Coordinate coord : oval) {
			Id nid = new IdImpl(nodeId++);
			CoordImpl c = new CoordImpl(coord.x,coord.y);
			NodeImpl n = nf.createNode(nid, c);
			nodes.add(n);
			sc.getNetwork().addNode(n);
		}

		int linkId = 0;

		Set<String> modes = new HashSet<String>();
		modes.add("walk2d");
		for (int i = 0; i < nodes.size()-1; i++) {
			NodeImpl n0 = nodes.get(i);
			NodeImpl n1 = nodes.get(i+1);
			Id lid = new IdImpl(linkId++);

			Coordinate c0 = MGC.coord2Coordinate(n0.getCoord());
			Coordinate c1 = MGC.coord2Coordinate(n1.getCoord());
			Link l = nf.createLink(lid, n0, n1, (NetworkImpl) sc.getNetwork(), c0.distance(c1), 1.34, 1, 1);
			l.setAllowedModes(modes);
			sc.getNetwork().addLink(l);
			links.add(l);

			lid = new IdImpl(linkId++);
			Link lr = nf.createLink(lid, n1, n0, (NetworkImpl) sc.getNetwork(), c0.distance(c1), 1.34, 1, 1);
			lr.setAllowedModes(modes);
			sc.getNetwork().addLink(lr);
			links.add(lr);
		}

		
		nodes = new ArrayList<NodeImpl>();
		oval = getCenterLineTB(a,b);
		for (Coordinate coord : oval) {
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
			l.setAllowedModes(modes);
			sc.getNetwork().addLink(l);
			links.add(l);

			lid = new IdImpl(linkId++);
			Link lr = nf.createLink(lid, n1, n0, (NetworkImpl) sc.getNetwork(), c0.distance(c1), 1.34, 1, 1);
			lr.setAllowedModes(modes);
			sc.getNetwork().addLink(lr);
			links.add(lr);
		}
		
//		NodeImpl n0 = nodes.get(nodes.size()-1);
//		NodeImpl n1 = nodes.get(0);
//		Id lid = new IdImpl(linkId++);
//		Coordinate c0 = MGC.coord2Coordinate(n0.getCoord());
//		Coordinate c1 = MGC.coord2Coordinate(n1.getCoord());
//		Link l = nf.createLink(lid, n0, n1, (NetworkImpl) sc.getNetwork(), c0.distance(c1), 1.34, 1, 1);
//		sc.getNetwork().addLink(l);
//		links.add(l);

		String networkOutputFile = dir+"/network.xml";
		((NetworkImpl)sc.getNetwork()).setEffectiveCellSize(0.26);
		((NetworkImpl)sc.getNetwork()).setEffectiveLaneWidth(0.71);
		new NetworkWriter(sc.getNetwork()).write(networkOutputFile);
		sc.getConfig().network().setInputFile(networkOutputFile);

		return links;
	}


	private static List<Coordinate> getCenterLineLR(double a, double b) {
		List<Coordinate> coords = new ArrayList<Coordinate>();

		for (double aa = a; aa >= 0; aa -= a/16) {
			coords.add(new Coordinate(aa,b/2));
		}
		return coords;
	}

	private static List<Coordinate> getCenterLineTB(double a, double b) {
		List<Coordinate> coords = new ArrayList<Coordinate>();

		for (double bb = b; bb >= 0; bb -= b/16) {
			coords.add(new Coordinate(a/2,bb));
		}
		return coords;
	}


	private static List<Coordinate> getShell(double a, double b) {
		List<Coordinate> coords = new ArrayList<Coordinate>();

		coords.add(new Coordinate(-1,-1));
		coords.add(new Coordinate(a,-1));
		coords.add(new Coordinate(a,b));
		coords.add(new Coordinate(-1,b));



		return coords;
	}



	private static void createAndSaveEnvironment(String dir, double a, double b) {

		a+=1;
		b+=1;

		GeometryFactory geofac = new GeometryFactory();


		List<Coordinate> o1 = getShell(a, b);
		Coordinate[] c1 = new Coordinate[o1.size()+1];
		for (int i = 0; i < o1.size(); i++) {
			c1[i] = o1.get(i);
		}
		c1[o1.size()] = c1[0];


		LineString ls0 = geofac.createLineString(c1);
		GisDebugger.addGeometry(ls0);


		GisDebugger.dump(dir + "/floorplan.shp");

	}


}
