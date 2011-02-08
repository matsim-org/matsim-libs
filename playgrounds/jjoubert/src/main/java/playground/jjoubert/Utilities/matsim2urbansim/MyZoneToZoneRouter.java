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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;

import cern.colt.matrix.impl.DenseDoubleMatrix2D;

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
	private DenseDoubleMatrix2D odMatrix;
	
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
		PersonalizableTravelCost travelCost = tccf.createTravelCostCalculator(travelTimeCalculator, scenario.getConfig().planCalcScore());
		
		EventsManagerImpl em = new EventsManagerImpl();
		em.addHandler(travelTimeCalculator);
		new EventsReaderTXTv1(em).readFile(eventsFilename);
		
		log.info("Preprocessing the network for zone-to-zone travel time calculation.");
		PreProcessDijkstra pp = new PreProcessDijkstra();
		pp.run(this.scenario.getNetwork());
		
		router = new Dijkstra(scenario.getNetwork(), travelCost, travelTimeCalculator,pp);
		
		log.info("Zone to zone router prepared.");
	}
	
	public boolean processZones(DenseDoubleMatrix2D matrix, Map<Id,Double> linkstats){
		/* Create a list of zone Ids with links within them. This list will be 
		 * used to generate the intra-zonal travel time for those zones without
		 * any links by taking the mode of this list. */
		List<Double> iztt = new ArrayList<Double>();
		
		log.info("Processing zone-to-zone travel times.");
		this.odMatrix = matrix;
		GeometryFactory gf = new GeometryFactory();
		NetworkImpl ni = (NetworkImpl) scenario.getNetwork();
		int total = odMatrix.rows()*odMatrix.columns();
		int counter = 0;
		int multiplier = 1;
		for(int row = 0; row < odMatrix.rows(); row++){
			for(int col = 0; col < odMatrix.columns(); col++){
				if(odMatrix.get(row, col) == Double.POSITIVE_INFINITY){
					if(row == col){
						/*=====================================================
						 *  Process diagonals
						 *-----------------------------------------------------
						 *  Currently I use the travel time of a link if EITHER 
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
								double ttLinkstats = linkstats.get(l.getId());
								tt += ttLinkstats;
							}
						}
						if(count != 0){
							odMatrix.set(row, col, tt / ((double) count) );
							iztt.add(tt / ((double) count));
						} else{
							log.warn("   Could not find any links within zone " + zone.getId());
						}
					} else{
						/*=====================================================
						 * Process nondiagonals;
						 *----------------------------------------------------- 
						 */
						
						Point fp = zones.get(row).getCentroid();
						Coord fc = new CoordImpl(fp.getX(), fp.getY());
						Node fn = ni.getNearestNode(fc);
						
						Point tp = zones.get(col).getCentroid();
						Coord tc = new CoordImpl(tp.getX(), tp.getY());
						Node tn = ni.getNearestNode(tc); 
						
						Path p = router.calcLeastCostPath(fn, tn, 25200); /* Use 07:00:00 */
						if(p != null){
							odMatrix.set(row, col, p.travelTime);
						} else{
							log.warn("Could not set travel time from zone " + zones.get(row).getId() + " to zone " + zones.get(col).getId());
						}
					}
				}
				// Report progress.
				if(++counter == multiplier){
					double percentage = (((double) counter) / ((double) total))*100;
					log.info(String.format("   matrix entries completed: %d (%3.2f%%)", counter, percentage));
					multiplier *= 2;
				}
			}
		}
		
		/* ====================================================================
		 * Now reprocess those diagonals that didn't have any links within them. 
		 * --------------------------------------------------------------------*/
		double intraZonalTravelTime = 5.0;
		if(iztt.size() > 0){
			/* Sort the intra-zonal travel time list. */
			Collections.sort(iztt);
			intraZonalTravelTime = iztt.get(Math.min(0, ((int)Math.round(iztt.size()))-1) );
		}
		for(int row = 0; row < odMatrix.rows(); row++){
			if(odMatrix.get(row, row) == Double.POSITIVE_INFINITY){
				odMatrix.set(row, row, intraZonalTravelTime);
			}
		}
		
		log.info(String.format("   matrix entries completed: %d (Done)", counter));

		// There should now be NO more null-entries.
		boolean empties = false;
		for(int row = 0; row < odMatrix.rows(); row++){
			for(int col = 0; col < odMatrix.columns(); col++){
				if(odMatrix.get(row, row) == Double.POSITIVE_INFINITY){
					log.warn("Empty travel time from zone " + zones.get(row).getId() + " to zone " + zones.get(col).getId());
					empties = true;
				}
			}
		}
		if(empties){
			log.info("Completed zone-to-zone processing... UNsuccesful.");
		} else{
			log.info("Completed zone-to-zone processing... SUCCESSFUL.");
		}
		return empties;
	}
	
	/**
	 * Writes the private car travel time (in seconds) to a comma-separated 
	 * flat file.
	 * @param filename the absolute path of the file to which the output is 
	 * 		  written.
	 * @param odMatrix the private car travel time matrix.
	 */
	public void writeOdMatrixToCsv(String filename, DenseDoubleMatrix2D odMatrix){
		log.info("Writing OD matrix travel time to " + filename);
		int nullcounter = 0;
		try {
			BufferedWriter output = IOUtils.getBufferedWriter(filename);
			try{
				output.write("fromZone,toZone,carTime");
				output.newLine();
				int totalSize = (int) Math.pow(odMatrix.rows(),2);
				int counter = 0;
				int multiplier = 1;
				for(int row = 0; row < odMatrix.rows(); row++){
					for(int col = 0; col < odMatrix.columns(); col++){
						if(odMatrix.get(row, col) != Double.POSITIVE_INFINITY){
							output.write(mapListEntryToZoneId.get(row).toString());
							output.write(",");
							output.write(mapListEntryToZoneId.get(col).toString());
							output.write(",");
							output.write(String.valueOf(odMatrix.get(row, col)));
							output.newLine();
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
			} finally {
				output.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		double density = Math.round((1 - ((double) nullcounter) / (double)(Math.pow(odMatrix.rows(),2)))*100);
		log.info("OD matrix written. Density: " + density + "% (" + nullcounter + " entries null)");
	}
	
	public void writeOdMatrixToDbf(String filename, DenseDoubleMatrix2D odMatrix) {
		log.info("Writing OD matrix travel time to " + filename);

		Field oId = new Field("fromZone", Type.NUMBER, 20);
		Field dId = new Field("toZone", Type.NUMBER, 20);
		Field carTT = new Field("carTime", Type.FLOAT, 4);
//		Field oId = new Field("from_zone_id", Type.NUMBER, 20);
//		Field dId = new Field("to_zone_id", Type.NUMBER, 20);
//		Field carTT = new Field("am_single_vehicle_to_work_travel_time", Type.FLOAT, 4);
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
							Value o = new NumberValue(Math.round(Double.parseDouble(mapListEntryToZoneId.get(row).toString())));
							Value d = new NumberValue(Math.round(Double.parseDouble(mapListEntryToZoneId.get(col).toString())));
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
	
	public DenseDoubleMatrix2D getOdMatrix(){
		return this.odMatrix;
	}

	public Object getRouter() {
		return this.router;
	}
	
	public Map<Id, Integer> getZoneToMatrixMap() {
		return mapZoneIdToListEntry;
	}

	public Map<Integer, Id> getMatrixToZoneMap() {
		return mapListEntryToZoneId;
	}


}
