/* *********************************************************************** *
 * project: org.matsim.*
 * PlansWriterHandlerImplV20.java
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

import org.matsim.utils.misc.Time;

abstract class PlansWriterHandlerImplV20 implements PlansWriterHandler {

	//////////////////////////////////////////////////////////////////////
	//
	// interface implementation
	//
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// <plans ... > ... </plans>
	//////////////////////////////////////////////////////////////////////

	public void startPlans(final Plans plans, final BufferedWriter out) throws IOException {
		out.write("<plans");
		if (plans.getName() != null)
			out.write(" name=\"" + plans.getName() + "\"");
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
	// <home ... > ... </home>
	//////////////////////////////////////////////////////////////////////

//	public void startHome(final Home home, final BufferedWriter out)
//			throws IOException {
//		out.write("\t\t<home");
//		if (home.getCoord() != null) {
//			out.write(" x=\"" + home.getCoord().getX() + "\"");
//			out.write(" y=\"" + home.getCoord().getY() + "\"");
//		}
//		if (home.getLink() != Integer.MIN_VALUE)
//			out.write(" link=\"" + home.getLink() + "\"");
//		out.write(">\n");
//	}
//
//	public void endHome(final BufferedWriter out) throws IOException {
//		out.write("\t\t</home>\n\n");
//	}

	//////////////////////////////////////////////////////////////////////
	// <prim_loc ... > ... </prim_loc>
	//////////////////////////////////////////////////////////////////////

//	public void startPrimLoc(final PrimLoc primloc, final BufferedWriter out)
//			throws IOException {
//		out.write("\t\t<prim_loc");
//		if (primloc.getCoord() != null) {
//			out.write(" x=\"" + primloc.getCoord().getX() + "\"");
//			out.write(" y=\"" + primloc.getCoord().getY() + "\"");
//		}
//		if (primloc.getLink() != Integer.MIN_VALUE)
//			out.write(" link=\"" + primloc.getLink() + "\"");
//		out.write(">\n");
//	}
//
//	public void endPrimLoc(final BufferedWriter out) throws IOException {
//		out.write("\t\t</prim_loc>\n\n");
//	}

	//////////////////////////////////////////////////////////////////////
	// <zone ... > ... </zone>
	//////////////////////////////////////////////////////////////////////

//	public void startZone(final Zone zone, final BufferedWriter out)
//			throws IOException {
//		out.write("\t\t\t<zone");
//		out.write(" type=\"" + zone.getLayer().getType() + "\"");
//		out.write(" id=\"" + zone.getId() + "\"");
//		if (zone.getCenter() != null) {
//			out.write(" center_x=\"" + zone.getCenter().getX() + "\"");
//			out.write(" center_y=\"" + zone.getCenter().getY() + "\"");
//		}
//		if (zone.getMin() != null) {
//			out.write(" min_x=\"" + zone.getMin().getX() + "\"");
//			out.write(" min_y=\"" + zone.getMin().getY() + "\"");
//		}
//		if (zone.getMax() != null) {
//			out.write(" max_x=\"" + zone.getMax().getX() + "\"");
//			out.write(" max_y=\"" + zone.getMax().getY() + "\"");
//		}
//		if (!Double.isNaN(zone.getArea()))
//			out.write(" area=\"" + zone.getArea() + "\"");
//		if (zone.getName() != null)
//			out.write(" name=\"" + zone.getName() + "\"");
//	}
//
//	public void endZone(final BufferedWriter out) throws IOException {
//		out.write(" />\n");
//	}

	//////////////////////////////////////////////////////////////////////
	// <travelcards ... > ... </travelcards>
	//////////////////////////////////////////////////////////////////////

//	public void startTravelCards(final TravelCards travelcards, final BufferedWriter out)
//			throws IOException {
//		out.write("\t\t<travelcards>\n");
//	}
//
//	public void endTravelCards(final BufferedWriter out) throws IOException {
//		out.write("\t\t</travelcards>\n\n");
//	}

	//////////////////////////////////////////////////////////////////////
	// <travelcard ... > ... </travelcard>
	//////////////////////////////////////////////////////////////////////

//	public void startTravelCard(final TravelCard travelcard, final BufferedWriter out)
//			throws IOException {
//		out.write("\t\t\t<travelcard");
//		out.write(" type=\"" + travelcard.getType() + "\"");
//		out.write(" />\n");
//	}
//
//	public void endTravelCard(final BufferedWriter out) throws IOException {
//		;
//	}

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
			out.write(" x=\"" + act.getCoord().getX() + "\"");
			out.write(" y=\"" + act.getCoord().getY() + "\"");
		}
		if (act.getLink() != null)
			out.write(" link=\"" + act.getLink().getId() + "\"");
		if (act.getStartTime() != Integer.MIN_VALUE)
			out.write(" start_time=\"" + Time.writeTime(act.getStartTime()) + "\"");
		if (act.getEndTime() != Integer.MIN_VALUE)
			out.write(" end_time=\"" + Time.writeTime(act.getEndTime()) + "\"");
		if (act.getDur() != Integer.MIN_VALUE)
			out.write(" dur=\"" + Time.writeTime(act.getDur()) +	"\"");
		out.write(">\n");
	}

	public void endAct(final BufferedWriter out) throws IOException {
		out.write("\t\t\t</act>\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <leg ... > ... </leg>
	//////////////////////////////////////////////////////////////////////

	public void startLeg(final Leg leg, final BufferedWriter out) throws IOException {
		out.write("\t\t\t<leg");
		if (leg.getNum() != Integer.MIN_VALUE)
			out.write(" num=\"" + leg.getNum() + "\"");
		out.write(" mode=\"" + leg.getMode() + "\"");
		if (leg.getDepTime() != Integer.MIN_VALUE)
			out.write(" dep_time=\"" + Time.writeTime(leg.getDepTime()) + "\"");
		if (leg.getTravTime() != Integer.MIN_VALUE)
			out.write(" trav_time=\"" + Time.writeTime(leg.getTravTime()) + "\"");
		if (leg.getArrTime() != Integer.MIN_VALUE)
			out.write(" arr_time=\"" + Time.writeTime(leg.getArrTime()) + "\"");
		out.write(">\n");
	}

	public void endLeg(final BufferedWriter out) throws IOException {
		out.write("\t\t\t</leg>\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <route ... > ... </route>
	//////////////////////////////////////////////////////////////////////

	public void startRoute(final Route route, final BufferedWriter out)
			throws IOException {
		out.write("\t\t\t\t<route>\n");
		if (route.getRoute().size() != 0) {
			out.write("\t\t\t\t\t");
			for (int i=0; i<route.getRoute().size(); i++) {
				out.write(route.getRoute().get(i) + " ");
			}
			out.write("\n");
		}
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
