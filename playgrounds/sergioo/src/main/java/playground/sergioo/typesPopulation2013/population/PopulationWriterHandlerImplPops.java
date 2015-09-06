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

package playground.sergioo.typesPopulation2013.population;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.*;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.misc.Time;

/**
 * @author mrieser
 * @author balmermi
 */
/*package*/ class PopulationWriterHandlerImplPops implements PopulationWriterHandler {

	@Override
	public void writeHeaderAndStartElement(final BufferedWriter out) throws IOException {
		out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
		out.write("<!DOCTYPE population SYSTEM \"" + MatsimXmlWriter.DEFAULT_DTD_LOCATION + "population_pops.dtd\">\n\n");
	}

	@Override
	public void startPlans(final Population plans, final BufferedWriter out) throws IOException {
		out.write("<population");
		if (plans.getName() != null) {
			out.write(" desc=\"" + plans.getName() + "\"");
		}
		out.write(">\n\n");
	}

	@Override
	public void writePerson(final Person person, final BufferedWriter out) throws IOException {
		this.startPerson(person, out);
		for (Plan plan : person.getPlans()) {
			this.startPlan(plan, out);
			// act/leg
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					Activity act = (Activity) pe;
					this.writeAct(act, out);
				}
				else if (pe instanceof Leg) {
					Leg leg = (Leg) pe;
					this.startLeg(leg, out);
					// route
					Route route = leg.getRoute();
					if (route != null) {
						this.startRoute(route, out);
						this.endRoute(out);
					}
					this.endLeg(out);
				}
			}
			this.endPlan(out);
		}
		this.endPerson(out);
		this.writeSeparator(out);
		out.flush();
	}

	@Override
	public void endPlans(final BufferedWriter out) throws IOException {
		out.write("</population>\n");
	}

	private void startPerson(final Person p, final BufferedWriter out) throws IOException {
		out.write("\t<person id=\"");
		out.write(p.getId().toString());
		out.write("\"");
		if (p instanceof PersonImplPops) {
			PersonImplPops person = (PersonImplPops)p;
			if (person.getPopulationId() != null && !person.getPopulationId().equals(PersonImplPops.DEFAULT_POP_ID)) {
				out.write(" pop_id=\"");
				out.write(person.getPopulationId().toString());
				out.write("\"");
			}
			if (PersonUtils.getSex(person) != null) {
				out.write(" sex=\"");
				out.write(PersonUtils.getSex(person));
				out.write("\"");
			}
			if (PersonUtils.getAge(person) != Integer.MIN_VALUE) {
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

	private void endPerson(final BufferedWriter out) throws IOException {
		out.write("\t</person>\n\n");
	}

	private void startPlan(final Plan plan, final BufferedWriter out) throws IOException {
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

	private void endPlan(final BufferedWriter out) throws IOException {
		out.write("\t\t</plan>\n\n");
	}

	private void writeAct(final Activity act, final BufferedWriter out) throws IOException {
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
				out.write(" max_dur=\"");
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

	private void startLeg(final Leg leg, final BufferedWriter out) throws IOException {
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

	private void endLeg(final BufferedWriter out) throws IOException {
		out.write("\t\t\t</leg>\n");
	}

	private void startRoute(final Route route, final BufferedWriter out) throws IOException {
		out.write("\t\t\t\t<route ");

		if (route instanceof GenericRoute) {
			out.write("type=\"");
			out.write(((GenericRoute) route).getRouteType());
			out.write("\">");
			String rd = ((GenericRoute) route).getRouteDescription();
			if (rd != null) {
				out.write(rd);
			}
		}
		else if (route instanceof NetworkRoute) {
			NetworkRoute nr = (NetworkRoute) route;
			
			if ( nr.getVehicleId()!=null ) {
				out.write("vehicleRefId=\""+ nr.getVehicleId() +"\" ") ;
			}
			
			out.write("type=\"links\">");
			out.write(nr.getStartLinkId().toString());
			for (Id<Link> linkId : nr.getLinkIds()) {
				out.write(" ");
				out.write(linkId.toString());
			}
			// If the start links equals the end link additionally check if its is a round trip. 
			if (!nr.getEndLinkId().equals(nr.getStartLinkId()) || nr.getLinkIds().size() > 0) {
				out.write(" ");
				out.write(nr.getEndLinkId().toString());
			}
		}
	}

	private void endRoute(final BufferedWriter out) throws IOException {
		out.write("</route>\n");
	}

	@Override
	public void writeSeparator(final BufferedWriter out) throws IOException {
		out.write("<!-- ====================================================================== -->\n\n");
	}

}
