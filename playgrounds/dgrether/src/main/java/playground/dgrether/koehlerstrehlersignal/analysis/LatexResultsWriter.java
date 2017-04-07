/* *********************************************************************** *
 * project: org.matsim.*
 * ResultsWriter
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.dgrether.koehlerstrehlersignal.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

import playground.dgrether.koehlerstrehlersignal.analysis.DgAnalyseCottbusKS2010.Result;


/**
 * @author dgrether
 *
 */
class LatexResultsWriter {
	
	private static final Logger log = Logger.getLogger(LatexResultsWriter.class);
	
	private String createHeader(String separator){
		StringBuilder header = new StringBuilder();
//		header.append("runId");
//		header.append(separator);
//		header.append("Iteration");
//		header.append(separator);
		header.append("simulation run");
		header.append(separator);
//		header.append("extent");
//		header.append(separator);
//		header.append("time interval");
//		header.append(separator);
//		header.append("travel time [s]");
//		header.append(separator);
		header.append("$tt$ [hh:mm]");
		header.append(separator);
//		header.append("delta travel time [s]");
//		header.append(separator);
//		header.append("delta travel time [hh:mm:ss]");
//		header.append(separator);
		header.append("$\\Delta$ $tt$ [%]");
		header.append(separator);

		header.append("$v$ [km/h]");
		header.append(separator);
		header.append("$\\Delta$ $v$ [%]");
		header.append(separator);

		
		header.append("veh km [km]");
		header.append(separator);
//		header.append("delta distance[km]");
//		header.append(separator);
		header.append("$\\Delta$ veh km [%]");
		header.append(separator);
		
//		header.append("total delay [s]");
//		header.append(separator);
		header.append("delay [hh:mm]");
		header.append(separator);
//		header.append("delta total delay [hh:mm:ss]");
//		header.append(separator);
		header.append("$\\Delta$ delay [%]");
		header.append(separator);
		
		header.append("\\#trips [veh]");
		header.append(separator);
//		header.append("$\\Delta$ \\#trips [veh]");
//		header.append(separator);
		header.append("$\\Delta$ \\#trips [%]");
		header.append(separator);
		
		header.append("\\#stucked [veh]");		
		
		header.append("\t\\\\");
		

		return header.toString();
	}
	
	private String createLine(String separator, Result r){
		StringBuilder out = new StringBuilder();
//		out.append(r.runInfo.runId);
//		out.append(separator);		
//		out.append(Integer.toString(r.runInfo.iteration));
//		out.append(separator);
		out.append(r.runInfo.remark);
		out.append(separator);
//		out.append(r.extent.name);
//		out.append(separator);
//		out.append(r.timeConfig.name);
//		out.append(separator);
//		out.append(formatDouble(r.travelTime));
//		out.append(separator);
		out.append(Time.writeTime(r.travelTime, Time.TIMEFORMAT_HHMM));
		out.append(separator);
//		out.append(formatDouble(r.travelTimeDelta));
//		out.append(separator);
//		out.append(Time.writeTime(r.travelTimeDelta));
//		out.append(separator);
		out.append(formatDouble(r.travelTimePercent));
		out.append(separator);

		out.append(formatDouble(r.speedKmH));
		out.append(separator);
		out.append(formatDouble(r.deltaSpeedKmH));
		out.append(separator);

		out.append(formatDoubleInt((r.distanceMeter/1000.0)));
		out.append(separator);
//		out.append(formatDouble((r.deltaDistance/1000.0)));
//		out.append(separator);
		out.append(formatDouble(r.distancePercent));
		out.append(separator);
		
//		out.append(formatDouble(r.totalDelay));
//		out.append(separator);
		out.append(Time.writeTime(r.totalDelay, Time.TIMEFORMAT_HHMM));
		out.append(separator);
//		out.append(Time.writeTime(r.deltaTotalDelay));
//		out.append(separator);
		out.append(formatDouble(r.delayPercent));
		out.append(separator);
		
		out.append(formatDoubleInt(r.noTrips));
		out.append(separator);
//		out.append(formatDoubleInt(r.deltaNoTrips));
//		out.append(separator);
		out.append(formatDouble(r.noTripsPercent));
		out.append(separator);
		
		out.append(formatDoubleInt(r.noStuckedVeh));
		
		out.append("\t\\\\");
		

		return out.toString();
	}
	
	
	void writeResultsTable(List<Result> results, String file) {
		List<String> lines = new ArrayList<String>();
		String separator = "\t&\t";
		String header = createHeader(separator);
		lines.add(header.toString());
		for (Result r : results) {
			String line = createLine(separator, r);
			lines.add(line);
			log.info(line);
		}
		writeFile(lines, file);
	}
	
	private void writeFile(List<String> lines, String file){
		BufferedWriter bw = IOUtils.getBufferedWriter(file);
		try {
			log.info("Result");
			for (String l : lines) {
				String l2 = l.replaceAll("%", Matcher.quoteReplacement("\\%"));
				l2 = l2.replaceAll("_", Matcher.quoteReplacement(" "));
				System.out.println(l2);
				bw.write(l2);
				bw.newLine();
				if (! l.equals(lines.get(lines.size() - 1))) {
					bw.write("\\hline");
					bw.newLine();
				}
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private String formatDoubleInt(double d){
		DecimalFormat format = new DecimalFormat("#");
		return format.format(d);
	}
	
	
	private String formatDouble(double d){
		DecimalFormat format = new DecimalFormat("#0.00");
		return format.format(d);
	}
	
}
