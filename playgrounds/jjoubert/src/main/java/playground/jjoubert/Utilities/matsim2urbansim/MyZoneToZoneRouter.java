/* *********************************************************************** *
 * project: org.matsim.*
 * MyZoneToZoneRouter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.jjoubert.Utilities.matsim2urbansim;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.knaw.dans.common.dbflib.CorruptedTableException;
import nl.knaw.dans.common.dbflib.DbfLibException;
import nl.knaw.dans.common.dbflib.Field;
import nl.knaw.dans.common.dbflib.IfNonExistent;
import nl.knaw.dans.common.dbflib.InvalidFieldLengthException;
import nl.knaw.dans.common.dbflib.InvalidFieldTypeException;
import nl.knaw.dans.common.dbflib.NumberValue;
import nl.knaw.dans.common.dbflib.Record;
import nl.knaw.dans.common.dbflib.Table;
import nl.knaw.dans.common.dbflib.Type;
import nl.knaw.dans.common.dbflib.Value;
import nl.knaw.dans.common.dbflib.Version;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactory;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactoryImpl;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.impl.DenseObjectMatrix2D;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class MyZoneToZoneRouter {
	private final Logger log = Logger.getLogger(MyZoneToZoneRouter.class);
	private final Scenario scenario;
	private final List<MyZone> zones;
	private Map<Id, Integer> mapZoneIdToListEntry;
	private Map<Integer, Id> mapListEntryToZoneId;
	private Dijkstra router;
	
	public MyZoneToZoneRouter(final Scenario scenario, final List<MyZone> zones) {
		this.scenario = scenario;
		this.zones = zones;
		this.router = null;	
		mapZoneIdToListEntry = new HashMap<Id, Integer>(zones.size());
		mapListEntryToZoneId = new HashMap<Integer, Id>(zones.size());
		int index = 0;
		for(MyZone z : zones){
			mapZoneIdToListEntry.put(z.getId(), index);
			mapListEntryToZoneId.put(index++, z.getId());
		}
	}
	
	
	public void prepareTravelTimeData(final String eventsFilename){
		log.info("Processing the events file for zone-to-zone travel time calculation");
		TravelTimeCalculatorFactory ttcf = new TravelTimeCalculatorFactoryImpl();
		TravelTimeCalculator travelTimeCalculator = ttcf.createTravelTimeCalculator(scenario.getNetwork(), scenario.getConfig().travelTimeCalculator());
		TravelCostCalculatorFactory tccf = new TravelCostCalculatorFactoryImpl();
		PersonalizableTravelCost travelCost = tccf.createTravelCostCalculator(travelTimeCalculator, scenario.getConfig().charyparNagelScoring());
		
		EventsManagerImpl em = new EventsManagerImpl();
		em.addHandler(travelTimeCalculator);
		new EventsReaderTXTv1(em).readFile(eventsFilename);
		
		log.info("Preprocessing the network for zone-to-zone travel time calculation.");
		PreProcessDijkstra pp = new PreProcessDijkstra();
		pp.run(this.scenario.getNetwork());
		
		router = new Dijkstra(scenario.getNetwork(), travelCost, travelTimeCalculator,pp);
		
		log.info("Zone to zone router prepared.");
	}
	
	public DenseDoubleMatrix2D processZones(DenseDoubleMatrix2D matrix, Map<Id,Double> linkstats){
		log.info("Processing zone-to-zone travel times.");
		GeometryFactory gf = new GeometryFactory();
		NetworkImpl ni = (NetworkImpl) scenario.getNetwork();
		for(int row = 0; row < matrix.rows(); row++){
			for(int col = 0; col < matrix.columns(); col++){
				if(matrix.get(row, col) == Double.POSITIVE_INFINITY){
					if(row == col){
						/*
						 *  TODO Process diagonals;
						 *  Current I use the travel time of a link if EITHER 
						 *  its fromNode OR its toNode reside in the zone. 
						 */
						int count = 0;
						double tt = 0.0;
						MyZone zone = zones.get(row);
						Polygon envelope = (Polygon) zones.get(row).getEnvelope();
						for(Link l : ni.getLinks().values()){
							boolean fromIn = false;
							Point fp = gf.createPoint(new Coordinate(l.getFromNode().getCoord().getX(), l.getFromNode().getCoord().getY()));
							if(envelope.contains(fp)){
								if(zone.contains(fp)){
									fromIn = true;
								}
							}
							boolean toIn = false;
							Point tp = gf.createPoint(new Coordinate(l.getToNode().getCoord().getX(), l.getToNode().getCoord().getY()));
							if(envelope.contains(tp)){
								if(zone.contains(tp)){
									toIn = true;
								}
							}
							if(fromIn || toIn){
								count++;
								double ttFreespeed = l.getFreespeed(21600);
								double ttLinkstats = linkstats.get(l.getId());
								tt += ttLinkstats;
							}
						}
						if(count != 0){
							matrix.set(row, col, tt / ((double) count) );
						} else{
							log.error("Could not find any links within zone " + zone.getId());
						}
					} else{
						// TODO Process nondiagonals;
						
						Point fp = zones.get(row).getCentroid();
						Coord fc = new CoordImpl(fp.getX(), fp.getY());
						Node fn = ni.getNearestNode(fc);
						
						Point tp = zones.get(col).getCentroid();
						Coord tc = new CoordImpl(tp.getX(), tp.getY());
						Node tn = ni.getNearestNode(tc); 
						Path p = router.calcLeastCostPath(fn, tn, 21600);
						if(p != null){
							matrix.set(row, col, p.travelTime);
						} else{
							log.warn("Could not set travel time from zone " + zones.get(row).getId() + " to zone " + zones.get(col).getId());
						}
					}
				}
			}
		}
		// There should now be NO more null-entries.
		boolean empties = false;
		for(int row = 0; row < matrix.rows(); row++){
			for(int col = 0; col < matrix.columns(); col++){
				if(matrix.get(row, row) == Double.POSITIVE_INFINITY){
					log.warn("Empty travel time from zone " + zones.get(row).getId() + " to zone " + zones.get(col).getId());
					empties = true;
				}
			}
		}
		if(!empties){
			// Write DBF to file.
			
		}
		log.info("Completed zone-to-zone processing.");
		return matrix;
	}
	
	public void writeOdMatrixToDbf(String filename, DenseDoubleMatrix2D odMatrix) {
		log.info("Writing OD matrix travel time to " + filename);
		

		Field oId = new Field("from_zone_id", Type.NUMBER, 20);
		Field dId = new Field("to_zone_id", Type.NUMBER, 20);
		Field carTT = new Field("am_single_vehicle_to_work_travel_time", Type.FLOAT, 4);
		List<Field> listOfFields = new ArrayList<Field>();
		listOfFields.add(oId); listOfFields.add(dId); listOfFields.add(carTT);
		Map<String, Value> map = new HashMap<String, Value>();
		int nullcounter = 0;
		try {
			Table t = new Table(new File(filename), Version.DBASE_5, listOfFields);
			t.open(IfNonExistent.CREATE);
			try{
				int totalSize = (int) Math.pow(odMatrix.rows(),2);
				int counter = 0;
				int multiplier = 1;
				for(int row = 0; row < odMatrix.rows(); row++){
					for(int col = 0; col < odMatrix.columns(); col++){
						if(odMatrix.get(row, col) != Double.POSITIVE_INFINITY){
							Value o = new NumberValue(Integer.parseInt(mapListEntryToZoneId.get(row).toString()));
							Value d = new NumberValue(Integer.parseInt(mapListEntryToZoneId.get(col).toString()));
							Value tt = new NumberValue(odMatrix.get(row, col));
							map.put(oId.getName(), o);
							map.put(dId.getName(), d);
							map.put(carTT.getName(), tt);
							t.addRecord(new Record(map));
						} else{
							nullcounter++;
						}
						
						// Report progress.
						if(++counter == multiplier){
							double percentage = (((double) counter) / ((double) totalSize))*100;
							log.info("   Entries written: " + counter + " (" + String.format("%3.2f%%)", percentage));
							multiplier *= 2;
						}
					}
				}
				log.info("   Entries written: " + counter + " (Done)");
			} catch (DbfLibException e) {
				e.printStackTrace();
			} finally{
				t.close();
			}
			double density = Math.round((1 - ((double) nullcounter) / (double)(Math.pow(odMatrix.rows(),2)))*100);
			log.info("OD matrix written. Density: " + density + "% (" + nullcounter + " entries null)");
		} catch (CorruptedTableException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidFieldTypeException e) {
			e.printStackTrace();
		} catch (InvalidFieldLengthException e) {
			e.printStackTrace();
		}
	}
	
	public Double getAvgOdTravelTime(int i, int j, List<Double> list) {
		Double travelTime = null;
		double total = 0;
		for(Double d : list){
			total += d;
		}
		if(total != 0){
			travelTime = total / ((double) list.size());
		}
		return travelTime;
	}




	public Object getRouter() {
		return this.router;
	}
	
	
}
