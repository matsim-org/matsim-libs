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

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationWriter;
import playground.boescpa.lib.tools.PopulationUtils;

import java.util.List;

/**
 * Transfers location choices from one population to another.
 *
 * @author boescpa
 */
public class TransferLocationChoices {

	public static void main(final String[] args) {
		final String pathToPopWithNewLocations = args[0];
		final String pathToPopWhichWillBeUpdated = args[1];
		final String pathToNewPop = args[2];

		Population popWithNewLocations = PopulationUtils.readPopulation(pathToPopWithNewLocations);
		Population popWhichWillBeUpdated = PopulationUtils.readPopulation(pathToPopWhichWillBeUpdated);

		transferLocations(popWithNewLocations, popWhichWillBeUpdated);

		new PopulationWriter(popWhichWillBeUpdated).write(pathToNewPop);
	}

	public static void transferLocations(Population popWithNewLocations, Population popWhichWillBeUpdated) {
		for (Person personOld : popWithNewLocations.getPersons().values()) {
			List<PlanElement> planOld = personOld.getSelectedPlan().getPlanElements();
			List<PlanElement> planNew = popWhichWillBeUpdated.getPersons().get(personOld.getId()).getSelectedPlan().getPlanElements();
			int j = 0;
			for (PlanElement planElementOld : planOld) {
				if (planElementOld instanceof ActivityImpl) {
					ActivityImpl actOld = (ActivityImpl) planElementOld;
					if (!actOld.getType().equals("pt interaction")) {
						ActivityImpl actNew = (ActivityImpl) planNew.get(j);
						actNew.setFacilityId(actOld.getFacilityId());
						actNew.setCoord(actOld.getCoord());
						j += 2;
						while (((ActivityImpl) planNew.get(j)).getType().equals("pt interaction")) {
							j += 2;
						}
					}
				}
			}
		}
	}

}
