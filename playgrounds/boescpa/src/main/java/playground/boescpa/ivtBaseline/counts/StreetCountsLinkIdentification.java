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

package playground.boescpa.ivtBaseline.counts;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import playground.boescpa.lib.tools.NetworkUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

import static playground.boescpa.ivtBaseline.counts.CountsIVTBaseline.COUNTS_DELIMITER;

/**
 * Identifies for a given count-station(coord) the according street-link in the network.
 *
 * @author boescpa
 */
public class StreetCountsLinkIdentification {

	private static final Logger log = Logger.getLogger(StreetCountsLinkIdentification.class);
	private static final Counter counter = new Counter(" handled count input: ");
	private static final CoordinateTransformation transformation
			= TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.CH1903_LV03_Plus_GT);

	public static void main(final String[] args) {
		final String pathToNetwork = args[0];
		final String pathToCountsInput = args[1];
		final String pathToCountsOutput = args[2];

		Network network = NetworkUtils.readNetwork(pathToNetwork);
		List<CountInput> countInputs = readCountInputs(pathToCountsInput);
		Map<Id<Link>, CountInput> identifiedLinks = identifyLinks(network, countInputs);
		writeStreetCounts(pathToCountsOutput, identifiedLinks);
	}

	private static void writeStreetCounts(String pathToCountsOutput, Map<Id<Link>, CountInput> identifiedLinks) {
		BufferedWriter writer = IOUtils.getBufferedWriter(pathToCountsOutput);
		try {
			String header = "countStationId" + COUNTS_DELIMITER +
					"direction" + COUNTS_DELIMITER +
					"linkId";
			writer.write(header);
			writer.newLine();
			for (Id<Link> linkId : identifiedLinks.keySet()) {
				writer.write(identifiedLinks.get(linkId).id + COUNTS_DELIMITER);
				writer.write(identifiedLinks.get(linkId).directionDescr + COUNTS_DELIMITER);
				writer.write(linkId.toString());
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static Map<Id<Link>, CountInput> identifyLinks(Network network, List<CountInput> countInputs) {
		Map<Id<Link>, CountInput> identifiedLinks = new LinkedHashMap<>();
		counter.reset();
		for (CountInput countInput : countInputs) {
			Link nearestLink = org.matsim.core.network.NetworkUtils.getNearestLinkExactly(network, countInput.stationCoord);
			double distanceNearestLink = CoordUtils.calcEuclideanDistance(nearestLink.getToNode().getCoord(), countInput.directionCoord);
			double distanceOppositeDirection = CoordUtils.calcEuclideanDistance(nearestLink.getFromNode().getCoord(), countInput.directionCoord);
			if (distanceNearestLink > distanceOppositeDirection) {
				Link oppositeDirectionLink = getOppositeDirectionLink(nearestLink);
				if (oppositeDirectionLink != null) {
					nearestLink = oppositeDirectionLink;
				} else {
					int radius = 1, direction = 0;
					while (distanceNearestLink > distanceOppositeDirection) {
						direction -= 4*(direction/4); direction++;
						Coord movedStationCoord = moveCoord(countInput.stationCoord, radius, direction);
						nearestLink = org.matsim.core.network.NetworkUtils.getNearestLinkExactly(network, movedStationCoord);
						distanceNearestLink = CoordUtils.calcEuclideanDistance(nearestLink.getToNode().getCoord(), countInput.directionCoord);
						distanceOppositeDirection =
								CoordUtils.calcEuclideanDistance(nearestLink.getFromNode().getCoord(), countInput.directionCoord);
						radius += direction/4;
					}
				}
			}
			identifiedLinks.put(nearestLink.getId(), countInput);
			counter.incCounter();
		}
		return identifiedLinks;
	}

	private static Coord moveCoord(Coord stationCoord, int radius, int direction) {
		int moveDistance = 10; //meters
		Coord movedStationCoord = null;
		switch (direction) {
			case 1: movedStationCoord = new Coord(stationCoord.getX() + radius*moveDistance, stationCoord.getY()); break;
			case 2: movedStationCoord = new Coord(stationCoord.getX(), stationCoord.getY() + radius*moveDistance); break;
			case 3: movedStationCoord = new Coord(stationCoord.getX() - radius*moveDistance, stationCoord.getY()); break;
			case 4: movedStationCoord = new Coord(stationCoord.getX(), stationCoord.getY() - radius*moveDistance); break;
		}
		return movedStationCoord;
	}

	/**
	 * Attention: If no opposite direction link exists, the method returns null.
	 */
	private static Link getOppositeDirectionLink(Link nearestLink) {
		Link oppositeDirectionLink = null;
		for (Link linkCandidate : nearestLink.getToNode().getOutLinks().values()) {
			if (linkCandidate.getToNode().getId().toString().equals(nearestLink.getFromNode().getId().toString())) {
				oppositeDirectionLink = linkCandidate;
				break;
			}
		}
		return oppositeDirectionLink;
	}

	private static List<CountInput> readCountInputs(String pathToCountsInput) {
		List<CountInput> countInputs = new ArrayList<>();
		BufferedReader reader = IOUtils.getBufferedReader(pathToCountsInput, Charset.forName("UTF-8"));
		try {
			reader.readLine(); // read header:
			// countStationID	xCoord_CountStation	yCoord_CountStation	direction	longDirection	latDirection
			String line = reader.readLine();
			while (line != null) {
				String[] lineElements = line.split("\t");
				if (lineElements.length > 6) {
					String[] firstDirection = Arrays.copyOfRange(lineElements, 0, 6);
					countInputs.add(getCountInput(firstDirection));
					String[] secondDirection = (String[]) ArrayUtils.addAll(
							Arrays.copyOfRange(lineElements, 0, 3), Arrays.copyOfRange(lineElements, 6, 9));
					countInputs.add(getCountInput(secondDirection));
				} else {
					countInputs.add(getCountInput(lineElements));
				}
				line = reader.readLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return countInputs;
	}

	private static CountInput getCountInput(String[] lineElements) {
		return new CountInput(
				lineElements[0],
				new Coord(Double.parseDouble(lineElements[1]), Double.parseDouble(lineElements[2])),
				lineElements[3],
				transformation.transform(new Coord(Double.parseDouble(lineElements[4]), Double.parseDouble(lineElements[5])))
		);
	}

	private static class CountInput {
		final String id;
		final Coord stationCoord;
		final String directionDescr;
		final Coord directionCoord;

		public CountInput(String id, Coord stationCoord, String directionDescr, Coord directionCoord) {
			this.id = id;
			this.stationCoord = stationCoord;
			this.directionDescr = directionDescr;
			this.directionCoord = directionCoord;
		}
	}
}
