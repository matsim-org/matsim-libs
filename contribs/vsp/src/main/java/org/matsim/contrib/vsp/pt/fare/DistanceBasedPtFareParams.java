package org.matsim.contrib.vsp.pt.fare;

import jakarta.validation.constraints.PositiveOrZero;
import org.apache.commons.math.stat.regression.SimpleRegression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.*;

/**
 * @author Chengqi Lu (luchengqi7)
 * The parameters for the distance-based PT trip fare calculation.
 * The default values are set based on the fitting results of the trip and fare data collected on September 2021
 * The values are based on the standard unit of meter (m) and Euro (EUR)
 */
public class DistanceBasedPtFareParams extends PtFareParams {
	public static final DistanceBasedPtFareParams GERMAN_WIDE_FARE_2024 = germanWideFare2024();

	public static final String SET_TYPE = "ptFareCalculationDistanceBased";
	public static final String MIN_FARE = "minFare";

	private static final Logger log = LogManager.getLogger(DistanceBasedPtFareParams.class);

	@PositiveOrZero
	private double minFare = 0.0;

	public DistanceBasedPtFareParams() {
		super(SET_TYPE);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(MIN_FARE, "Minimum fare for a PT trip (e.g. Kurzstrecke/short distance ticket in cities, ticket for 1 zone in rural areas). " +
			"Default is 2.0EUR.");
		return map;
	}

	@StringGetter(MIN_FARE)
	public double getMinFare() {
		return minFare;
	}

	@StringSetter(MIN_FARE)
	public void setMinFare(double minFare) {
		this.minFare = minFare;
	}

	// in Deutschlandtarif, the linear function for the prices above 100km seem to have a different steepness
	// hence the following difference in data points
	// prices taken from https://deutschlandtarifverbund.de/wp-content/uploads/2024/07/20231201_TBDT_J_10_Preisliste_V07.pdf
	// TODO: This fare will change. We might need a way to select the fare of a specific year
	private static DistanceBasedPtFareParams germanWideFare2024() {
		final double MIN_FARE = 1.70;

		SimpleRegression under100kmTrip = new SimpleRegression();
		under100kmTrip.addData(1, MIN_FARE);
		under100kmTrip.addData(2, 1.90);
		under100kmTrip.addData(3, 2.00);
		under100kmTrip.addData(4, 2.10);
		under100kmTrip.addData(5, 2.20);
		under100kmTrip.addData(6, 3.20);
		under100kmTrip.addData(7, 3.70);
		under100kmTrip.addData(8, 3.80);
		under100kmTrip.addData(9, 3.90);
		under100kmTrip.addData(10, 4.10);
		under100kmTrip.addData(11, 5.00);
		under100kmTrip.addData(12, 5.40);
		under100kmTrip.addData(13, 5.60);
		under100kmTrip.addData(14, 5.80);
		under100kmTrip.addData(15, 5.90);
		under100kmTrip.addData(16, 6.40);
		under100kmTrip.addData(17, 6.50);
		under100kmTrip.addData(18, 6.60);
		under100kmTrip.addData(19, 6.70);
		under100kmTrip.addData(20, 6.90);
		under100kmTrip.addData(30, 9.90);
		under100kmTrip.addData(40, 13.70);
		under100kmTrip.addData(50, 16.30);
		under100kmTrip.addData(60, 18.10);
		under100kmTrip.addData(70, 20.10);
		under100kmTrip.addData(80, 23.20);
		under100kmTrip.addData(90, 26.20);
		under100kmTrip.addData(100, 28.10);

		SimpleRegression longDistanceTrip = new SimpleRegression();
		longDistanceTrip.addData(100, 28.10);
		longDistanceTrip.addData(200, 47.20);
		longDistanceTrip.addData(300, 59.70);
		longDistanceTrip.addData(400, 71.70);
		longDistanceTrip.addData(500, 83.00);
		longDistanceTrip.addData(600, 94.60);
		longDistanceTrip.addData(700, 106.30);
		longDistanceTrip.addData(800, 118.20);
		longDistanceTrip.addData(900, 130.10);
		longDistanceTrip.addData(1000, 141.00);
		longDistanceTrip.addData(1100, 148.60);
		longDistanceTrip.addData(1200, 158.10);
		longDistanceTrip.addData(1300, 169.20);
		longDistanceTrip.addData(1400, 179.80);
		longDistanceTrip.addData(1500, 190.10);
		longDistanceTrip.addData(1600, 201.50);
		longDistanceTrip.addData(1700, 212.80);
		longDistanceTrip.addData(1800, 223.30);
		longDistanceTrip.addData(1900, 233.90);
		longDistanceTrip.addData(2000, 244.00);

		var params = new DistanceBasedPtFareParams();

		DistanceClassLinearFareFunctionParams distanceClass100kmFareParams = params.getOrCreateDistanceClassFareParams(100_000.);
		// above values are per km, fare deduction is based on meter instead
		distanceClass100kmFareParams.setFareSlope(under100kmTrip.getSlope() / 1000.0);
		distanceClass100kmFareParams.setFareIntercept(under100kmTrip.getIntercept());

		DistanceClassLinearFareFunctionParams distanceClassLongFareParams = params.getOrCreateDistanceClassFareParams(Double.POSITIVE_INFINITY);
		distanceClassLongFareParams.setFareSlope(longDistanceTrip.getSlope() / 1000.0);
		distanceClassLongFareParams.setFareIntercept(longDistanceTrip.getIntercept());

		params.setTransactionPartner("Deutschlandtarif");
		params.setMinFare(MIN_FARE);

		return params;
	}

