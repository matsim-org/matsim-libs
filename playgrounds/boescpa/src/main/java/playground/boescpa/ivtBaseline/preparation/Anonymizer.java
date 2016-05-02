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

package playground.boescpa.ivtBaseline.preparation;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.households.Household;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.households.Income;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.matsim.vehicles.VehicleWriterV1;

import java.io.File;

import static playground.boescpa.ivtBaseline.preparation.IVTConfigCreator.*;

/**
 * Provides the functionality to anonymize a scenario before publication.
 *
 * @author boescpa
 */
public class Anonymizer {
	private final static Logger log = Logger.getLogger(Anonymizer.class);

	private final Scenario scenario;

	private Anonymizer(Scenario scenario) {
		this.scenario = scenario;
	}

	public static void main (final String[] args) {
		final String pathToConfig = args[0];
		final String pathToOutputFolder = args[1];
		
		final Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(pathToConfig));

		Anonymizer anonymizer = new Anonymizer(scenario);
		// facilities
		anonymizer.removeDescFromFacilities();
		// households_attributes
		// 		don't make public
		// households
		anonymizer.simplifyIncomeCategories();
		// mmPackage (mmNetwork, mmSchedule, mmVehicles)
		//		ok to make public
		// population
		anonymizer.simplifySocioDemographics();
		anonymizer.unifyCoords();
		// population_attributes
		anonymizer.removeFreightTag();
		anonymizer.simplifySeasonTickets();
		anonymizer.removeMZId();
		// counts
		//		don't make public (they are proprietary!)
		
