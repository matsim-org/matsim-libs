/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.minibus.stats.operatorLogger;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Reads a pLogger file and writes it to tex.
 * 
 * @author aneumann
 *
 */
final class Log2Tex {
	
	private static final Logger log = Logger.getLogger(Log2Tex.class);

	public static void convertLog2Tex(String inputFile, String outputFile){
		ArrayList<LogElement> logElements = LogReader.readFile(inputFile);
		
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);

		int lastIteration = -1;
		int operatorCounterInThatIteration = 1;
		String lastOperatorName = "";
		int lastPaxSum = 0;

			try {
				try {
					for (LogElement logElement : logElements) {

					if (logElement.getIteration() != lastIteration) {
						lastIteration = logElement.getIteration();
						operatorCounterInThatIteration = 1;
						lastOperatorName = "";
						lastPaxSum = 0;
						writer.write("\\addlinespace[1.0em]"); writer.newLine();
					}

					StringBuffer strB = new StringBuffer();

					strB.append(" & ");

					String operatorName = logElement.getOperatorId().toString().split("_")[1];
					if (!lastOperatorName.equalsIgnoreCase(operatorName)) {
						strB.append("O" + operatorCounterInThatIteration + " -- ");
						operatorCounterInThatIteration++;
						strB.append(operatorName);
						lastOperatorName = operatorName;
					}

					strB.append(" & ");

					String iteration = logElement.getPlanId().toString().split("_")[0];
					strB.append(iteration);

					strB.append(" & ");

					String startTime = Time.writeTime(logElement.getStartTime(), Time.TIMEFORMAT_HHMM); //row[9].split(":")[0] + ":" + row[9].split(":")[1];
					strB.append(startTime);
					strB.append("--");
					String endTime = Time.writeTime(logElement.getEndTime(), Time.TIMEFORMAT_HHMM); //row[10].split(":")[0] + ":" + row[10].split(":")[1];
					strB.append(endTime);

					strB.append(" & ");

					ArrayList<Id<TransitStopFacility>> stops = logElement.getStopsToBeServed(); //nodes.split(",");
					boolean firstIsDone = false;
					for (Id<TransitStopFacility> stop : stops) {
						if (firstIsDone) {
							strB.append("--");
						}
						firstIsDone = true;
						strB.append(stop.toString());
					}

					strB.append(" & ");

					String veh = Integer.toString(logElement.getnVeh());
					strB.append(veh);

					strB.append(" & ");

					strB.append(Integer.toString(logElement.getnPax()));
					lastPaxSum += logElement.getnPax();

					strB.append(" & ");

					double score = logElement.getScore();
					score = score / Double.parseDouble(veh);
					score = Math.round(score * 10.) / 10.;
					DecimalFormat df = (DecimalFormat)DecimalFormat.getInstance(Locale.US);
					df.applyPattern("#,###,##0.0");
					String s = df.format(score);

					if (s.contains("-")) {
						s = s.substring(1, s.length());
						strB.append("$-$");
					} else {
						strB.append("$+$");
					}

					strB.append(s);

					strB.append(" \\tabularnewline");

					strB.append(" % pax sum " + lastPaxSum);

					writer.write(strB.toString()); writer.newLine();

					}
				} catch (NumberFormatException e) {
					log.info("Had one NumberFormatException. Run Debugger...");
				}

				writer.flush();
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
}