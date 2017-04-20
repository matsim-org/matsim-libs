/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
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

package playground.michalm.stockholm;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.util.CompactCSVWriter;
import org.matsim.core.utils.io.IOUtils;

import playground.michalm.demand.taxi.ServedRequest;

public class TaxiTracesAnalyser {
	private final List<TaxiTrace> taxiTraces;
	private final List<StockholmServedRequest> servedRequests = new ArrayList<>();

	private static final Date TIME_ZERO = TaxiTracesReader.parseDate("2014-10-06 00:00:00");

	private static final long getSecondsAfterTimeZero(Date date) {
		return (date.getTime() - TIME_ZERO.getTime()) / 1000;
	}

	private TaxiTracesAnalyser(List<TaxiTrace> taxiTraces) {
		this.taxiTraces = taxiTraces;
		Map<String, List<TaxiTrace>> currentHiredTraces = new HashMap<>();

		for (TaxiTrace tt : taxiTraces) {
			if (tt.hired) {
				List<TaxiTrace> hiredTrace = currentHiredTraces.get(tt.taxiId);

				if (hiredTrace == null) {
					hiredTrace = new ArrayList<>();
					currentHiredTraces.put(tt.taxiId, hiredTrace);
				}

				hiredTrace.add(tt);
			} else {
				List<TaxiTrace> hiredTrace = currentHiredTraces.remove(tt.taxiId);

				if (hiredTrace != null) {
					Id<ServedRequest> id = Id.create(servedRequests.size(), ServedRequest.class);
					servedRequests.add(new StockholmServedRequest(id, hiredTrace, tt.taxiId));
				}
			}
		}
	}

	private void analyseRequests() {
		System.out.println("Req Count = " + servedRequests.size());

		int[] reqsPerHour = new int[7 * 24];
		Set<String> taxiIds = new HashSet<>();

		for (StockholmServedRequest r : servedRequests) {
			taxiIds.add(r.taxiId);

			@SuppressWarnings("deprecation")
			int hourInWeek = r.getStartTime().getDay() * 24 + r.getStartTime().getHours();
			reqsPerHour[hourInWeek]++;
		}

		System.out.println("Taxi Count = " + taxiIds.size());

		System.out.println("Requests/Hour");
		for (int d = 0; d < 7; d++) {
			for (int h = 0; h < 24; h++) {
				System.out.println(d + "\t" + h + "\t" + reqsPerHour[d * 24 + h]);
			}
		}
		System.out.println("===========");
	}

	private void analyseTaxis() {
		@SuppressWarnings("unchecked")
		Set<String>[] taxisInHour = new Set[7 * 24];
		for (int d = 0; d < 7; d++) {
			for (int h = 0; h < 24; h++) {
				taxisInHour[d * 24 + h] = new HashSet<>();
			}
		}

		for (TaxiTrace tt : taxiTraces) {
			@SuppressWarnings("deprecation")
			int hourInWeek = tt.time.getDay() * 24 + tt.time.getHours();
			taxisInHour[hourInWeek].add(tt.taxiId);
		}

		System.out.println("Taxis/Hour");
		for (int d = 0; d < 7; d++) {
			for (int h = 0; h < 24; h++) {
				System.out.println(d + "\t" + h + "\t" + taxisInHour[d * 24 + h].size());
			}
		}
		System.out.println("===========");
	}

	@SuppressWarnings("unused")
	private void saveHiredTaxiTracesAsXYPlot(String file) {
		try (CompactCSVWriter writer = new CompactCSVWriter(IOUtils.getBufferedWriter(file))) {
			writer.writeNext("id", "time", "x", "y");
			for (StockholmServedRequest r : servedRequests) {
				for (TaxiTrace tt : r.trace) {
					writer.writeNext(r.id + "", getSecondsAfterTimeZero(tt.time) + "", tt.coord.getX() + "",
							tt.coord.getY() + "");
				}
			}
		}
	}

	@SuppressWarnings("unused")
	private void saveODAsXYPlot(String file) {
		try (CompactCSVWriter writer = new CompactCSVWriter(IOUtils.getBufferedWriter(file))) {
			writer.writeNext("id", "time", "x0", "y0", "x1", "y1");
			for (StockholmServedRequest r : servedRequests) {
				writer.writeNext(r.id + "", getSecondsAfterTimeZero(r.getStartTime()) + "", r.getFrom().getX() + "",
						r.getFrom().getY() + "", r.getTo().getX() + "", r.getTo().getY() + "");
			}
		}
	}

	public static void main(String[] args) {
		String dir = "d:/temp/Stockholm/";
		List<TaxiTrace> taxiTraces = new ArrayList<>();
		new TaxiTracesReader(taxiTraces).readFile(dir + "taxi_1_week_2014_10_6-13.csv");

		TaxiTracesAnalyser analyser = new TaxiTracesAnalyser(taxiTraces);
		analyser.analyseRequests();
		analyser.analyseTaxis();
		// analyser.saveHiredTaxiTracesAsXYPlot(dir + "hiredTaxiTraces.xy");
		// analyser.saveODAsXYPlot(dir + "OD.xy");
	}
}
