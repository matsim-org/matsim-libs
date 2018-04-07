/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.contrib.noise.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.PtConstants;



/**
 * Computes a grid of receiver points and provides some basic spatial functionality,
 * e.g. the nearest receiver point for the coordinates of each 'considered' activity type. 
 * 
 * @author lkroeger, ikaddoura
 *
 */
public class Grid {
	
	private static final Logger log = Logger.getLogger(Grid.class);
			
	private final Scenario scenario;
	private final NoiseConfigGroup noiseParams;
		
	private final Map<Id<Person>, List<Coord>> personId2consideredActivityCoords = new HashMap<Id<Person>, List<Coord>>();
	
	private final Set<Coord> consideredActivityCoordsForSpatialFunctionality = new HashSet <Coord>();
	private final Set<Coord> consideredActivityCoordsForReceiverPointGrid = new HashSet <Coord>();
	
	private final Set<String> consideredActivitiesForSpatialFunctionality = new HashSet<String>();
	private final Set<String> consideredActivitiesForReceiverPointGrid = new HashSet<String>();
	
	private double xCoordMin = Double.MAX_VALUE;
	private double xCoordMax = Double.MIN_VALUE;
	private double yCoordMin = Double.MAX_VALUE;
	private double yCoordMax = Double.MIN_VALUE;
	
	private final Map<Coord,Id<ReceiverPoint>> activityCoord2receiverPointId = new HashMap<Coord, Id<ReceiverPoint>>();
	private Map<Id<ReceiverPoint>, ReceiverPoint> receiverPoints;
	
	public Grid(Scenario scenario) {
		this.scenario = scenario;	

		if ((NoiseConfigGroup) this.scenario.getConfig().getModule("noise") == null) {
			throw new RuntimeException("Could not find a noise config group. "
					+ "Check if the custom module is loaded, e.g. 'ConfigUtils.loadConfig(configFile, new NoiseConfigGroup())'"
					+ " Aborting...");
		}
		
		this.noiseParams = (NoiseConfigGroup) this.scenario.getConfig().getModule("noise");
		
		this.receiverPoints = new HashMap<Id<ReceiverPoint>, ReceiverPoint>();
		
		String[] consideredActTypesForDamagesArray = noiseParams.getConsideredActivitiesForDamageCalculationArray();
		for (int i = 0; i < consideredActTypesForDamagesArray.length; i++) {
			this.consideredActivitiesForSpatialFunctionality.add(consideredActTypesForDamagesArray[i]);
		}
		
		String[] consideredActTypesForReceiverPointGridArray = noiseParams.getConsideredActivitiesForReceiverPointGridArray();
		for (int i = 0; i < consideredActTypesForReceiverPointGridArray.length; i++) {
			this.consideredActivitiesForReceiverPointGrid.add(consideredActTypesForReceiverPointGridArray[i]);
		}

//		this.noiseParams.checkGridParametersForConsistency();
		initialize();
	}

	private void initialize() {
		setActivityCoords();
		
		if (this.noiseParams.getReceiverPointsCSVFile() == null) {
			log.info("Creating receiver point square grid...");
			createGrid();
		} else {
			log.info("Loading receiver points based on provided point coordinates in " + this.noiseParams.getReceiverPointsCSVFile());
			loadGrid();
		}
		
		setActivityCoord2NearestReceiverPointId();
		
		// delete unnecessary information
		this.consideredActivityCoordsForReceiverPointGrid.clear();
		this.consideredActivityCoordsForSpatialFunctionality.clear();
	}

