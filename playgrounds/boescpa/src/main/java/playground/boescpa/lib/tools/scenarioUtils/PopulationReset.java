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

package playground.boescpa.lib.tools.scenarioUtils;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.*;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesUtils;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import java.util.Collection;

/**
 * Resets a population (and its attributes) so that it can be used with a new scenario...
 *
 * @author boescpa
 */
public class PopulationReset {
	private static PopulationFactory popFactory;

	public static void main(String[] args) {
		if (args.length < 2 || args.length > 3) {
			System.out.println("Wrong number of arguments. Will abort.");
			return;
		}

		// Load scenario:
		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.loadConfig(args[0]));

		// Reset population:
		new MatsimPopulationReader(scenario).readFile(scenario.getConfig().plans().getInputFile());
		final Population population = scenario.getPopulation();
		final Population newPopulation = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		popFactory = newPopulation.getFactory();
		for (Person person : population.getPersons().values()) {
			Person newPerson = resetPerson(person);
			newPopulation.addPerson(newPerson);
		}
		new PopulationWriter(newPopulation).write(args[1]);

		// Filter person attributes
		if (scenario.getConfig().plans().getInputPersonAttributeFile() != null && args.length > 2) {
			final ObjectAttributes personAttributes = scenario.getPopulation().getPersonAttributes();
			new ObjectAttributesXmlReader(personAttributes).parse(scenario.getConfig().plans().getInputPersonAttributeFile());
			final ObjectAttributes filteredPersonAttributes = filterPersonAttributes(personAttributes, newPopulation);
			new ObjectAttributesXmlWriter(filteredPersonAttributes).writeFile(args[2]);
		} else {
			System.out.println("Person attributes not handled (either no file specified in config or no output path given in arguments).");
		}
	}

	private static ObjectAttributes filterPersonAttributes(final ObjectAttributes personAttributes, final Population newPopulation) {
		// Which persons in new population (and therefore attributes to keep)?
		final ObjectAttributes filteredObjectAttributes = new ObjectAttributes();
		for (Person person : newPopulation.getPersons().values()) {
			Collection<String> attributeNames = ObjectAttributesUtils.getAllAttributeNames(personAttributes, person.getId().toString());
			for (String attributeName : attributeNames) {
				filteredObjectAttributes.putAttribute(
						person.getId().toString(),
						attributeName,
						personAttributes.getAttribute(person.getId().toString(), attributeName));
			}
		}
		return filteredObjectAttributes;
	}

	private static Person resetPerson(final Person oldPerson) {
		final PersonImpl oldPersonImpl = (PersonImpl) oldPerson;
		final Person person = popFactory.createPerson(Id.create(oldPerson.getId().toString(), Person.class));
		final PersonImpl personImpl = (PersonImpl) person;

		PersonUtils.setSex(personImpl, PersonUtils.getSex(oldPersonImpl));
		PersonUtils.setAge(personImpl, PersonUtils.getAge(oldPersonImpl));
		PersonUtils.setLicence(personImpl, PersonUtils.getLicense(oldPersonImpl));
		PersonUtils.setEmployed(personImpl, PersonUtils.isEmployed(oldPersonImpl));
		final Plan plan = popFactory.createPlan();
		person.addPlan(plan);
		boolean lastWasLeg = false;
		for (PlanElement planElement : oldPerson.getSelectedPlan().getPlanElements()) {
			if (planElement instanceof Activity) {
				final Activity oldActivity = (Activity) planElement;
				if (oldActivity.getType().equals("pt interaction")) {
					//continue;
				}
				final Coord actCoord = new Coord(oldActivity.getCoord().getX(), oldActivity.getCoord().getY());
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
					//continue;
				}
				plan.addLeg(popFactory.createLeg(oldLeg.getMode()));
				lastWasLeg = true;
			}
		}
		return person;
	}
}
