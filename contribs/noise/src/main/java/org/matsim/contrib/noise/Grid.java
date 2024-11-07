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
package org.matsim.contrib.noise;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.StageActivityHandling;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;



/**
 * Computes a grid of receiver points and provides some basic spatial functionality,
 * e.g. the nearest receiver point for the coordinates of each 'considered' activity type.
 *
 * @author lkroeger, ikaddoura
 *
 */
final class Grid {

	private static final Logger log = LogManager.getLogger(Grid.class);

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
	private Map<Id<ReceiverPoint>, NoiseReceiverPoint> receiverPoints;

	public Grid(Scenario scenario) {
		this.scenario = scenario;

		if (this.scenario.getConfig().getModule("noise") == null) {
			throw new RuntimeException("Could not find a noise config group. "
					+ "Check if the custom module is loaded, e.g. 'ConfigUtils.loadConfig(configFile, new NoiseConfigGroup())'"
					+ " Aborting...");
		}

		this.noiseParams = (NoiseConfigGroup) this.scenario.getConfig().getModule("noise");

		this.receiverPoints = new HashMap<>();

		String[] consideredActTypesForDamagesArray = noiseParams.getConsideredActivitiesForDamageCalculationArray();
		Collections.addAll(this.consideredActivitiesForSpatialFunctionality, consideredActTypesForDamagesArray);

		String[] consideredActTypesForReceiverPointGridArray = noiseParams.getConsideredActivitiesForReceiverPointGridArray();
		Collections.addAll(this.consideredActivitiesForReceiverPointGrid, consideredActTypesForReceiverPointGridArray);

//		this.noiseParams.checkGridParametersForConsistency();
		initialize();
	}

