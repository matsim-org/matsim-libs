package org.matsim.contrib.vsp.pt.fare;

import jakarta.validation.constraints.PositiveOrZero;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Map;
import java.util.stream.Stream;

public class PtFareConfigGroup extends ReflectiveConfigGroup {
	public static final String PT_FARE = "pt fare";
	public static final String MODULE_NAME = "ptFare";
	public static final String APPLY_UPPER_BOUND = "applyUpperBound";
	public static final String UPPER_BOUND_FACTOR = "upperBoundFactor";

	public static final String UPPER_BOUND_FACTOR_CMT = "When upper bound is applied, upperBound  = upperBoundFactor * max Fare of the day. " +
		"This value is decided by the ratio between average daily cost of a ticket subscription and the single " +
		"trip ticket of the same trip. Usually this value should be somewhere between 1.0 and 2.0";
	public static final String APPLY_UPPER_BOUND_CMT = "Enable the upper bound for daily PT fare to count for ticket subscription. Input value: " +
		"true" +
		" or false";

	private boolean applyUpperBound = true;
	@PositiveOrZero
	private double upperBoundFactor = 1.5;

	public PtFareConfigGroup() {
		super(MODULE_NAME);
	}

	public void addPtFareParameterSet(PtFareParams ptFareParams) {
		addParameterSet(ptFareParams);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(APPLY_UPPER_BOUND, APPLY_UPPER_BOUND_CMT);
		map.put(UPPER_BOUND_FACTOR, UPPER_BOUND_FACTOR_CMT);
		return map;
	}

	@StringGetter(APPLY_UPPER_BOUND)
	public boolean getApplyUpperBound() {
		return applyUpperBound;
	}

	@StringSetter(APPLY_UPPER_BOUND)
	public void setApplyUpperBound(boolean applyUpperBound) {
		this.applyUpperBound = applyUpperBound;
	}


	@StringGetter(UPPER_BOUND_FACTOR)
	public double getUpperBoundFactor() {
		return upperBoundFactor;
	}

	@StringSetter(UPPER_BOUND_FACTOR)
	public void setUpperBoundFactor(double upperBoundFactor) {
		this.upperBoundFactor = upperBoundFactor;
	}

	@Override
	public ConfigGroup createParameterSet(final String type) {
		return switch (type) {
			case DistanceBasedPtFareParams.SET_TYPE -> new DistanceBasedPtFareParams();
			case FareZoneBasedPtFareParams.SET_TYPE -> new FareZoneBasedPtFareParams();
			default -> throw new IllegalArgumentException(type);
		};
	}

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);

		var distanceBasedParameterSets = getParameterSets(DistanceBasedPtFareParams.SET_TYPE);
		var fareZoneBasedParameterSets = getParameterSets(FareZoneBasedPtFareParams.SET_TYPE);

		if (distanceBasedParameterSets.isEmpty() && fareZoneBasedParameterSets.isEmpty()) {
			throw new IllegalArgumentException("No parameter sets found for pt fare calculation. Please add at least one parameter set.");
		}

		long distinctOrders = Stream.concat(distanceBasedParameterSets.stream(), fareZoneBasedParameterSets.stream())
									.map(PtFareParams.class::cast)
									.map(PtFareParams::getOrder)
									.distinct().count();

		if (distinctOrders != distanceBasedParameterSets.size() + fareZoneBasedParameterSets.size()) {
			throw new IllegalArgumentException("Duplicate order values found in parameter sets. Please make sure that order values are unique.");
		}
	}
}