		anonymizer.writeScenario(pathToOutputFolder + File.separator);
	}

	private void writeScenario(String pathToOutputFolder) {
		new PopulationWriter(scenario.getPopulation()).write(pathToOutputFolder + POPULATION);
		new ObjectAttributesXmlWriter(scenario.getPopulation().getPersonAttributes())
				.writeFile(pathToOutputFolder + POPULATION_ATTRIBUTES);
		new HouseholdsWriterV10(scenario.getHouseholds()).writeFile(pathToOutputFolder + HOUSEHOLDS);
		// We don't publish the Household Attributes.
		scenario.getConfig().households().setInputHouseholdAttributesFile(null);
		//new ObjectAttributesXmlWriter(scenario.getHouseholds().getHouseholdAttributes())
		//		.writeFile(pathToOutputFolder + HOUSEHOLD_ATTRIBUTES);
		new FacilitiesWriter(scenario.getActivityFacilities()).write(pathToOutputFolder + FACILITIES);
		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(pathToOutputFolder + SCHEDULE);
		new VehicleWriterV1(scenario.getTransitVehicles()).writeFile(pathToOutputFolder + VEHICLES);
		new NetworkWriter(scenario.getNetwork()).write(pathToOutputFolder + NETWORK);
		new ConfigWriter(scenario.getConfig()).write(pathToOutputFolder + PreparationScript.CONFIG);
	}

	private void removeMZId() {
		log.info("Removing MZIds...");
		Counter counter = new Counter(" person ");
		for (Person person : scenario.getPopulation().getPersons().values()) {
			scenario.getPopulation().getPersonAttributes().removeAttribute(person.getId().toString(), "mz_id");
			counter.incCounter();
		}
		counter.printCounter();
		log.info("Removing MZIds... done.");
	}

	private void simplifySeasonTickets() {
		// only GA, HT or nothing.
		log.info("Simplifying Season Tickets...");
		Counter counter = new Counter(" person ");
		for (Person person : scenario.getPopulation().getPersons().values()) {
			String seasonTicket = (String) scenario.getPopulation().getPersonAttributes().getAttribute(person.getId().toString(), "season_ticket");
			if (seasonTicket != null) {
				if (seasonTicket.contains("Generalabo")) {
					scenario.getPopulation().getPersonAttributes().putAttribute(person.getId().toString(), "season_ticket", "Generalabo");
				} else if (seasonTicket.contains("Halbtaxabo")) {
					scenario.getPopulation().getPersonAttributes().putAttribute(person.getId().toString(), "season_ticket", "Halbtaxabo");
				} else {
					scenario.getPopulation().getPersonAttributes().removeAttribute(person.getId().toString(), "season_ticket");
				}
			}
			counter.incCounter();
		}
		counter.printCounter();
		log.info("Simplifying Season Tickets... done.");
	}

	private void removeFreightTag() {
		// remove tags that specify what type of freight it is...
		log.info("Removing freight tag...");
		Counter counter = new Counter(" person ");
		for (Person person : scenario.getPopulation().getPersons().values()) {
			scenario.getPopulation().getPersonAttributes().removeAttribute(person.getId().toString(), "freight_type");
			counter.incCounter();
		}
		counter.printCounter();
		log.info("Removing freight tag... done.");
	}

	private void unifyCoords() {
		log.info("Unifying coordinates...");
		Counter counter = new Counter(" person ");
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Coord facilityCoord = null;
			for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
				if (pe instanceof Activity) {
					Activity activity = (Activity) pe;
					if (activity.getFacilityId() != null) {
						facilityCoord = activity.getCoord();
						break;
					}
				}
			}
			if (facilityCoord != null) {
				for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
					if (pe instanceof Activity) {
						Activity activity = (Activity) pe;
						if (activity.getFacilityId() == null) {
							((ActivityImpl) activity).setCoord(facilityCoord);
						}
					}
				}
			}
			counter.incCounter();
		}
		counter.printCounter();
		log.info("Unifying coordinates... done.");
	}

	private void simplifySocioDemographics() {
		log.info("Simplify socio-demographics...");
		Counter counter = new Counter(" person ");
		for (Person person : scenario.getPopulation().getPersons().values()) {
			roundAgeToNextDecade(person);
			removeGender(person);
			counter.incCounter();
		}
		counter.printCounter();
		log.info("Simplify socio-demographics... done.");
	}

	private void removeGender(Person person) {
		if (person.getCustomAttributes().get("sex") != null) {
			person.getCustomAttributes().put("sex", null);
			//person.getCustomAttributes().put("sex", "x");
		}
	}

	private void roundAgeToNextDecade(Person person) {
		if (person.getCustomAttributes().get("age") != null) {
			int age = (int) person.getCustomAttributes().get("age");
			if (age >= 16) {
				age = (int) (10 * Math.round(age / 10.));
			} else {
				age = 10;
			}
			person.getCustomAttributes().put("age", age);
		}
	}

	private void simplifyIncomeCategories() {
		// e.g. simplify to low, middle, high
		log.info("Simplify income categories...");
		int numHigh = 0, numMiddle = 0, numLow = 0;

		Counter counter = new Counter(" household ");
		for (Household household : scenario.getHouseholds().getHouseholds().values()) {
			Income income = household.getIncome();
			if (income.getIncome() > 10000) {
				scenario.getHouseholds().getHouseholdAttributes().putAttribute(household.getId().toString(), "incomeClass", "high");
				numHigh++;
			} else if (income.getIncome() > 5000) {
				scenario.getHouseholds().getHouseholdAttributes().putAttribute(household.getId().toString(), "incomeClass", "middle");
				numMiddle++;
			} else {
				scenario.getHouseholds().getHouseholdAttributes().putAttribute(household.getId().toString(), "incomeClass", "low");
				numLow++;
			}
			household.setIncome(null);
			counter.incCounter();
		}
		counter.printCounter();
		log.info("Number of High Income Households: " + numHigh);
		log.info("Number of Middle Income Households: " + numMiddle);
		log.info("Number of Low Income Households: " + numLow);
		log.info("Simplify income categories... done.");
	}

	private void removeDescFromFacilities() {
		log.info("Removing descriptions...");
		Counter counter = new Counter(" facility ");
		for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {
			((ActivityFacilityImpl) facility).setDesc(null);
			counter.incCounter();
		}
		counter.printCounter();
		log.info("Removing descriptions... done.");
	}
}
