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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verify;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.common.timeprofile.TimeDiscretizer;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DvrpOfflineTravelTimes {

	public static void saveLinkTravelTimes(TimeDiscretizer timeDiscretizer, double[][] linkTravelTimes,
			String filename, String delimiter) {
		try (Writer writer = IOUtils.getBufferedWriter(filename)) {
			saveLinkTravelTimes(timeDiscretizer, linkTravelTimes, writer, delimiter);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void saveLinkTravelTimes(TimeDiscretizer timeDiscretizer, double[][] linkTravelTimes,
										   Writer writer, String delimiter)
			throws IOException {
		int intervalCount = timeDiscretizer.getIntervalCount();
		//header row
		writer.append("linkId").append(delimiter);
		for (int i = 0; i < intervalCount; i++) {
			double time = i * timeDiscretizer.getTimeInterval();
			writer.append(String.valueOf(time)).append(delimiter);
		}
		writer.append('\n');

		//regular rows
		for (int idx = 0; idx < linkTravelTimes.length; idx++) {
			double[] ttRow = linkTravelTimes[idx];

			// rows in linkTTs that are null are skipped
			if (ttRow != null) {
				checkArgument(ttRow.length == intervalCount);

				writer.append(String.valueOf(Id.get(idx, Link.class))).append(delimiter);
				for (int t = 0; t < intervalCount; t++) {
					// rounding up to full seconds, otherwise the output files are sometimes huge (even when gzipped)
					// consider having a switch for enabling/disabling rounding
					int tt = (int)Math.ceil(ttRow[t]);//rounding up to avoid zeros; also QSim rounds up
					writer.append(String.valueOf(tt)).append(delimiter);
				}
				writer.append('\n');
			}
		}
	}

	public static double[][] convertToLinkTravelTimeMatrix(TravelTime travelTime, Collection<? extends Link> links,
			TimeDiscretizer timeDiscretizer) {
		var linkTTs = new double[Id.getNumberOfIds(Link.class)][];
		for (Link link : links) {
			double[] tt = linkTTs[link.getId().index()] = new double[timeDiscretizer.getIntervalCount()];
			timeDiscretizer.forEach((bin, time) -> tt[bin] = travelTime.getLinkTravelTime(link, time, null, null));
		}
		return linkTTs;
	}

	public static TravelTime asTravelTime(TimeDiscretizer timeDiscretizer, double[][] linkTravelTimes) {
		return (link, time, person, vehicle) -> {
			var linkTT = checkNotNull(linkTravelTimes[link.getId().index()],
					"Link (%s) does not belong to network. No travel time data.", link.getId());
			return linkTT[timeDiscretizer.getIdx(time)];
		};
	}

	public static double[][] loadLinkTravelTimes(TimeDiscretizer timeDiscretizer, URL url, String delimiter) {
		try (BufferedReader reader = IOUtils.getBufferedReader(url)) {
			return loadLinkTravelTimes(timeDiscretizer, reader, delimiter);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static double[][] loadLinkTravelTimes(TimeDiscretizer timeDiscretizer, BufferedReader reader,
												 String delimiter)
			throws IOException {
		//start with IdMap and then convert to array (to avoid index out of bounds)
		IdMap<Link, double[]> linkTravelTimes = new IdMap<>(Link.class);

		//header row
		String[] headerLine = reader.readLine().split(delimiter);
		verify(timeDiscretizer.getIntervalCount() == headerLine.length - 1);
		verify(headerLine[0].equals("linkId"));
		timeDiscretizer.forEach((bin, time) -> verify(Double.parseDouble(headerLine[bin + 1]) == time));

		//regular rows
		reader.lines().map(line -> line.split(delimiter)).forEach(cells -> {
			verify(timeDiscretizer.getIntervalCount() == cells.length - 1);

			double[] row = new double[timeDiscretizer.getIntervalCount()];
			for (int i = 0; i < row.length; i++) {
				row[i] = Double.parseDouble(cells[i + 1]);
			}
			linkTravelTimes.put(Id.createLinkId(cells[0]), row);
		});

		// rows in linkTTs for which we do not have TT data, will remain null
		double[][] linkTravelTimeArray = new double[Id.getNumberOfIds(Link.class)][];
		linkTravelTimes.forEach((linkId, row) -> linkTravelTimeArray[linkId.index()] = row);

		return linkTravelTimeArray;
	}
}
