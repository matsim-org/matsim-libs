/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.dvrp.trafficmonitoring;

import static com.google.common.base.Verify.verify;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.io.IOUtils;

import com.google.common.base.Preconditions;

/**
 * @author Michal Maciejewski (michalm)
 */
class DvrpOfflineTravelTimes {
	private static final String DELIMITER = ";";

	static void saveLinkTravelTimes(int interval, int intervalCount, double[][] linkTTs, String filename) {
		try (Writer writer = IOUtils.getBufferedWriter(filename)) {
			saveLinkTravelTimes(interval, intervalCount, linkTTs, writer);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	static void saveLinkTravelTimes(int interval, int intervalCount, double[][] linkTTs, Writer writer)
			throws IOException {
		Preconditions.checkArgument(interval > 0);
		Preconditions.checkArgument(intervalCount > 0);

		//header row
		writer.append("linkId" + DELIMITER);
		for (int i = 0; i < intervalCount; i++) {
			int time = i * interval;
			writer.append(time + DELIMITER);
		}
		writer.append('\n');

		//regular rows
		for (int idx = 0; idx < linkTTs.length; idx++) {
			double[] ttRow = linkTTs[idx];

			// rows in linkTTs that are null are skipped
			if (ttRow != null) {
				Preconditions.checkArgument(ttRow.length == intervalCount);

				writer.append(Id.get(idx, Link.class) + DELIMITER);
				for (int t = 0; t < intervalCount; t++) {
					writer.append(ttRow[t] + DELIMITER);// some precision lost while writing TTs
				}
				writer.append('\n');
			}
		}
	}

	static double[][] loadLinkTravelTimes(int interval, int intervalCount, URL url) {
		try (BufferedReader reader = IOUtils.getBufferedReader(url)) {
			return loadLinkTravelTimes(interval, intervalCount, reader);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	static double[][] loadLinkTravelTimes(int interval, int intervalCount, BufferedReader reader) throws IOException {
		Preconditions.checkArgument(interval > 0);
		Preconditions.checkArgument(intervalCount > 0);

		double[][] linkTTs = new double[Id.getNumberOfIds(Link.class)][];
		//header row
		String[] headerLine = reader.readLine().split(";");
		verify(intervalCount == headerLine.length - 1);
		verify(headerLine[0].equals("linkId"));
		for (int i = 0; i < intervalCount; i++) {
			verify(Integer.parseInt(headerLine[i + 1]) == i * interval);
		}

		//regular rows
		// rows in linkTTs for which we do not have TT data, will remain null
		reader.lines().map(line -> line.split(DELIMITER)).forEach(cells -> {
			int linkIndex = Id.createLinkId(cells[0]).index();
			double[] row = linkTTs[linkIndex] = new double[intervalCount];
			verify(row.length == cells.length - 1);

			for (int i = 0; i < row.length; i++) {
				row[i] = Double.parseDouble(cells[i + 1]);
			}
		});

		return linkTTs;
	}
}
