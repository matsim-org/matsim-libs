/* *********************************************************************** *
 * project: org.matsim.*
 * MarathonController.java
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

package playground.christoph.icem2012;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.WalkTravelTimeFactory;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.router.IntermodalLeastCostPathCalculator;
import org.matsim.core.router.LegRouter;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.population.algorithms.PlanAlgorithm;

import com.vividsolutions.jts.geom.Geometry;

import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.api.core.v01.Coord3d;
import playground.christoph.evacuation.core.utils.geometry.Coord3dImpl;
import playground.christoph.evacuation.network.AddZCoordinatesToNetwork;
import playground.christoph.evacuation.withinday.replanning.utils.SHPFileUtil;
import playground.gregor.sim2d_v3.events.XYDataWriter;
import playground.gregor.sim2d_v3.router.Walk2DLegRouter;
import playground.gregor.sim2d_v3.scenario.ScenarioLoader2DImpl;
import playground.gregor.sim2d_v3.simulation.HybridQ2DMobsimFactory;
import playground.gregor.sim2d_v3.simulation.Sim2DEngine;
import playground.gregor.sim2d_v3.simulation.floor.DefaultVelocityCalculator;
import playground.gregor.sim2d_v3.simulation.floor.VelocityCalculator;

public class MarathonController extends Controler implements StartupListener, MobsimInitializedListener {

	private final static Logger log = Logger.getLogger(MarathonController.class);
	
	public static String dhm25File = "D:/Users/Christoph/workspace/matsim/mysimulations/networks/GIS/nodes_3d_ivtch_dhm25.shp";
	public static String srtmFile = "D:/Users/Christoph/workspace/matsim/mysimulations/networks/GIS/nodes_3d_srtm.shp";
	public static String affectedAreaFile = "D:/Users/Christoph/workspace/matsim/mysimulations/icem2012/input/affectedArea.shp";
	
	/*
	 * innerBuffer ... minimum distance between affected area and an exit node
	 * outerBuffer ... maximum distance between affected area and an exit node
	 */
	private double innerBuffer = 2000.0;
	private double outerBuffer = 4000.0;
	
	private Geometry affectedArea;
	private CoordAnalyzer coordAnalyzer;
	private Set<Id> affectedNodes;
	private Set<Id> affectedLinks;
	private Set<Id> affectedFacilities;
	
	public MarathonController(String[] args) {
		super(args[0]);
		setOverwriteFiles(true);
	}

	/*
	 * TODO: 
	 * - add exit nodes and links 
	 * - add within-day replanners
	 * - allow walk and bike mode on track after evacuation has started
	 * - switch all affected walk agents to walk2d
	 */
	public MarathonController(Scenario sc) {
		super(sc);
		setOverwriteFiles(true);
		HybridQ2DMobsimFactory factory = new HybridQ2DMobsimFactory();
		
		// explicit set the mobsim factory
		this.setMobsimFactory(factory);
		this.addCoreControlerListener(this);
		this.getQueueSimulationListener().add(this);
	}
	
	@Override
	protected void loadData() {
		super.loadData();
		
		/*
		 * Adding z-coordinates to the network
		 */
		AddZCoordinatesToNetwork zCoordinateAdder;
		zCoordinateAdder = new AddZCoordinatesToNetwork(this.scenarioData, dhm25File, srtmFile);
		zCoordinateAdder.addZCoordinatesToNetwork();
		zCoordinateAdder.checkSteepness();
		
		/*
		 * Fixing height coordinates of nodes that have been added and that
		 * are not included in the height shp files.
		 */
		for (Node node : scenarioData.getNetwork().getNodes().values()) {
			String idString = node.getId().toString(); 
			if (idString.contains("_shifted")) {
				idString = idString.replace("_shifted", "");
				Node node2 = scenarioData.getNetwork().getNodes().get(scenarioData.createId(idString));
				Coord3d coord2 = (Coord3d) node2.getCoord();
				
				Coord coord = node.getCoord();
				Coord3d coord3d = new Coord3dImpl(coord.getX(), coord.getY(), coord2.getZ());
				((NodeImpl) node).setCoord(coord3d);
			}
		}
		
		ScenarioLoader2DImpl loader = new ScenarioLoader2DImpl(this.scenarioData);
		loader.load2DScenario();
	}

	@Override
	public PlanAlgorithm createRoutingAlgorithm(TravelDisutility travelCosts, PersonalizableTravelTime travelTimes) {
		
		PlansCalcRoute plansCalcRoute = (PlansCalcRoute) super.createRoutingAlgorithm(travelCosts, travelTimes);
		
		PersonalizableTravelTime travelTime = new WalkTravelTimeFactory(config.plansCalcRoute()).createTravelTime();
		LegRouter walk2DLegRouter = new Walk2DLegRouter(network, travelTime, (IntermodalLeastCostPathCalculator) plansCalcRoute.getLeastCostPathCalculator());
		plansCalcRoute.addLegHandler("walk2d", walk2DLegRouter);
		
		return plansCalcRoute;
	}

	public static void main(String[] args) {

		String configFile = args[0];
		Config c = ConfigUtils.loadConfig(configFile);

		Scenario sc = ScenarioUtils.createScenario(c);
		((PopulationFactoryImpl)sc.getPopulation().getFactory()).setRouteFactory("walk2d", new LinkNetworkRouteFactory());
		((PopulationFactoryImpl)sc.getPopulation().getFactory()).setRouteFactory(TransportMode.walk, new LinkNetworkRouteFactory());
		
		if (c.multiModal().isMultiModalSimulationEnabled()) {
			for (String transportMode : CollectionUtils.stringToArray(c.multiModal().getSimulatedModes())) {
				((PopulationFactoryImpl)sc.getPopulation().getFactory()).setRouteFactory(transportMode, new LinkNetworkRouteFactory());
			}	
		}
				
		ScenarioUtils.loadScenario(sc);

		Controler controller = new MarathonController(sc);
		controller.run();
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		
		XYDataWriter xyDataWriter = new XYDataWriter();
		this.getEvents().addHandler(xyDataWriter);
		this.addControlerListener(xyDataWriter);
		this.getQueueSimulationListener().add(xyDataWriter);
		
		readAffectedArea();
		
		identifyAffectedInfrastructure();
		
		createExitLinks();
	}

	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		MobsimFactory factory = super.getMobsimFactory();
		if (factory instanceof HybridQ2DMobsimFactory) {
			log.info("Replacing VelocityCalculator with MarathonVelocityCalculator");
			Sim2DEngine sim2DEngine = ((HybridQ2DMobsimFactory) factory).getSim2DEngine();
			
			VelocityCalculator velocityCalculator = new MarathonVelocityCalculator(
					new DefaultVelocityCalculator(this.config.plansCalcRoute())); 
			sim2DEngine.setVelocityCalculator(velocityCalculator);
		}
	}
	
	private void readAffectedArea() {
		SHPFileUtil util = new SHPFileUtil();
		Set<Feature> features = new HashSet<Feature>();
		features.addAll(util.readFile(affectedAreaFile));		
		this.affectedArea = util.mergeGeomgetries(features);
		
		this.coordAnalyzer = new CoordAnalyzer(this.affectedArea);
	}
	
	private void identifyAffectedInfrastructure() {
		affectedNodes = new HashSet<Id>();
		affectedLinks = new HashSet<Id>();
		affectedFacilities = new HashSet<Id>();
		
		for (Node node : scenarioData.getNetwork().getNodes().values()) {
			if (coordAnalyzer.isNodeAffected(node)) affectedNodes.add(node.getId());
		}
		for (Link link : scenarioData.getNetwork().getLinks().values()) {
			if (coordAnalyzer.isLinkAffected(link)) affectedLinks.add(link.getId());
		}
		for (Facility facility : scenarioData.getActivityFacilities().getFacilities().values()) {
			if (coordAnalyzer.isFacilityAffected(facility)) affectedFacilities.add(facility.getId());
		}
	}
	
	private void createExitLinks() {
		Geometry innerBuffer = affectedArea.buffer(this.innerBuffer);
		Geometry outerBuffer = affectedArea.buffer(this.outerBuffer);
		
		CoordAnalyzer innerAnalyzer = new CoordAnalyzer(innerBuffer);
		CoordAnalyzer outerAnalyzer = new CoordAnalyzer(outerBuffer);
		
		Set<Node> exitNodes = new LinkedHashSet<Node>();
		for (Node node : scenarioData.getNetwork().getNodes().values()) {
			if (outerAnalyzer.isNodeAffected(node) && !innerAnalyzer.isNodeAffected(node)) {
				exitNodes.add(node);
			}
		}
		log.info("Found " + exitNodes.size() + " exit nodes");
	}
}
