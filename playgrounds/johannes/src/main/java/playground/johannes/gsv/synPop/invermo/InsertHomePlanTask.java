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

package playground.johannes.gsv.synPop.invermo;

import playground.johannes.synpop.data.*;
import playground.johannes.synpop.source.mid2008.processing.PersonTask;

/**
 * @author johannes
 *
 */
public class InsertHomePlanTask implements PersonTask {

	/* (non-Javadoc)
	 * @see playground.johannes.synpop.source.mid2008.processing.PersonTask#apply(playground.johannes.synpop.data.PlainPerson)
	 */
	@Override
	public void apply(Person person) {
		if(person.getEpisodes().isEmpty()) {
			Episode plan = new PlainEpisode();
			PlainSegment act = new PlainSegment();
			act.setAttribute(CommonKeys.ACTIVITY_TYPE, "home");
			act.setAttribute(CommonKeys.ACTIVITY_START_TIME, "0");
			act.setAttribute(CommonKeys.ACTIVITY_END_TIME, "86400");
			act.setAttribute(InvermoKeys.LOCATION, "home");
			
			plan.addActivity(act);
			person.addEpisode(plan);
		}

	}

}
