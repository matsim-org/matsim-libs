package org.matsim.counts;

import it.unimi.dsi.fastutil.ints.Int2DoubleArrayMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A MultiModeCount station can hold any kind of measurable data to calibrate a scenario provided as an implementation of this interface.
 * A single instance holds values for only one transport mode.
 * Average velocities and traffic volumes are already implemented.
 * */
public class Measurable {

	public static final String ELEMENT_NAME = "measurable";

	public static String VOLUMES = "volumes";
	public static String VELOCITIES = "velocities";
	public static String PASSENGERS = "passengers";

	private final String type;

	private final String mode;
	//TODO find a better implementation for Int2DoubleMap
	private final Int2DoubleMap hourlyVolume;
	private final boolean dailyValuesOnly;

	private double dailyValue;

	private final Logger logger = LogManager.getLogger(Measurable.class);

	Measurable(String mode, boolean dailyValuesOnly, String type){
		this.mode = mode;
		this.dailyValuesOnly = dailyValuesOnly;
		this.hourlyVolume = new Int2DoubleArrayMap();
		this.type = type;
	}

	public Int2DoubleMap getHourlyValues() {
		return hourlyVolume;
	}

	/**
	 * Adds a value observed at a certain hour.
	 * */
	public void addAtHour(int hour, double value) {
		if(dailyValuesOnly)
			throw new RuntimeException("Volume is supposed to contain daily values only!");

		this.hourlyVolume.put(hour, value);
	}

	/**
	 * Sets an aggregated daily value, daily traffic volume e.g.
	 * */
	public void setDailyValue(double value) {
		if(!dailyValuesOnly)
			logger.warn("Daily volume is set but hourly volumes are allowed too! Might produces trash data!");

		this.dailyValue = value;
	}

	/**
	 * Returns the daily aggregated value.
	 * */
	public double getDailyValue() {

		if(dailyValuesOnly){
			return dailyValue;
		} else {
			if(this.hourlyVolume.size() < 24)
				logger.warn("Less than 24 hourly traffic volumes were provided. Daily traffic volume might be incorrect.");
			return this.hourlyVolume.values().doubleStream().sum();
		}
	}

	/**
	 * Returns the observed value at a certain hour.
	 * */
	public double getAtHour(int hour) {
		return hourlyVolume.get(hour);
	}

	/**
	 * Returns if instance holds only aggregated daily values.
	 * */
	public boolean hasOnlyDailyValues() {
		return dailyValuesOnly;
	}

	/**
	 * Returns the transport mode of the observed data.
	 * */
	public String getMode() {
		return mode;
	}

	/**
	 * Returns the name of the implementation. Information is needed for data writing.
	 * */
	public String getMeasurableType() {
		return type;
	}
}

