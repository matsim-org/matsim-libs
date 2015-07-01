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
package playground.vsp.analysis.modules.ptAccessibility;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

import playground.vsp.analysis.modules.AbstractAnalysisModule;
import playground.vsp.analysis.modules.ptAccessibility.activity.ActivityLocation;
import playground.vsp.analysis.modules.ptAccessibility.activity.LocationMap;
import playground.vsp.analysis.modules.ptAccessibility.stops.Circle;
import playground.vsp.analysis.modules.ptAccessibility.stops.Mode2StopMap;
import playground.vsp.analysis.modules.ptAccessibility.stops.PtStopMap;
import playground.vsp.analysis.modules.ptAccessibility.utils.DistCluster2ActCnt;
import playground.vsp.analysis.modules.ptAccessibility.utils.PtAccesShapeWriter;
import playground.vsp.analysis.modules.ptAccessibility.utils.PtAccessMapShapeWriter;

import com.vividsolutions.jts.geom.MultiPolygon;

/**
 * @author droeder
 *
 */
public class PtAccessibility extends AbstractAnalysisModule {
	
	public static final String MODULENAME = "ptAccessibility";

	private Scenario scenario;

	private Map<String, Circle> distanceCluster;

	private SortedMap<String, List<String>> activityCluster;
	
	private LocationMap locationMap;

	private SortedMap<String, DistCluster2ActCnt> mode2DistanceCluster2ActCnt;

	private Mode2StopMap mode2Stop;

	private int quadrantSegments;

	private final String targetCoordinateSystem;

	private final int gridSize;
	


	/**
	 * This module creates circles around all used TransitStops within the specified distances for all modes 
	 * and writes one shapefile per distance-cluster.
	 * After that the activities will be clustered as specified and written to a shapefile.
	 * Out of the processed data a csv-file is generated, containing a table of the relative number of stops per mode within 
	 * the specified clusters. 
	 * 
	 * @param sc, the scenario
	 * @param quadrantSegments, the number of point per quadrant, more point return more accurate shapes but for a higher computational price
	 * @param distanceCluster, the distances you want to cluster (make up your mind about the used coordinate-system)
	 * @param activityCluster, the name you want to the see, mapped to the activity-names you want to add to this cluster
	 */
	public PtAccessibility(Scenario sc, List<Integer> distanceCluster, int quadrantSegments, SortedMap<String, List<String>> activityCluster, String targetCoordinateSystem, int gridSize) {
		super(PtAccessibility.class.getSimpleName());
		this.scenario = sc;
		this.quadrantSegments = quadrantSegments;
		this.distanceCluster = createClusterCircles(distanceCluster, this.quadrantSegments);
		this.activityCluster = activityCluster;
		this.activityCluster.put("unknown", new ArrayList<String>());
		this.targetCoordinateSystem = targetCoordinateSystem;
		this.gridSize = gridSize;
	}

	/**
	 * @param cluster2
	 * @return
	 */
	private Map<String, Circle> createClusterCircles(List<Integer> distances, int quadrantSegments) {
		Map<String, Circle> circles = new HashMap<String, Circle>();
		for(Integer i: distances){
			circles.put(i.toString(), new Circle(Double.valueOf(i), quadrantSegments));
		}
		return circles;
	}

	@Override
	public List<EventHandler> getEventHandler() {
		return new ArrayList<EventHandler>();
	}

	@Override
	public void preProcessData() {
		// init stops, only served stops	
		this.mode2Stop = new Mode2StopMap(this.distanceCluster);
		for(TransitLine l: this.scenario.getTransitSchedule().getTransitLines().values()){
			for(TransitRoute r: l.getRoutes().values()){
				for(TransitRouteStop s: r.getStops()){
					this.mode2Stop.addStop(r.getTransportMode(), s.getStopFacility());
				}
			}
		}
		
		// prepare the activity-locations. count activity from the same type, at the same location only once...
		this.locationMap = new LocationMap(this.activityCluster);
		
		for(Person p: this.scenario.getPopulation().getPersons().values()){
			for(PlanElement pe: p.getSelectedPlan().getPlanElements()){
				if(pe instanceof Activity){
					locationMap.addActivity((Activity) pe);
				}
			}
		}
	}


