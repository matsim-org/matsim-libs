/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.boescpa.ivtBaseline.preparation.freightCreation;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.CH1903LV03toCH1903LV03Plus;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.*;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import playground.boescpa.ivtBaseline.preparation.IVTConfigCreator;
import playground.boescpa.lib.tools.FacilityUtils;
import playground.boescpa.lib.tools.PopulationUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

/**
 * Creates single-trip populations from OD-matrices.
 * Based on ivtExt.herbie.creation.freight.CreateFreightTraffic
 *
 * @author boescpa
 */
public class CreateFreightTraffic {
	private final static Logger log = Logger.getLogger(CreateFreightTraffic.class);
	private final static CoordinateTransformation transformation = new CH1903LV03toCH1903LV03Plus();

	private final static String DELIMITER = ";";
	public final static String FREIGHT_TAG = "freight";
	private final static int VICINITY_RADIUS = 10000; // radius [m] around zone centroid which is considered vicinity

	private final Random random;
	private final double percentage;
	private final double upScaling;
	private int totalPopulationSize = 0;
	private int roundDowns = 0;
	private int roundUps = 0;
	private final double[] cummulativeDepartureProbability = new double[24];
	private final Map<Integer, List<ActivityFacility>> zones = new HashMap<>();
	private final Population freightPopulation;
	private final ActivityFacilities freightFacilities;
	private int personIndex = 0;

	private CreateFreightTraffic(String coordFile, String facilitiesFile, double scalingFactor, int randomSeed) {
		readZones(coordFile, facilitiesFile);
		if (scalingFactor <= 1.0) {
			this.percentage = scalingFactor;
			this.upScaling = 1.0;
		} else {
			this.percentage = 1.0;
			this.upScaling = scalingFactor;
		}
		this.random = new Random(randomSeed);
		this.freightPopulation = PopulationUtils.getEmptyPopulation();
		this.freightFacilities = FacilitiesUtils.createActivityFacilities();
	}

	public static void main(final String[] args) {
		final String coordFile = args[0];
		final String facilitiesFile = args[1]; // all scenario facilities incl secondary facilities and bc facilities.
		final String utilityVehiclesFile = args[2];
		final String trucksFile = args[3];
		final String heavyDutyVehiclesFile = args[4];
		final String cumulativeProbabilityFreightDeparturesFile = args[5];
		final double scalingFactor = Double.parseDouble(args[6]); // for example for a 1% population enter "0.01"
		final int randomSeed = Integer.parseInt(args[7]);
		final String outputFacilities = args[8];
		final String outputPopulation = args[9];

		log.info("Freight creation...");
		CreateFreightTraffic creator = new CreateFreightTraffic(coordFile, facilitiesFile, scalingFactor, randomSeed);
		creator.readDepartures(cumulativeProbabilityFreightDeparturesFile);

		creator.createFreightTraffic("UtilityVehicle", utilityVehiclesFile);
		creator.createFreightTraffic("Truck", trucksFile);
		creator.createFreightTraffic("HeavyDutyVehicle", heavyDutyVehiclesFile);

		creator.writeFreightFacilities(outputFacilities);
		creator.writeFreightPopulation(outputPopulation);
		log.info("Freight creation... done.");
		creator.printStats();
	}

