/* *********************************************************************** *
 * project: org.matsim.*
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
 * *********************************************************************** */

package playground.johannes.gsv.matrices.plans2matrix;

import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntIterator;
import playground.johannes.coopsim.mental.choice.ChoiceSet;
import playground.johannes.gsv.synPop.ActivityType;
import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyPersonsTask;
import playground.johannes.socialnetworks.utils.XORShiftRandom;
import playground.johannes.synpop.data.Element;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.PlainPerson;

import java.util.Collection;

/**
 * @author johannes
 *
 */
public class ReplaceMiscType implements ProxyPersonsTask {

	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.ProxyPersonsTask#apply(java.util.Collection)
	 */
	@Override
	public void apply(Collection<PlainPerson> persons) {
		TObjectIntHashMap<String> typeCounts = new TObjectIntHashMap<>();
		
		for(PlainPerson person : persons) {
			for(Episode plan : person.getPlans()) {
				for(Element act : plan.getActivities()) {
					String type = act.getAttribute(CommonKeys.ACTIVITY_TYPE);
					if(!ActivityType.HOME.equalsIgnoreCase(type) && !ActivityType.MISC.equalsIgnoreCase(type)) {
						typeCounts.adjustOrPutValue(type, 1, 1);
					}
				}
			}
		}

		ChoiceSet<String> types = new ChoiceSet<>(new XORShiftRandom());
		TObjectIntIterator<String> it = typeCounts.iterator();
		for(int i = 0; i < typeCounts.size(); i++) {
			it.advance();
			types.addChoice(it.key(), it.value());
		}
		
		for(PlainPerson person : persons) {
			for(Episode plan : person.getPlans()) {
				for(Element act : plan.getActivities()) {
					String type = act.getAttribute(CommonKeys.ACTIVITY_TYPE);
					if(type == null || type.equalsIgnoreCase(ActivityType.MISC)) {
						type = types.randomWeightedChoice();
						act.setAttribute(CommonKeys.ACTIVITY_TYPE, type);
					}
				}
			}
		}
	}
}
