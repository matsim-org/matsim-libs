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
import java.util.zip.Adler32;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.config.groups.PlanomatConfigGroup.SimLegInterpretation;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.world.Location;

public class LinearInterpolationLegTravelTimeEstimator implements
LegTravelTimeEstimator {

	public static final double SAMPLING_DISTANCE = 3600.0;

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

	public double getLegTravelTimeEstimation(Id personId, double departureTime,
			ActivityImpl actOrigin, ActivityImpl actDestination,
			LegImpl legIntermediate) {

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
				
				// TODO clarify usage of departure delay calculator
				// TODO pt legs prodcued by the router are compatibe to cetin-like traffic flows simulations, but not to charypar et al like simulations 
				
				if (legIntermediate.getMode().equals(TransportMode.car)) {
					if (this.simLegInterpretation.equals(PlanomatConfigGroup.SimLegInterpretation.CharyparEtAlCompatible)) {
						samplingPointsTravelTimes[ii] += this.linkTravelTimeEstimator.getLinkTravelTime(actOrigin.getLink(), samplingPoint);
					}
				}
				samplingPointsTravelTimes[ii] += this.plansCalcRoute.handleLeg(legIntermediate, actOrigin, actDestination, samplingPoint + samplingPointsTravelTimes[ii]);
				if (legIntermediate.getMode().equals(TransportMode.car)) {
					if (this.simLegInterpretation.equals(PlanomatConfigGroup.SimLegInterpretation.CetinCompatible)) {
						samplingPointsTravelTimes[ii] += this.linkTravelTimeEstimator.getLinkTravelTime(actDestination.getLink(), samplingPoint + samplingPointsTravelTimes[ii]);
					}
				}

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

	public void reset() {
		this.dynamicODMatrix.clear();

	}

	public void setDoLogging(boolean doLogging) {
		this.doLogging = doLogging;
	}

}
