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

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

import playground.dgrether.koehlerstrehlersignal.analysis.DgAnalyseCottbusKS2010.Result;
import playground.dgrether.koehlerstrehlersignal.analysis.DgAnalyseCottbusKS2010.Results;


/**
 * @author dgrether
 *
 */
class ResultsWriter {
	
	private static final Logger log = Logger.getLogger(ResultsWriter.class);
	
	private String createHeader(String separator){
		StringBuilder header = new StringBuilder();
		header.append("runId");
		header.append(separator);
		header.append("Iteration");
		header.append(separator);
		header.append("run?");
		header.append(separator);
		header.append("extent");
		header.append(separator);
		header.append("time interval");
		header.append(separator);
		header.append("travel time [s]");
		header.append(separator);
		header.append("travel time [hh:mm:ss]");
		header.append(separator);
		header.append("delta travel time [s]");
		header.append(separator);
		header.append("delta travel time [hh:mm:ss]");
		header.append(separator);
		header.append("delta travel time [%]");
		header.append(separator);
		header.append("total delay [s]");
		header.append(separator);
		header.append("total delay [hh:mm:ss]");
		header.append(separator);
		header.append("delta total delay [hh:mm:ss]");
		header.append(separator);
		header.append("delay [%]");
		header.append(separator);
		
		header.append("distance [km]");
		header.append(separator);
		header.append("delta distance [km]");
		header.append(separator);
		header.append("distance [%]");
		header.append(separator);
		
		header.append("speed [km/h]");
		header.append(separator);
		header.append("delta speed [km/h]");
		header.append(separator);
		
		
		header.append("noTrips");
		header.append(separator);
		header.append("delta noTrips");
		header.append(separator);
		
		header.append("number of drivers");
		header.append(separator);
		header.append("delta drivers");
		header.append(separator);
		header.append("average travel time");
		header.append(separator);
		
		header.append("number of stucked/aborted agents");
		header.append(separator);

		return header.toString();
	}
	
	private String createLine(String separator, Result r){
		StringBuilder out = new StringBuilder();
		out.append(r.runInfo.runId);
		out.append(separator);		
		out.append(Integer.toString(r.runInfo.iteration));
		out.append(separator);
		out.append(r.runInfo.remark);
		out.append(separator);
		out.append(r.extent.name);
		out.append(separator);
		out.append(r.timeConfig.name);
		out.append(separator);
		out.append(formatDouble(r.travelTime));
		out.append(separator);
		out.append(Time.writeTime(r.travelTime));
		out.append(separator);
		out.append(formatDouble(r.travelTimeDelta));
		out.append(separator);
		out.append(Time.writeTime(r.travelTimeDelta));
		out.append(separator);
		out.append(formatDouble(r.travelTimePercent));
		out.append(separator);
		out.append(formatDouble(r.totalDelay));
		out.append(separator);
		out.append(Time.writeTime(r.totalDelay));
		out.append(separator);
		out.append(Time.writeTime(r.deltaTotalDelay));
		out.append(separator);
		out.append(formatDouble(r.delayPercent));
		out.append(separator);

		out.append(formatDouble((r.distanceMeter/1000.0)));
		out.append(separator);
		out.append(formatDouble((r.deltaDistance/1000.0)));
		out.append(separator);
		out.append(formatDouble(r.distancePercent));
		out.append(separator);
		
		out.append(formatDouble(r.speedKmH));
		out.append(separator);
		out.append(formatDouble(r.deltaSpeedKmH));
		out.append(separator);
		
		out.append(formatDouble(r.noTrips));
		out.append(separator);
		out.append(formatDouble(r.deltaNoTrips));
		out.append(separator);

		
		out.append(formatDouble(r.numberOfPersons));
		out.append(separator);
		out.append(formatDouble(r.personsDelta));
		out.append(separator);
		out.append(formatDouble(r.averageTravelTime));
		out.append(separator);
		
		out.append(formatDouble(r.noStuckedVeh));
		out.append(separator);

		return out.toString();
	}
	
	
	void writeResultsTable(Results results, String file) {
		List<String> lines = new ArrayList<String>();
		String separator = "\t";
		String header = createHeader(separator);
		lines.add(header.toString());
		for (Result r : results.getResults()) {
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
				System.out.println(l);
				bw.write(l);
				bw.newLine();
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private String formatDouble(double d){
		DecimalFormat format = new DecimalFormat("#.##");
		return format.format(d);
	}
	
}
