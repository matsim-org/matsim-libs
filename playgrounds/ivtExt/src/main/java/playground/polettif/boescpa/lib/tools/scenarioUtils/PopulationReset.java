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

package playground.polettif.boescpa.lib.tools.scenarioUtils;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.*;
import org.matsim.core.population.PopulationWriter;
import org.matsim.facilities.ActivityFacility;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesUtils;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import java.util.Collection;

/**
 * Resets a population (and its attributes) so that it can be used with a new scenario...
 * If the switch FULL_RESET is turned to true the plans are completely cleared to be used with a new scenario.
 * If the switch FULL_RESET is turned to false the plans are just reduced to the selected plan.
 *
 * @author boescpa
 */
public class PopulationReset {
	private static final boolean FULL_RESET = false;
	private static PopulationFactory popFactory;

	public static void main(String[] args) {
		final String pathToInputPopulation = args[0];
		final String pathToOutputPopulation = args[1];
		String pathToInputPopulationAttributes = null;
		String pathToOutputPopulationAttributes = null;
		if (args.length > 2) {
			pathToInputPopulationAttributes = args[2];
			pathToOutputPopulationAttributes = args[3];
		}

		// Reset population:
		final Population population = playground.polettif.boescpa.lib.tools.PopulationUtils.readPopulation(pathToInputPopulation);
		final Population newPopulation = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		popFactory = newPopulation.getFactory();
		ResetPerson resetPersonInstance;
		if (FULL_RESET) {
			resetPersonInstance = new FullyResetPerson();
		} else {
			resetPersonInstance = new FilterPlans();
		}
		for (Person person : population.getPersons().values()) {
			Person newPerson = resetPersonInstance.resetPerson(person);
			newPopulation.addPerson(newPerson);
		}
		new PopulationWriter(newPopulation).write(pathToOutputPopulation);

		// Filter person attributes
		if (pathToInputPopulationAttributes != null) {
			final ObjectAttributes personAttributes = new ObjectAttributes();
			new ObjectAttributesXmlReader(personAttributes).parse(pathToInputPopulationAttributes);
			final ObjectAttributes filteredPersonAttributes = filterPersonAttributes(personAttributes, newPopulation);
			new ObjectAttributesXmlWriter(filteredPersonAttributes).writeFile(pathToOutputPopulationAttributes);
		} else {
			System.out.println("Person attributes not handled (either no files or no output path given in arguments).");
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

	private static abstract class ResetPerson {
		abstract Person resetPerson(final Person oldPerson);

		Person copyPerson(Person oldPerson) {
			final PersonImpl oldPersonImpl = (PersonImpl) oldPerson;
			final Person person = popFactory.createPerson(Id.create(oldPerson.getId().toString(), Person.class));
			final PersonImpl personImpl = (PersonImpl) person;

			PersonUtils.setSex(personImpl, PersonUtils.getSex(oldPersonImpl));
			PersonUtils.setAge(personImpl, PersonUtils.getAge(oldPersonImpl));
			PersonUtils.setLicence(personImpl, PersonUtils.getLicense(oldPersonImpl));
			PersonUtils.setEmployed(personImpl, PersonUtils.isEmployed(oldPersonImpl));
			PersonUtils.setCarAvail(personImpl, PersonUtils.getCarAvail(oldPersonImpl));
			return person;
		}
	}

	private static class FilterPlans extends ResetPerson {
		@Override
		Person resetPerson(Person oldPerson) {
			final Person person = copyPerson(oldPerson);
			person.addPlan(oldPerson.getSelectedPlan());
			return person;
		}
	}

	private static class FullyResetPerson extends ResetPerson{
		@Override
		Person resetPerson(final Person oldPerson) {
			final Person person = copyPerson(oldPerson);
			final Plan plan = popFactory.createPlan();
			person.addPlan(plan);
			boolean lastWasLeg = false;
			for (PlanElement planElement : oldPerson.getSelectedPlan().getPlanElements()) {
				if (planElement instanceof Activity) {
					final Activity oldActivity = (Activity) planElement;
					if (oldActivity.getType().equals("pt interaction")) {
						continue;
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
						continue;
					}
					plan.addLeg(popFactory.createLeg(oldLeg.getMode()));
					lastWasLeg = true;
				}
			}
			return person;
		}
	}
}
