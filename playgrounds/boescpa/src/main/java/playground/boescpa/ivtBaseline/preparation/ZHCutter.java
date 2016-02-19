/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.boescpa.ivtBaseline.preparation;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.*;
import org.matsim.households.*;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Cuts an IVT baseline scenario to a predefined area.
 *
 * @author boescpa
 */
public class ZHCutter {

	private Map<Coord, Boolean> coordCache = new HashMap<>();
	private Map<Id<Person>, Person> filteredAgents = new HashMap<>();
	private Coord center;
	private int radius;

	protected static final String FACILITIES = File.separator + IVTConfigCreator.FACILITIES;
	protected static final String HOUSEHOLD_ATTRIBUTES = File.separator + IVTConfigCreator.HOUSEHOLD_ATTRIBUTES;
	protected static final String HOUSEHOLDS = File.separator + IVTConfigCreator.HOUSEHOLDS;
	protected static final String POPULATION = File.separator + IVTConfigCreator.POPULATION;
	protected static final String POPULATION_ATTRIBUTES = File.separator + IVTConfigCreator.POPULATION_ATTRIBUTES;

	public ZHCutter() {
		this.reset();
	}

	public static void main(final String[] args) {
		final String pathToFolder = args[0];
		final String pathToTargetFolder = args[1];
		final double xCoordCenter = Double.parseDouble(args[2]);
		final double yCoordCenter = Double.parseDouble(args[3]);
		final int radius = Integer.parseInt(args[4]);
		// For 30km around Zurich Center (Bellevue): X - 2683518.0, Y - 1246836.0, radius - 30000

		// load files
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReaderMatsimV5(scenario).readFile(pathToFolder + POPULATION);
		ObjectAttributes personAttributes = new ObjectAttributes();
		new ObjectAttributesXmlReader(personAttributes).parse(pathToFolder + POPULATION_ATTRIBUTES);
		Households origHouseholds = new HouseholdsImpl();
		new HouseholdsReaderV10(origHouseholds).readFile(pathToFolder + HOUSEHOLDS);
		ObjectAttributes householdAttributes = new ObjectAttributes();
		new ObjectAttributesXmlReader(householdAttributes).parse(pathToFolder + HOUSEHOLD_ATTRIBUTES);
		new FacilitiesReaderMatsimV1(scenario).readFile(pathToFolder + FACILITIES);
		// cut to area
		ZHCutter cutter = new ZHCutter();
		cutter.setArea(new Coord(xCoordCenter, yCoordCenter), radius);
		Population filteredPopulation = cutter.geographicallyFilterPopulation(scenario.getPopulation(), personAttributes);
		Households filteredHouseholds = cutter.filterHouseholdsWithPopulation(origHouseholds, householdAttributes);
		ActivityFacilities filteredFacilities = cutter.filterFacilitiesWithPopulation(scenario.getActivityFacilities());
		// write new files
		new PopulationWriter(filteredPopulation).write(pathToTargetFolder + POPULATION);
		new ObjectAttributesXmlWriter(personAttributes).writeFile(pathToTargetFolder + POPULATION_ATTRIBUTES);
		new HouseholdsWriterV10(filteredHouseholds).writeFile(pathToTargetFolder + HOUSEHOLDS);
		new ObjectAttributesXmlWriter(householdAttributes).writeFile(pathToTargetFolder + HOUSEHOLD_ATTRIBUTES);
		new FacilitiesWriter(filteredFacilities).writeV1(pathToTargetFolder + FACILITIES);
	}

	public void reset() {
		coordCache.clear();
		filteredAgents.clear();
	}

	public void setArea(Coord center, int radius) {
		this.center = center;
		this.radius = radius;
	}

	public Population geographicallyFilterPopulation(final Population origPopulation, final ObjectAttributes personAttributes) {
		Population filteredPopulation = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		Counter counter = new Counter(" person # ");
		boolean actInArea;
		for (Person p : origPopulation.getPersons().values()) {
			counter.incCounter();
			if (p.getSelectedPlan() != null) {
				actInArea = false;
				for (PlanElement pe : p.getSelectedPlan().getPlanElements()) {
					if (!actInArea && pe instanceof ActivityImpl) {
						ActivityImpl act = (ActivityImpl) pe;
						if (inArea(act.getCoord())) {
							actInArea = true;
						}
					}
				}
				if (actInArea) {
					filteredPopulation.addPerson(p);
					filteredAgents.put(p.getId(), p);
				} else {
					personAttributes.removeAllAttributes(p.toString());
				}
			}
		}
		return filteredPopulation;
	}

	public Households filterHouseholdsWithPopulation(final Households households, final ObjectAttributes householdAttributes) {
		Households filteredHouseholds = new HouseholdsImpl();

		for (Household household : households.getHouseholds().values()) {
			Set<Id<Person>> personIdsToRemove = new HashSet<>();
			for (Id<Person> personId : household.getMemberIds()) {
				if (!filteredAgents.keySet().contains(personId)) {
					personIdsToRemove.add(personId);
				}
			}
			for (Id<Person> personId : personIdsToRemove) {
				household.getMemberIds().remove(personId);
			}
			if (!household.getMemberIds().isEmpty()) {
				filteredHouseholds.getHouseholds().put(household.getId(), household);
			} else {
				householdAttributes.removeAllAttributes(household.getId().toString());
			}
		}

		return filteredHouseholds;
	}

	public ActivityFacilities filterFacilitiesWithPopulation(final ActivityFacilities origFacilities) {
		ActivityFacilities filteredFacilities = FacilitiesUtils.createActivityFacilities();

		for (Person person : filteredAgents.values()) {
			if (person.getSelectedPlan() != null) {
				for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
					if (pe instanceof ActivityImpl) {
						ActivityImpl act = (ActivityImpl) pe;
						if (act.getFacilityId() != null && !filteredFacilities.getFacilities().containsKey(act.getFacilityId())) {
							filteredFacilities.addActivityFacility(origFacilities.getFacilities().get(act.getFacilityId()));
						}
					}
				}
			}
		}

		return filteredFacilities;
	}

	private boolean inArea(Coord coord) {
		if (coordCache.containsKey(coord)) {
			return coordCache.get(coord);
		} else {
			boolean coordIsInArea = CoordUtils.calcEuclideanDistance(center, coord) <= radius;
			coordCache.put(coord, coordIsInArea);
			return coordIsInArea;
		}
	}
}
