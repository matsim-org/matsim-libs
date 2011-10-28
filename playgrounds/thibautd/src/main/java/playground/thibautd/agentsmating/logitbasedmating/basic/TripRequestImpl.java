/* *********************************************************************** *
 * project: org.matsim.*
 * TripRequestImpl.java
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
package playground.thibautd.agentsmating.logitbasedmating.basic;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.population.Activity;

import playground.thibautd.agentsmating.logitbasedmating.framework.Alternative;
import playground.thibautd.agentsmating.logitbasedmating.framework.DecisionMaker;
import playground.thibautd.agentsmating.logitbasedmating.framework.TripRequest;
import playground.thibautd.agentsmating.logitbasedmating.framework.UnexistingAttributeException;

/**
 * default implementation of a TripRequest
 * @author thibautd
 */
public class TripRequestImpl implements TripRequest {
	public static final String DRIVER_MODE = "driver";
	public static final String PASSENGER_MODE = "passenger";

	private final Alternative alternativeDelegate;
	private final Type type;
	private final DecisionMaker decisionMaker;
	private final List<Alternative> alternatives;
	private final int indexInPlan;
	//private final Id origin;
	//private final Id destination;
	private final Activity origin;
	private final Activity destination;
	//private final double departureTime;
	private final double arrivalTime;

	public TripRequestImpl(
			final String mode,
			final Map<String, Object> attributes,
			final int indexInPlan,
			final Activity origin,
			final Activity destination,
			final double arrivalTime,
			final DecisionMaker decisionMaker,
			final List<Alternative> otherAlternatives) {
		this(new AlternativeImpl(mode, attributes),
			indexInPlan,
			origin,
			destination,
			arrivalTime,
			decisionMaker,
			otherAlternatives);
	}

	public TripRequestImpl(
			final Alternative baseAlternative,
			final int indexInPlan,
			final Activity origin,
			final Activity destination,
			final double arrivalTime,
			final DecisionMaker decisionMaker,
			final List<Alternative> otherAlternatives) {
		this.alternativeDelegate = baseAlternative;
		this.type = stringToType( baseAlternative.getMode() );
		this.decisionMaker = decisionMaker;
		this.alternatives = otherAlternatives;
		this.indexInPlan = indexInPlan;
		this.origin = origin;
		this.destination = destination;
		this.arrivalTime = arrivalTime;
	}

	private Type stringToType( final String mode ) {
		if ( mode.equals( DRIVER_MODE ) ) {
			return Type.DRIVER;
		}
		if ( mode.equals( PASSENGER_MODE ) ) {
			return Type.PASSENGER;
		}
		throw new IllegalArgumentException("unhandled mode: "+mode);
	}

	@Override
	public String getMode() {
		return alternativeDelegate.getMode();
	}

	@Override
	public double getAttribute(final String attribute)
			throws UnexistingAttributeException {
		return alternativeDelegate.getAttribute(attribute);
	}

	@Override
	public Map<String, Object> getAttributes() {
		return alternativeDelegate.getAttributes();
	}

	@Override
	public Type getTripType() {
		return type;
	}

	@Override
	public List<Alternative> getAlternatives() {
		return alternatives;
	}

	@Override
	public DecisionMaker getDecisionMaker() {
		return decisionMaker;
	}

	@Override
	public int getIndexInPlan() {
		return indexInPlan;
	}

	@Override
	public double getDepartureTime() {
		return this.origin.getEndTime();
	}

	@Override
	public double getPlanArrivalTime() {
		return arrivalTime;
	}

	@Override
	public Activity getOrigin() {
		return origin;
	}

	@Override
	public Activity getDestination() {
		return destination;
	}

	@Override
	public boolean equals(final Object object) {
		if ( !(object instanceof TripRequest) ) return false;
		TripRequest other = (TripRequest) object;

		return
			getDecisionMaker().equals( other.getDecisionMaker() ) &&
			( getIndexInPlan() == ( other.getIndexInPlan() ) ) &&
			getMode().equals( other.getMode() ) &&
			getTripType().equals( other.getTripType() ) &&
			getAttributes().equals( other.getAttributes() ) &&
			getAlternatives().equals( other.getAlternatives() ) &&
			getOrigin().equals( other.getOrigin() ) &&
			getDestination().equals( other.getDestination() ) &&
			( Math.abs( getDepartureTime() - other.getDepartureTime() ) < 1E-7 );
	}

	@Override
	public int hashCode() {
		return alternativeDelegate.hashCode();
	}
}

