/* *********************************************************************** *
 * project: org.matsim.*
 * MyDemandMatrix.java
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

package playground.jjoubert.Utilities.roadpricing;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;

public class MyDemandMatrix {
	private Logger log = Logger.getLogger(MyDemandMatrix.class);
	private Map<Id, Coord> locationCoordinates;
	private Matrix demandMatrix;

	public MyDemandMatrix() {
		locationCoordinates = new TreeMap<Id, Coord>();
	}
	
	/**
	 * Reads a matrix file and parses a demand {@link Matrix}.
	 * <P>
	 * <b><i>Note:</i></b>
	 * <ul>
	 * 	<li> First row is <i>not</i> a column header.
	 * 	<li> Each row's first entry is a location identifier. 
	 * </ul>
	 * </P>
	 * @param filename the absolute path of the <code>csv</code>-file that 
	 * 		contains the origin-destination matrix.
	 * @param matrixName for the origin-destination matrix provided.
	 * @param matrixDesc describingthe given origin-destination matrix.
	 * @see {@link Matrix}
	 */
	public void parseMatrix(String filename, String matrixName, String matrixDesc) {
		log.info("Reading matrix from " + filename);
		List<Id> locations = new ArrayList<Id>(); 
		Matrix matrix = new Matrix(matrixName, matrixDesc);
		try {
			BufferedReader br = IOUtils.getBufferedReader(filename);
			try{
				String line = null;
				while((line = br.readLine()) != null){
					String [] sa = line.split(",");
					locations.add(new IdImpl(sa[0]));
				}
				log.info("   ...matrix seem to have " + locations.size() + " locations.");
			} finally{
				br.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			BufferedReader br = IOUtils.getBufferedReader(filename);
			try{
				String line = null;
				while((line = br.readLine()) != null){
					String [] sa = line.split(",");
					if(sa.length-1 != locations.size()){
						log.warn("Locations: " + locations.size() + "; Row length: " + sa.length);
					}
					Id fromId = new IdImpl(sa[0]);
					if(!locationCoordinates.containsKey(fromId)){
						log.warn("Location " + fromId + " not found in list of locations. May not be fatal.");
					}
					for(int i = 1; i < sa.length; i++){
						Id toId = locations.get(i-1);
						if(!locationCoordinates.containsKey(fromId)){
							log.warn("Location " + toId + " not found in list of locations. May not be fatal.");
						}
						matrix.createEntry(fromId, toId, Double.parseDouble(sa[i]));
					}					
				}
			} finally{
				br.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		demandMatrix = matrix;
	}

	public void readLocationCoordinates(String filename) {
		log.info("Reading location coordinates from " + filename);
		try {
			BufferedReader br = IOUtils.getBufferedReader(filename);
			try{
				String line = br.readLine(); // Header line.
				while((line = br.readLine()) != null){
					String[] sa = line.split(",");
					if(sa.length == 3){
						locationCoordinates.put(new IdImpl(sa[2]), new CoordImpl(sa[0], sa[1]));						
					}
				}
			} finally{
				br.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public Map<Id, Coord> getLocationCoordinates() {
		return locationCoordinates;
	}

	public Matrix getDemandMatrix() {
		return demandMatrix;
	}

	public Scenario generateDemand(List<Id> list, Random r, double fraction) {
		if(locationCoordinates.size() == 0){
			throw new RuntimeException("Cannot create plans if no locations coordinates exist.");
		}
		if(demandMatrix == null){
			throw new RuntimeException("Cannot create plans demand matrix is null.");
		}
		Scenario sc = new ScenarioImpl();
		Population pop = sc.getPopulation();
		PopulationFactory pf = pop.getFactory();
		
		// Determine the total number of agents to create entering FROM each external zone.
		Map<Id, Double> rowSum = new TreeMap<Id, Double>();
		for(Id fromId : demandMatrix.getFromLocations().keySet()){
			if(list.contains(fromId)){
				double sum = 0.0;
				for(Entry e : demandMatrix.getFromLocEntries(fromId)){
					sum += e.getValue();
				}
				rowSum.put(fromId, sum);
//				log.info("Row sum (" + fromId + "): " + sum);
			}
		}
		
		// Determine the total number of agents to create leaving TO each external zone.
		Map<Id, Double> columnSum = new TreeMap<Id, Double>();
		for(Id toId : demandMatrix.getToLocations().keySet()){
			if(list.contains(toId)){
				double sum = 0.0;
				for(Entry e : demandMatrix.getToLocEntries(toId)){
					sum += e.getValue();
				}
				columnSum.put(toId, sum);
//				log.info("Column sum (" + toId + "): " + sum);
			}
		}

		// Create agents
		long newId = 0;
		for(Id id : list){
			/*
			 * First the agents leaving FROM each zone, i.e. row sums.
			 */
			Map<Id,Double> rowProb = new TreeMap<Id, Double>();
			// Create probabilities of selection.
			List<Id> idList = new ArrayList<Id>();
			List<Double> probList = new ArrayList<Double>();
	
			for(Id toId : demandMatrix.getToLocations().keySet()){
				Double prob = new Double(0.0); 
				if(rowSum.get(id) > 0){
					prob = demandMatrix.getEntry(id, toId).getValue() / rowSum.get(id);
				}
				rowProb.put(toId, prob);
			}
			double cumSum = 0.0;
			for(Id toId : rowProb.keySet()){
				idList.add(toId);
				probList.add(cumSum += rowProb.get(toId));
			}
			/*
			 * It does happen, due to rounding errors, that the cumulative sum 
			 * is not exactly 1.000 - start at the end and work your way back
			 * through the cumulative sum list, changing the values to 1.0 
			 */
			if(probList.get(probList.size()-1) != 1.0){
				log.warn("Cumulative sum for location Id " + id + " does not add up to 1, but " + probList.get(probList.size()-1));
				log.warn("   Row sum: " + rowSum.get(id));
				int step = 1;
				boolean found = false;
				while(!found && step < probList.size()){
					probList.set(probList.size()-step, 1.0);
					if(probList.get(probList.size()-(step+1)) < 1.0){
						found = true;
						log.warn("   Fixed the last " + step + " value(s) to 1.0");
					} else{
						step++;
					}
				}
			}
			
			int numberOfPlans = (int)(rowSum.get(id)*fraction);
			if( numberOfPlans >= 1){
				for(int i = 0; i < numberOfPlans; i++){
					Double d = r.nextDouble();
					boolean found = false;
					int index = 0;
					while(!found && index < probList.size()){
						if(probList.get(index).compareTo(new Double(d)) > 0){
							found = true;
						} else{
							index++;
						}
					}
					Id theDestination = null;
					if(index < probList.size()){
						theDestination = idList.get(index);		
					} else{
						log.warn("Something wrong! Couldn't find an appropriate destination.");	
						log.warn("    Random number: " + d);
						log.warn("   Cumulative sum: " + probList.get(probList.size()-1));
					}
					
					Plan plan = pf.createPlan();
					
					Activity home1 = pf.createActivityFromCoord("home", locationCoordinates.get(id));
					home1.setEndTime(21600); // 06:00
					plan.addActivity(home1);
					Leg toWork = pf.createLeg("car");
					plan.addLeg(toWork);
					Activity work = pf.createActivityFromCoord("work", locationCoordinates.get(theDestination));
					work.setStartTime(25200); // 07:00
					plan.addActivity(work);
					Leg fromWork = pf.createLeg("car");
					plan.addLeg(fromWork);
					Activity home2 = pf.createActivityFromCoord("home", locationCoordinates.get(id));
					home2.setStartTime(64800);
					plan.addActivity(home2);
					
					Person p = pf.createPerson(new IdImpl(newId++));
					p.addPlan(plan);
					pop.addPerson(p);				
				}
			}
			
			/*
			 * Second the agents arriving TO each zone, i.e. column sums.
			 */
			Map<Id,Double> columnProb = new TreeMap<Id, Double>();
			// Create probabilities of selection.
			idList = new ArrayList<Id>();
			probList = new ArrayList<Double>();
			
			for(Id fromId : demandMatrix.getFromLocations().keySet()){
				Double prob = new Double(0.0);
				if(columnSum.get(id) > 0){
					prob = new Double(demandMatrix.getEntry(fromId, id).getValue() / columnSum.get(id));
				}
				columnProb.put(fromId, prob);
			}
			
			cumSum = 0.0;
			for(Id fromId : columnProb.keySet()){
				idList.add(fromId);
				probList.add(cumSum += columnProb.get(fromId));
			}
			/*
			 * It does happen, due to rounding errors, that the cumulative sum 
			 * is not exactly 1.000 - start at the end and work your way back
			 * through the cumulative sum list, changing the values to 1.0 
			 */
			if(probList.get(probList.size()-1) != 1.0){
				log.warn("Cumulative sum for location Id " + id + " does not add up to 1, but " + probList.get(probList.size()-1));
				log.warn("   Row sum: " + rowSum.get(id));
				int step = 1;
				boolean found = false;
				while(!found && step < probList.size()){
					probList.set(probList.size()-step, 1.0);
					if(probList.get(probList.size()-(step+1)) < 1.0){
						found = true;
						log.warn("   Fixed the last " + step + " value(s) to 1.0");
					} else{
						step++;
					}
				}
			}

			numberOfPlans = (int)(columnSum.get(id)*fraction);
			if(numberOfPlans >= 1){
				for(int i = 0; i < numberOfPlans; i++){
					Double d = r.nextDouble();
					boolean found = false;
					int index = 0;
					while(!found && index < probList.size()){
						if(probList.get(index).compareTo(new Double(d)) > 0){
							found = true;
						} else{
							index++;
						}
					}
					
					Id theOrigin = null;
					if(index < probList.size()){
						theOrigin = idList.get(index);		
					} else{
						log.warn("Something wrong! Couldn't find an appropriate origin.");	
						log.warn("    Random number: " + d);
						log.warn("   Cumulative sum: " + probList.get(probList.size()-1));
					}
					
					Plan plan = pf.createPlan();
					
					Activity home1 = pf.createActivityFromCoord("home", locationCoordinates.get(theOrigin));
					home1.setEndTime(21600); // 06:00
					plan.addActivity(home1);
					Leg toWork = pf.createLeg("car");
					plan.addLeg(toWork);
					Activity work = pf.createActivityFromCoord("work", locationCoordinates.get(id));
					work.setStartTime(25200); // 07:00
					plan.addActivity(work);
					Leg fromWork = pf.createLeg("car");
					plan.addLeg(fromWork);
					Activity home2 = pf.createActivityFromCoord("home", locationCoordinates.get(theOrigin));
					home2.setStartTime(64800);
					plan.addActivity(home2);
					
					Person p = pf.createPerson(new IdImpl(newId++));
					p.addPlan(plan);
					pop.addPerson(p);				
				}
			}
		}
		return sc;
	}

}
