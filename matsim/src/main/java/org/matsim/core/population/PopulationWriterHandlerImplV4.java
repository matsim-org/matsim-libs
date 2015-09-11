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

package org.matsim.core.population;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityOptionImpl;

public class PopulationWriterHandlerImplV4 extends AbstractPopulationWriterHandler {

	private final Network network;

	public PopulationWriterHandlerImplV4(final Network network) {
		this.network = network;
	}

	@Override
	public void writeHeaderAndStartElement(final BufferedWriter out) throws IOException {
		out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
		out.write("<!DOCTYPE plans SYSTEM \"" + MatsimXmlWriter.DEFAULT_DTD_LOCATION + "plans_v4.dtd\">\n\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <plans ... > ... </plans>
	//////////////////////////////////////////////////////////////////////

	@Override
	public void startPlans(final Population plans, final BufferedWriter out) throws IOException {
		out.write("<plans");
		if (plans.getName() != null) {
			out.write(" name=\"" + plans.getName() + "\"");
		}
		out.write(">\n\n");
	}

	@Override
	public void endPlans(final BufferedWriter out) throws IOException {
		out.write("</plans>\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <person ... > ... </person>
	//////////////////////////////////////////////////////////////////////

	@Override
	public void startPerson(final Person p, final BufferedWriter out) throws IOException {
		out.write("\t<person id=\"");
		out.write(p.getId().toString());
		out.write("\"");
		if (p instanceof PersonImpl){
			Person person = p;
			if (PersonUtils.getSex(person) != null) {
				out.write(" sex=\"");
				out.write(PersonUtils.getSex(person));
				out.write("\"");
			}
			if (PersonUtils.getAge(person) != null) {
				out.write(" age=\"");
				out.write(Integer.toString(PersonUtils.getAge(person)));
				out.write("\"");
			}
			if (PersonUtils.getLicense(person) != null) {
				out.write(" license=\"");
				out.write(PersonUtils.getLicense(person));
				out.write("\"");
			}
			if (PersonUtils.getCarAvail(person) != null) {
				out.write(" car_avail=\"");
				out.write(PersonUtils.getCarAvail(person));
				out.write("\"");
			}
			if (PersonUtils.isEmployed(person) != null) {
				out.write(" employed=\"");
				out.write((PersonUtils.isEmployed(person) ? "yes" : "no"));
				out.write("\"");
			}
		}
		out.write(">\n");
	}

	@Override
	public void endPerson(final BufferedWriter out) throws IOException {
		out.write("\t</person>\n\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <travelcard ... />
	//////////////////////////////////////////////////////////////////////

	@Override
	public void startTravelCard(final String travelcard, final BufferedWriter out) throws IOException {
		out.write("\t\t<travelcard type=\"");
		out.write(travelcard);
		out.write("\" />\n\n");
	}

	@Override
	public void endTravelCard(final BufferedWriter out) throws IOException {
	}

	//////////////////////////////////////////////////////////////////////
	// <activity ... > ... </activity>
	//////////////////////////////////////////////////////////////////////

	@Override
	public void startActivity(final String act_type, final BufferedWriter out) throws IOException {
		out.write("\t\t\t<activity");
		out.write(" type=\"" + act_type + "\"");
		out.write(">\n");
	}

	@Override
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

	@Override
	public void startPrimaryLocation(final ActivityOptionImpl activity, final BufferedWriter out) throws IOException {
		out.write("\t\t\t\t<location");
		out.write(" id=\"" + activity.getFacility().getId() + "\"");
		out.write(" isPrimary=\"" + "yes" + "\"");
		out.write(">\n");
	}

	@Override
	public void endPrimaryLocation(final BufferedWriter out) throws IOException {
		out.write("\t\t\t\t</location>\n");
	}

	@Override
	public void startSecondaryLocation(final ActivityOptionImpl activity, final BufferedWriter out) throws IOException {
		out.write("\t\t\t\t<location");
		out.write(" id=\"" + activity.getFacility().getId() + "\"");
		out.write(">\n");
	}

	@Override
	public void endSecondaryLocation(final BufferedWriter out) throws IOException {
		out.write("\t\t\t\t</location>\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <plan ... > ... </plan>
	//////////////////////////////////////////////////////////////////////

	@Override
	public void startPlan(final Plan plan, final BufferedWriter out) throws IOException {
		out.write("\t\t<plan");
		if (plan.getScore() != null) {
			out.write(" score=\"");
			out.write(plan.getScore().toString());
			out.write("\"");
		}
		if (plan.isSelected())
			out.write(" selected=\"yes\"");
		else
			out.write(" selected=\"no\"");
		if (plan instanceof PlanImpl){
			PlanImpl p = (PlanImpl)plan;
			if ((p.getType() != null)) {
				out.write(" type=\"");
				out.write(p.getType());
				out.write("\"");
			}
		}
		out.write(">\n");
	}

	@Override
	public void endPlan(final BufferedWriter out) throws IOException {
		out.write("\t\t</plan>\n\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <act ... > ... </act>
	//////////////////////////////////////////////////////////////////////

	@Override
	public void startAct(final Activity act, final BufferedWriter out) throws IOException {
		out.write("\t\t\t<act type=\"");
		out.write(act.getType());
		out.write("\"");
		if (act.getLinkId() != null) {
			out.write(" link=\"");
			out.write(act.getLinkId().toString());
			out.write("\"");
		}
		if (act.getFacilityId() != null) {
			out.write(" facility=\"");
			out.write(act.getFacilityId().toString());
			out.write("\"");
		}
		if (act.getCoord() != null) {
			out.write(" x=\"");
			out.write(Double.toString(act.getCoord().getX()));
			out.write("\" y=\"");
			out.write(Double.toString(act.getCoord().getY()));
			out.write("\"");
		}
		if (act.getStartTime() != Time.UNDEFINED_TIME) {
			out.write(" start_time=\"");
			out.write(Time.writeTime(act.getStartTime()));
			out.write("\"");
		}
		if (act instanceof ActivityImpl){
			ActivityImpl a = (ActivityImpl)act;
			if (a.getMaximumDuration() != Time.UNDEFINED_TIME) {
				out.write(" dur=\"");
				out.write(Time.writeTime(a.getMaximumDuration()));
				out.write("\"");
			}
		}
		if (act.getEndTime() != Time.UNDEFINED_TIME) {
			out.write(" end_time=\"");
			out.write(Time.writeTime(act.getEndTime()));
			out.write("\"");
		}
		out.write(" />\n");
	}

	@Override
	public void endAct(final BufferedWriter out) throws IOException {
	}

	//////////////////////////////////////////////////////////////////////
	// <leg ... > ... </leg>
	//////////////////////////////////////////////////////////////////////

	@Override
	public void startLeg(final Leg leg, final BufferedWriter out) throws IOException {
		out.write("\t\t\t<leg mode=\"");
		out.write(leg.getMode());
		out.write("\"");
		if (leg.getDepartureTime() != Time.UNDEFINED_TIME) {
			out.write(" dep_time=\"");
			out.write(Time.writeTime(leg.getDepartureTime()));
			out.write("\"");
		}
		if (leg.getTravelTime() != Time.UNDEFINED_TIME) {
			out.write(" trav_time=\"");
			out.write(Time.writeTime(leg.getTravelTime()));
			out.write("\"");
		}
		if (leg instanceof LegImpl) {
			LegImpl l = (LegImpl)leg;
			if (l.getArrivalTime() != Time.UNDEFINED_TIME) {
				out.write(" arr_time=\"");
				out.write(Time.writeTime(l.getArrivalTime()));
				out.write("\"");
			}
		}
		out.write(">\n");
	}

	@Override
	public void endLeg(final BufferedWriter out) throws IOException {
		out.write("\t\t\t</leg>\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <route ... > ... </route>
	//////////////////////////////////////////////////////////////////////

	@Override
	public void startRoute(final Route route, final BufferedWriter out) throws IOException {
		out.write("\t\t\t\t<route");
		if (!Double.isNaN(route.getDistance())) {
			out.write(" dist=\"");
			out.write(Double.toString(route.getDistance()));
			out.write("\"");
		}
		if (route.getTravelTime() != Time.UNDEFINED_TIME) {
			out.write(" trav_time=\"");
			out.write(Time.writeTime(route.getTravelTime()));
			out.write("\"");
		}
		out.write(">\n");

		out.write("\t\t\t\t\t");
		if (route instanceof NetworkRoute) {
			for (Node n : RouteUtils.getNodes((NetworkRoute) route, this.network)) {
				out.write(n.getId().toString());
				out.write(" ");
			}
		} else {
			String rd = route.getRouteDescription();
			if (rd != null) {
				out.write(rd);
				out.write(" "); // this is at the moment only to maintain binary compatibility
			}
		}

		out.write("\n");
	}

	@Override
	public void endRoute(final BufferedWriter out) throws IOException {
		out.write("\t\t\t\t</route>\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <!-- ============ ... ========== -->
	//////////////////////////////////////////////////////////////////////

	@Override
	public void writeSeparator(final BufferedWriter out) throws IOException {
		out.write("<!-- ====================================================================== -->\n\n");
	}

}