	private void setActivityCoords () {
		StageActivityTypes stages = new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE);
		for (Person person: scenario.getPopulation().getPersons().values()) {

			for(Activity activity: TripStructureUtils.getActivities(person.getSelectedPlan(), stages)){
				if (this.consideredActivitiesForSpatialFunctionality.contains(activity.getType()) || consideredActivityPrefix(activity.getType(), this.consideredActivitiesForSpatialFunctionality)) {
					List<Coord> activityCoordinates = personId2consideredActivityCoords.get(person.getId());//new ArrayList<Coord>();
					
					if (activityCoordinates == null) {
						activityCoordinates = new ArrayList<>();
						personId2consideredActivityCoords.put(person.getId(), activityCoordinates);
					}
					
					activityCoordinates.add(activity.getCoord());
					
					consideredActivityCoordsForSpatialFunctionality.add(activity.getCoord());
				}
				
				if (this.consideredActivitiesForReceiverPointGrid.contains(activity.getType()) || consideredActivityPrefix(activity.getType(), consideredActivitiesForReceiverPointGrid)) {
					consideredActivityCoordsForReceiverPointGrid.add(activity.getCoord());
				}
			}
		}
	}
	
	private boolean consideredActivityPrefix(String type, Set<String> list) {
		for (String consideredActivity : list) {
			if (consideredActivity.endsWith("*")) {
				if (type.startsWith(consideredActivity.substring(0, consideredActivity.length() - 1))) {
					return true;
				}
			}
		}
		return false;
	}

	private void loadGrid() {
				
		String gridCSVFile = this.noiseParams.getReceiverPointsCSVFile();
		
		Map<Id<ReceiverPoint>, Coord> gridPoints = null;
		try {
			gridPoints = readCSVFile(gridCSVFile, ",", 0, 1, 2);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(this.noiseParams.getReceiverPointsCSVFileCoordinateSystem(), this.scenario.getConfig().global().getCoordinateSystem());
		
		for (Id<ReceiverPoint> id : gridPoints.keySet()) {
			
			Coord coord = gridPoints.get(id);
			Coord transformedCoord = ct.transform(coord);
			
			ReceiverPoint rp = new ReceiverPoint(id, transformedCoord);			
			receiverPoints.put(id, rp);
		}
		
		log.info("Total number of receiver points: " + receiverPoints.size());
	}

	
	private void createGrid() {
		
		if (this.noiseParams.getReceiverPointsGridMinX() == 0. && this.noiseParams.getReceiverPointsGridMinY() == 0. && this.noiseParams.getReceiverPointsGridMaxX() == 0. && this.noiseParams.getReceiverPointsGridMaxY() == 0.) {
			
			log.info("Creating receiver points for the entire area between the minimum and maximium x and y activity coordinates of all activity locations.");
						
			for (Coord coord : consideredActivityCoordsForReceiverPointGrid) {
				if (coord.getX() < xCoordMin) {
					xCoordMin = coord.getX();
				}
				if (coord.getX() > xCoordMax) {
					xCoordMax = coord.getX();
				}
				if (coord.getY() < yCoordMin) {
					yCoordMin = coord.getY();
				}
				if (coord.getY() > yCoordMax) {
					yCoordMax = coord.getY();
				}
			}
			
		} else {
			
			xCoordMin = this.noiseParams.getReceiverPointsGridMinX();
			xCoordMax = this.noiseParams.getReceiverPointsGridMaxX();
			yCoordMin = this.noiseParams.getReceiverPointsGridMinY();
			yCoordMax = this.noiseParams.getReceiverPointsGridMaxY();
			
			log.info("Creating receiver points for the area between the coordinates (" + xCoordMin + "/" + yCoordMin + ") and (" + xCoordMax + "/" + yCoordMax + ").");			
		}
		
		createReceiverPoints();		
	}
	
	private void createReceiverPoints() {

		Counter counter = new Counter("create receiver point #");
		
		for (double y = yCoordMax + 100. ; y > yCoordMin - 100. - noiseParams.getReceiverPointGap() ; y = y - noiseParams.getReceiverPointGap()) {
		
			for (double x = xCoordMin - 100. ; x < xCoordMax + 100. + noiseParams.getReceiverPointGap() ; x = x + noiseParams.getReceiverPointGap()) {
				
				Id<ReceiverPoint> id = Id.create(counter.getCounter(), ReceiverPoint.class);
				Coord coord = new Coord(x, y);
				ReceiverPoint rp = new ReceiverPoint(id, coord);
				receiverPoints.put(id, rp);
				counter.incCounter();
			}
		}
		counter.printCounter();
		log.info("Total number of receiver points: " + receiverPoints.size());
	}
	
	private void setActivityCoord2NearestReceiverPointId () {
		double gap = noiseParams.getReceiverPointGap();
		Counter counter = new Counter("fill quadtree #") ;
		QuadTree<ReceiverPoint> qTree = new QuadTree<>(xCoordMin - 4*gap, yCoordMin - 4* gap, xCoordMax + 4*gap, yCoordMax + 4*gap);
		for(ReceiverPoint p: receiverPoints.values()) {
			qTree.put(p.getCoord().getX(), p.getCoord().getY(), p);
			counter.incCounter();
		}
		counter.printCounter();
		
		counter = new Counter("compute nearest receiver-points #");
		for (Coord coord : consideredActivityCoordsForSpatialFunctionality) {
			
			// TODO maybe add a check here so we consider only the rp in the 9 surrounding cells?
			ReceiverPoint rp = qTree.getClosest(coord.getX(), coord.getY());
			if(rp != null) {
				if(activityCoord2receiverPointId.put(coord, rp.getId()) != null){
					log.warn("this must not happen");
				}
			}
			
			counter.incCounter();
		}
		counter.printCounter();
	}

	private Map<Id<ReceiverPoint>, Coord> readCSVFile(String file, String separator, int idColumn, int xCoordColumn, int yCoordColumn) throws IOException {
		
		Map<Id<ReceiverPoint>, Coord> id2Coord = new HashMap<>();
		
		BufferedReader br = IOUtils.getBufferedReader(file);
		String line = null;
		try {
			line = br.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String[] headers = line.split(separator);
		if (idColumn >= 0) log.info("id: " + headers[idColumn]);
		log.info("xCoord: " + headers[xCoordColumn]);
		log.info("yCoord: " + headers[yCoordColumn]);
		
		int lineCounter = 0;

		while( (line = br.readLine()) != null) {

			if (lineCounter % 1000000 == 0.) {
				log.info("# " + lineCounter);
			}

			String[] columns = line.split(separator);
			if (line.isEmpty() || line.equals("") || columns.length != headers.length) {
				log.warn("Skipping line " + lineCounter + ". Line is empty or the columns are inconsistent with the headers: [" + line.toString() + "]");
			
			} else {
				String id = null;
				double x = 0;
				double y = 0;

				for (int column = 0; column < columns.length; column++){					
					if (column == idColumn) {
						id = columns[column];
					} else if (column == xCoordColumn) {
						x = Double.valueOf(columns[column]);
					} else if (column == yCoordColumn) {
						y = Double.valueOf(columns[column]);
					}
				}
				if (idColumn >= 0) {
					id2Coord.put(Id.create(id, ReceiverPoint.class), new Coord(x,y));
				} else {
					id2Coord.put(Id.create(String.valueOf(lineCounter), ReceiverPoint.class), new Coord(x,y));
				}
				
				lineCounter++;
			}			
		}
		
		log.info("Done. Number of read lines: " + lineCounter);
		
		return id2Coord;
	}
	
	public Map<Id<Person>, List<Coord>> getPersonId2listOfConsideredActivityCoords() {
		return personId2consideredActivityCoords;
	}

	public Map<Coord, Id<ReceiverPoint>> getActivityCoord2receiverPointId() {
		return activityCoord2receiverPointId;
	}
	
	Map<Id<ReceiverPoint>, ReceiverPoint> getAndClearReceiverPoints() {
		Map<Id<ReceiverPoint>, ReceiverPoint> receiverPoints = this.receiverPoints;
		this.receiverPoints = null;
		return receiverPoints;
	}
	
	
	public NoiseConfigGroup getGridParams() {
		return noiseParams;
	}

}
