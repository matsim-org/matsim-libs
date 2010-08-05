/* *********************************************************************** *
 * project: org.matsim.*
 * LMwCSCustomized.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.mfeil;

import java.util.ArrayList;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.knowledges.Knowledges;
import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSet;


/**
 * Extension of Horni's location choice: Removes origin and destination facilities from choice set
 * to avoid 0m trips. 
 * @author Matthias Feil
 *
 */
public class LMwCSCustomized extends LocationMutatorwChoiceSet {

	public LMwCSCustomized(final Network network, Controler controler, Knowledges kn) {
		super(network, controler, kn);
	}

	@Override
	protected boolean modifyLocation(ActivityImpl act, Coord startCoord, Coord endCoord, double radius, int trialNr) {

		ArrayList<ActivityFacility> choiceSet = this.computeChoiceSetCircle(startCoord, endCoord, radius, act.getType());

		for (ActivityFacility fac: choiceSet){
			if (fac.getCoord().equals(startCoord)) {
				choiceSet.remove(fac);
				break;
			}
		}
		for (ActivityFacility fac: choiceSet){
			if (fac.getCoord().equals(endCoord)) {
				choiceSet.remove(fac);
			break;
			}
		}

		if (choiceSet.size()>1) {
			final ActivityFacility facility = choiceSet.get(MatsimRandom.getRandom().nextInt(choiceSet.size()));

			act.setFacilityId(facility.getId());
       		act.setLinkId(((NetworkImpl) this.network).getNearestLink(facility.getCoord()).getId());
       		act.setCoord(facility.getCoord());
       		return true;
		}
		return false;
	}
}
