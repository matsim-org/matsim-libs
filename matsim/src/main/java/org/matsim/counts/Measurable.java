package org.matsim.counts;

import it.unimi.dsi.fastutil.ints.Int2DoubleAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleSortedMap;

import java.util.Iterator;
import java.util.Objects;
import java.util.OptionalDouble;

/**
 * A MeasurementLocation station can hold any kind of measurable data to calibrate a scenario provided as an implementation of this interface.
 * A single instance holds values for only one transport mode.
 * Average velocities and traffic volumes are already implemented.
 */
public final class Measurable implements Iterable<Int2DoubleMap.Entry> {

	/**
	 * String to denote that the mode includes all vehicles.
	 */
	public static final String ANY_MODE = "any_vehicle";
	static final String ELEMENT_NAME = "measurements";
	public static String VOLUMES = "volumes";
	public static String VELOCITIES = "velocities";
	public static String PASSENGERS = "passengers";

	/**
	 * Daily interval in seconds.
	 */
	public static int DAILY = 24 * 60 * 60;
	/**
	 * Hourly interval in seconds.
	 */
	public static int HOURLY = 60 * 60;
	/**
	 * Quarter hourly interval in seconds.
	 */
	public static int QUARTER_HOURLY = 15 * 60;

	private final String type;
	private final String mode;

	private final Int2DoubleSortedMap values;

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
		setAtSecond(hour * HOURLY, value);
	}

	/**
	 * Adds a value observed at a certain minute. Note that the minute must match the given interval, for example if the intervall is set to 15 minutes
	 * the minute must be something like 15, 30, 45, 300 etc.
	 */
	public void setAtMinute(int minute, double value) {
		setAtSecond(minute * 60, value);
	}

	public void setAtSecond(int seconds, double value) {
		if (seconds < 0)
			throw new IllegalArgumentException("Time value starts at 0.");

		if (seconds % this.interval != 0)
			throw new IllegalArgumentException("Time value doesn't match the interval!");

		this.values.put(seconds, value);
	}

	/**
	 * Returns the observed daily value.
	 */
	public OptionalDouble getDailyValue() {
		if (interval != DAILY)
			throw new IllegalArgumentException("Does not contain daily values!");

		return getAtHour(0);
	}

	public void setDailyValue(double value) {
		if (interval != DAILY)
			throw new IllegalArgumentException("Does not contain daily values!");

		setAtHour(0, value);
	}

	/**
	 * Return sum of values or daily value.
	 */
	public double aggregateDaily() {
		if (interval == DAILY)
			return values.get(0);

		return values.values().doubleStream().sum();
	}

	/**
	 * Returns the observed value at a certain hour.
	 */
	public OptionalDouble getAtHour(int hour) {
		return getAtSecond(hour * HOURLY);
	}

	/**
	 * Whether the interval allows for an hourly aggregation.
	 */
	public boolean supportsHourlyAggregate() {
		return (interval <= HOURLY) && (HOURLY % interval) == 0;
	}

	/**
	 * Returns the aggregate for an specific hour. If the resolution does not allow this aggregation an error is thrpwn.
	 */
	public OptionalDouble aggregateAtHour(int hour) {
		if (!supportsHourlyAggregate())
			throw new IllegalArgumentException("Can not aggregate hourly values.");

		if (interval == HOURLY)
			return getAtHour(hour);

		Int2DoubleSortedMap values = this.values.subMap(hour * HOURLY, (hour + 1) * HOURLY);
		if (values.isEmpty())
			return OptionalDouble.empty();

		return OptionalDouble.of(values.values().doubleStream().sum());
	}

	public OptionalDouble getAtSecond(int second) {
		if (values.containsKey(second))
			return OptionalDouble.of(values.get(second));

		return OptionalDouble.empty();
	}

	/**
	 * Returns the observed value at a certain minute.
	 */
	public OptionalDouble getAtMinute(int minutes) {
		return getAtSecond(minutes * 60);
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
	 * Iterate over all values as seconds-value pairs.
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

