/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package contrib.publicTransitMapping.plausibility;

import org.matsim.core.utils.charts.BarChart;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import contrib.publicTransitMapping.config.PublicTransitMappingStrings;
import contrib.publicTransitMapping.tools.CsvTools;
import contrib.publicTransitMapping.tools.MiscUtils;
import contrib.publicTransitMapping.tools.ScheduleTools;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Generates a histogram for number of child stop
 * facilities per parent stop of a schedule
 *
 * @author polettif
 */
public class StopFacilityHistogram {

	private TransitSchedule schedule;
	private Map<String, Integer> histMap = new TreeMap<>();
	private double[] hist;
	private double[] histNr;

	private static final String SUFFIX_PATTERN = PublicTransitMappingStrings.SUFFIX_CHILD_STOP_FACILITIES_REGEX;
	private static final String SUFFIX = PublicTransitMappingStrings.SUFFIX_CHILD_STOP_FACILITIES;

	/**
	 * @param args [0] schedule file, [1] output file (csv or png), [2] output file (csv or png, optional)
	 */
	public static void main(final String[] args) {
		String csvFile = args[1].contains(".csv") ? args[1] : null;
		String pngFile = args[1].contains(".png") ? args[1] : null;

		if(args.length == 3 && csvFile == null) {
			csvFile = args[2];
		} else if(args.length == 3 && pngFile == null){
			pngFile = args[2];
		}

		run(ScheduleTools.readTransitSchedule(args[0]), pngFile, csvFile);
	}

	public static void run(TransitSchedule schedule, String outputPngFile, String outputCsvFile) {
		StopFacilityHistogram h = new StopFacilityHistogram(schedule);
		if(outputPngFile != null) h.createPng(outputPngFile);
		if(outputCsvFile != null) h.createCsv(outputCsvFile);
	}

	public StopFacilityHistogram(TransitSchedule schedule) {
		this.schedule = schedule;
		calcHistogram();
	}

	private void calcHistogram() {
		Map<String, Integer> stopStat = new TreeMap<>();

		for(TransitStopFacility stopFacility : this.schedule.getFacilities().values()) {
			String parentFacility = stopFacility.getId().toString().split(SUFFIX_PATTERN)[0];
			int count = MapUtils.getInteger(parentFacility, stopStat, 0);
			stopStat.put(parentFacility, ++count);
		}

		histMap = MiscUtils.sortMapAscendingByValue(stopStat);

		Map<Integer, Integer> histNrMap = new TreeMap<>();

		hist = new double[histMap.size()];
		int i=0;
		for(Integer value : histMap.values()) {
			hist[i] = (double) value;
			MapUtils.addToInteger(value, histNrMap, 1, 1);
			i++;
		}

		histNr = new double[new ArrayList<>(histNrMap.keySet()).get(histNrMap.size()-1)];
		i=0;
		for(Map.Entry<Integer, Integer> e : histNrMap.entrySet()) {
			histNr[i] = e.getValue()-1;
			i++;
		}
	}

	public double median() {
		int m = hist.length / 2;
		return hist[m];
	}

	public double average() {
		double sum = 0;
		for(double m : hist) {
			sum += m;
		}
		return sum/hist.length;
	}

	public double max() {
		return hist[hist.length - 1];
	}

	public void createCsv(String outputFile) {
		Map<Tuple<Integer, Integer>, String> stopStatCsv = new HashMap<>();
		stopStatCsv.put(new Tuple<>(1, 1), "parent stop id");
		stopStatCsv.put(new Tuple<>(1, 2), "nr of child stop facilities");
		int i=2;
		for(Map.Entry<String, Integer> e : histMap.entrySet()) {
			stopStatCsv.put(new Tuple<>(i, 1), e.getKey());
			stopStatCsv.put(new Tuple<>(i, 2), e.getValue().toString());
			i++;
		}

		try {
			CsvTools.writeToFile(CsvTools.convertToCsvLines(stopStatCsv, ';'), outputFile);
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @see org.matsim.core.utils.charts
	 */
	public void createPng(final String filename) {
		BarChart chart = new BarChart("Stop Facility Histogram", "", "# of stops");
		chart.addSeries("number of child stop facilities", histNr);
		chart.saveAsPng(filename, 800, 600);
	}
}