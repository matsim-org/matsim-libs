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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.common.timeprofile.TimeDiscretizer;
import org.matsim.core.router.util.TravelTime;
import org.matsim.testcases.fakes.FakeLink;

import one.util.streamex.EntryStream;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DvrpOfflineTravelTimesTest {
	private final Id<Link> linkIdA = Id.createLinkId("A");
	private final Id<Link> linkIdB = Id.createLinkId("B");

	@Test
	void saveLinkTravelTimes() throws IOException {
		//the matrix may have more than 2 rows (depends on how many link ids are cached)
		var linkTTs = new double[Id.getNumberOfIds(Link.class)][];

		linkTTs[linkIdA.index()] = new double[] { 0.1, 1.1, 2.2, 3.3, 4.4 };
		linkTTs[linkIdB.index()] = new double[] { 5.5, 6.6, 7.7, 8.8, 9.9 };

		var stringWriter = new StringWriter();
		DvrpOfflineTravelTimes.saveLinkTravelTimes(new TimeDiscretizer(3600, 900), linkTTs, stringWriter);

		var lines = stringWriter.toString().split("\n");
		assertThat(lines).hasSize(3);
		assertThat(lines[0].split(";")).containsExactly("linkId", "0.0", "900.0", "1800.0", "2700.0", "3600.0");
		assertThat(lines[1].split(";")).containsExactly("A", "1", "2", "3", "4", "5");
		assertThat(lines[2].split(";")).containsExactly("B", "6", "7", "8", "9", "10");
	}

	@Test
	void loadLinkTravelTimes() throws IOException {
		var line0 = String.join(";", "linkId", "0.0", "900.0", "1800.0", "2700.0", "3600.0");
		var line1 = String.join(";", "A", 0.1 + "", 1.1 + "", 2.2 + "", 3.3 + "", 4.4 + "");
		var line2 = String.join(";", "B", 5.5 + "", 6.6 + "", 7.7 + "", 8.8 + "", 9.9 + "");
		var lines = String.join("\n", line0, line1, line2);

		var stringReader = new BufferedReader(new StringReader(lines));
		var linkTTs = DvrpOfflineTravelTimes.loadLinkTravelTimes(new TimeDiscretizer(3600, 900), stringReader);

		//the matrix may have more than 2 rows (depends on how many link ids are cached)
		//all rows are null (except for links A and B)
		var existingLinkTTs = EntryStream.of(linkTTs).filterValues(Objects::nonNull).toMap();

		assertThat(existingLinkTTs).containsOnlyKeys(linkIdA.index(), linkIdB.index());
		assertThat(existingLinkTTs.get(linkIdA.index())).containsExactly(0.1, 1.1, 2.2, 3.3, 4.4);
		assertThat(existingLinkTTs.get(linkIdB.index())).containsExactly(5.5, 6.6, 7.7, 8.8, 9.9);
	}

	@Test
	void convertToLinkTravelTimes() {
		var link = new FakeLink(Id.createLinkId("link_A"));
		var timeDiscretizer = new TimeDiscretizer(100, 100);

		var travelTime = mock(TravelTime.class);
		when(travelTime.getLinkTravelTime(eq(link), eq(0.), isNull(), isNull())).thenReturn(100.);// bin 0
		when(travelTime.getLinkTravelTime(eq(link), eq(50.), isNull(), isNull())).thenReturn(110.);// ignored
		when(travelTime.getLinkTravelTime(eq(link), eq(100.), isNull(), isNull())).thenReturn(150.);// bin 1

		var matrix = DvrpOfflineTravelTimes.convertToLinkTravelTimeMatrix(travelTime, List.of(link), timeDiscretizer);

		assertThat(Arrays.stream(matrix).filter(Objects::nonNull)).hasSize(1);
		assertThat(matrix[link.getId().index()]).containsExactly(100, 150);
	}

	@Test
	void asTravelTime() {
		var linkA = new FakeLink(Id.createLinkId("link_A"));
		var linkB = new FakeLink(Id.createLinkId("link_B"));
		var timeDiscretizer = new TimeDiscretizer(100, 100);

		var matrix = new double[Id.getNumberOfIds(Link.class)][];
		matrix[linkA.getId().index()] = new double[] { 100, 150 };

		var travelTime = DvrpOfflineTravelTimes.asTravelTime(timeDiscretizer, matrix);

		assertThat(travelTime.getLinkTravelTime(linkA, 0, null, null)).isEqualTo(100);
		assertThat(travelTime.getLinkTravelTime(linkA, 99, null, null)).isEqualTo(100);
		assertThat(travelTime.getLinkTravelTime(linkA, 100, null, null)).isEqualTo(150);

		assertThatThrownBy(() -> travelTime.getLinkTravelTime(linkB, 0, null, null)).isExactlyInstanceOf(
				NullPointerException.class)
				.hasMessage("Link (%s) does not belong to network. No travel time data.", linkB.getId());
	}
}
