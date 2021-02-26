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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Objects;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import one.util.streamex.EntryStream;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DvrpOfflineTravelTimesTest {
	private final Id<Link> linkIdA = Id.createLinkId("A");
	private final Id<Link> linkIdB = Id.createLinkId("B");

	@Test
	public void saveLinkTravelTimes() throws IOException {
		//the matrix may have more than 2 rows (depends on how many link ids are cached)
		var linkTTs = new double[Id.getNumberOfIds(Link.class)][];

		linkTTs[linkIdA.index()] = new double[] { 0.0, 1.1, 2.2, 3.3 };
		linkTTs[linkIdB.index()] = new double[] { 5.5, 6.6, 7.7, 8.8 };

		var stringWriter = new StringWriter();
		DvrpOfflineTravelTimes.saveLinkTravelTimes(900, 4, linkTTs, stringWriter);

		var lines = stringWriter.toString().split("\n");
		assertThat(lines).hasSize(3);
		assertThat(lines[0].split(";")).containsExactly("linkId", "0", "900", "1800", "2700");
		assertThat(lines[1].split(";")).containsExactly("A", 0.0 + "", 1.1 + "", 2.2 + "", 3.3 + "");
		assertThat(lines[2].split(";")).containsExactly("B", 5.5 + "", 6.6 + "", 7.7 + "", 8.8 + "");
	}

	@Test
	public void loadLinkTravelTimes() throws IOException {
		var line0 = String.join(";", "linkId", "0", "900", "1800", "2700");
		var line1 = String.join(";", "A", 0.0 + "", 1.1 + "", 2.2 + "", 3.3 + "");
		var line2 = String.join(";", "B", 5.5 + "", 6.6 + "", 7.7 + "", 8.8 + "");
		var lines = String.join("\n", line0, line1, line2);

		var stringReader = new BufferedReader(new StringReader(lines));
		var linkTTs = DvrpOfflineTravelTimes.loadLinkTravelTimes(900, 4, stringReader);

		//the matrix may have more than 2 rows (depends on how many link ids are cached)
		//all rows are null (except for links A and B)
		var existingLinkTTs = EntryStream.of(linkTTs).filterValues(Objects::nonNull).toMap();

		assertThat(existingLinkTTs).containsOnlyKeys(linkIdA.index(), linkIdB.index());
		assertThat(existingLinkTTs.get(linkIdA.index())).containsExactly(0.0, 1.1, 2.2, 3.3);
		assertThat(existingLinkTTs.get(linkIdB.index())).containsExactly(5.5, 6.6, 7.7, 8.8);
	}
}