	@Override
	public void postProcessData() {
		this.mode2DistanceCluster2ActCnt = new TreeMap<String, DistCluster2ActCnt>();
		DistCluster2ActCnt cnt;
		// iterate over all ptStopMaps (per mode)
		for(Entry<String, PtStopMap> mode2Stops: this.mode2Stop.getStopMaps().entrySet()){
			cnt = new DistCluster2ActCnt(this.distanceCluster.keySet(), this.activityCluster.keySet());
			// count the number of activities per Type
			for(Entry<String, List<ActivityLocation>> type2locations: this.locationMap.getType2Locations().entrySet()){
				for(ActivityLocation l: type2locations.getValue()){
					for(String dist: this.distanceCluster.keySet()){
						// increase when the activity takes place within this distance
						if(mode2Stops.getValue().contains(l.getCoord(), dist)){
							cnt.increase(dist, type2locations.getKey());
						}
					}
				}
			}
			this.mode2DistanceCluster2ActCnt.put(mode2Stops.getKey(), cnt);
		}
	}


	@Override
	public void writeResults(String outputFolder) {
		Map<String, Map<String, MultiPolygon>> cluster2mode2area = new HashMap<String, Map<String, MultiPolygon>>();
		
		// write stopCluster
		for(Entry<String, PtStopMap> m: this.mode2Stop.getStopMaps().entrySet()){
//			PtAccesShapeWriter.writeMultiPolygons(m.getValue().getCluster(), outputFolder + m.getKey() + PtStopMap.FILESUFFIX, m.getKey());
			for(Entry<String, MultiPolygon> e: m.getValue().getCluster().entrySet()){
				if(!cluster2mode2area.containsKey(e.getKey())){
					cluster2mode2area.put(e.getKey(), new HashMap<String, MultiPolygon>());
				}
				cluster2mode2area.get(e.getKey()).put(m.getKey(), e.getValue());
			}
		}
		for(Entry<String, Map<String, MultiPolygon>> e: cluster2mode2area.entrySet()){
			PtAccesShapeWriter.writeMultiPolygons(e.getValue(), outputFolder + PtStopMap.FILESUFFIX + "_" + e.getKey() + ".shp", e.getKey(), this.targetCoordinateSystem);
		}
		PtAccessMapShapeWriter.writeAccessMap(cluster2mode2area, this.quadrantSegments, outputFolder, this.targetCoordinateSystem);
		
		// write activity-cluster
		PtAccesShapeWriter.writeActivityLocations(this.locationMap, outputFolder, "activities", this.targetCoordinateSystem, this.gridSize);
		// write locations to csv
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder + "activityLocations.csv");
		try {
			writer.write("index;x;y;type");
			writer.newLine();
			int i = 0;
			for(Entry<String, List<ActivityLocation>> e: this.locationMap.getType2Locations().entrySet()){
				for(ActivityLocation l: e.getValue()){
					writer.write(String.valueOf(i) + ";" + l.getCoord().x + ";" + l.getCoord().y + ";" + l.getType());
					writer.newLine();
					i++;
				}
			}
			writer.flush();
			writer.close();
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		
		//write csv-file, agregated
		// get the number of activities in distanceCluster, per mode
		for(Entry<String, DistCluster2ActCnt> e: this.mode2DistanceCluster2ActCnt.entrySet()){
			writer = IOUtils.getBufferedWriter(outputFolder + e.getKey().toString() + ".csv");
			try {
				//write the header
				writer.write("cluster;");
				for(String s: this.activityCluster.keySet()){
					writer.write(s + ";");
				}
				writer.newLine();
				// get the ActivityTypes and corresponding number of activities within in specified distance...
				for(Entry<String, SortedMap<String, Double>> ee: e.getValue().getResults().entrySet()){
					writer.write(ee.getKey() + ";");
					// get the counts per actType and write the relative value
					for(Entry<String, Double> eee: ee.getValue().entrySet()){
						writer.write(eee.getValue()/this.locationMap.getType2Locations().get(eee.getKey()).size() +";");
					}
					writer.newLine();
				}
				writer.flush();
				writer.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			 
		}
		
	}
	
}

