/* *********************************************************************** *
 * project: org.matsim.*
 * MyPlansProcessor.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import cern.colt.matrix.ObjectMatrix2D;
import cern.colt.matrix.impl.DenseObjectMatrix2D;

public class MyPlansProcessor {
	private Logger log = Logger.getLogger(MyPlansProcessor.class);
	private Scenario scenario;
	private List<MyZone> zones;
	private Map<Id, Integer> mapZoneIdToListEntry;
	private Map<Integer, Id> mapListEntryToZoneId;
	private DenseObjectMatrix2D odMatrix;
	
	public MyPlansProcessor(Scenario scenario, List<MyZone> zones) {
		this.scenario = scenario;
		this.zones = zones;
		this.odMatrix = new DenseObjectMatrix2D(zones.size(), zones.size());
		mapZoneIdToListEntry = new HashMap<Id, Integer>(zones.size());
		mapListEntryToZoneId = new HashMap<Integer, Id>(zones.size());
		int index = 0;
		for(MyZone z : zones){
			mapZoneIdToListEntry.put(z.getId(), index);
			mapListEntryToZoneId.put(index++, z.getId());
		}
	}
	
	public void processPlans(){
		log.info("Processing plans (" + scenario.getPopulation().getPersons().size() + " agents)");
		int counter = 0;
		int multiplier = 1;
		GeometryFactory gf = new GeometryFactory();
		for(Person person : scenario.getPopulation().getPersons().values()){
			Plan plan = person.getSelectedPlan();
			PlanElement pe;
			for(int i = 0; i < plan.getPlanElements().size(); i++){
				pe = plan.getPlanElements().get(i);
				if(pe instanceof Leg){
					Leg l = (Leg) pe;
					if(l.getMode().equals(TransportMode.car)){
						// Only process the leg if it has been executed, i.e. has a travel time.
						if(l.getTravelTime() > 0){
							// TODO find the origin node's zone;
							Integer oIndex = null;
							int o = getLastActivity(plan, i);
							Activity oa = (Activity) plan.getPlanElements().get(o);
							Point op = gf.createPoint(new Coordinate(oa.getCoord().getX(), oa.getCoord().getY()));
							boolean found = false;
							int ii = 0;
							do {
								if(zones.get(ii).getEnvelope().contains(op)){
									if(zones.get(ii).contains(op)){
										found = true;
										oIndex = mapZoneIdToListEntry.get(zones.get(ii).getId());
									} else {
										ii++;
									}
								} else{
									ii++;
								}
							} while (found == false && ii < zones.size());
							if(!found){break; }//log.error("Could not find a zone for the origin activity.");}
							// TODO find the destination node's zone;
							Integer dIndex = null;
							int d = getNextActivity(plan, i);
							Activity da = (Activity) plan.getPlanElements().get(d);
							Point dp = gf.createPoint(new Coordinate(da.getCoord().getX(), da.getCoord().getY()));
							found = false;
							ii = 0;
							do {
								if(zones.get(ii).getEnvelope().contains(dp)){
									if(zones.get(ii).contains(dp)){
										found = true;
										dIndex = mapZoneIdToListEntry.get(zones.get(ii).getId());
									} else {
										ii++;
									}
								} else{
									ii++;
								}
							} while (found == false && ii < zones.size());
							if(!found){break;}//log.error("Could not find a zone for the destination activity.");}
							
							// Update travel
							if(odMatrix.get(oIndex, dIndex) == null){
								List<Double> list = new ArrayList<Double>();
								list.add(l.getTravelTime());
								odMatrix.set(oIndex, dIndex, list);
							} else{
								if(odMatrix.get(oIndex, dIndex) instanceof List<?>){
									((List<Double>) odMatrix.get(oIndex, dIndex)).add(l.getTravelTime());
								} else{
									log.warn("odMatrix contains an object other than a list.");
								}
							}
						}
					}
				}
			}
			
			// Report progress;
			if(++counter == multiplier){
				log.info("   agents: " + counter);
				multiplier *= 2;
			}
			
		}
		log.info("   agents: " + counter + " (Done)");
	}
	
	private Integer getLastActivity(Plan plan, int index){
		Integer result = null;
		do {
			if(plan.getPlanElements().get(index-1) instanceof Activity){
				result = index-1;
			} else{
				result = getLastActivity(plan, index-1);
			}
		} while (index >= 0 && result==null);
		if(index < 0){
			throw new RuntimeException("Plan does not start with an activity.");
		}
		return result;
	}
	
	private Integer getNextActivity(Plan plan, int index){
		Integer result = null;
		do {
			if(plan.getPlanElements().get(index+1) instanceof Activity){
				result = index+1;
			} else{
				result = getNextActivity(plan, index+1);
			}
		} while (index < plan.getPlanElements().size() && result == null);
		if(index >= plan.getPlanElements().size()){
			throw new RuntimeException("Plan does not end with an activity.");
		}
		return result;
	}

	public Scenario getScenario() {
		return scenario;
	}

	public DenseObjectMatrix2D getOdMatrix() {
		return odMatrix;
	}

	public List<MyZone> getZones() {
		return this.zones;
	}


	public Double getAvgOdTravelTime(int i, int j) {
		Double travelTime = null;
		double total = 0;
		List<Double> list = (List<Double>) this.odMatrix.get(i, j);
		for(Double d : list){
			total += d;
		}
		if(total != 0){
			travelTime = total / ((double) list.size());
		}
		return travelTime;
	}

	public void writeOdMatrixToDbf(String filename) {
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
						if(odMatrix.get(row, col) != null){
							Value o = new NumberValue(Integer.parseInt(mapListEntryToZoneId.get(row).toString()));
							Value d = new NumberValue(Integer.parseInt(mapListEntryToZoneId.get(col).toString()));
							Value tt = new NumberValue(this.getAvgOdTravelTime(row, col));
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
}

