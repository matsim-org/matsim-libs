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

package playground.polettif.publicTransitMapping.plausibility;

import org.matsim.core.utils.charts.BarChart;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import playground.polettif.publicTransitMapping.tools.CsvTools;
import playground.polettif.publicTransitMapping.tools.MiscUtils;
import playground.polettif.publicTransitMapping.tools.ScheduleTools;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * Generates a histogram for all child stop facilities
 * of a schedule
 *
 * @author polettif
 */
public class StopFacilityHistogram {
	
	private TransitSchedule schedule;
	private Map<Integer, String> stopFacilityHistogramMap = new TreeMap<>();
	private double[] stopFacilityHistogram;

	public static void main(final String[] args) {
		// "C:/Users/polettif/Desktop/output/results_2016-05-04/zurich_gtfs_schedule.xml"
		// "C:/Users/polettif/Desktop/output/results_2016-05-04/histogram.png"
		TransitSchedule schedule = ScheduleTools.loadTransitSchedule(args[0]);

		StopFacilityHistogram check = new StopFacilityHistogram(schedule);

		check.calcHistogram(schedule);
		check.createPng(args[1]);
	}

	public StopFacilityHistogram(TransitSchedule schedule) {
		this.schedule = schedule;
		calcHistogram(this.schedule);
	}

	public void calcHistogram(TransitSchedule schedule) {
		Map<String, Integer> stopStat = new TreeMap<>();


		for(TransitStopFacility stopFacility : schedule.getFacilities().values()) {
			String parentFacility = stopFacility.getId().toString().split("[.]link:")[0];
			int count = MapUtils.getInteger(parentFacility, stopStat, 0);
			stopStat.put(parentFacility, ++count);
		}

		Map<String, Integer> stopStatSorted = MiscUtils.sortAscendingByValue(stopStat);

		stopFacilityHistogram = new double[stopStatSorted.size()];
		int i=0;
		for(Integer value : stopStatSorted.values()) {
			stopFacilityHistogram[i] = (double) value;
			i++;
		}
	}

	public void createCsv(String outputFile) throws FileNotFoundException, UnsupportedEncodingException {
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
	public void createPng(final String filename) {
		BarChart chart = new BarChart("Stop Facility Histogram", "", "# of child stop facilities");
		chart.addSeries("# StopFacilities", stopFacilityHistogram);
		chart.saveAsPng(filename, 800, 600);
	}


}