	@Override
	public ConfigGroup createParameterSet(final String type) {
		switch (type) {
			case DistanceClassLinearFareFunctionParams.SET_TYPE:
				return new DistanceClassLinearFareFunctionParams();
			default:
				throw new IllegalArgumentException(type);
		}
	}

	@Override
	protected final void checkConsistency(final Config config) {
		super.checkConsistency(config);
		getDistanceClassFareParams();
	}

	public SortedMap<Double, DistanceClassLinearFareFunctionParams> getDistanceClassFareParams() {
		@SuppressWarnings("unchecked")
		final Collection<DistanceClassLinearFareFunctionParams> distanceClassFareParams =
			(Collection<DistanceClassLinearFareFunctionParams>) getParameterSets(DistanceClassLinearFareFunctionParams.SET_TYPE);
		final SortedMap<Double, DistanceClassLinearFareFunctionParams> map = new TreeMap<>();

		for (DistanceClassLinearFareFunctionParams pars : distanceClassFareParams) {
			if (this.isLocked()) {
				pars.setLocked();
			}
			if (map.containsKey(pars.getMaxDistance())) {
				log.error("Multiple " + DistanceClassLinearFareFunctionParams.class +
					" with same max distance in same DistanceBasedPtFareParams. Max distance must be unique.");
				throw new RuntimeException("Multiple " + DistanceClassLinearFareFunctionParams.class);
			}
			map.put(pars.getMaxDistance(), pars);
		}
		if (this.isLocked()) {
			return Collections.unmodifiableSortedMap(map);
		} else {
			return map;
		}
	}

	public DistanceClassLinearFareFunctionParams getOrCreateDistanceClassFareParams(double maxDistance) {
		DistanceClassLinearFareFunctionParams distanceClassFareParams = this.getDistanceClassFareParams().get(maxDistance);
		if (distanceClassFareParams == null) {
			distanceClassFareParams = new DistanceClassLinearFareFunctionParams();
			distanceClassFareParams.setMaxDistance(maxDistance);
			addParameterSet(distanceClassFareParams);
		}
		return distanceClassFareParams;
	}

	public static class DistanceClassLinearFareFunctionParams extends ReflectiveConfigGroup {

		public static final String SET_TYPE = "distanceClassLinearFare";
		public static final String FARE_SLOPE = "fareSlope";
		public static final String FARE_INTERCEPT = "fareIntercept";
		public static final String MAX_DISTANCE = "maxDistance";

		@PositiveOrZero
		private double fareSlope;
		@PositiveOrZero
		private double fareIntercept;
		@PositiveOrZero
		private double maxDistance;

		public DistanceClassLinearFareFunctionParams() {
			super(SET_TYPE);
		}

		@StringGetter(FARE_SLOPE)
		public double getFareSlope() {
			return fareSlope;
		}

		@StringSetter(FARE_SLOPE)
		public void setFareSlope(double fareSlope) {
			this.fareSlope = fareSlope;
		}

		@StringGetter(FARE_INTERCEPT)
		public double getFareIntercept() {
			return fareIntercept;
		}


		@StringSetter(FARE_INTERCEPT)
		public void setFareIntercept(double fareIntercept) {
			this.fareIntercept = fareIntercept;
		}

		@StringGetter(MAX_DISTANCE)
		public double getMaxDistance() {
			return maxDistance;
		}

		@StringSetter(MAX_DISTANCE)
		public void setMaxDistance(double maxDistance) {
			this.maxDistance = maxDistance;
		}

		@Override
		public Map<String, String> getComments() {
			Map<String, String> map = super.getComments();
			map.put(FARE_SLOPE, "Linear function fare = slope * distance + intercept: the value of the slope factor in currency units / m" +
				"(not km!).");
			map.put(FARE_INTERCEPT, "Linear function fare = slope * distance + intercept: the value of the intercept in currency units.");
			map.put(MAX_DISTANCE, "The given linear function is applied to trips up to this distance threshold in meters. If set to a finite value" +
				", the linear function for the next distance class will be tried out. If no fare is defined with " + MAX_DISTANCE + " greater than" +
				"pt trip length, an error is thrown. If multiple distance classes have the same max distance it is unclear which applies, therefore " +
				"an error is thrown.");
			return map;
		}
	}
}
