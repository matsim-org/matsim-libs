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

package org.matsim.core.population.io;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.misc.Time;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;
import org.matsim.utils.objectattributes.attributable.AttributesUtils;
import org.matsim.utils.objectattributes.attributable.AttributesXmlWriterDelegate;
import org.matsim.vehicles.Vehicle;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;

import static org.matsim.core.utils.io.XmlUtils.encodeAttributeValue;
import static org.matsim.core.utils.io.XmlUtils.encodeContent;

/**
 * @author thibautd
 * @author mrieser
 * @author balmermi
 * @author steffenaxer
 */
/*package*/ class ParallelPopulationCreatorV6 implements Runnable {
	//static final Logger LOG = LogManager.getLogger(ParallelPopulationCreatorV6.class);
	private final AttributesXmlWriterDelegate attributesWriter = new AttributesXmlWriterDelegate();
	private final CoordinateTransformation coordinateTransformation;
	private final BlockingQueue<ParallelPopulationWriterHandlerV6.PersonData> queue;
	private final StringBuilder routeDescStringBuilder = new StringBuilder(100);
	private final StringBuilder stringBuilder = new StringBuilder(100_000);
	private boolean finish = false;

	ParallelPopulationCreatorV6(CoordinateTransformation coordinateTransformation, BlockingQueue<ParallelPopulationWriterHandlerV6.PersonData> queue) {
		this.coordinateTransformation = coordinateTransformation;
		this.queue = queue;
	}

	private static void endPerson(final StringBuilder out)  {
		out.append("\t</person>\n\n");
	}

	private static void endPlan(final StringBuilder out) {
		out.append("\t\t</plan>\n\n");
	}

	private static void endLeg(final StringBuilder out)  {
		out.append("\t\t\t</leg>\n");
	}

	private void startRoute(final Route route, final StringBuilder out) {
		out.append("\t\t\t\t<route ");
		out.append("type=\"");
		out.append(encodeAttributeValue(route.getRouteType()));
		out.append("\"");
		out.append(" start_link=\"");
		out.append(encodeAttributeValue(route.getStartLinkId().toString()));
		out.append("\"");
		out.append(" end_link=\"");
		out.append(encodeAttributeValue(route.getEndLinkId().toString()));
		out.append("\"");
		out.append(" trav_time=\"");
		out.append(Time.writeTime(route.getTravelTime()));
		out.append("\"");
		out.append(" distance=\"");
		out.append(route.getDistance());
		out.append("\"");
		if (route instanceof NetworkRoute networkRoute) {
			out.append(" vehicleRefId=\"");
			final Id<Vehicle> vehicleId = networkRoute.getVehicleId();
			if (vehicleId == null) {
				out.append("null");
			} else {
				out.append(encodeAttributeValue(vehicleId.toString()));
			}
			out.append("\"");
		}
		out.append(">");

		String rd;
		if (route instanceof NetworkRoute networkRoute) {
			rd = getRouteDescriptionNetworkRoute(this.routeDescStringBuilder, networkRoute);
			this.routeDescStringBuilder.setLength(0);
		} else {
			rd = route.getRouteDescription();
		}
		if (rd != null) {
			out.append(encodeContent(rd));
		}
	}

	private static void endRoute(final StringBuilder out) {
		out.append("</route>\n");
	}

	public void putAttributeConverters(final Map<Class<?>, AttributeConverter<?>> converters) {
		this.attributesWriter.putAttributeConverters(converters);
	}

	private void process() throws IOException {
		do {
			//LOG.info("{} Current queue size # {}",this.getClass().getName() ,this.inputQueue.size());
			ParallelPopulationWriterHandlerV6.PersonData personData = this.queue.poll();
			if (personData != null) {
				Person person = personData.person();
				StringBuilder out = stringBuilder;

				this.startPerson(person, out);
				for (Plan plan : person.getPlans()) {
					startPlan(plan, out);
					// act/leg
					for (PlanElement pe : plan.getPlanElements()) {
						if (pe instanceof Activity act) {
							this.writeAct(act, out);
						} else if (pe instanceof Leg leg) {
							this.startLeg(leg, out);
							// route
							Route route = leg.getRoute();
							if (route != null) {
								startRoute(route, out);
								endRoute(out);
							}
							endLeg(out);
						}
					}
					endPlan(out);
				}
				endPerson(out);
				this.writeSeparator(out);
				CompletableFuture<String> completableFuture = personData.futurePersonString();
				completableFuture.complete(out.toString());

				// Reset stringBuilder instead of instantiate
				stringBuilder.setLength(0);
			}


		} while (!(this.queue.isEmpty() && finish));

	}

	public void finish() {
		this.finish = true;
	}

	private void startPerson(final Person person, final StringBuilder out) {
		out.append("\t<person id=\"");
		out.append(encodeAttributeValue(person.getId().toString()));
		out.append("\"");
		out.append(">\n");
		this.attributesWriter.writeAttributes("\t\t", out, person.getAttributes());
	}

	private void startPlan(final Plan plan, final StringBuilder out)  {
		out.append("\t\t<plan");
		if (plan.getScore() != null) {
			out.append(" score=\"");
			out.append(plan.getScore().toString());
			out.append("\"");
		}
		if (PersonUtils.isSelected(plan))
			out.append(" selected=\"yes\"");
		else
			out.append(" selected=\"no\"");
		if ((plan.getType() != null)) {
			out.append(" type=\"");
			out.append(encodeAttributeValue(plan.getType()));
			out.append("\"");
		}
		out.append(">\n");

		this.attributesWriter.writeAttributes("\t\t\t\t", out, plan.getAttributes());
	}

	private void writeAct(final Activity act, final StringBuilder out) {
		out.append("\t\t\t<activity type=\"");
		out.append(encodeAttributeValue(act.getType()));
		out.append("\"");
		if (act.getLinkId() != null) {
			out.append(" link=\"");
			out.append(encodeAttributeValue(act.getLinkId().toString()));
			out.append("\"");
		}
		if (act.getFacilityId() != null) {
			out.append(" facility=\"");
			out.append(encodeAttributeValue(act.getFacilityId().toString()));
			out.append("\"");
		}
		if (act.getCoord() != null) {
			final Coord coord = this.coordinateTransformation.transform(act.getCoord());
			out.append(" x=\"");
			out.append(coord.getX());
			out.append("\" y=\"");
			out.append(coord.getY());
			out.append("\"");

			if (act.getCoord().hasZ()) {
				out.append(" z=\"");
				out.append(coord.getZ());
				out.append("\"");
			}
		}
		if (act.getStartTime().isDefined()) {
			out.append(" start_time=\"");
			out.append(Time.writeTime(act.getStartTime().seconds()));
			out.append("\"");
		}
		if (act.getMaximumDuration().isDefined()) {
			out.append(" max_dur=\"");
			out.append(Time.writeTime(act.getMaximumDuration().seconds()));
			out.append("\"");
		}
		if (act.getEndTime().isDefined()) {
			out.append(" end_time=\"");
			out.append(Time.writeTime(act.getEndTime().seconds()));
			out.append("\"");
		}
		out.append(" >\n");

		this.attributesWriter.writeAttributes("\t\t\t\t", out, act.getAttributes());

		out.append("\t\t\t</activity>\n");
	}

	private void startLeg(final Leg leg, final StringBuilder out) {
		out.append("\t\t\t<leg mode=\"");
		out.append(encodeAttributeValue(leg.getMode()));
		out.append("\"");
		if (leg.getDepartureTime().isDefined()) {
			out.append(" dep_time=\"");
			out.append(Time.writeTime(leg.getDepartureTime().seconds()));
			out.append("\"");
		}
		if (leg.getTravelTime().isDefined()) {
			out.append(" trav_time=\"");
			out.append(Time.writeTime(leg.getTravelTime().seconds()));
			out.append("\"");
		}

		out.append(">\n");

		if (leg.getRoutingMode() != null) {
			Attributes attributes = new AttributesImpl();
			AttributesUtils.copyTo(leg.getAttributes(), attributes);
			attributes.putAttribute(TripStructureUtils.routingMode, leg.getRoutingMode());
			this.attributesWriter.writeAttributes("\t\t\t\t", out, attributes);
		} else this.attributesWriter.writeAttributes("\t\t\t\t", out, leg.getAttributes());
	}

	public void writeSeparator(final StringBuilder out) throws IOException {
		out.append("<!-- ====================================================================== -->\n\n");
	}

	@Override
	public void run() {
		try {
			this.process();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	// This method re-uses the existing StringBuilder
	public static String getRouteDescriptionNetworkRoute(StringBuilder desc, NetworkRoute route) {
		desc.append(route.getStartLinkId().toString());
		for (Id<Link> linkId : route.getLinkIds()) {
			desc.append(" ");
			desc.append(linkId.toString());
		}
		// If the start links equals the end link additionally check if its is a round trip.
		if (!route.getEndLinkId().equals(route.getStartLinkId()) || route.getLinkIds().size() > 0) {
			desc.append(" ");
			desc.append(route.getEndLinkId().toString());
		}
		return desc.toString();
	}
}
