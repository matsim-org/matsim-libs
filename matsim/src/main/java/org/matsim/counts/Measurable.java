package org.matsim.counts;

import it.unimi.dsi.fastutil.ints.Int2DoubleAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;

import java.util.OptionalDouble;

/**
 * A MultiModeCount station can hold any kind of measurable data to calibrate a scenario provided as an implementation of this interface.
 * A single instance holds values for only one transport mode.
 * Average velocities and traffic volumes are already implemented.
 */
public class Measurable {

	public static final String ELEMENT_NAME = "measurable";

	public static String VOLUMES = "volumes";
	public static String VELOCITIES = "velocities";
	public static String PASSENGERS = "passengers";

	private final String type;
	private final String mode;

	private final Int2DoubleMap values;

	/**
	 * Measurement interval in minutes.
	 */
	private final int interval;

	Measurable(String mode, String type, int interval) {
		this.mode = mode;
		this.type = type;
		this.values = new Int2DoubleAVLTreeMap();
		this.interval = interval;
	}

	Int2DoubleMap getValues() {
		return values;
	}

	public void setDailyValue(double value){
		setAtMinute(24 * 60, value);
	}

	/**
	 * Adds a value observed at a certain hour.
	 */
	public void setAtHour(int hour, double value) {
		setAtMinute(hour * 60, value);
	}

	/**
	 * Adds a value observed at a certain minute. Note that the minute must match the given interval, for example if the intervall is set to 15 minutes
	 * the minute must be something like 15, 30, 45, 300 etc.
	 */
	public void setAtMinute(int minute, double value) {
		if (minute % this.interval != 0)
			throw new RuntimeException("Time value doesn't match the interval!");



		this.values.put(minute, value);
	}

	/**
	 * Returns the observed daily value.
	 */
	public OptionalDouble getDailyValue() {
		return getAtHour(60 * 24);
	}

	/**
	 * Returns the observed value at a certain hour.
	 */
	public OptionalDouble getAtHour(int hour) {
		return getAtMinute(hour * 60);
	}

	/**
	 * Returns the observed value at a certain minute.
	 */
	public OptionalDouble getAtMinute(int minutes) {
		if (values.containsKey(minutes))
			return OptionalDouble.of(values.get(minutes));

		return OptionalDouble.empty();
	}

	/**
	 * Returns the transport mode of the observed data.
	 */
	public String getMode() {
		return mode;
	}

	/**
	 * Returns the name of the implementation. Information is needed for data writing.
	 */
	public String getMeasurableType() {
		return type;
	}

	public int getInterval() {
		return interval;
	}
}

