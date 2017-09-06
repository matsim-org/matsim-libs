/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
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
 * *********************************************************************** *
 */

package contrib.baseline.counts;

import static contrib.baseline.counts.CountsIVTBaseline.COUNTS_DELIMITER;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import contrib.baseline.lib.NetworkUtils;

/**
 * Identifies for a given from-to-relationship the according pt-link in the network.
 *
 * @author boescpa
 */
public class PTCountsLinkIdentification {
	private static final Logger log = Logger.getLogger(PTCountsLinkIdentification.class);
	private static final Counter counter = new Counter(" handled count input: ");

	private final static double ALLOWED_DISTANCE_FACTOR_IF_MORE_EQUAL_STATION_DISTANCE = 1.1;

	public static void main(final String[] args) {
		final String pathToNetwork = args[0];
		final String pathToCountsInput = args[1];
		final String pathToCountsOutput = args[2];

		Network network = NetworkUtils.readNetwork(pathToNetwork);
		List<CountInput> countInputs = readCountInputs(pathToCountsInput);
		Map<CountInput, Id<Link>> identifiedLinks = identifyLinks(network, countInputs);
		writePTCounts(pathToCountsOutput, identifiedLinks);
	}

	private static void writePTCounts(String pathToCountsOutput, Map<CountInput, Id<Link>> identifiedLinks) {
		BufferedWriter writer = IOUtils.getBufferedWriter(pathToCountsOutput, Charset.forName("UTF-8"));
		try {
			String header = "linkId" + COUNTS_DELIMITER + "countStationDescr" + COUNTS_DELIMITER + "countVolumes";
			writer.write(header);
			writer.newLine();
			//for (Id<Link> linkId : identifiedLinks.keySet()) {
			for (CountInput countInput : identifiedLinks.keySet()) {
				writer.write(identifiedLinks.get(countInput).toString() + COUNTS_DELIMITER);
				writer.write(countInput.descr + COUNTS_DELIMITER);
				writer.write(Double.toString(countInput.counts));
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static Map<CountInput, Id<Link>> identifyLinks(Network network, List<CountInput> countInputs) {
		Map<CountInput, Id<Link>> identifiedLinks = new LinkedHashMap<>();
		counter.reset();
		for (CountInput countInput : countInputs) {
			double distFromNode, distToNode;
			double totalMinDist = Double.MAX_VALUE, absMinDifferenceStation = Double.MAX_VALUE;
			Id<Link> currentMinLink = null;
			for (Link link : network.getLinks().values()) {
				//if (link.getAllowedModes().contains(TransportMode.pt) && link.getId().toString().contains("pt")) {
				if (link.getAllowedModes().contains("rail")) {
					distFromNode = CoordUtils.calcEuclideanDistance(countInput.fromCoord, link.getFromNode().getCoord());
					distToNode = CoordUtils.calcEuclideanDistance(countInput.toCoord, link.getToNode().getCoord());
					if (distFromNode + distToNode <= (totalMinDist)){//*ALLOWED_DISTANCE_FACTOR_IF_MORE_EQUAL_STATION_DISTANCE)
							//&& Math.abs(distFromNode - distToNode) < absMinDifferenceStation) {
						totalMinDist = distFromNode + distToNode;
						absMinDifferenceStation = Math.abs(distFromNode - distToNode);
						currentMinLink = link.getId();
					}
				}
			}
			if (currentMinLink != null) {
				identifiedLinks.put(countInput, currentMinLink);
			} else {
				log.error("For count input " + countInput.descr + " no matching link was found.");
			}
			counter.incCounter();
		}
		return identifiedLinks;
	}

	private static List<CountInput> readCountInputs(String pathToCountsInput) {
		List<CountInput> countInputs = new ArrayList<>();
		BufferedReader reader = IOUtils.getBufferedReader(pathToCountsInput, Charset.forName("UTF-8"));
		try {
			reader.readLine(); // read header: VON; VON_X; VON_Y; NACH; NACH_X; NACH_Y; ANZAHL
			String line = reader.readLine();
			while (line != null) {
				String[] lineElements = line.split(";");
				countInputs.add(new CountInput(
						new Coord(Double.parseDouble(lineElements[1]), Double.parseDouble(lineElements[2])),
						new Coord(Double.parseDouble(lineElements[4]), Double.parseDouble(lineElements[5])),
						(lineElements[0].replace(" ", "") + "_" + lineElements[3].replace(" ", "")),
						Double.parseDouble(lineElements[6])
				));
				line = reader.readLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return countInputs;
	}

	private static class CountInput {
		public final Coord fromCoord;
		public final Coord toCoord;
		public final String descr;
		public final double counts;

		public CountInput(Coord fromCoord, Coord toCoord, String descr, double counts) {
			this.fromCoord = fromCoord;
			this.toCoord = toCoord;
			this.descr = descr;
			this.counts = counts;
		}
	}

}
