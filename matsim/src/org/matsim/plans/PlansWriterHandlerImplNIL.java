/* *********************************************************************** *
 * project: org.matsim.*
 * PlansWriterHandlerImplNIL.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.plans;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.facilities.Activity;
import org.matsim.facilities.Facility;
import org.matsim.facilities.Opentime;

class PlansWriterHandlerImplNIL implements PlansWriterHandler {

	public void startPlans(final Plans plans, final BufferedWriter out) throws IOException {}
	public void endPlans(final BufferedWriter out) throws IOException {}

	public void startPerson(final Person person, final BufferedWriter out) throws IOException {}
	public void endPerson(final BufferedWriter out) throws IOException {}

	public void startPlan(final Plan plan, final BufferedWriter out) throws IOException {}
	public void endPlan(final BufferedWriter out) throws IOException {}

	public void startAct(final Act act, final BufferedWriter out) {}
	public void endAct(final BufferedWriter out) throws IOException {}

	public void startLeg(final Leg leg, final BufferedWriter out) throws IOException {}
	public void endLeg(final BufferedWriter out) throws IOException {}

	public void startRoute(final Route route, final BufferedWriter out) throws IOException {}
	public void endRoute(final BufferedWriter out) throws IOException {}

	public void writeSeparator(final BufferedWriter out) throws IOException {}

	public void startTravelCard(String travelcard, BufferedWriter out) throws IOException {}
	public void endTravelCard(BufferedWriter out) throws IOException {}

	public void startKnowledge(Knowledge knowledge, BufferedWriter out) throws IOException {}
	public void endKnowledge(BufferedWriter out) throws IOException {}

	public void startActivity(String act_type, BufferedWriter out) throws IOException {}
	public void endActivity(BufferedWriter out) throws IOException {}

	public void startLocation(Facility facility, BufferedWriter out) throws IOException {}
	public void endLocation(BufferedWriter out) throws IOException {}

	public void startCapacity(Activity activity, BufferedWriter out) throws IOException {}
	public void endCapacity(BufferedWriter out) throws IOException {}

	public void startOpentime(Opentime opentime, BufferedWriter out) throws IOException {}
	public void endOpentime(BufferedWriter out) throws IOException {}

	public void startActivitySpace(ActivitySpace as, BufferedWriter out) throws IOException {}
	public void endActivitySpace(BufferedWriter out) throws IOException {}

	public void startParam(String name, String value, BufferedWriter out) throws IOException {}
	public void endParam(BufferedWriter out) throws IOException {}
}
