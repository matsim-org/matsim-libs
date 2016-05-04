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

package playground.polettif.multiModalMap.validation;

import org.matsim.core.utils.charts.BarChart;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import playground.polettif.multiModalMap.tools.CsvTools;
import playground.polettif.multiModalMap.tools.ScheduleTools;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.*;

public class SchedulePlausibilityCheck {
	
	private TransitSchedule schedule;
	private Map<Integer, String> stopFacilityHistogramMap = new TreeMap<>();
	private double[] stopFacilityHistogram;

	public static void main(final String[] args) {
		TransitSchedule schedule = ScheduleTools.loadTransitSchedule("C:/Users/polettif/Desktop/output/results_2016-05-03/zurich_schedule.xml");

		SchedulePlausibilityCheck check = new SchedulePlausibilityCheck(schedule);

		check.calcStopFacilityHistogram(schedule);
		check.createHistogramPng("C:/Users/polettif/Desktop/output/results_2016-05-03/histogram.png");
	}

	public SchedulePlausibilityCheck(TransitSchedule schedule) {
		this.schedule = schedule;
	}

	public void calcStopFacilityHistogram(TransitSchedule schedule) {
		Map<String, Integer> stopStat = new TreeMap<>();


		for(TransitStopFacility stopFacility : schedule.getFacilities().values()) {
			String parentFacility = stopFacility.getId().toString().split("[.]link:")[0];
			int count = MapUtils.getInteger(parentFacility, stopStat, 0);
			stopStat.put(parentFacility, ++count);
		}

		Map<String, Integer> stopStatSorted = sortAscending(stopStat);

		stopFacilityHistogram = new double[stopStatSorted.size()];
		int i=0;
		for(Integer value : stopStatSorted.values()) {
			stopFacilityHistogram[i] = (double) value;
			i++;
		}
	}

	public void writeStopFacilityHisogramCsv(String outputFile) throws FileNotFoundException, UnsupportedEncodingException {
		Map<Tuple<Integer, Integer>, String> stopStatCsv = new HashMap<>();
		int i=1;
		for(Map.Entry<Integer, String> e : stopFacilityHistogramMap.entrySet()) {
			stopStatCsv.put(new Tuple<>(i, 1), e.getValue());
			stopStatCsv.put(new Tuple<>(i, 2), e.getKey().toString());
			i++;
		}

		CsvTools.writeToFile(CsvTools.convertToCsvLines(stopStatCsv), outputFile);
	}

	/**
	 * @see org.matsim.core.utils.charts
	 */
	public void createHistogramPng(final String filename) {
		BarChart chart = new BarChart("Stop Facility Histogram", "", "# of child stop facilities");
		chart.addSeries("# StopFacilities", stopFacilityHistogram);
		chart.saveAsPng(filename, 800, 600);
	}


	private static Map<String, Integer> sortAscending(Map<String, Integer> unsortMap) {
		// Convert Map to List
		List<Map.Entry<String, Integer>> list =	new LinkedList<>(unsortMap.entrySet());

		// Sort list with comparator, to compare the Map values
		Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
			public int compare(Map.Entry<String, Integer> o1,
							   Map.Entry<String, Integer> o2) {
				return (o1.getValue()).compareTo(o2.getValue());
			}
		});

		// Convert sorted map back to a Map
		Map<String, Integer> sortedMap = new LinkedHashMap<>();
		for (Iterator<Map.Entry<String, Integer>> it = list.iterator(); it.hasNext();) {
			Map.Entry<String, Integer> entry = it.next();
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}
}