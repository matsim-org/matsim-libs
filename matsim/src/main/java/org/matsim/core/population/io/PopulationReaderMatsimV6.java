/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.StageActivityTypeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacility;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.matsim.utils.objectattributes.ObjectAttributesConverter;
import org.matsim.utils.objectattributes.attributable.AttributesUtils;
import org.matsim.utils.objectattributes.attributable.AttributesXmlReaderDelegate;
import org.matsim.vehicles.Vehicle;
import org.xml.sax.Attributes;

import com.google.inject.Inject;

/**
 * A reader for plans files of MATSim according to <code>population_v6.dtd</code>.
 *
 * @author thibautd
 * @author mrieser
 * @author balmermi
 */
/* deliberately package */ class PopulationReaderMatsimV6 extends MatsimXmlParser implements MatsimReader {
    private static final Logger log = LogManager.getLogger(PopulationReaderMatsimV6.class);

	/* package */ final static String POPULATION = "population";
	/* package */ final static String PERSON = "person";
	/* package */ final static String ATTRIBUTES = "attributes";
	private final static String ATTRIBUTE = "attribute";
	private final static String PLAN = "plan";
	private final static String ACT = "activity";
	private final static String LEG = "leg";
	private final static String ROUTE = "route";

	private final static String ATTR_POPULATION_DESC = "desc";
	/* package */ final static String ATTR_PERSON_ID = "id";
	private final static String ATTR_PLAN_SCORE = "score";
	private final static String ATTR_PLAN_TYPE = "type";
	private final static String ATTR_PLAN_SELECTED = "selected";
	private final static String ATTR_ACT_TYPE = "type";
	private final static String ATTR_ACT_X = "x";
	private final static String ATTR_ACT_Y = "y";
	private static final String ATTR_ACT_Z = "z";
	private final static String ATTR_ACT_LINK = "link";
	private final static String ATTR_ACT_FACILITY = "facility";
	private final static String ATTR_ACT_STARTTIME = "start_time";
	private final static String ATTR_ACT_ENDTIME = "end_time";
	private final static String ATTR_ACT_MAXDUR = "max_dur";
	private final static String ATTR_LEG_MODE = "mode";
	private final static String ATTR_LEG_DEPTIME = "dep_time";
	private final static String ATTR_LEG_TRAVTIME = "trav_time";
//	private final static String ATTR_LEG_ARRTIME = "arr_time";
	private static final String ATTR_ROUTE_STARTLINK = "start_link";
	private static final String ATTR_ROUTE_ENDLINK = "end_link";

	private final static String VALUE_YES = "yes";
	private final static String VALUE_NO = "no";
	private final static String VALUE_UNDEF = "undef";

	// TODO: infrastructure to configure converters
	private final AttributesXmlReaderDelegate attributesReader = new AttributesXmlReaderDelegate();

	final Scenario scenario;
	final Population plans;
	private final String externalInputCRS;

	Person currperson = null;
	private Plan currplan = null;
	private Activity curract = null;
	private Leg currleg = null;
	private Route currRoute = null;
	private String routeDescription = null;
	private org.matsim.utils.objectattributes.attributable.Attributes currAttributes = null;

	private final String targetCRS;
	private CoordinateTransformation coordinateTransformation = new IdentityTransformation();

	private Activity prevAct = null;


    PopulationReaderMatsimV6(
            final String inputCRS,
			final String targetCRS,
			final Scenario scenario) {
			super(ValidationType.DTD_ONLY);
			this.externalInputCRS = inputCRS;
			this.targetCRS = targetCRS;
			this.scenario = scenario;
			this.plans = scenario.getPopulation();
			if (targetCRS != null && externalInputCRS !=null) {
				this.coordinateTransformation = TransformationFactory.getCoordinateTransformation(externalInputCRS, targetCRS);
				ProjectionUtils.putCRS(this.plans, targetCRS);
			}
	}

	public ObjectAttributesConverter getObjectAttributesConverter()
	{
		return attributesReader.getObjectAttributesConverter();
	}

	public void putAttributeConverter( final Class<?> clazz , AttributeConverter<?> converter ) {
		attributesReader.putAttributeConverter( clazz , converter );
	}

	@Inject
	public void putAttributeConverters( final Map<Class<?>, AttributeConverter<?>> converters ) {
		attributesReader.putAttributeConverters( converters );
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		switch( name ) {
			case POPULATION:
				startPopulation(atts);
				break;
			case PERSON:
				startPerson(atts);
				break;
			case ATTRIBUTES:
				switch( context.peek() ) {
					case POPULATION:
						currAttributes = scenario.getPopulation().getAttributes();
						break;
					case PERSON:
						currAttributes = currperson.getAttributes();
						break;
					case PLAN:
						currAttributes = currplan.getAttributes();
						break;
					case ACT:
						currAttributes = curract.getAttributes();
						break;
					case LEG:
						currAttributes = new org.matsim.utils.objectattributes.attributable.AttributesImpl();
						break;
					default:
						throw new RuntimeException( context.peek() );
				}
				// deliberate fall-through
			case ATTRIBUTE:
				attributesReader.startTag( name , atts ,context , currAttributes );
				break;
			case PLAN:
				startPlan(atts);
				break;
			case ACT:
				startAct(atts);
				break;
			case LEG:
				startLeg(atts);
				break;
			case ROUTE:
				startRoute(atts);
				break;
			default:
				throw new RuntimeException(this + "[tag=" + name + " not known or not supported]");
		}
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
		switch ( name ) {
			case PERSON:
				this.plans.addPerson(this.currperson);
				this.currperson = null;
				break;
			case ATTRIBUTE:
				this.attributesReader.endTag( name , content , context );
				break;
			case ATTRIBUTES:
				switch( context.peek() ) {
					case POPULATION:
						String inputCRS = ProjectionUtils.getCRS(scenario.getPopulation());

						if (inputCRS != null && targetCRS != null) {
							if (externalInputCRS != null) {
								// warn or crash?
								log.warn("coordinate transformation defined both in config and in input file: setting from input file will be used");
							}
							coordinateTransformation = TransformationFactory.getCoordinateTransformation(inputCRS, targetCRS);
							ProjectionUtils.putCRS(scenario.getPopulation(), targetCRS);
						}
						break;
					case LEG:
						Object routingMode = currAttributes.getAttribute(TripStructureUtils.routingMode);
						if (Objects.nonNull(routingMode) && routingMode instanceof String) {
							currAttributes.removeAttribute(TripStructureUtils.routingMode);
							currleg.setRoutingMode((String) routingMode);
						}
						AttributesUtils.copyTo(currAttributes, currleg.getAttributes());
						break;
				}
				break;
			case PLAN:
				if (this.currplan.getPlanElements() instanceof ArrayList<?>) {
					((ArrayList<?>) this.currplan.getPlanElements()).trimToSize();
				}
				this.currplan = null;
				break;
			 case ACT:
				this.prevAct = this.curract;
				this.curract = null;
				 break;
			case ROUTE:
				endRoute(content);
				break;
		}
	}

	private void startPopulation(final Attributes atts) {
		this.plans.setName(atts.getValue(ATTR_POPULATION_DESC));
	}

	private void startPerson(final Attributes atts) {
		this.currperson = PopulationUtils.getFactory().createPerson(Id.create(atts.getValue(ATTR_PERSON_ID), Person.class));
	}

	private void startPlan(final Attributes atts) {
		String sel = atts.getValue(ATTR_PLAN_SELECTED);
		boolean selected;
		if (VALUE_YES.equals(sel)) {
			selected = true;
		}
		else if (VALUE_NO.equals(sel)) {
			selected = false;
		}
		else {
			throw new IllegalArgumentException(
					"Attribute 'selected' of Element 'Plan' is neither 'yes' nor 'no', is " + sel);
		}
		this.routeDescription = null;
		this.currplan = PersonUtils.createAndAddPlan(this.currperson, selected);

		String scoreString = atts.getValue(ATTR_PLAN_SCORE);
		if (scoreString != null) {
			double score = Double.parseDouble(scoreString);
			this.currplan.setScore(score);
		}

		String type = atts.getValue(ATTR_PLAN_TYPE);
		if (type != null) {
			this.currplan.setType(type);
		}
	}

	private void startAct(final Attributes atts) {
		final String actType = atts.getValue(ATTR_ACT_TYPE);
		final boolean isStageActivity = StageActivityTypeIdentifier.isStageActivity(actType);
		if (atts.getValue(ATTR_ACT_FACILITY) != null) {
			final Id<ActivityFacility> facilityId = Id.create(atts.getValue(ATTR_ACT_FACILITY), ActivityFacility.class);
			if (isStageActivity) {
				this.curract = PopulationUtils.createInteractionActivityFromFacilityId(actType, facilityId);
			} else {
				this.curract = PopulationUtils.createActivityFromFacilityId(actType, facilityId);
			}
			if (atts.getValue(ATTR_ACT_LINK) != null) {
				final Id<Link> linkId = Id.create(atts.getValue(ATTR_ACT_LINK), Link.class);
				this.curract.setLinkId(linkId);
			}
			if ((atts.getValue(ATTR_ACT_X) != null) && (atts.getValue(ATTR_ACT_Y) != null)) {
				final Coord coord = parseCoord(atts);
				this.curract.setCoord(coord);
			}
		} else if (atts.getValue(ATTR_ACT_LINK) != null) {
			Id<Link> linkId = Id.create(atts.getValue(ATTR_ACT_LINK), Link.class);
			if (isStageActivity) {
				this.curract = PopulationUtils.createInteractionActivityFromLinkId(actType, linkId);
			} else {
				this.curract = PopulationUtils.createActivityFromLinkId(actType, linkId);
			}
			if ((atts.getValue(ATTR_ACT_X) != null) && (atts.getValue(ATTR_ACT_Y) != null)) {
				final Coord coord = parseCoord(atts);
				this.curract.setCoord(coord);
			}
		} else if ((atts.getValue(ATTR_ACT_X) != null) && (atts.getValue(ATTR_ACT_Y) != null)) {
			final Coord coord = parseCoord(atts);
			if (isStageActivity) {
				this.curract = PopulationUtils.createInteractionActivityFromCoord(actType, coord);
			} else {
				this.curract = PopulationUtils.createActivityFromCoord(actType, coord);
			}
		} else {
			throw new IllegalArgumentException("In this version of MATSim either the facility, the link or the coords must be specified for an Act.");
		}

		final OptionalTime startTime = Time.parseOptionalTime(atts.getValue(ATTR_ACT_STARTTIME));
		final OptionalTime duration = Time.parseOptionalTime(atts.getValue(ATTR_ACT_MAXDUR));
		final OptionalTime endTime = Time.parseOptionalTime(atts.getValue(ATTR_ACT_ENDTIME));

		// Check whether the given times match the assumptions made in InteractionActivity. Otherwise, convert it to a regular Activity.
		if (isStageActivity && (startTime.isDefined() || endTime.isDefined() || duration.isUndefined() || duration.seconds() > 0.0)) {
			this.curract = PopulationUtils.createActivity(this.curract);
		} else {
			startTime.ifDefinedOrElse(this.curract::setStartTime, this.curract::setStartTimeUndefined);
			duration.ifDefinedOrElse(this.curract::setMaximumDuration, this.curract::setMaximumDurationUndefined);
			endTime.ifDefinedOrElse(this.curract::setEndTime, this.curract::setEndTimeUndefined);
		}
		this.currplan.addActivity(this.curract);

		if (this.routeDescription != null) {
			finishLastRoute();
		}
	}

	private Coord parseCoord(Attributes atts) {
		if ( atts.getValue( ATTR_ACT_Z ) != null ) {
			return coordinateTransformation.transform(
					new Coord(
							Double.parseDouble(atts.getValue(ATTR_ACT_X)),
							Double.parseDouble(atts.getValue(ATTR_ACT_Y)),
							Double.parseDouble(atts.getValue(ATTR_ACT_Z)) ) );
		}
		else {
			return coordinateTransformation.transform(
					new Coord(
							Double.parseDouble(atts.getValue(ATTR_ACT_X)),
							Double.parseDouble(atts.getValue(ATTR_ACT_Y))));
		}
	}

	private void finishLastRoute() {
		Id<Link> startLinkId = null;
		if (this.currRoute.getStartLinkId() != null) {
			startLinkId = this.currRoute.getStartLinkId();
		} else if (this.prevAct.getLinkId() != null) {
			startLinkId = this.prevAct.getLinkId();
		}
		Id<Link> endLinkId = null;
		if (this.currRoute.getEndLinkId() != null) {
			endLinkId = this.currRoute.getEndLinkId();
		} else if (this.curract != null && this.curract.getLinkId() != null) {
			endLinkId = this.curract.getLinkId();
		}

		this.currRoute.setStartLinkId(startLinkId);
		this.currRoute.setEndLinkId(endLinkId);
		this.currRoute.setRouteDescription(this.routeDescription.trim());

		// yy I think that my intuition would be to put the following into prepareForSim. kai, dec'16
		if (Double.isNaN(this.currRoute.getDistance())) {
			if (this.currRoute instanceof NetworkRoute) {
				if (!this.scenario.getNetwork().getLinks().isEmpty()) {
					this.currRoute.setDistance(RouteUtils.calcDistanceExcludingStartEndLink((NetworkRoute) this.currRoute, this.scenario.getNetwork()));
				}
			} else {
				Coord fromCoord = getCoord(this.prevAct);
				Coord toCoord = getCoord(this.curract);
				if (fromCoord != null && toCoord != null) {
					double dist = CoordUtils.calcEuclideanDistance(fromCoord, toCoord);
					if ( this.scenario.getConfig().routing().
							getModeRoutingParams().containsKey(  this.currleg.getMode()  ) ) {
						double estimatedNetworkDistance = dist * this.scenario.getConfig().routing().
								getModeRoutingParams().get( this.currleg.getMode() ).getBeelineDistanceFactor() ;
						this.currRoute.setDistance(estimatedNetworkDistance);
					}
				}
			}
		}
		if (this.currRoute.getTravelTime().isUndefined()) {
			this.currleg.getTravelTime().ifDefined(this.currRoute::setTravelTime);
		}

		this.routeDescription = null;
		this.currRoute = null;

	}

	private Coord getCoord(Activity activity) {
		// yy I think that my intuition would be to put the following into prepareForSim. kai, dec'16
		if (activity == null) {
			return null;
		}
		Coord fromCoord;
		if (activity.getCoord() != null) {
			fromCoord = activity.getCoord();
		} else {
			if (!this.scenario.getNetwork().getLinks().isEmpty()) {
				fromCoord = this.scenario.getNetwork().getLinks().get(activity.getLinkId()).getCoord();
			} else {
				fromCoord = null;
			}
		}
		return fromCoord;
	}

	private void startLeg(final Attributes atts) {
		if (this.routeDescription != null) {
			finishLastRoute();
		}

		String mode = atts.getValue(ATTR_LEG_MODE);
		if (VALUE_UNDEF.equals(mode)) {
			mode = "undefined";
		}
		this.currleg = PopulationUtils.createAndAddLeg( this.currplan, mode.intern() );
		Time.parseOptionalTime(atts.getValue(ATTR_LEG_DEPTIME))
				.ifDefinedOrElse(currleg::setDepartureTime, currleg::setDepartureTimeUndefined);
		Time.parseOptionalTime(atts.getValue(ATTR_LEG_TRAVTIME))
				.ifDefinedOrElse(currleg::setTravelTime, currleg::setTravelTimeUndefined);
//		LegImpl r = this.currleg;
//		r.setTravelTime( Time.parseTime(atts.getValue(ATTR_LEG_ARRTIME)) - r.getDepartureTime() );
		// arrival time is in dtd, but no longer evaluated in code (according to not being in API).  kai, jun'16
	}

	private void startRoute(final Attributes atts) {
		String startLinkId = atts.getValue(ATTR_ROUTE_STARTLINK);
		String endLinkId = atts.getValue(ATTR_ROUTE_ENDLINK);
		String routeType = atts.getValue("type");

		if (routeType == null) {
			String legMode = this.currleg.getMode();
			if ("pt".equals(legMode)) {
				routeType = "experimentalPt1";
			} else if ("car".equals(legMode)) {
				//yyyy couldn't we check against all network modes of config here? paul, jul '24
				routeType = "links";
			} else {
				routeType = "generic";
			}
		}

		RouteFactories factory = this.scenario.getPopulation().getFactory().getRouteFactories();
		Class<? extends Route> routeClass = factory.getRouteClassForType(routeType);

		this.currRoute = this.scenario.getPopulation().getFactory().getRouteFactories().createRoute(routeClass, startLinkId == null ? null : Id.create(startLinkId, Link.class), endLinkId == null ? null : Id.create(endLinkId, Link.class));
		this.currleg.setRoute(this.currRoute);

		if (atts.getValue("trav_time") != null) {
			Time.parseOptionalTime(atts.getValue("trav_time"))
					.ifDefinedOrElse(currRoute::setTravelTime, currRoute::setTravelTimeUndefined);
		}

		if (atts.getValue("distance") != null) {
			this.currRoute.setDistance(Double.parseDouble(atts.getValue("distance")));
		}
		final String vehicleRefId = atts.getValue("vehicleRefId");
		if (vehicleRefId != null && !vehicleRefId.equals("null") && this.currRoute instanceof NetworkRoute ) {
			((NetworkRoute)this.currRoute).setVehicleId(Id.create(vehicleRefId, Vehicle.class));
		}
	}

	private void endRoute(final String content) {
		this.routeDescription = content;

		Id<Link> startLinkId = this.currRoute.getStartLinkId();
		Id<Link> endLinkId = this.currRoute.getEndLinkId();
		this.currRoute.setStartLinkId(startLinkId);
		this.currRoute.setEndLinkId(endLinkId);
		this.currRoute.setRouteDescription(this.routeDescription.trim());

		// yy I think that my intuition would be to put the following into prepareForSim. kai, dec'16
		if (Double.isNaN(this.currRoute.getDistance())) {
			if (this.currRoute instanceof NetworkRoute) {
				if (!this.scenario.getNetwork().getLinks().isEmpty()) {
					this.currRoute.setDistance(RouteUtils.calcDistanceExcludingStartEndLink((NetworkRoute) this.currRoute, this.scenario.getNetwork()));
				}
			} else {
				Coord fromCoord = getCoord(this.prevAct);
				Coord toCoord = getCoord(this.curract);
				if (fromCoord != null && toCoord != null) {
					double dist = CoordUtils.calcEuclideanDistance(fromCoord, toCoord);
					if ( this.scenario.getConfig().routing().
							getModeRoutingParams().containsKey(  this.currleg.getMode()  ) ) {
						double estimatedNetworkDistance = dist * this.scenario.getConfig().routing().
								getModeRoutingParams().get( this.currleg.getMode() ).getBeelineDistanceFactor() ;
						this.currRoute.setDistance(estimatedNetworkDistance);
					}
				}
			}
		}
		if (this.currRoute.getTravelTime().isUndefined()) {
			this.currleg.getTravelTime().ifDefined(this.currRoute::setTravelTime);
		}

		if (this.currRoute.getEndLinkId() != null) {
			// this route is complete
			this.currRoute = null;
			this.routeDescription = null;
		}
	}

}
