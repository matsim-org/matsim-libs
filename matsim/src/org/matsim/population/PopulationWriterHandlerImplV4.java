/* *********************************************************************** *
 * project: org.matsim.*
 * PlansWriterHandlerImplV4.java
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

import org.matsim.facilities.Activity;
import org.matsim.facilities.OpeningTime;
import org.matsim.gbl.Gbl;
import org.matsim.network.Node;
import org.matsim.utils.misc.Time;
import org.matsim.writer.MatsimXmlWriter;

public class PopulationWriterHandlerImplV4 implements PopulationWriterHandler {


	public void writeHeaderAndStartElement(BufferedWriter out) throws IOException {
		out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
		out.write("<!DOCTYPE plans SYSTEM \"" + MatsimXmlWriter.DEFAULT_DTD_LOCATION + "plans_v4.dtd\">\n\n");
	}
	
	//////////////////////////////////////////////////////////////////////
	// <plans ... > ... </plans>
	//////////////////////////////////////////////////////////////////////

	public void startPlans(final Population plans, final BufferedWriter out) throws IOException {
		out.write("<plans");
		if (plans.getName() != null) {
			out.write(" name=\"" + plans.getName() + "\"");
		}
		out.write(">\n\n");
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
		if (person.getSex() != null)
			out.write(" sex=\"" + person.getSex() + "\"");
		if (person.getAge() != Integer.MIN_VALUE)
			out.write(" age=\"" + person.getAge() + "\"");
		if (person.getLicense() != null)
			out.write(" license=\"" + person.getLicense() + "\"");
		if (person.getCarAvail() != null)
			out.write(" car_avail=\"" + person.getCarAvail() + "\"");
		if (person.getEmployed() != null)
			out.write(" employed=\"" + person.getEmployed() + "\"");
		out.write(">\n");
	}

	public void endPerson(final BufferedWriter out) throws IOException {
		out.write("\t</person>\n\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <travelcard ... />
	//////////////////////////////////////////////////////////////////////

	public void startTravelCard(final String travelcard, final BufferedWriter out) throws IOException {
		out.write("\t\t<travelcard");
		out.write(" type=\"" + travelcard + "\"");
		out.write(" />\n\n");
	}

	public void endTravelCard(final BufferedWriter out) throws IOException {
	}

	//////////////////////////////////////////////////////////////////////
	// <desires ... > ... </desires>
	//////////////////////////////////////////////////////////////////////

	public void startDesires(final Desires desires, final BufferedWriter out) throws IOException {
		out.write("\t\t<desires");
		if (desires.getDesc() != null)
			out.write(" desc=\"" + desires.getDesc() + "\"");
		out.write(">\n");
	}

	public void endDesires(final BufferedWriter out) throws IOException {
		out.write("\t\t</desires>\n\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <actDur ... />
	//////////////////////////////////////////////////////////////////////

	public void startActDur(final String act_type, final double dur, final BufferedWriter out) throws IOException {
		out.write("\t\t\t<actDur");
		out.write(" type=\"" + act_type + "\"");
		out.write(" dur=\"" + Time.writeTime(dur) + "\"");
		out.write(" />\n");
	}

	public void endActDur(final BufferedWriter out) throws IOException {
	}

	//////////////////////////////////////////////////////////////////////
	// <knowledge ... > ... </knowledge>
	//////////////////////////////////////////////////////////////////////

	public void startKnowledge(final Knowledge knowledge, final BufferedWriter out) throws IOException {
		out.write("\t\t<knowledge");
		if (knowledge.getDescription() != null)
			out.write(" desc=\"" + knowledge.getDescription() + "\"");
		out.write(">\n");
	}

	public void endKnowledge(final BufferedWriter out) throws IOException {
		out.write("\t\t</knowledge>\n\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <activityspace ... > ... </activityspace>
	//////////////////////////////////////////////////////////////////////

	public void startActivitySpace(final ActivitySpace as, final BufferedWriter out) throws IOException {
		out.write("\t\t\t<activityspace");
		if (as instanceof ActivitySpaceEllipse) {
			out.write(" type=\"" + "ellipse" + "\"");
		} else if (as instanceof ActivitySpaceCassini) {
			out.write(" type=\"" + "cassini" + "\"");

		}else if (as instanceof ActivitySpaceSuperEllipse) {
			out.write(" type=\"" + "superellipse" + "\"");

		}else if (as instanceof ActivitySpaceBean) {
			out.write(" type=\"" + "bean" + "\"");

		} else {
			Gbl.errorMsg("[something is completely wrong!]");
		}
		out.write(" activity_type=\"" + as.getActType() + "\"");
		out.write(">\n");
	}

	public void endActivitySpace(final BufferedWriter out) throws IOException {
		out.write("\t\t\t</activityspace>\n\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <param ... />
	//////////////////////////////////////////////////////////////////////

	public void startParam(final String name, final String value, final BufferedWriter out) throws IOException {
		out.write("\t\t\t\t<param");
		out.write(" name=\"" + name + "\"");
		out.write(" value=\"" + value + "\"");
		out.write(" />\n");
	}

	public void endParam(final BufferedWriter out) throws IOException {
	}

	//////////////////////////////////////////////////////////////////////
	// <activity ... > ... </activity>
	//////////////////////////////////////////////////////////////////////

	public void startActivity(final String act_type, final BufferedWriter out) throws IOException {
		out.write("\t\t\t<activity");
		out.write(" type=\"" + act_type + "\"");
		out.write(">\n");
	}

	public void endActivity(final BufferedWriter out) throws IOException {
		out.write("\t\t\t</activity>\n\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <location ... > ... </location>
	//////////////////////////////////////////////////////////////////////

//	public void startLocation(final Facility facility, final BufferedWriter out) throws IOException {
//		out.write("\t\t\t\t<location");
//		out.write(" type=\"" + facility.getLayer().getType() + "\"");
//		out.write(" id=\"" + facility.getId() + "\"");
//		out.write(">\n");
//	}
//
//	public void endLocation(final BufferedWriter out) throws IOException {
//		out.write("\t\t\t\t</location>\n");
//	}

	public void startPrimaryLocation(final Activity activity, final BufferedWriter out) throws IOException {
		out.write("\t\t\t\t<location");
		out.write(" id=\"" + activity.getFacility().getId() + "\"");
		out.write(" isPrimary=\"" + "yes" + "\"");
		out.write(">\n");
	}

	public void endPrimaryLocation(final BufferedWriter out) throws IOException {
		out.write("\t\t\t\t</location>\n");
	}

	public void startSecondaryLocation(final Activity activity, final BufferedWriter out) throws IOException {
		out.write("\t\t\t\t<location");
		out.write(" id=\"" + activity.getFacility().getId() + "\"");
		out.write(">\n");
	}

	public void endSecondaryLocation(final BufferedWriter out) throws IOException {
		out.write("\t\t\t\t</location>\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <capacity ... />
	//////////////////////////////////////////////////////////////////////

	public void startCapacity(final Activity activtiy, final BufferedWriter out) throws IOException {
		if (activtiy.getCapacity() != Integer.MAX_VALUE) {
			out.write("\t\t\t\t\t<capacity");
			out.write(" value=\"" + activtiy.getCapacity() + "\"");
			out.write(" />\n");
		}
	}

	public void endCapacity(final BufferedWriter out) throws IOException {
	}

	//////////////////////////////////////////////////////////////////////
	// <opentime ... />
	//////////////////////////////////////////////////////////////////////

	public void startOpentime(final OpeningTime opentime, final BufferedWriter out) throws IOException {
		out.write("\t\t\t\t\t<opentime");
		out.write(" day=\"" + opentime.getDay() + "\"");
		out.write(" start_time=\"" + Time.writeTime(opentime.getStartTime()) + "\"");
		out.write(" end_time=\"" + Time.writeTime(opentime.getEndTime()) + "\"");
		out.write(" />\n");
	}

	public void endOpentime(final BufferedWriter out) throws IOException {
	}

	//////////////////////////////////////////////////////////////////////
	// <plan ... > ... </plan>
	//////////////////////////////////////////////////////////////////////

	public void startPlan(final Plan plan, final BufferedWriter out) throws IOException {
		out.write("\t\t<plan");
		if (!plan.hasUndefinedScore())
			out.write(" score=\"" + plan.getScore() + "\"");
		if (plan.isSelected())
			out.write(" selected=\"" + "yes" + "\"");
		else
			out.write(" selected=\"" + "no" + "\"");
		if ((plan.getType() != null) && (plan.getType() != Plan.Type.UNDEFINED))
			out.write(" type=\"" + plan.getType() + "\"");
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
		if (act.getLink() != null)
			out.write(" link=\"" + act.getLink().getId() + "\"");
		if (act.getFacility() != null)
			out.write(" facility=\"" + act.getFacility().getId() + "\"");
		if (act.getCoord() != null) {
			out.write(" x=\"" + act.getCoord().getX() + "\" y=\"" + act.getCoord().getY() + "\"");
		}
		if (act.getStartTime() != Time.UNDEFINED_TIME)
			out.write(" start_time=\"" + Time.writeTime(act.getStartTime()) + "\"");
		if (act.getDur() != Time.UNDEFINED_TIME)
			out.write(" dur=\"" + Time.writeTime(act.getDur()) + "\"");
		if (act.getEndTime() != Time.UNDEFINED_TIME)
			out.write(" end_time=\"" + Time.writeTime(act.getEndTime()) + "\"");
		out.write(" />\n");
	}

	public void endAct(final BufferedWriter out) throws IOException {
	}

	//////////////////////////////////////////////////////////////////////
	// <leg ... > ... </leg>
	//////////////////////////////////////////////////////////////////////

	public void startLeg(final Leg leg, final BufferedWriter out) throws IOException {
		out.write("\t\t\t<leg");
		if (leg.getNum() != Integer.MIN_VALUE)
			out.write(" num=\"" + leg.getNum() + "\"");
		out.write(" mode=\"" + leg.getMode() + "\"");
		if (leg.getDepTime() != Time.UNDEFINED_TIME)
			out.write(" dep_time=\"" + Time.writeTime(leg.getDepTime()) + "\"");
		if (leg.getTravTime() != Time.UNDEFINED_TIME)
			out.write(" trav_time=\"" + Time.writeTime(leg.getTravTime()) + "\"");
		if (leg.getArrTime() != Time.UNDEFINED_TIME)
			out.write(" arr_time=\"" + Time.writeTime(leg.getArrTime()) + "\"");
		out.write(">\n");
	}

	public void endLeg(final BufferedWriter out) throws IOException {
		out.write("\t\t\t</leg>\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <route ... > ... </route>
	//////////////////////////////////////////////////////////////////////

	public void startRoute(final Route route, final BufferedWriter out) throws IOException {
		out.write("\t\t\t\t<route");
		if (!Double.isNaN(route.getDist()))
			out.write(" dist=\"" + route.getDist() + "\"");
		if (route.getTravTime() != Time.UNDEFINED_TIME)
			out.write(" trav_time=\"" + Time.writeTime(route.getTravTime()) + "\"");
		out.write(">\n");

		out.write("\t\t\t\t\t");
		for (Node n : route.getRoute()) {
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
