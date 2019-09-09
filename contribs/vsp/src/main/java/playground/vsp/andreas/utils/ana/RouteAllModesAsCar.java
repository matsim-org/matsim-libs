/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.vsp.andreas.utils.ana;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.analysis.filters.population.AbstractPersonFilter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.StreamingDeprecated;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.pt.PtConstants;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Routes all modes of transport (except for transit_walk) as car modes and counts the number of trips per link.
 * Output as Shape
 * 
 * @author aneumann
 *
 */
public class RouteAllModesAsCar extends AbstractPersonFilter {
	
	private final static Logger log = Logger.getLogger(RouteAllModesAsCar.class);

	private final Scenario sc;
	private final LeastCostPathCalculator routingAlgo;

	private HashMap<Link, Integer> link2totals = new HashMap<Link, Integer>();
	private HashMap<String, HashMap<Link, Integer>> mode2link2totals = new HashMap<String, HashMap<Link,Integer>>();
	
	public RouteAllModesAsCar(String networkFilename) {
		Gbl.startMeasurement();
		Gbl.printMemoryUsage();
		
		// read input data
		this.sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.sc.getConfig().network().setInputFile(networkFilename);
		ScenarioUtils.loadScenario(this.sc);
		
		FreespeedTravelTimeAndDisutility tC = new FreespeedTravelTimeAndDisutility(-6.0, 0.0, 0.0);
		this.routingAlgo = new DijkstraFactory().createPathCalculator(this.sc.getNetwork(), tC, tC);
		@SuppressWarnings("serial")
		Set<String> modes =  new HashSet<String>(){{
			// this is the networkmode and explicitly not the transportmode
			add(TransportMode.car);
			}};
		((Dijkstra)this.routingAlgo).setModeRestriction(modes);
	}

	public static void main(String[] args) {
		String outputDir = args[0];
		String networkFilename = args[1];
		String popFilename = args[2];
		String targetCoordinateSystem = args[3];
		
		RouteAllModesAsCar routeAllModesAsCar = new RouteAllModesAsCar(networkFilename);
		routeAllModesAsCar.run(popFilename);
		routeAllModesAsCar.writeAsShape(outputDir, targetCoordinateSystem);
	}

	private void writeAsShape(String outputDir, String targetCoordinateSystem) {
		SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setCRS(MGC.getCRS(targetCoordinateSystem));
		typeBuilder.setName("linkCount");
		typeBuilder.add("location", LineString.class);
		typeBuilder.add("linkId", String.class);
		typeBuilder.add("total", Double.class);
		for (String mode : this.mode2link2totals.keySet()) {
			typeBuilder.add("count " + mode, Double.class);
		}
		
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(typeBuilder.buildFeatureType());
		
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		
		Object[] featureAttribs;
		
		for(Link link: this.sc.getNetwork().getLinks().values()){
			
			featureAttribs = createLinkFeatureAttribs(link, new Object[3 + this.mode2link2totals.keySet().size()]);

			// skip links without count
			if (featureAttribs == null) {
				continue;
			}

			try {
				features.add(builder.buildFeature(link.getId().toString(), featureAttribs));
			} catch (IllegalArgumentException e1) {
				e1.printStackTrace();
			}
		}
		
		ShapeFileWriter.writeGeometries(features, outputDir + "routeAllModesAsCar.shp");
	}

	private Object[] createLinkFeatureAttribs(Link link, Object[] objects) {
		if(this.link2totals.get(link) == null) {
			return null;
		}
		
		int total = this.link2totals.get(link).intValue();
		
		Coordinate[] coord =  new Coordinate[2];
		coord[0] = new Coordinate(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY(), 0.);
		coord[1] = new Coordinate(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY(), 0.);
		
		objects[0] = new GeometryFactory().createLineString(coord);
		objects[1] = link.getId().toString();
		objects[2] = total;
		
		int index = 3;
		for(String mode : this.mode2link2totals.keySet()){
			if (this.mode2link2totals.get(mode).get(link) == null) {
				objects[index] = 0;
			} else {
				objects[index] = this.mode2link2totals.get(mode).get(link).intValue();
			}
			index++;
		}
		return objects;
	}

	private void run(String popFilename) {
		this.sc.getConfig().plans().setInputFile(popFilename);
		Population pop = (Population) this.sc.getPopulation();
		StreamingDeprecated.setIsStreaming(pop, true);
		PopulationReader popReader = new PopulationReader(this.sc);
		StreamingDeprecated.addAlgorithm(pop, this);
		Gbl.printMemoryUsage();

		log.info("Start reading population...");
		popReader.readFile(popFilename);
		PopulationUtils.printPlansCount(pop) ;
		log.info("...done.");
		Gbl.printMemoryUsage();
		Gbl.printElapsedTime();
	}

	@Override
	public boolean judge(Person person) {
		// will never be called - we want to process all agents
		return true;
	}

	@Override
	public void run(Person person) {
		count();
		Activity lastAct = null;
		String currentMode = null;
		
		for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
			if (planElement instanceof Activity) {
				Activity act = (Activity) planElement;
				
				if (act.getType().equalsIgnoreCase(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
					continue;
				}
				
				if (lastAct == null) {
					lastAct = act;
				} else {
					Link lastLink = this.sc.getNetwork().getLinks().get(lastAct.getLinkId());
					Link currentLink = this.sc.getNetwork().getLinks().get(act.getLinkId());
					
					Path path = this.routingAlgo.calcLeastCostPath(lastLink.getToNode(), currentLink.getFromNode(), 0.0, null, null);
					this.storeLinks(currentMode, path.links);
					lastAct = act;
				}
			}
			
			if (planElement instanceof Leg) {
				Leg leg = (Leg) planElement;
				if (!leg.getMode().equalsIgnoreCase(TransportMode.transit_walk)) {
					currentMode = leg.getMode();
				}
			}
		}
	}

	private void storeLinks(String currentMode, List<Link> links) {
		for (Link link : links) {
			this.addOne(this.link2totals, link);
		
			if (this.mode2link2totals.get(currentMode) == null) {
				this.mode2link2totals.put(currentMode, new HashMap<Link, Integer>());
			}
		
			this.addOne(this.mode2link2totals.get(currentMode), link);
		}
	}

	private void addOne(HashMap<Link, Integer> link2count, Link link) {
		if (link2count.get(link) == null) {
			link2count.put(link, new Integer(0));
		}
		link2count.put(link, new Integer(link2count.get(link) + 1));
	}
}
