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

package playground.vsp.andreas.utils.pop;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.StreamingDeprecated;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.GeoFileReader;

/**
 * Filters a given Population by a given shape - includes all routed modes
 *
 * @author aneumann
 *
 */
public class FilterPopulationByShape implements LinkEnterEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler{

	private final static Logger log = LogManager.getLogger(FilterPopulationByShape.class);
	private GeometryFactory factory;
	private Geometry areaToExclude;
	private Geometry areaToInclude;
	private TreeSet<String> includeLinks;
	private TreeSet<Id> agentsToKeep;

	private HashMap<Id, TreeSet<Id>> vehId2AgentIdsMap;

	public FilterPopulationByShape(String netFile, String popInFile, String eventsFile, String areaShapeFile, String popOutFile){
		Gbl.startMeasurement();
		Gbl.printMemoryUsage();

		log.info("Network: " + netFile);
		log.info("Population: " + popInFile);
		log.info("Events: " + eventsFile);
		log.info("Shape: " + areaShapeFile);
		log.info("Population out: " + popOutFile);

		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		sc.getConfig().network().setInputFile(netFile);
		ScenarioUtils.loadScenario(sc);

		this.factory = new GeometryFactory();
		this.areaToExclude = this.factory.buildGeometry(new ArrayList<Geometry>());
		this.createServiceAreaShp(areaShapeFile);
		this.includeLinks = new TreeSet<String>();

		log.info(sc.getNetwork().getLinks().values().size() + " links in given network");
		for (Link link : sc.getNetwork().getLinks().values()) {
			if (this.linkToNodeInServiceArea(link)) {
				this.includeLinks.add(link.getId().toString());
			}
		}
		log.info(this.includeLinks.size() + " links in area of interest");

		this.agentsToKeep = new TreeSet<Id>();
		this.vehId2AgentIdsMap = new HashMap<Id, TreeSet<Id>>();
		EventsManager eventsManager = EventsUtils.createEventsManager();
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(eventsManager);
		eventsManager.addHandler(this);
		reader.readFile(eventsFile);
		log.info("Found " + this.agentsToKeep.size() + " agent ids to keep.");
		Gbl.printMemoryUsage();
		Gbl.printElapsedTime();

		sc.getConfig().plans().setInputFile(popInFile);
		Population pop = (Population) sc.getPopulation();
		StreamingDeprecated.setIsStreaming(pop, true);
		PopulationReader popReader = new PopulationReader(sc);
		StreamingPopulationWriter popWriter = new StreamingPopulationWriter();
		popWriter.startStreaming(popOutFile);

		StreamingDeprecated.addAlgorithm(pop, new PersonIdFilter(this.agentsToKeep, popWriter));
		Gbl.printMemoryUsage();

		popReader.readFile(popInFile);
		PopulationUtils.printPlansCount(pop) ;
		popWriter.closeStreaming();
		Gbl.printMemoryUsage();
		Gbl.printElapsedTime();
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (!this.includeLinks.contains(event.getLinkId().toString())) {
			// link not of interest
			return;
		}

		if (this.vehId2AgentIdsMap.get(event.getVehicleId()) == null) {
			// it's a private car
			this.agentsToKeep.add(event.getDriverId());
		} else {
			// it's a public transport
			this.agentsToKeep.addAll(this.vehId2AgentIdsMap.get(event.getVehicleId()));
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		this.vehId2AgentIdsMap.get(event.getVehicleId()).remove(event.getPersonId());
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (this.vehId2AgentIdsMap.get(event.getVehicleId()) == null) {
			this.vehId2AgentIdsMap.put(event.getVehicleId(), new TreeSet<Id>());
		}

		this.vehId2AgentIdsMap.get(event.getVehicleId()).add(event.getPersonId());
	}

	private boolean linkToNodeInServiceArea(Link link) {
		Point p = factory.createPoint(MGC.coord2Coordinate(link.getToNode().getCoord()));
		if(this.areaToInclude.contains(p)){
			if(areaToExclude.contains(p)){
				return false;
			}
			return true;
		}
		return false;
	}

	private void createServiceAreaShp(String serviceAreaFile) {
		Collection<SimpleFeature> features = GeoFileReader.getAllFeatures(serviceAreaFile);
		Collection<Geometry> include = new ArrayList<Geometry>();
		Collection<Geometry> exclude = new ArrayList<Geometry>();

		for (SimpleFeature f: features) {
			boolean incl = true;
			Geometry g = null;
			for(Object o: f.getAttributes()){
				if(o instanceof Polygon){
					g = (Geometry) o;
				}else if (o instanceof MultiPolygon){
					g = (Geometry) o;
				}
				// TODO use a better way to get the attributes, maybe directly per index.
				// Now the last attribute is used per default...
				else if (o instanceof String){
					incl = Boolean.parseBoolean((String) o);
				}
			}
			if(! (g == null)){
				if(incl){
					include.add(g);
				}else{
					exclude.add(g);
				}
			}
		}
		this.areaToInclude = this.factory.createGeometryCollection(
				include.toArray(new Geometry[include.size()])).buffer(0);
		this.areaToExclude = this.factory.createGeometryCollection(
				exclude.toArray(new Geometry[exclude.size()])).buffer(0);
	}

	public static void main(String[] args) {
		// Charlottenburg
//		String netFile = "e:/_shared-svn/andreas/paratransit/input/trb_2012/network.final.xml.gz";
//		String popInFile = "e:/_shared-svn/andreas/paratransit/input/trb_2012/plans/bvg.run189.10pct.100.plans.selected_movedToTXL.xml.gz";
//		String eventsFile = "e:/berlin-bvg09_runs/bvg.run189.10pct/ITERS/it.100/bvg.run189.10pct.100.events.xml.gz";
//		String areaShapeFile = "e:/_shared-svn/andreas/paratransit/input/trb_2012/area/simulated_area.shp";
//		String popOutFile = "e:/_shared-svn/andreas/paratransit/input/trb_2012/plans/bvg.run189.10pct.100.plans.selected_movedToTXL_diluted.xml.gz";

		// Spandau
//		String netFile = "e:/_shared-svn/andreas/paratransit/input/trb_2012/network.final.xml.gz";
//		String popInFile = "e:/berlin-bvg09_runs/bvg.run192.100pct/ITERS/it.100/bvg.run192.100pct.100.plans.selected.xml.gz";
//		String eventsFile = "e:/berlin-bvg09_runs/bvg.run192.100pct/ITERS/it.100/bvg.run192.100pct.100.events.xml.gz";
//		String areaShapeFile = "e:/_shared-svn/andreas/paratransit/input/trb_2012/area/spandau_area.shp";
//		String popOutFile = "e:/_shared-svn/andreas/paratransit/input/trb_2012/plans/bvg.run192.100pct.100.plans.selected_spandau.xml.gz";

		// Spandau neu
		String netFile = "e:/_shared-svn/andreas/paratransit/input/trb_2012/network.final.xml.gz";
		String popInFile = "e:/_shared-svn/andreas/paratransit/input/trb_2012/plans/bvg.run189.10pct.100.plans.selected_movedToTXL.xml.gz";
		String eventsFile = "e:/berlin-bvg09_runs/bvg.run189.10pct/ITERS/it.100/bvg.run189.10pct.100.events.xml.gz";
		String areaShapeFile = "e:/_shared-svn/andreas/paratransit/input/trb_2012/area/spandau_area.shp";
		String popOutFile = "e:/_shared-svn/andreas/paratransit/input/trb_2012/plans/bvg.run189.10pct.100.plans.selected_movedToTXL_spandau.xml.gz";

		FilterPopulationByShape filter = new FilterPopulationByShape(netFile, popInFile, eventsFile, areaShapeFile, popOutFile);
	}

}
