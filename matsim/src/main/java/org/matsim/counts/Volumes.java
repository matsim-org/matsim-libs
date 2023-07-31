package org.matsim.counts;

import it.unimi.dsi.fastutil.ints.Int2DoubleArrayMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Volumes {

	public static final String ELEMENT_NAME = "volumes";

	private final String mode;
	private final Int2DoubleMap hourlyVolume;
	private final boolean dailyVolumesOnly;

	private double dailyVolume;

	private final Logger logger = LogManager.getLogger(Volumes.class);

	Volumes(String mode, boolean dailyVolumesOnly){
		this.mode = mode;
		this.dailyVolumesOnly = dailyVolumesOnly;
		this.hourlyVolume = new Int2DoubleArrayMap();
	}

	public void addVolumeAtHour(int hour, double volume){
		if(dailyVolumesOnly)
			throw new RuntimeException("Volume is supposed to contain daily values only!");

		this.hourlyVolume.put(hour, volume);
	}

	public void setDailyVolume(double dailyVolume) {
		if(!dailyVolumesOnly)
			logger.warn("Daily volume is set but hourly volumes are allowed too! Might produces trash data!");

		this.dailyVolume = dailyVolume;
	}

	public Int2DoubleMap getHourlyVolume() {
		return hourlyVolume;
	}

	public Double getDailyVolume(){
		return dailyVolume;
	}

	public String getMode() {
		return mode;
	}
}

