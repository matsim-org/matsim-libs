/* *********************************************************************** *
 * project: org.matsim.*
 * PlansWriterHandlerImplV0.java
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

package org.matsim.population;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;

import org.matsim.facilities.Activity;
import org.matsim.facilities.OpeningTime;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.CarRoute;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Node;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.utils.misc.Time;
import org.matsim.writer.MatsimXmlWriter;

public class PopulationWriterHandlerImplV0 implements PopulationWriterHandler {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	@SuppressWarnings("unused")
	private static final int DEBUG_LEVEL = 0;

	//////////////////////////////////////////////////////////////////////
	//
	// interface implementation
	//
	//////////////////////////////////////////////////////////////////////

	public void writeHeaderAndStartElement(BufferedWriter out) throws IOException {
		out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
		out.write("<!DOCTYPE plans SYSTEM \"" + MatsimXmlWriter.DEFAULT_DTD_LOCATION + "plans_v0.dtd\">\n\n");
	}
	
	//////////////////////////////////////////////////////////////////////
	// <plans ... > ... </plans>
	//////////////////////////////////////////////////////////////////////

	public void startPlans(final Population plans, final BufferedWriter out) throws IOException {
		out.write("<plans>\n\n");
	}

	public void endPlans(final BufferedWriter out) throws IOException {
		out.write("</plans>\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <person ... > ... </person>
	//////////////////////////////////////////////////////////////////////

	public void startPerson(final Person person, final BufferedWriter out) throws IOException {
		out.write("\t<person");
		out.write(" id=\"" + person.getId() + "\"");
		out.write(">\n");
	}

	public void endPerson(final BufferedWriter out) throws IOException {
		out.write("\t</person>\n\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <travelcard ... />
	//////////////////////////////////////////////////////////////////////

	public void startTravelCard(final String travelcard, final BufferedWriter out) throws IOException {
	}

	public void endTravelCard(final BufferedWriter out) throws IOException {
	}

	//////////////////////////////////////////////////////////////////////
	// <desires ... > ... </desires>
	//////////////////////////////////////////////////////////////////////

	public void startDesires(final Desires desires, final BufferedWriter out) throws IOException {
	}

	public void endDesires(final BufferedWriter out) throws IOException {
	}

	//////////////////////////////////////////////////////////////////////
	// <actDur ... />
	//////////////////////////////////////////////////////////////////////

	public void startActDur(final String act_type, final double dur, final BufferedWriter out) throws IOException {
	}

	public void endActDur(final BufferedWriter out) throws IOException {
	}

	//////////////////////////////////////////////////////////////////////
	// <knowledge ... > ... </knowledge>
	//////////////////////////////////////////////////////////////////////

	public void startKnowledge(final Knowledge knowledge, final BufferedWriter out) throws IOException {
	}

	public void endKnowledge(final BufferedWriter out) throws IOException {
	}

	//////////////////////////////////////////////////////////////////////
	// <activityspace ... > ... </activityspace>
	//////////////////////////////////////////////////////////////////////

	public void startActivitySpace(final ActivitySpace as, final BufferedWriter out) throws IOException {
	}

	public void endActivitySpace(final BufferedWriter out) throws IOException {
	}

	//////////////////////////////////////////////////////////////////////
	// <param ... />
	//////////////////////////////////////////////////////////////////////

	public void startParam(final String name, final String value, final BufferedWriter out) throws IOException {
	}

	public void endParam(final BufferedWriter out) throws IOException {
	}

	//////////////////////////////////////////////////////////////////////
	// <activity ... > ... </activity>
	//////////////////////////////////////////////////////////////////////

	public void startActivity(final String act_type, final BufferedWriter out) throws IOException {
	}

	public void endActivity(final BufferedWriter out) throws IOException {
	}

	//////////////////////////////////////////////////////////////////////
	// <location ... > ... </location>
	//////////////////////////////////////////////////////////////////////

	public void startPrimaryLocation(final Activity activity, final BufferedWriter out) throws IOException {
	}

	public void endPrimaryLocation(final BufferedWriter out) throws IOException {
	}

	public void startSecondaryLocation(final Activity activity, final BufferedWriter out) throws IOException {
	}

	public void endSecondaryLocation(final BufferedWriter out) throws IOException {
	}

	//////////////////////////////////////////////////////////////////////
	// <capacity ... />
	//////////////////////////////////////////////////////////////////////

	public void startCapacity(final Activity activtiy, final BufferedWriter out) throws IOException {
	}

	public void endCapacity(final BufferedWriter out) throws IOException {
	}

	//////////////////////////////////////////////////////////////////////
	// <opentime ... />
	//////////////////////////////////////////////////////////////////////

	public void startOpentime(final OpeningTime opentime, final BufferedWriter out) throws IOException {
	}

	public void endOpentime(final BufferedWriter out) throws IOException {
	}

	//////////////////////////////////////////////////////////////////////
	// <plan ... > ... </plan>
	//////////////////////////////////////////////////////////////////////

	public void startPlan(final Plan plan, final BufferedWriter out) throws IOException {
		out.write("\t\t<plan");
		if (!Double.isNaN(plan.getScore()))
			out.write(" score=\"" + plan.getScore() + "\"");
		if (plan.isSelected())
			out.write(" selected=\"" + "yes" + "\"");
		else
			out.write(" selected=\"" + "no" + "\"");
		out.write(">\n");
	}

	public void endPlan(final BufferedWriter out) throws IOException {
		out.write("\t\t</plan>\n\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <act ... > ... </act>
	//////////////////////////////////////////////////////////////////////

	public void startAct(final Act act, final BufferedWriter out) throws IOException {
		out.write("\t\t\t<act");
		out.write(" type=\"" + act.getType() + "\"");
		if (act.getCoord() != null) {
			out.write(" x100=\"" + act.getCoord().getX() + "\"");
			out.write(" y100=\"" + act.getCoord().getY() + "\"");
		}
		if (act.getLink() != null)
			out.write(" link=\"" + act.getLink().getId() + "\"");
		if (act.getStartTime() != Integer.MIN_VALUE)
			out.write(" start_time=\"" + Time.writeTime(act.getStartTime()) + "\"");
		if (act.getDuration() != Integer.MIN_VALUE)
			out.write(" dur=\"" + Time.writeTime(act.getDuration()) + "\"");
		if (act.getEndTime() != Integer.MIN_VALUE)
			out.write(" end_time=\"" + Time.writeTime(act.getEndTime()) + "\"");
		out.write(" />\n");
//		Gbl.debugMsg(DEBUG_LEVEL,this.getClass(),"startRoute(...)","there's no 'zone' info at the moment!");
	}

	public void endAct(final BufferedWriter out) throws IOException {
	}

	//////////////////////////////////////////////////////////////////////
	// <leg ... > ... </leg>
	//////////////////////////////////////////////////////////////////////

	public void startLeg(final Leg leg, final BufferedWriter out) throws IOException {
		out.write("\t\t\t<leg");
		out.write(" mode=\"" + leg.getMode() + "\"");
		if (leg.getDepartureTime() != Integer.MIN_VALUE)
			out.write(" dep_time=\"" + Time.writeTime(leg.getDepartureTime()) + "\"");
		if (leg.getTravelTime() != Integer.MIN_VALUE)
			out.write(" trav_time=\"" + Time.writeTime(leg.getTravelTime()) + "\"");
		if (leg.getArrivalTime() != Integer.MIN_VALUE)
			out.write(" arr_time=\"" + Time.writeTime(leg.getArrivalTime()) + "\"");
		out.write(">\n");
	}

	public void endLeg(final BufferedWriter out) throws IOException {
		out.write("\t\t\t</leg>\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <route ... > ... </route>
	//////////////////////////////////////////////////////////////////////

	public void startRoute(final CarRoute route, final BufferedWriter out) throws IOException {
		out.write("\t\t\t\t<route");
		out.write(">\n");

		out.write("\t\t\t\t\t");
		Iterator<Node> it = route.getNodes().iterator();
		while (it.hasNext()) {
			Node n = it.next();
			out.write(n.getId() + " ");
		}
		out.write("\n");
	}

	public void endRoute(final BufferedWriter out) throws IOException {
		out.write("\t\t\t\t</route>\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <!-- ============ ... ========== -->
	//////////////////////////////////////////////////////////////////////

	public void writeSeparator(final BufferedWriter out) throws IOException {
		out.write("<!-- =================================================" +
							"===================== -->\n\n");
	}
}
