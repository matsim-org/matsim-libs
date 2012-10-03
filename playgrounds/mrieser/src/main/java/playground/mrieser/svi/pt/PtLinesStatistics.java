/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mrieser.svi.pt;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Time;

/**
 * @author mrieser
 */
public class PtLinesStatistics {
	
	private final static Logger log = Logger.getLogger(PtLinesStatistics.class);
	private final PtLines lines;
	
	public PtLinesStatistics(final PtLines lines) {
		this.lines = lines;
	}
	
	public void writeStatsToFile(final String filename, final TravelTimeCalculator ttc) {
		BufferedWriter writer = IOUtils.getBufferedWriter(filename);
		try {
			writer.write("line\tdir\tdepTime\ttravelTime\r\n");
			for (PtLine line : lines.lines) {
				for (int i = 0, n = ttc.getNumSlots(); i < n; i++) {
					double depTime = i * ttc.getTimeSlice();
					double ttime = 0;
					for (Link link : line.links) {
						ttime += ttc.getLinkTravelTime(link.getId(), depTime + ttime);
					}
					writer.write(line.name);
					writer.write("\t");
					writer.write(line.direction);
					writer.write("\t");
					writer.write(Time.writeTime(depTime));
					writer.write("\t");
					writer.write(Time.writeTime(ttime));
					writer.write("\r\n");
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				log.error("Could not close writer for file " + filename);
			}
		}
	}

}
