/* *********************************************************************** *
 * project: org.matsim.*
 * PlansWriterHandler.java
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

package org.matsim.core.population;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.basic.v01.population.BasicActivity;
import org.matsim.api.basic.v01.population.BasicLeg;
import org.matsim.api.basic.v01.population.BasicPerson;
import org.matsim.api.basic.v01.population.BasicPlan;
import org.matsim.api.basic.v01.population.BasicPopulation;
import org.matsim.api.basic.v01.population.BasicRoute;
import org.matsim.core.api.facilities.ActivityOption;
import org.matsim.core.api.facilities.OpeningTime;
import org.matsim.core.utils.io.WriterHandler;
import org.matsim.population.ActivitySpace;
import org.matsim.population.Desires;
import org.matsim.population.Knowledge;

public interface PopulationWriterHandler extends WriterHandler {

	//////////////////////////////////////////////////////////////////////
	// <plans ... > ... </plans>
	//////////////////////////////////////////////////////////////////////

	public void startPlans(final BasicPopulation plans, final BufferedWriter out) throws IOException;

	public void endPlans(final BufferedWriter out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <person ... > ... </person>
	//////////////////////////////////////////////////////////////////////

	public void startPerson(final BasicPerson person, final BufferedWriter out) throws IOException;

	public void endPerson(final BufferedWriter out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <travelcard ... />
	//////////////////////////////////////////////////////////////////////

	public void startTravelCard(final String travelcard, final BufferedWriter out) throws IOException;

	public void endTravelCard(final BufferedWriter out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <desires ... > ... </desires>
	//////////////////////////////////////////////////////////////////////

	public void startDesires(final Desires desires, final BufferedWriter out) throws IOException;

	public void endDesires(final BufferedWriter out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <actDur ... />
	//////////////////////////////////////////////////////////////////////

	public void startActDur(final String act_type, final double dur, final BufferedWriter out) throws IOException;

	public void endActDur(final BufferedWriter out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <knowledge ... > ... </knowledge>
	//////////////////////////////////////////////////////////////////////

	public void startKnowledge(final Knowledge knowledge, final BufferedWriter out) throws IOException;

	public void endKnowledge(final BufferedWriter out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <activityspace ... > ... </activityspace>
	//////////////////////////////////////////////////////////////////////

	public void startActivitySpace(final ActivitySpace as, final BufferedWriter out) throws IOException;

	public void endActivitySpace(final BufferedWriter out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <param ... />
	//////////////////////////////////////////////////////////////////////

	public void startParam(final String name, final String value, final BufferedWriter out) throws IOException;

	public void endParam(final BufferedWriter out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <activity ... > ... </activity>
	//////////////////////////////////////////////////////////////////////

	public void startActivity(final String act_type, final BufferedWriter out) throws IOException;

	public void endActivity(final BufferedWriter out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <location ... > ... </location>
	//////////////////////////////////////////////////////////////////////

//	public void startLocation(final Facility facility, final BufferedWriter out) throws IOException;

//	public void endLocation(final BufferedWriter out) throws IOException;

	public void startPrimaryLocation(final ActivityOption activity, final BufferedWriter out) throws IOException;

	public void endPrimaryLocation(final BufferedWriter out) throws IOException;

	public void startSecondaryLocation(final ActivityOption activity, final BufferedWriter out) throws IOException;

	public void endSecondaryLocation(final BufferedWriter out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <capacity ... />
	//////////////////////////////////////////////////////////////////////

	public void startCapacity(final ActivityOption activity, final BufferedWriter out) throws IOException;

	public void endCapacity(final BufferedWriter out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <opentime ... />
	//////////////////////////////////////////////////////////////////////

	public void startOpentime(final OpeningTime opentime, final BufferedWriter out) throws IOException;

	public void endOpentime(final BufferedWriter out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <plan ... > ... </plan>
	//////////////////////////////////////////////////////////////////////

	public void startPlan(final BasicPlan plan, final BufferedWriter out) throws IOException;

	public void endPlan(final BufferedWriter out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <act ... > ... </act>
	//////////////////////////////////////////////////////////////////////

	public void startAct(final BasicActivity act, final BufferedWriter out) throws IOException;

	public void endAct(final BufferedWriter out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <leg ... > ... </leg>
	//////////////////////////////////////////////////////////////////////

	public void startLeg(final BasicLeg leg, final BufferedWriter out) throws IOException;

	public void endLeg(final BufferedWriter out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <route ... > ... </route>
	//////////////////////////////////////////////////////////////////////

	public void startRoute(final BasicRoute route, final BufferedWriter out) throws IOException;

	public void endRoute(final BufferedWriter out) throws IOException;

	public void writeHeaderAndStartElement(BufferedWriter out) throws IOException;
}
