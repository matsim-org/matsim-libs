/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractModeHistogram
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package air.analysis.modehistogram;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;


public abstract class AbstractModeHistogram {

	public static final String allModes = "all";

	private final Map<String, ModeHistogramData> data = new HashMap<String, ModeHistogramData>();
	private int iteration = 0;
	private final int binSize;
	private Integer firstIndex;
	private Integer lastIndex;

	public AbstractModeHistogram(int binSizeSeconds) {
		this.binSize = binSizeSeconds;
		this.resetIteration(0);
	}

	protected void resetIteration(final int iter) {
		this.setIteration(iter);
		this.getModeData().clear();
		this.getModeData().put(allModes, new ModeHistogramData());
		this.setFirstIndex(null);
		this.setLastIndex(null);
	}

	public abstract void write(final PrintStream stream);
	
	public abstract JFreeChart getGraphic(final ModeHistogramData modeData, final String modeName);
	/**
	 * Writes the gathered data tab-separated into a text file.
	 *
	 * @param filename The name of a file where to write the gathered data.
	 */
	public void write(final String filename) {
		PrintStream stream;
		try {
			stream = new PrintStream(new File(filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		write(stream);
		stream.close();
	}
	

	public void increase(double time_seconds, String mode){
		this.increase(time_seconds, 1, mode);
	}
	
	public void increase(double time_seconds, int count, String mode){
		int index = getBinIndex(time_seconds);
		ModeHistogramUtils.add2MapEntry(this.getModeData().get(allModes).countsDep, index, count);
		if (mode != null) {
			ModeHistogramData modeData = getDataForMode(mode);
			ModeHistogramUtils.add2MapEntry(modeData.countsDep, index, count);
		}
	}
	
	public int getDepartures(String mode, int index){
		return ModeHistogramUtils.getNotNullInteger(this.getModeData().get(mode).countsDep,
				index);
	}

	public int getArrivals(String mode, int index){
		return ModeHistogramUtils.getNotNullInteger(this.getModeData().get(mode).countsArr,
				index);
	}
	
	public int getAbort(String mode, int index){
		return ModeHistogramUtils.getNotNullInteger(this.getModeData().get(mode).countsStuck,
				index);
	}

	public void decrease(double time_seconds, int count, String mode){
		int index = this.getBinIndex(time_seconds);
		ModeHistogramUtils.add2MapEntry(this.getModeData().get(allModes).countsArr, index, count);
		if (mode != null) {
			ModeHistogramData modeData = getDataForMode(mode);
			ModeHistogramUtils.add2MapEntry(modeData.countsArr, index, count);
		}

	}

	
	
	public void decrease(double time_seconds, String mode){
		this.decrease(time_seconds, 1, mode);
	}

	
	public void abort(double time_seconds, String mode){
		int index = this.getBinIndex(time_seconds);
		ModeHistogramUtils.increaseMapEntry(this.getModeData().get(allModes).countsStuck, index);
		if (mode != null) {
			ModeHistogramData modeData = getDataForMode(mode);
			ModeHistogramUtils.increaseMapEntry(modeData.countsStuck, index);
		}
	}
	
	/**
	 * @return a graphic showing the number of departures, arrivals and vehicles
	 * en route of all legs/trips
	 */
	public JFreeChart getGraphic() {
		return getGraphic(this.getModeData().get(allModes), "all");
	}

	public JFreeChart getGraphic(String legMode) {
		return this.getGraphic(this.getModeData().get(legMode), legMode);
	}

	/**
	 * @return Set of all transportation modes data is available for
	 */
	public Set<String> getLegModes() {
		return this.getModeData().keySet();
	}

	/**
	 * Writes a graphic showing the number of departures, arrivals and vehicles
	 * en route of all legs/trips to the specified file.
	 *
	 * @param filename
	 *
	 * @see #getGraphic()
	 */
	public void writeGraphic(final String filename) {
		try {
			ChartUtilities.saveChartAsPNG(new File(filename), getGraphic(), 1024, 768);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Writes a graphic showing the number of departures, arrivals and vehicles
	 * en route of all legs/trips with the specified transportation mode to the
	 * specified file.
	 *
	 * @param filename
	 * @param legMode
	 *
	 * @see #getGraphic(String)
	 */
	public void writeGraphic(final String filename, final String legMode) {
		try {
			ChartUtilities.saveChartAsPNG(new File(filename), getGraphic(legMode), 1024, 768);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected int getBinIndex(final double time) {
		int bin = (int)(time / this.getBinSize());
		if (! this.getModeData().get(allModes).countsArr.containsKey(bin)) { //initialize for mode all
			this.getModeData().get(allModes).countsArr.put(bin, 0);
			this.getModeData().get(allModes).countsDep.put(bin, 0);
			this.getModeData().get(allModes).countsStuck.put(bin, 0);
		}
		if (this.getFirstIndex() == null){
			this.setFirstIndex(bin);
		}
		this.setLastIndex(bin);
		return bin;
	}

	protected ModeHistogramData getDataForMode(final String legMode) {
		ModeHistogramData modeData = this.getModeData().get(legMode);
		if (modeData == null) {
			modeData = new ModeHistogramData(); // +1 for all times out of our range
			this.getModeData().put(legMode, modeData);
		}
		return modeData;
	}

	public Map<String, ModeHistogramData> getModeData() {
		return data;
	}

	public int getIteration() {
		return iteration;
	}

	private void setIteration(int iteration) {
		this.iteration = iteration;
	}

	public int getBinSize() {
		return binSize;
	}

	public Integer getFirstIndex() {
		return firstIndex;
	}

	private void setFirstIndex(Integer firstIndex) {
		this.firstIndex = firstIndex;
	}

	public Integer getLastIndex() {
		return lastIndex;
	}

	private void setLastIndex(Integer lastIndex) {
		this.lastIndex = lastIndex;
	}

}