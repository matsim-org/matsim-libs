/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.basic.v01;

import java.util.List;

import org.matsim.basic.v01.BasicLeg.Mode;
import org.matsim.interfaces.basic.v01.BasicLocation;

/**
 * @author dgrether
 */
public interface PopulationBuilder {

	BasicPerson createPerson(Id id) throws Exception;

	BasicPlan createPlan(BasicPerson currentPerson);

	BasicAct createAct(BasicPlan basicPlan, String currentActType, BasicLocation currentlocation);

	BasicLeg createLeg(BasicPlan basicPlan, Mode legMode);

	BasicRoute createRoute(final List<Id> currentRouteLinkIds);

	BasicPlan createPlan(BasicPerson person, boolean selected);

	BasicActivity createActivity(String type, BasicLocation currentlocation);

	BasicKnowledge createKnowledge(List<BasicActivity> currentActivities);

}
