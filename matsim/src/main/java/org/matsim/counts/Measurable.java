package org.matsim.counts;

import it.unimi.dsi.fastutil.ints.Int2DoubleAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;

import java.util.Iterator;
import java.util.Objects;
import java.util.OptionalDouble;

/**
 * A MeasurementLocation station can hold any kind of measurable data to calibrate a scenario provided as an implementation of this interface.
 * A single instance holds values for only one transport mode.
 * Average velocities and traffic volumes are already implemented.
 */
public final class Measurable implements Iterable<Int2DoubleMap.Entry> {

	static final String ELEMENT_NAME = "measurements";

	public static String VOLUMES = "volumes";
	public static String VELOCITIES = "velocities";
	public static String PASSENGERS = "passengers";

	/**
	 * Daily interval in minutes.
	 */
	public static int DAILY = 1440;
	/**
	 * Hourly interval in minutes.
	 */
	public static int HOURLY = 60;
	/**
	 * Quarter hourly interval in minutes.
	 */
	public static int QUARTER_HOURLY = 15;

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
		if (minute <= 0)
			throw new IllegalArgumentException("Time value starts at 1 and *not* zero.");

		if (minute % this.interval != 0)
			throw new IllegalArgumentException("Time value doesn't match the interval!");


		this.values.put(minute, value);
	}

	/**
	 * Returns the observed daily value.
	 */
	public OptionalDouble getDailyValue() {
		if (interval == DAILY)
			return getAtHour(24);

		throw new IllegalArgumentException("Does not contain daily values!");
	}

	public void setDailyValue(double value) {
		if (interval != DAILY)
			throw new IllegalArgumentException("Does not contain daily values!");

		setAtHour(24, value);
	}

	/**
	 * Return sum of values or daily value.
	 */
	public double getDailySum() {
		if (interval != DAILY)
			return getAtHour(24).orElse(0);

		return values.values().doubleStream().sum();
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

	/**
	 * Iterate over all values as minute-value pairs.
	 */
	@Override
	public Iterator<Int2DoubleMap.Entry> iterator() {
		return values.int2DoubleEntrySet().iterator();
	}

	/**
	 * Number of entries.
	 */
	public int size() {
		return values.size();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Measurable entries = (Measurable) o;
		return interval == entries.interval && Objects.equals(type, entries.type) && Objects.equals(mode, entries.mode) && Objects.equals(values, entries.values);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, mode, values, interval);
	}
}

