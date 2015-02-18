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

package playground.boescpa.netcap.preparation;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.*;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;

/**
 * Resets a population so that it can be used with a new scenario...
 *
 * @author boescpa
 */
public class NCPopulation {
	private static PopulationFactory popFactory;

	public static void main(String[] args) {
		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.loadConfig(args[0]));
		new MatsimPopulationReader(scenario).readFile(scenario.getConfig().plans().getInputFile());
		final Population population = scenario.getPopulation();
		final Population newPopulation = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		popFactory = newPopulation.getFactory();

		for (Person person : population.getPersons().values()) {
			Person newPerson = resetPerson(person);
			newPopulation.addPerson(newPerson);
		}

		new PopulationWriter(newPopulation).write(args[1]);
	}

	private static Person resetPerson(final Person oldPerson) {
		final PersonImpl oldPersonImpl = (PersonImpl) oldPerson;
		final Person person = popFactory.createPerson(Id.create(oldPerson.getId().toString(), Person.class));
		final PersonImpl personImpl = (PersonImpl) person;
		personImpl.setSex(oldPersonImpl.getSex());
		personImpl.setAge(oldPersonImpl.getAge());
		personImpl.setLicence(oldPersonImpl.getLicense());
		personImpl.setEmployed(oldPersonImpl.isEmployed());
		final Plan plan = popFactory.createPlan();
		person.addPlan(plan);
		boolean lastWasLeg = false;
		for (PlanElement planElement : oldPerson.getSelectedPlan().getPlanElements()) {
			if (planElement instanceof Activity) {
				final Activity oldActivity = (Activity) planElement;
				if (oldActivity.getType().equals("pt interaction")) {
					continue;
				}
				final Coord actCoord = new CoordImpl(oldActivity.getCoord().getX(), oldActivity.getCoord().getY());
				final Activity activity = popFactory.createActivityFromCoord(oldActivity.getType(), actCoord);
				activity.setEndTime(oldActivity.getEndTime());
				activity.setMaximumDuration(oldActivity.getMaximumDuration());
				activity.setStartTime(oldActivity.getStartTime());
				if (oldActivity.getFacilityId() != null) {
					final ActivityImpl activityImpl = (ActivityImpl) activity;
					activityImpl.setFacilityId(Id.create(oldActivity.getFacilityId().toString(), ActivityFacility.class));
				}
				plan.addActivity(activity);
				lastWasLeg = false;
			} else if (planElement instanceof Leg && !lastWasLeg) {
				final Leg oldLeg = (Leg) planElement;
				if (oldLeg.getMode().equals("transit_walk")) {
					continue;
				}
				plan.addLeg(popFactory.createLeg(oldLeg.getMode()));
				lastWasLeg = true;
			}
		}
		return person;
	}
}