	private void readDepartures(String cumulativeProbabilityFreightDeparturesFile) {
		BufferedReader reader = IOUtils.getBufferedReader(cumulativeProbabilityFreightDeparturesFile);
		try {
			for (int i = 0; i < 24; i++) {
				String[] line = reader.readLine().split(DELIMITER);
				cummulativeDepartureProbability[i] = Double.parseDouble(line[1]);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void printStats() {
		log.info("Found " + totalPopulationSize + " freight agents");
		log.info("Created " + freightPopulation.getPersons().size() + " freight agents");
		log.info("Created " + freightFacilities.getFacilities().size() + " freight facilities");
		log.info("Round ups: " + roundUps);
		log.info("Round downs: " + roundDowns);
	}

	private void createFreightTraffic(String type, String vehiclesFile) {
		Counter counter = new Counter(" OD-relationship # ");
		BufferedReader reader = IOUtils.getBufferedReader(vehiclesFile);
		try {
			String nextLine = reader.readLine();
			while (nextLine != null) {
				String[] line = nextLine.split(DELIMITER);
				int numberOfTrips = getNumberOfTrips(line[4]);
				for (int i = 0; i < numberOfTrips; i++) {
					totalPopulationSize++;
					if (random.nextDouble() <= percentage) {
						ActivityFacility startFacility = getFacility(Integer.parseInt(line[0]));
						ActivityFacility endFacility = getFacility(Integer.parseInt(line[2]));
						createSingleTripAgent(type, ++personIndex, startFacility, endFacility);
					}
				}
				nextLine = reader.readLine();
				counter.incCounter();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		counter.printCounter();
	}

	private void createSingleTripAgent(String type, int index, ActivityFacility startFacility, ActivityFacility endFacility) {
		// create and add new agent
		Person p = org.matsim.core.population.PopulationUtils.createPerson(Id.create(FREIGHT_TAG + "_" + index, Person.class));
		freightPopulation.addPerson(p);
		freightPopulation.getPersonAttributes().putAttribute(p.getId().toString(), "subpopulation", FREIGHT_TAG);
		freightPopulation.getPersonAttributes().putAttribute(p.getId().toString(), "freight_type", type);
		// create and add new plan
		p.addPlan(createSingleTripPlan(startFacility, endFacility));
	}

	private Plan createSingleTripPlan(ActivityFacility startFacility, ActivityFacility endFacility) {
		Plan plan = new PlanImpl();
		int departureTime = getDepartureTime();

		ActivityImpl actStart = new ActivityImpl(FREIGHT_TAG, startFacility.getCoord(), startFacility.getLinkId());
		actStart.setFacilityId(startFacility.getId());
		actStart.setStartTime(0.0);
		actStart.setMaximumDuration(departureTime);
		actStart.setEndTime(departureTime);
		plan.addActivity(actStart);

		plan.addLeg(new LegImpl("car"));

		ActivityImpl actEnd = new ActivityImpl(FREIGHT_TAG, endFacility.getCoord(), endFacility.getLinkId());
		actEnd.setFacilityId(endFacility.getId());
		actEnd.setStartTime(departureTime);
		//actEnd.setMaximumDuration(24.0 * 3600.0 - departureTime);
		plan.addActivity(actEnd);
		return plan;
	}

	private int getDepartureTime() {
		double randDep = random.nextDouble();
		// identify selected hour of day
		int hour = 0;
		while (hour < 23 && cummulativeDepartureProbability[hour + 1] < randDep) {
			hour++;
		}
		int time = hour*60*60;
		// random assignment within that hour of the day
		time += random.nextInt(3600);
		return time;
	}

	private int getNumberOfTrips(String floatingNumberOfTripsForThisODRelationship) {
		// first scale number of trips
		double floatingNumberOfTrips = this.upScaling * Double.parseDouble(floatingNumberOfTripsForThisODRelationship);
		// first all full trips for this OD-relationship
		int numberOfTrips = (int)Math.floor(floatingNumberOfTrips);
		// then - if chance allows - another trip
		double residualTrips = floatingNumberOfTrips - numberOfTrips;
		if (random.nextDouble() <= residualTrips) {
			numberOfTrips++;
			roundUps++;
		} else {
			roundDowns++;
		}
		// return the total number of trips
		return numberOfTrips;
	}

	private ActivityFacility getFacility(int zoneId) {
		List<ActivityFacility> facilityList = zones.get(zoneId);
		ActivityFacility origFacility = facilityList.get(random.nextInt(facilityList.size()));
		Id<ActivityFacility> facilityId = Id.create(FREIGHT_TAG + "_" + (int)origFacility.getCoord().getX() + "_" + (int)origFacility.getCoord().getY(), ActivityFacility.class);
		ActivityFacility freightFacility;
		if (freightFacilities.getFacilities().containsKey(facilityId)) {
			freightFacility = freightFacilities.getFacilities().get(facilityId);
		} else {
			// the facility is used for the first time for freight and needs to be created
			freightFacility = freightFacilities.getFactory().createActivityFacility(facilityId, origFacility.getCoord(), origFacility.getLinkId());
			freightFacilities.addActivityFacility(freightFacility);
			addFreightActivity2Facility(freightFacility);
		}
		return freightFacility;
	}

	private void addFreightActivity2Facility(ActivityFacility facility) {
		((ActivityFacilityImpl)facility).createAndAddActivityOption(FREIGHT_TAG);
		OpeningTime ot = new OpeningTimeImpl(0.0 * 3600.0, 24.0 * 3600.0);
		facility.getActivityOptions().get(FREIGHT_TAG).addOpeningTime(ot);
	}

	private void readZones(String coordFile, String facilitiesFile) {
		ActivityFacilities origFacilities = FacilityUtils.readFacilities(facilitiesFile);
		// read zone centroids and assign all facilities close to centroid
		Counter counter = new Counter(" zone # ");
		BufferedReader reader = IOUtils.getBufferedReader(coordFile);
		try {
			String nextLine = reader.readLine();
			while (nextLine != null) {
				String[] line = nextLine.split(DELIMITER);
				int zoneId = Integer.parseInt(line[0]);
				if (zoneId < 2000000) { // these are the Swiss zones
					double xCoord = Double.parseDouble(line[2]);
					double yCoord = Double.parseDouble(line[3]);
					Coord coord = transformation.transform(new Coord(xCoord, yCoord));
					List<ActivityFacility> facilitiesInVicinity = getFacilities(origFacilities, coord);
					zones.put(zoneId, facilitiesInVicinity);
				} else { // these are the international zones without centroids;
					// here - instead of the coords of the centroid - the id of the appropriate border facility is specified.
					List<ActivityFacility> borderFacility = new ArrayList<>();
					borderFacility.add(origFacilities.getFacilities().get(Id.create(line[2], ActivityFacility.class)));
					zones.put(zoneId, borderFacility);
				}
				nextLine = reader.readLine();
				counter.incCounter();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		counter.printCounter();
	}

	private List<ActivityFacility> getFacilities(ActivityFacilities origFacilities, Coord zoneCentroidCoord) {
		List<ActivityFacility> facilityList = new ArrayList<>();
		for (ActivityFacility facility : origFacilities.getFacilities().values()) {
			if (facility.getActivityOptions().keySet().contains(IVTConfigCreator.WORK)
					&& CoordUtils.calcEuclideanDistance(facility.getCoord(), zoneCentroidCoord) <= VICINITY_RADIUS) {
				facilityList.add(facility);
			}
		}
		if (facilityList.isEmpty()) {
			Id<ActivityFacility> facilityId = Id.create("temp_" + zoneCentroidCoord.toString(), ActivityFacility.class);
			ActivityFacility newFacility = origFacilities.getFactory().createActivityFacility(facilityId, zoneCentroidCoord);
			facilityList.add(newFacility);
		}
		return facilityList;
	}

	private void writeFreightPopulation(String outputPopulation) {
		PopulationWriter writer = new PopulationWriter(freightPopulation);
		writer.write(outputPopulation);
		ObjectAttributesXmlWriter attributesWriter = new ObjectAttributesXmlWriter(freightPopulation.getPersonAttributes());
		attributesWriter.writeFile(outputPopulation.substring(0, outputPopulation.indexOf(".xml")) + "_Attributes.xml.gz");
	}

	private void writeFreightFacilities(String outputFacilities) {
		FacilitiesWriter writer = new FacilitiesWriter(freightFacilities);
		writer.write(outputFacilities);
	}
}
