/* *********************************************************************** *
 * project: org.matsim.*
 * LinearInterpolationLegTravelTimeEstimator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.planomat.costestimators;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.Adler32;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.config.groups.PlanomatConfigGroup.SimLegInterpretation;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;

/**
 *
 * Travel times for modes with variable travel times are approximated via a linear interpolation with sampling points at the full hours,
 * based on the travel times approximated by a routing algorithm.
 *
 * @author meisterk
 *
 * @deprecated This class is marked as deprecated because the algorithm has too a long computation time to be useful.
 *
 */
@Deprecated
public class LinearInterpolationLegTravelTimeEstimator implements
LegTravelTimeEstimator {

	public static final double SAMPLING_DISTANCE = 3600.0;
	/**
	 * Modes whose optimal routes and travel times are variable throughout the day, and therefore have to be approximated.
	 * They are in opposite to modes whose optimal routes and travel times are constant can be computed once and be cached afterwards.
	 *
	 * TODO possibly a MATSim config parameter
	 */
	public static final Set<String> MODES_WITH_VARIABLE_TRAVEL_TIME;

	protected final TravelTime linkTravelTimeEstimator;
	protected final DepartureDelayAverageCalculator tDepDelayCalc;
	private final PlansCalcRoute plansCalcRoute;
	private final PlanomatConfigGroup.SimLegInterpretation simLegInterpretation;
	private final Network network;

	private boolean doLogging = false;

	private final static Logger logger = Logger.getLogger(LinearInterpolationLegTravelTimeEstimator.class);

	static {
		MODES_WITH_VARIABLE_TRAVEL_TIME = new HashSet<String>();
		MODES_WITH_VARIABLE_TRAVEL_TIME.add(TransportMode.car);
	}

	public LinearInterpolationLegTravelTimeEstimator(
			TravelTime linkTravelTimeEstimator,
			DepartureDelayAverageCalculator depDelayCalc,
			PlansCalcRoute plansCalcRoute,
			SimLegInterpretation simLegInterpretation,
			Network network) {
		super();
		this.linkTravelTimeEstimator = linkTravelTimeEstimator;
		tDepDelayCalc = depDelayCalc;
		this.network = network;
		this.plansCalcRoute = plansCalcRoute;
		this.simLegInterpretation = simLegInterpretation;
	}

	private static class DynamicODMatrixEntry {

		private final BasicLocation origin;
		private final BasicLocation destination;
		private final String mode;
		private final double departureTime;
		private final int hash;

		public DynamicODMatrixEntry(BasicLocation origin, BasicLocation destination,
				String mode, double departureTime) {
			super();
			this.origin = origin;
			this.destination = destination;
			this.mode = mode;
			this.departureTime = departureTime;

			Adler32 adler32 = new Adler32();
			adler32.update(this.origin.getId().toString().getBytes());
			adler32.update(this.destination.getId().toString().getBytes());
			adler32.update(this.mode.getBytes());
			adler32.update((int) this.departureTime);
			this.hash = (int) adler32.getValue();
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof DynamicODMatrixEntry)) {
				return false;
			}
			DynamicODMatrixEntry other = (DynamicODMatrixEntry) obj;

			if (this.departureTime != other.departureTime) {
				return false;
			}
			if (!this.mode.equals(other.mode)) {
				return false;
			}
			if (!this.origin.getId().equals(other.origin.getId())) {
				return false;
			}
			if (!this.destination.getId().equals(other.destination.getId())) {
				return false;
			}

			return true;
		}

		@Override
		public int hashCode() {
			return this.hash;
		}

	}

	private final HashMap<DynamicODMatrixEntry, Double> dynamicODMatrix = new HashMap<DynamicODMatrixEntry, Double>();
	private final HashMap<Leg, HashMap<String, Double>> travelTimeCache = new HashMap<Leg, HashMap<String, Double>>();

	protected double getInterpolation(Person person, double departureTime, Activity actOrigin, Activity actDestination, Leg legIntermediate) {

		// get values at sampling points
		double samplingPoint = Double.MIN_VALUE;
		double[] samplingPointsTravelTimes = new double[2];
		for (int ii = 0; ii <= 1; ii++) {

			samplingPointsTravelTimes[ii] = 0.0;

			switch(ii) {
			case 0:
				samplingPoint = Math.floor(departureTime / LinearInterpolationLegTravelTimeEstimator.SAMPLING_DISTANCE) * LinearInterpolationLegTravelTimeEstimator.SAMPLING_DISTANCE;
				break;
			case 1:
				samplingPoint = Math.ceil(departureTime / LinearInterpolationLegTravelTimeEstimator.SAMPLING_DISTANCE) * LinearInterpolationLegTravelTimeEstimator.SAMPLING_DISTANCE;
				break;
			}

			Link originLink = this.network.getLinks().get(actOrigin.getLinkId());
			Link destinationLink = this.network.getLinks().get(actDestination.getLinkId());
			DynamicODMatrixEntry entry = new DynamicODMatrixEntry(originLink, destinationLink, legIntermediate.getMode(), samplingPoint);

			if (this.dynamicODMatrix.containsKey(entry)) {

				samplingPointsTravelTimes[ii] = this.dynamicODMatrix.get(entry);

				if (this.doLogging) {
					logger.info(Time.writeTime(samplingPoint) + "\t" + Time.writeTime(samplingPointsTravelTimes[ii]) + "\t" + " [from cache]");
				}

			} else {

				samplingPointsTravelTimes[ii] = this.simulateLegAndGetTravelTime(person, samplingPoint, actOrigin, actDestination, legIntermediate);

				this.dynamicODMatrix.put(entry, samplingPointsTravelTimes[ii]);

				if (this.doLogging) {
					logger.info(Time.writeTime(samplingPoint) + "\t" + Time.writeTime(samplingPointsTravelTimes[ii]) + "\t" + " [from router]");
				}
			}

		}

		// linear interpolation
		double m = (samplingPointsTravelTimes[1] - samplingPointsTravelTimes[0]) / LinearInterpolationLegTravelTimeEstimator.SAMPLING_DISTANCE;
		// use right-hand sampling point to calculate y_0
		double y_0 = samplingPointsTravelTimes[1] - m * samplingPoint;

		return m * departureTime + y_0;
	}

	public void resetPlanSpecificInformation() {
		this.dynamicODMatrix.clear();
		this.travelTimeCache.clear();

	}

	public void setDoLogging(boolean doLogging) {
		this.doLogging = doLogging;
	}

	protected double simulateLegAndGetTravelTime(Person person, double departureTime, Activity actOrigin, Activity actDestination, Leg legIntermediate) {

		double legTravelTimeEstimation = 0.0;
		// TODO clarify usage of departure delay calculator

		if (MODES_WITH_VARIABLE_TRAVEL_TIME.contains(legIntermediate.getMode())) {

			if (this.simLegInterpretation.equals(PlanomatConfigGroup.SimLegInterpretation.CharyparEtAlCompatible)) {
				legTravelTimeEstimation += this.linkTravelTimeEstimator.getLinkTravelTime(this.network.getLinks().get(actOrigin.getLinkId()), departureTime);
			}
			legTravelTimeEstimation += this.plansCalcRoute.handleLeg(person, legIntermediate, actOrigin, actDestination, departureTime + legTravelTimeEstimation);
			if (this.simLegInterpretation.equals(PlanomatConfigGroup.SimLegInterpretation.CetinCompatible)) {
				legTravelTimeEstimation += this.linkTravelTimeEstimator.getLinkTravelTime(this.network.getLinks().get(actDestination.getLinkId()), departureTime + legTravelTimeEstimation);
			}

		} else {

			HashMap<String, Double> legInformation = null;
			if (this.travelTimeCache.containsKey(legIntermediate)) {
				legInformation = this.travelTimeCache.get(legIntermediate);
			} else {
				legInformation = new HashMap<String, Double>();
				this.travelTimeCache.put(legIntermediate, legInformation);
			}
			if (legInformation.containsKey(legIntermediate.getMode())) {
				legTravelTimeEstimation = legInformation.get(legIntermediate.getMode()).doubleValue();
			} else {
				legTravelTimeEstimation = this.plansCalcRoute.handleLeg(person, legIntermediate, actOrigin, actDestination, departureTime);
				legInformation.put(legIntermediate.getMode(), legTravelTimeEstimation);
			}

		}

		return legTravelTimeEstimation;

	}

	@Override
	public double getLegTravelTimeEstimation(Id personId, double departureTime,
			Activity actOrigin, Activity actDestination,
			Leg legIntermediate, boolean doModifyLeg) {

		// TODO: get person from population
		Person person = null;

		double legTravelTimeEstimation = 0.0;

		if (
				doModifyLeg ||
				(!MODES_WITH_VARIABLE_TRAVEL_TIME.contains(legIntermediate.getMode()))
				) {
			legTravelTimeEstimation = this.simulateLegAndGetTravelTime(person, departureTime, actOrigin, actDestination, legIntermediate);
		} else if (!doModifyLeg) {
			legTravelTimeEstimation = this.getInterpolation(person, departureTime, actOrigin, actDestination, legIntermediate);
		}

		return legTravelTimeEstimation;

	}

	@Override
	public LegImpl getNewLeg(String mode, Activity actOrigin,
			Activity actDestination, int legPlanElementIndex, double departureTime) {
		// not implemented here
		return null;
	}

	public void initPlanSpecificInformation(PlanImpl plan) {
		// not implemented here
	}

}
