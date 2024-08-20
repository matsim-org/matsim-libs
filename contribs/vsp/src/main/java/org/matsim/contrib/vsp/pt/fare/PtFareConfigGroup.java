package org.matsim.contrib.vsp.pt.fare;

import jakarta.validation.constraints.PositiveOrZero;
import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Map;
import java.util.stream.Stream;

public class PtFareConfigGroup extends ReflectiveConfigGroup {
	public static final String PT_FARE = "pt fare";
	public static final String MODULE_NAME = "ptFare";
	public static final String APPLY_DAILY_CAP = "applyDailyCap";
	public static final String DAILY_CAP_FACTOR = "dailyCapFactor";

	public static final String DAILY_CAP_FACTOR_CMT = "When daily cap is applied, daily PT fare cap  = dailyCapFactor * max Fare of the day. " +
		"This value is decided by the ratio between average daily cost of a ticket subscription and the single " +
		"trip ticket of the same trip. Usually this value should be somewhere between 1.0 and 2.0";
	public static final String APPLY_DAILY_CAP_CMT = "Enable the upper bound for daily PT fare to count for ticket subscription. Input value: " +
		"true" +
		" or false";

	private boolean applyDailyCap = true;
	@PositiveOrZero
	private double dailyCapFactor = 1.5;

	public PtFareConfigGroup() {
		super(MODULE_NAME);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(APPLY_DAILY_CAP, APPLY_DAILY_CAP_CMT);
		map.put(DAILY_CAP_FACTOR, DAILY_CAP_FACTOR_CMT);
		return map;
	}

	@StringGetter(APPLY_DAILY_CAP)
	public boolean isDailyCapApplied() {
		return applyDailyCap;
	}

	@StringSetter(APPLY_DAILY_CAP)
	public void setApplyDailyCap(boolean applyDailyCap) {
		this.applyDailyCap = applyDailyCap;
	}


	@StringGetter(DAILY_CAP_FACTOR)
	public double getDailyCapFactor() {
		return dailyCapFactor;
	}

	@StringSetter(DAILY_CAP_FACTOR)
	public void setDailyCapFactor(double dailyCapFactor) {
		this.dailyCapFactor = dailyCapFactor;
	}

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);

		var distanceBasedParameterSets = getParameterSets(DistanceBasedPtFareParams.SET_NAME);
		var fareZoneBasedParameterSets = getParameterSets(FareZoneBasedPtFareParams.SET_NAME);

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
