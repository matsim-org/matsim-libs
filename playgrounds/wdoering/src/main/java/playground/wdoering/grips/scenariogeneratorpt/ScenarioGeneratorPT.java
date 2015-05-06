/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.wdoering.grips.scenariogeneratorpt;

import com.vividsolutions.jts.geom.Geometry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.evacuation.model.config.GripsConfigModule;
import org.matsim.contrib.evacuation.model.events.InfoEvent;
import org.matsim.contrib.evacuation.scenariogenerator.ScenarioGenerator;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.SimulationConfigGroup;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.TravelTimeAndDistanceBasedTravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.*;
import org.opengis.feature.simple.SimpleFeature;

import playground.gregor.grips.scenariogeneratorpt.PopulationFromESRIShapeFielGeneratorPT;

import java.util.*;


public class ScenarioGeneratorPT extends ScenarioGenerator {

	private static final Logger log = Logger.getLogger(ScenarioGeneratorPT.class);
	private List<TransitRouteStop> stops;
	
	public ScenarioGeneratorPT(String config) {
		super(config);
	}

	@Override
	public void run()
	{
		
		log.info("loading config file");
		InfoEvent e = new InfoEvent(System.currentTimeMillis(), "loading config file");
		this.em.processEvent(e);
		this.matsimConfig = ConfigUtils.loadConfig(this.configFile);
		
		this.matsimConfig.scenario().setUseTransit(true);
		this.matsimConfig.scenario().setUseVehicles(true);
		
		this.matsimConfig.addModule( new SimulationConfigGroup() );
		this.matsimConfig.global().setCoordinateSystem("EPSG:3395");
		this.matsimConfig.controler().setOutputDirectory(getGripsConfig(this.matsimConfig).getOutputDir()+"/output");
		this.matsimScenario = ScenarioUtils.createScenario(this.matsimConfig);
		this.safeLinkId = Id.create("el1", Link.class);

		log.info("generating network file");
		e = new InfoEvent(System.currentTimeMillis(), "generating network file");
		this.em.processEvent(e);
		generateAndSaveNetwork(this.matsimScenario);
		if (DEBUG) {
			dumpNetworkAsShapeFile(this.matsimScenario);
		}

		log.info("generating population file");
		e = new InfoEvent(System.currentTimeMillis(), "generating population file");
		this.em.processEvent(e);
		createPTSchedule();
		generateAndSavePopulation(this.matsimScenario);

		log.info("saving simulation config file");
		e = new InfoEvent(System.currentTimeMillis(), "simulation config file");
		this.em.processEvent(e);

		

		this.matsimConfig.controler().setLastIteration(10);
		this.matsimConfig.controler().setOutputDirectory(getGripsConfig(this.matsimConfig).getOutputDir()+"/output");

		
		
		QSimConfigGroup qsim = this.matsimConfig.qsim();
		qsim.setEndTime(4*3600);
		
//		Sim2DConfigGroup s2d = new Sim2DConfigGroup();
//		s2d.setFloorShapeFile("/Users/laemmel/devel/pt_evac_demo/input/floorplan.shp");
//
//		s2d.setEnableCircularAgentInterActionModule("false");
//		s2d.setEnableCollisionPredictionAgentInteractionModule("false");
//		s2d.setEnableCollisionPredictionEnvironmentForceModule("false");
//		s2d.setEnableDrivingForceModule("false");
//		s2d.setEnableEnvironmentForceModule("false");
//		s2d.setEnablePathForceModule("false");
//		s2d.setEnableVelocityObstacleModule("true");
//		s2d.setEnablePhysicalEnvironmentForceModule("false");
//
//
//		String shpFile = s2d.getFloorShapeFile();
//		ShapeFileReader r = new ShapeFileReader();
//		r.readFileAndInitialize(shpFile);

//		s2d.setTimeStepSize("1");
		
//		this.matsimConfig.addModule("sim2d", s2d);



//		this.matsimConfig.controler().setMobsim("hybridQ2D");

		
		
		this.matsimConfig.strategy().setMaxAgentPlanMemorySize(3);

		this.matsimConfig.strategy().addParam("maxAgentPlanMemorySize", "3");
		this.matsimConfig.strategy().addParam("Module_1", "ReRoute");
		this.matsimConfig.strategy().addParam("ModuleProbability_1", "0.1");
		this.matsimConfig.strategy().addParam("Module_2", "ChangeExpBeta");
		this.matsimConfig.strategy().addParam("ModuleProbability_2", "0.9");
//		this.matsimConfig.strategy().addParam("Module_3", "TransitChangeLegMode");
//		this.matsimConfig.strategy().addParam("ModuleProbability_3", "0.1");
		

		this.matsimConfigFile = getGripsConfig(this.matsimConfig).getOutputDir() + "/config.xml";

		
		new ConfigWriter(this.matsimConfig).write(this.matsimConfigFile);
		e = new InfoEvent(System.currentTimeMillis(), "scenario generation finished.");
		this.em.processEvent(e);
		
		
		
	}