	private void initialize() {
		setActivityCoords();

		if(scenario.getScenarioElement(NoiseReceiverPoints.NOISE_RECEIVER_POINTS) != null) {
			log.info("Loading receiver points based on provided coordinates.");
			loadGridFromScenario();
		} else if (this.noiseParams.getReceiverPointsCSVFile() == null) {
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
		for (Person person: scenario.getPopulation().getPersons().values()) {

			for(Activity activity: TripStructureUtils.getActivities(person.getSelectedPlan(), StageActivityHandling.ExcludeStageActivities)){
				if (this.consideredActivitiesForSpatialFunctionality.contains(activity.getType()) || consideredActivityPrefix(activity.getType(), this.consideredActivitiesForSpatialFunctionality)) {
					List<Coord> activityCoordinates = personId2consideredActivityCoords.computeIfAbsent(person.getId(), value -> new ArrayList<>());

					activityCoordinates.add(activity.getCoord());

					//activity.getCoord() might be null, so we need to handle that (by outsourcing that to PopulationUtils)
					consideredActivityCoordsForSpatialFunctionality.add(PopulationUtils.decideOnCoordForActivity(activity, scenario));
				}

				if (this.consideredActivitiesForReceiverPointGrid.contains(activity.getType()) || consideredActivityPrefix(activity.getType(), consideredActivitiesForReceiverPointGrid)) {
					//activity.getCoord() might be null, so we need to handle that (by outsourcing that to PopulationUtils)
					consideredActivityCoordsForReceiverPointGrid.add(PopulationUtils.decideOnCoordForActivity(activity, scenario));
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

		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(this.noiseParams.getReceiverPointsCSVFileCoordinateSystem(), this.scenario.getConfig().global().getCoordinateSystem());
		try {
			readReceiverPoints(gridCSVFile, ct);
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (NoiseReceiverPoint nrp : receiverPoints.values()) {
			final Coord coord = nrp.getCoord();
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

		log.info("Total number of receiver points: " + receiverPoints.size());
	}

	private void loadGridFromScenario() {
		this.receiverPoints = (NoiseReceiverPoints) scenario.getScenarioElement(NoiseReceiverPoints.NOISE_RECEIVER_POINTS);
		log.info("Total number of receiver points: " + receiverPoints.size());

		for (NoiseReceiverPoint nrp : receiverPoints.values()) {
			final Coord coord = nrp.getCoord();
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
				NoiseReceiverPoint rp = new NoiseReceiverPoint(id, coord);
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
		QuadTree<ReceiverPoint> qTree = new QuadTree<>(xCoordMin - 15*gap, yCoordMin - 15* gap, xCoordMax + 15*gap, yCoordMax + 15*gap);
		for(ReceiverPoint p: receiverPoints.values()) {
			qTree.put(p.getCoord().getX(), p.getCoord().getY(), p);
			counter.incCounter();
		}
		counter.printCounter();

		counter = new Counter("compute nearest receiver-points #");
		Counter otherCounter = new Counter("activities outside grid #");
		for (Coord coord : consideredActivityCoordsForSpatialFunctionality) {

			// TODO maybe add a check here so we consider only the rp in the 9 surrounding cells?
			// ts, nov' 24:  ---> might be done by the following filtering (by grid) ??

			// Filter activity coords that are within the quadTree.
			// I do not know, why whe put a buffer around the grid when instantiating the QuadTree, above, but I'll keep it for now
			// tschlenther, nov '24
			if (coord.getX() >= xCoordMin && coord.getX() <= xCoordMax &&
					coord.getY() >= yCoordMin && coord.getY() <= yCoordMax){

				ReceiverPoint rp = qTree.getClosest(coord.getX(), coord.getY());
				if(rp != null) {
					if(activityCoord2receiverPointId.put(coord, rp.getId()) != null){
						log.warn("this must not happen");
					}
				}

				counter.incCounter();
			} else {
				otherCounter.incCounter();
			}
		}
		counter.printCounter();
		otherCounter.printCounter();
	}

	private void readReceiverPoints(String file, CoordinateTransformation ct) throws IOException {

		Map<Id<ReceiverPoint>, Coord> id2Coord = new HashMap<>();

		BufferedReader br = IOUtils.getBufferedReader(file);
		String line = null;
		try {
			line = br.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}

		String[] headers = line.split(",");
		log.info("id: " + headers[0]);
		log.info("xCoord: " + headers[1]);
		log.info("yCoord: " + headers[2]);

		int lineCounter = 0;

		while( (line = br.readLine()) != null) {

			if (lineCounter % 1000000 == 0.) {
				log.info("# " + lineCounter);
			}

			String[] columns = line.split(",");
			if (line.isEmpty() || line.equals("") || columns.length != headers.length) {
				log.warn("Skipping line " + lineCounter + ". Line is empty or the columns are inconsistent with the headers: [" + line.toString() + "]");

			} else {
				String id = null;
				double x = 0;
				double y = 0;

				for (int column = 0; column < columns.length; column++){
					if (column == 0) {
						id = columns[column];
					} else if (column == 1) {
						x = Double.valueOf(columns[column]);
					} else if (column == 2) {
						y = Double.valueOf(columns[column]);
					}
				}

                Coord coord = new Coord(x,y);
                Coord transformedCoord = ct.transform(coord);
                NoiseReceiverPoint rp = new NoiseReceiverPoint(Id.create(id, ReceiverPoint.class), transformedCoord);
                receiverPoints.put(rp.getId(), rp);
				lineCounter++;
			}
		}
		log.info("Done. Number of read lines: " + lineCounter);
	}

	public Map<Id<Person>, List<Coord>> getPersonId2listOfConsideredActivityCoords() {
		return personId2consideredActivityCoords;
	}

	public Map<Coord, Id<ReceiverPoint>> getActivityCoord2receiverPointId() {
		return activityCoord2receiverPointId;
	}

	public Map<Id<ReceiverPoint>, NoiseReceiverPoint> getReceiverPoints() {
		return receiverPoints;
	}

	public NoiseConfigGroup getGridParams() {
		return noiseParams;
	}

}
