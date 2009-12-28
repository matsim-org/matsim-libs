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

import java.util.EnumSet;
import java.util.HashMap;
import java.util.zip.Adler32;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.config.groups.PlanomatConfigGroup.SimLegInterpretation;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.world.Location;

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
	public static final EnumSet<TransportMode> MODES_WITH_VARIABLE_TRAVEL_TIME = EnumSet.of(TransportMode.car);
	
	protected final TravelTime linkTravelTimeEstimator;
	protected final DepartureDelayAverageCalculator tDepDelayCalc;
	private final PlansCalcRoute plansCalcRoute;
	private final PlanomatConfigGroup.SimLegInterpretation simLegInterpretation;

	private boolean doLogging = false;

	private final static Logger logger = Logger.getLogger(LinearInterpolationLegTravelTimeEstimator.class);

	public LinearInterpolationLegTravelTimeEstimator(
			TravelTime linkTravelTimeEstimator,
			DepartureDelayAverageCalculator depDelayCalc,
			PlansCalcRoute plansCalcRoute,
			SimLegInterpretation simLegInterpretation) {
		super();
		this.linkTravelTimeEstimator = linkTravelTimeEstimator;
		tDepDelayCalc = depDelayCalc;
		this.plansCalcRoute = plansCalcRoute;
		this.simLegInterpretation = simLegInterpretation;
	}

	private class DynamicODMatrixEntry {

		private final Location origin;
		private final Location destination;
		private final TransportMode mode;
		private final double departureTime;
		private final int hash;

		public DynamicODMatrixEntry(Location origin, Location destination,
				TransportMode mode, double departureTime) {
			super();
			this.origin = origin;
			this.destination = destination;
			this.mode = mode;
			this.departureTime = departureTime;

			Adler32 adler32 = new Adler32();
			adler32.update(this.origin.getId().toString().getBytes());
			adler32.update(this.destination.getId().toString().getBytes());
			adler32.update(this.mode.toString().getBytes());
			adler32.update((int) this.departureTime);
			this.hash = (int) adler32.getValue();
		}

		@Override
		public boolean equals(Object obj) {

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

	private HashMap<DynamicODMatrixEntry, Double> dynamicODMatrix = new HashMap<DynamicODMatrixEntry, Double>();
	private HashMap<LegImpl, HashMap<TransportMode, Double>> travelTimeCache = new HashMap<LegImpl, HashMap<TransportMode, Double>>();

	protected double getInterpolation(double departureTime, ActivityImpl actOrigin, ActivityImpl actDestination, LegImpl legIntermediate) {

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

			DynamicODMatrixEntry entry = new DynamicODMatrixEntry(actOrigin.getLink(), actDestination.getLink(), legIntermediate.getMode(), samplingPoint);

			if (this.dynamicODMatrix.containsKey(entry)) {

				samplingPointsTravelTimes[ii] = this.dynamicODMatrix.get(entry);

				if (this.doLogging) {
					logger.info(Time.writeTime(samplingPoint) + "\t" + Time.writeTime(samplingPointsTravelTimes[ii]) + "\t" + " [from cache]");
				}

			} else {

				samplingPointsTravelTimes[ii] = this.simulateLegAndGetTravelTime(samplingPoint, actOrigin, actDestination, legIntermediate);

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

	protected double simulateLegAndGetTravelTime(double departureTime, ActivityImpl actOrigin, ActivityImpl actDestination, LegImpl legIntermediate) {

		double legTravelTimeEstimation = 0.0;
		// TODO clarify usage of departure delay calculator

		if (MODES_WITH_VARIABLE_TRAVEL_TIME.contains(legIntermediate.getMode())) {
			
			if (this.simLegInterpretation.equals(PlanomatConfigGroup.SimLegInterpretation.CharyparEtAlCompatible)) {
				legTravelTimeEstimation += this.linkTravelTimeEstimator.getLinkTravelTime(actOrigin.getLink(), departureTime);
			}
			legTravelTimeEstimation += this.plansCalcRoute.handleLeg(legIntermediate, actOrigin, actDestination, departureTime + legTravelTimeEstimation);
			if (this.simLegInterpretation.equals(PlanomatConfigGroup.SimLegInterpretation.CetinCompatible)) {
				legTravelTimeEstimation += this.linkTravelTimeEstimator.getLinkTravelTime(actDestination.getLink(), departureTime + legTravelTimeEstimation);
			}
			
		} else {
			
			HashMap<TransportMode, Double> legInformation = null; 
			if (this.travelTimeCache.containsKey(legIntermediate)) {
				legInformation = this.travelTimeCache.get(legIntermediate);
			} else {
				legInformation = new HashMap<TransportMode, Double>();
				this.travelTimeCache.put(legIntermediate, legInformation);
			}
			if (legInformation.containsKey(legIntermediate.getMode())) {
				legTravelTimeEstimation = legInformation.get(legIntermediate.getMode()).doubleValue();
			} else {
				legTravelTimeEstimation = this.plansCalcRoute.handleLeg(legIntermediate, actOrigin, actDestination, departureTime);
				legInformation.put(legIntermediate.getMode(), legTravelTimeEstimation);
			}
			
		}
		
		return legTravelTimeEstimation;

	}

	public double getLegTravelTimeEstimation(Id personId, double departureTime,
			ActivityImpl actOrigin, ActivityImpl actDestination,
			LegImpl legIntermediate, boolean doModifyLeg) {

		double legTravelTimeEstimation = 0.0;

		if (
				doModifyLeg || 
				(!MODES_WITH_VARIABLE_TRAVEL_TIME.contains(legIntermediate.getMode()))
				) {
			legTravelTimeEstimation = this.simulateLegAndGetTravelTime(departureTime, actOrigin, actDestination, legIntermediate);
		} else if (!doModifyLeg) {
			legTravelTimeEstimation = this.getInterpolation(departureTime, actOrigin, actDestination, legIntermediate);
		}

		return legTravelTimeEstimation;

	}

	public LegImpl getNewLeg(TransportMode mode, ActivityImpl actOrigin,
			ActivityImpl actDestination, int legPlanElementIndex, double departureTime) {
		// not implemented here
		return null;
	}

	public void initPlanSpecificInformation(PlanImpl plan) {
		// not implemented here
	}

}
