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

package org.matsim.core.population;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.misc.Time;
import org.matsim.knowledges.KnowledgeImpl;
import org.matsim.population.Desires;

public class PopulationWriterHandlerImplV0 implements PopulationWriterHandler {

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

	public void startKnowledge(final KnowledgeImpl knowledge, final BufferedWriter out) throws IOException {
	}

	public void endKnowledge(final BufferedWriter out) throws IOException {
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

	public void startPrimaryLocation(final ActivityOptionImpl activity, final BufferedWriter out) throws IOException {
	}

	public void endPrimaryLocation(final BufferedWriter out) throws IOException {
	}

	public void startSecondaryLocation(final ActivityOptionImpl activity, final BufferedWriter out) throws IOException {
	}

	public void endSecondaryLocation(final BufferedWriter out) throws IOException {
	}

	//////////////////////////////////////////////////////////////////////
	// <plan ... > ... </plan>
	//////////////////////////////////////////////////////////////////////

	public void startPlan(final Plan plan, final BufferedWriter out) throws IOException {
		out.write("\t\t<plan");
		if (plan.getScore() != null)
			out.write(" score=\"" + plan.getScore().toString() + "\"");
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

	public void startAct(final Activity act, final BufferedWriter out) throws IOException {
		out.write("\t\t\t<act");
		out.write(" type=\"" + act.getType() + "\"");
		if (act.getCoord() != null) {
			out.write(" x100=\"" + act.getCoord().getX() + "\"");
			out.write(" y100=\"" + act.getCoord().getY() + "\"");
		}
		if (act.getLinkId() != null)
			out.write(" link=\"" + act.getLinkId() + "\"");
		if (act.getStartTime() != Integer.MIN_VALUE)
			out.write(" start_time=\"" + Time.writeTime(act.getStartTime()) + "\"");
		if (act instanceof ActivityImpl){
			ActivityImpl a = (ActivityImpl)act;
			if (a.getDuration() != Time.UNDEFINED_TIME)
				out.write(" dur=\"" + Time.writeTime(a.getDuration()) + "\"");
		}
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
		if (leg instanceof LegImpl){
			LegImpl l = (LegImpl)leg;
			if (l.getArrivalTime() != Time.UNDEFINED_TIME)
				out.write(" arr_time=\"" + Time.writeTime(l.getArrivalTime()) + "\"");
		}
		out.write(">\n");
	}

	public void endLeg(final BufferedWriter out) throws IOException {
		out.write("\t\t\t</leg>\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <route ... > ... </route>
	//////////////////////////////////////////////////////////////////////

	public void startRoute(final Route route, final BufferedWriter out) throws IOException {
		out.write("\t\t\t\t<route>");

		if (route instanceof NetworkRouteWRefs) {
			for (Node n : ((NetworkRouteWRefs) route).getNodes()) {
				out.write(n.getId() + " ");
			}
		} else if (route instanceof GenericRoute) {
			out.write(((GenericRoute) route).getRouteDescription());
		}
	}

	public void endRoute(final BufferedWriter out) throws IOException {
		out.write("</route>\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <!-- ============ ... ========== -->
	//////////////////////////////////////////////////////////////////////

	public void writeSeparator(final BufferedWriter out) throws IOException {
		out.write("<!-- =================================================" +
							"===================== -->\n\n");
	}
}