	private void createPTSchedule() {
		
		Network network = this.matsimScenario.getNetwork();
		
		System.exit(4711);
		FreeSpeedTravelTime fs = new FreeSpeedTravelTime();
		TravelDisutility cost = new TravelTimeAndDistanceBasedTravelDisutilityFactory().createTravelDisutility(fs,this.matsimScenario.getConfig().planCalcScore() );
		LeastCostPathCalculator dijkstra = new Dijkstra(network, cost, fs);
		
		
		TransitScheduleFactory fac = new TransitScheduleFactoryImpl();
		TransitSchedule schedule = fac.createTransitSchedule();

		
		String dir = "C:/temp/pt_evac_demo/input/transitStops.shp"; //TODO put it in the grips config group
		ShapeFileReader r = new ShapeFileReader();
		r.readFileAndInitialize(dir);
		Collection<SimpleFeature> fts = r.getFeatureSet();
		
		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		for (SimpleFeature ft : fts) {
			long id = (Long)ft.getAttribute("id");
			TransitStopFacility facility = fac.createTransitStopFacility(Id.create(id, TransitStopFacility.class), MGC.coordinate2Coord(((Geometry) ft.getDefaultGeometry()).getCoordinate()), false);
			schedule.addStopFacility(facility);
			TransitRouteStop rs = fac.createTransitRouteStop(facility, 0, 0);
			stops.add(rs);
		}
		Collections.sort(stops, new Comparator<TransitRouteStop>() {

			@Override
			public int compare(TransitRouteStop o1, TransitRouteStop o2) {
				int id1 = Integer.parseInt(o1.getStopFacility().getId().toString());
				int id2 = Integer.parseInt(o2.getStopFacility().getId().toString());
				if (id1 < id2) {
					return -1;
				} 
				if (id2 < id1) {
					return 1;
				}
				return 0;
			}
		});
		List<Link> allLinks = new ArrayList<Link>();
		TransitRouteStop f = stops.get(0);
		Link l1 = NetworkUtils.getNearestLink(((NetworkImpl) network), f.getStopFacility().getCoord());
		f.getStopFacility().setLinkId(l1.getId());
		allLinks.add(l1);
		for (int i = 1; i < stops.size(); i++) { 

			l1 = allLinks.get(allLinks.size()-1);
			
			TransitRouteStop curr = stops.get(i);
			Link l2 = NetworkUtils.getNearestLink(((NetworkImpl) network), curr.getStopFacility().getCoord());
			
			Node start = l1.getToNode();
			Node end = l2.getFromNode();
			
			Path nr = dijkstra.calcLeastCostPath(start, end, 0, null, null);
			List<Link> links = nr.links;
			
//			//u-turn starts!
//			if (links.get(0).getFromNode().equals(links.get(1).getToNode())) {
//				
//				links = links.subList(1, links.size()-1);
//			}
//			prev.getStopFacility().setLinkId(links.get(0).getId());
			
			//u-turn ends!
			if (links.get(links.size()-1).getToNode().equals(links.get(links.size()-2).getFromNode())) {
				links = links.subList(0, links.size()-2);
			}
			curr.getStopFacility().setLinkId(links.get(links.size()-1).getId());
			allLinks.addAll(links);
			
		}
		
		
//		allLinks = allLinks.subList(0, allLinks.size()-2);
		List<Id<Link>> allLinksIds = new ArrayList<Id<Link>>();
		Set<String> modes  = new HashSet<String>();
		modes.add("bus");
		for (Link link : allLinks) {
			((LinkImpl)link).setAllowedModes(modes);
			allLinksIds.add(link.getId());
		}
		
		TransitLine line = fac.createTransitLine(Id.create("evac_line0", TransitLine.class));
		NetworkRoute route = new LinkNetworkRouteImpl(allLinks.get(0).getId(), allLinks.get(allLinks.size()-1).getId());
		route.setLinkIds(allLinksIds.get(0), allLinksIds.subList(1, allLinksIds.size()-1), allLinksIds.get(allLinksIds.size()-1));
		TransitRoute tr = fac.createTransitRoute(Id.create("er0", TransitRoute.class), route, stops, "bus");

		
		
		//Vehicles
		Vehicles vehicles = ((ScenarioImpl)this.matsimScenario).getTransitVehicles();
		VehiclesFactory vf = vehicles.getFactory();
		VehicleType vt = vf.createVehicleType(Id.create("bus", VehicleType.class));
		VehicleCapacity vc = vf.createVehicleCapacity();
		vc.setSeats(50);
		vc.setStandingRoom(0);
		vt.setCapacity(vc);
		vehicles.addVehicleType( vt);

		
		
		//Departures w/ vehicles
		for (int i =0; i < 30; i++) {
			Vehicle veh = vf.createVehicle(Id.create("bus_"+(i*5)*60, Vehicle.class), vt);
			vehicles.addVehicle( veh);
			Departure dep = fac.createDeparture(Id.create(i, Departure.class), (i*5)*60);
			dep.setVehicleId(veh.getId());
			tr.addDeparture(dep);
		
		}
		line.addRoute(tr);
		schedule.addTransitLine(line);
		
		String out = getGripsConfig().getOutputDir();
		new TransitScheduleWriterV1(schedule).write(out+"/transitSchedule.xml");
		new VehicleWriterV1(vehicles).writeFile(out+"/transitVehicles.xml");
		
		TransitConfigGroup tc = this.matsimConfig.transit();
		Set<String> tms = new HashSet<String>();
		tms.add("pt");
		tc.setTransitModes(tms);
		tc.setTransitScheduleFile(out+"/transitSchedule.xml");
		tc.setVehiclesFile(out+"/transitVehicles.xml");
		
//		Module clm = new Module("changeLegMode");
//		clm.addParam("modes", "pt,walk2d");
//		this.matsimConfig.addModule("changeLegMode", clm);
		this.safeLinkId = stops.get(stops.size()-1).getStopFacility().getLinkId();
		this.stops = stops;
		
	}
	
	
	@Override
	protected void generateAndSavePopulation(Scenario sc) {
		// for now a simple ESRI shape file format is used to emulated the a more sophisticated not yet defined population meta format
		GripsConfigModule gcm = getGripsConfig(sc.getConfig());
		String gripsPopulationFile = gcm.getPopulationFileName();
//		PopulationFromESRIShapeFielGeneratorPT gen = 
		new PopulationFromESRIShapeFielGeneratorPT(sc, gripsPopulationFile, this.safeLinkId,this.stops).run();
		
		String outputPopulationFile = gcm.getOutputDir() + "/population.xml.gz";
		new PopulationWriter(sc.getPopulation(), sc.getNetwork(), gcm.getSampleSize()).write(outputPopulationFile);
		sc.getConfig().plans().setInputFile(outputPopulationFile);

		((SimulationConfigGroup) sc.getConfig().getModule(SimulationConfigGroup.GROUP_NAME)).setStorageCapFactor(gcm.getSampleSize());
		((SimulationConfigGroup) sc.getConfig().getModule(SimulationConfigGroup.GROUP_NAME)).setFlowCapFactor(gcm.getSampleSize());

		ActivityParams pre = new ActivityParams("pre-evac");
		pre.setTypicalDuration(49); // needs to be geq 49, otherwise when running a simulation one gets "java.lang.RuntimeException: zeroUtilityDuration of type pre-evac must be greater than 0.0. Did you forget to specify the typicalDuration?"
		// the reason is the double precision. see also comment in ActivityUtilityParameters.java (gl)
		pre.setMinimalDuration(49);
		pre.setClosingTime(49);
		pre.setEarliestEndTime(49);
		pre.setLatestStartTime(49);
		pre.setOpeningTime(49);

		
		ActivityParams wait = new ActivityParams("wait");
		wait.setTypicalDuration(49); // needs to be geq 49, otherwise when running a simulation one gets "java.lang.RuntimeException: zeroUtilityDuration of type pre-evac must be greater than 0.0. Did you forget to specify the typicalDuration?"
		// the reason is the double precision. see also comment in ActivityUtilityParameters.java (gl)
		wait.setMinimalDuration(49);
		wait.setClosingTime(49);
		wait.setEarliestEndTime(49);
		wait.setLatestStartTime(49);
		wait.setOpeningTime(49);

		ActivityParams post = new ActivityParams("post-evac");
		post.setTypicalDuration(49); // dito
		post.setMinimalDuration(49);
		post.setClosingTime(49);
		post.setEarliestEndTime(49);
		post.setLatestStartTime(49);
		post.setOpeningTime(49);
		
		
		sc.getConfig().planCalcScore().addActivityParams(pre);
		sc.getConfig().planCalcScore().addActivityParams(wait);
		sc.getConfig().planCalcScore().addActivityParams(post);

		//		sc.getConfig().planCalcScore().addParam("activityPriority_0", "1");
		//		sc.getConfig().planCalcScore().addParam("activityTypicalDuration_0", "00:00:49");
		//		sc.getConfig().planCalcScore().addParam("activityMinimalDuration_0", "00:00:49");
		//		sc.getConfig().planCalcScore().addParam("activityPriority_1", "1");
		//		sc.getConfig().planCalcScore().addParam("activityTypicalDuration_1", "00:00:49");
		//		sc.getConfig().planCalcScore().addParam("activityMinimalDuration_1", "00:00:49");


	}
	
	
	public static void main(String [] args) {
		if (args.length != 1) {
			printUsage();
			System.exit(-1);
		}

		new ScenarioGeneratorPT(args[0]).run();

	}

	private static void printUsage() {
		System.err.println("no idea how to use this!");
		
	}
}
