/* *********************************************************************** *
 * project: org.matsim.* 												   *
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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
package ch.sbb.matsim.analysis.skims;

import ch.sbb.matsim.analysis.skims.RooftopUtils.ODConnection;
import org.junit.jupiter.api.Assertions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.matsim.core.utils.misc.Time;

/**
 * @author mrieser
 */
public class RooftopUtilsTest {

	@Test
	void testSortAndFilterConnections() {
        List<ODConnection> connections = new ArrayList<>();

        // we'll misuse the transferCount as a connection identifier
        // 15-min headway
        connections.add(new ODConnection(Time.parseTime("08:05:00"), 600, 60, 150, 0, null));
        connections.add(new ODConnection(Time.parseTime("08:20:00"), 600, 60, 150, 5, null));
        connections.add(new ODConnection(Time.parseTime("08:35:00"), 600, 60, 150, 3, null));
        connections.add(new ODConnection(Time.parseTime("08:50:00"), 600, 60, 150, 1, null));
        connections.add(new ODConnection(Time.parseTime("09:05:00"), 600, 60, 150, 4, null));

        // two special, fast courses
        connections.add(new ODConnection(Time.parseTime("08:22:00"), 300, 60, 150, 2, null));
        connections.add(new ODConnection(Time.parseTime("08:48:00"), 300, 60, 150, 6, null));

        // randomize the list. instead of randomizing, we sort the connections by transferCount which we misused to specify an order

        connections.sort((c1, c2) -> Double.compare(c1.transferCount, c2.transferCount));

        connections = RooftopUtils.sortAndFilterConnections(connections, 9 * 3600);

        // connection 5 (dep 10900) should be dominated by connection 2 (dep 11000)
        // connection 1 (dep 12700) should be dominated by connection 6 (dep 12600)

        Assertions.assertEquals(5, connections.size());
        Assertions.assertEquals(0, connections.get(0).transferCount, 0.0);
        Assertions.assertEquals(2, connections.get(1).transferCount, 0.0);
        Assertions.assertEquals(3, connections.get(2).transferCount, 0.0);
        Assertions.assertEquals(6, connections.get(3).transferCount, 0.0);
        Assertions.assertEquals(4, connections.get(4).transferCount, 0.0);
    }

	@Test
	void testCalcAverageAdaptionTime_1() {
        List<ODConnection> connections = new ArrayList<>();

        // 15-min headway, starting at 07:50, so the first rooftop is short and the last rooftop is cut
        connections.add(new ODConnection(Time.parseTime("07:50:00"), 600, 60, 150, 0, null));
        connections.add(new ODConnection(Time.parseTime("08:05:00"), 600, 60, 150, 0, null));
        connections.add(new ODConnection(Time.parseTime("08:20:00"), 600, 60, 150, 0, null));
        connections.add(new ODConnection(Time.parseTime("08:35:00"), 600, 60, 150, 0, null));
        connections.add(new ODConnection(Time.parseTime("08:50:00"), 600, 60, 150, 0, null));
        connections.add(new ODConnection(Time.parseTime("09:05:00"), 600, 60, 150, 0, null));

        double adaptionTime = RooftopUtils.calcAverageAdaptionTime(connections, Time.parseTime("08:00:00"), Time.parseTime("09:00:00"));
        // there is a departure every 900 seconds, max adaption time would be 450, average of that would be 225.0.
        Assertions.assertEquals(225, adaptionTime, 1e-7);
        // the frequency would be 3600 / 225 / 4 = 4.0

        // two special, fast courses
        connections.add(new ODConnection(Time.parseTime("08:22:00"), 300, 60, 150, 0, null));
        connections.add(new ODConnection(Time.parseTime("08:48:00"), 300, 60, 150, 0, null));

        connections = RooftopUtils.sortAndFilterConnections(connections, 9 * 3600);
        Assertions.assertEquals(6, connections.size());

        // there should now be departures at 08:05, 08:22, 08:35, 08:48, 09:05
        // resulting in a slightly higher adaption time

        adaptionTime = RooftopUtils.calcAverageAdaptionTime(connections, Time.parseTime("08:00:00"), Time.parseTime("09:00:00"));
        Assertions.assertEquals(254, adaptionTime, 1e-7);
        // the frequency would be 3600 / 254 / 4 = 3.5433

        connections.add(new ODConnection(Time.parseTime("08:15:00"), 300, 60, 150, 0, null));

        connections = RooftopUtils.sortAndFilterConnections(connections, 9 * 3600);
        Assertions.assertEquals(7, connections.size());

        adaptionTime = RooftopUtils.calcAverageAdaptionTime(connections, Time.parseTime("08:00:00"), Time.parseTime("09:00:00"));
        Assertions.assertEquals(219, adaptionTime, 1e-7);
        // the frequency would be 3600 / 219 / 4 = 4.1096
    }

	@Test
	void testCalcAverageAdaptionTime_2() {
        List<ODConnection> connections = new ArrayList<>();

        // 15-min headway, starting at 07:59 (so the first rooftop is cut, and the last is short)
        connections.add(new ODConnection(Time.parseTime("07:59:00"), 600, 60, 150, 0, null));
        connections.add(new ODConnection(Time.parseTime("08:14:00"), 600, 60, 150, 0, null));
        connections.add(new ODConnection(Time.parseTime("08:29:00"), 600, 60, 150, 0, null));
        connections.add(new ODConnection(Time.parseTime("08:44:00"), 600, 60, 150, 0, null));
        connections.add(new ODConnection(Time.parseTime("08:59:00"), 600, 60, 150, 0, null));
        connections.add(new ODConnection(Time.parseTime("09:14:00"), 600, 60, 150, 0, null));

        double adaptionTime = RooftopUtils.calcAverageAdaptionTime(connections, Time.parseTime("08:00:00"), Time.parseTime("09:00:00"));
        // there is a departure every 900 seconds, max adaption time would be 450, average of that would be 225.0.
        Assertions.assertEquals(225, adaptionTime, 1e-7);
        // the frequency would be 3600 / 225 / 4 = 4.0
    }

	@Test
	void testCalcAverageAdaptionTime_noEarlierDeparture() {
        List<ODConnection> connections = new ArrayList<>();

        ODConnection c0, c1;
        connections.add(c0 = new ODConnection(Time.parseTime("07:46:00"), 16080, 360, 125, 5, null));
        connections.add(c1 = new ODConnection(Time.parseTime("08:46:00"), 16080, 360, 125, 5, null));

        double adaptionTime = RooftopUtils.calcAverageAdaptionTime(connections, Time.parseTime("07:00:00"), Time.parseTime("08:00:00"));

        // important: there is no earlier connection, esp. no connection before the start time of 7am.

        // actual departure time is 7:40 and 8:40
        // average adaption time should be: 20min between 7:00 and 7:40, and 10min between 7:40 and 8:00
        // ==> 40*20 + 20*10 = 800 + 200 = 1000 --> 1000 / 60 = 16.666min = 1000 seconds

        Assertions.assertEquals(1000.0, adaptionTime, 1e-7);
    }

	@Test
	void testCalcAverageAdaptionTime_noLaterDeparture() {
        List<ODConnection> connections = new ArrayList<>();

        ODConnection c0, c1;
        connections.add(c0 = new ODConnection(Time.parseTime("06:45:00"), 16080, 300, 125, 5, null));
        connections.add(c1 = new ODConnection(Time.parseTime("07:45:00"), 16080, 300, 125, 5, null));

        double adaptionTime = RooftopUtils.calcAverageAdaptionTime(connections, Time.parseTime("07:00:00"), Time.parseTime("08:00:00"));

        // actual departure time is 6:40 and 7:40, zenith is 7:10
        // average adaption time should be: 25min between 7:00 and 7:10, 15min between 7:10 and 7:40, 10min between 7:40 and 8:00
        // ==> 25*10 + 15*30 + 10*20 = 250 + 450 + 200 = 900 --> 900 seconds

        Assertions.assertEquals(900.0, adaptionTime, 1e-7);
    }

	@Test
	void testCalcAverageAdaptionTime_noDepartureInRange() {
        List<ODConnection> connections = new ArrayList<>();

        ODConnection c0, c1;
        connections.add(c0 = new ODConnection(Time.parseTime("06:45:00"), 16080, 300, 125, 5, null));
        connections.add(c1 = new ODConnection(Time.parseTime("08:45:00"), 16080, 300, 125, 5, null));

        double adaptionTime = RooftopUtils.calcAverageAdaptionTime(connections, Time.parseTime("07:00:00"), Time.parseTime("08:00:00"));

        // actual departure time is 6:40 and 8:40, zenith is 7:40
        // average adaption time should be: 40min between 7:00 and 7:40, 50min between 7:40 and 8:00
        // ==> 40*40 + 50*20 = 1600 + 1000 = 2600 --> 2600 seconds

        Assertions.assertEquals(2600.0, adaptionTime, 1e-7);
    }

	@Test
	void testCalcAverageAdaptionTime_singleDepartureEarly() {
        List<ODConnection> connections = new ArrayList<>();

        ODConnection c0, c1;
        connections.add(c0 = new ODConnection(Time.parseTime("06:15:00"), 16080, 300, 125, 5, null));

        double adaptionTime = RooftopUtils.calcAverageAdaptionTime(connections, Time.parseTime("07:00:00"), Time.parseTime("08:00:00"));

        // actual departure time is 6:10
        // average adaption time should be: 80min between 7:00 and 8:00
        // ==> 80*60 = 4800 --> 4800 seconds

        Assertions.assertEquals(4800.0, adaptionTime, 1e-7);
    }

	@Test
	void testCalcAverageAdaptionTime_singleDepartureInRange() {
        List<ODConnection> connections = new ArrayList<>();

        ODConnection c0;
        connections.add(c0 = new ODConnection(Time.parseTime("07:15:00"), 16080, 300, 125, 5, null));

        double adaptionTime = RooftopUtils.calcAverageAdaptionTime(connections, Time.parseTime("07:00:00"), Time.parseTime("08:00:00"));

        // actual departure time is 7:10
        // average adaption time should be: 5min between 7:00 and 7:10, 25min between 7:10 and 8:00
        // ==> 5*10 + 25*50 = 50 + 1250 = 1300 --> 1300 seconds

        Assertions.assertEquals(1300.0, adaptionTime, 1e-7);
    }

	@Test
	void testCalcAverageAdaptionTime_singleDepartureLate() {
        List<ODConnection> connections = new ArrayList<>();

        ODConnection c0;
        connections.add(c0 = new ODConnection(Time.parseTime("08:15:00"), 16080, 300, 125, 5, null));

        double adaptionTime = RooftopUtils.calcAverageAdaptionTime(connections, Time.parseTime("07:00:00"), Time.parseTime("08:00:00"));

        // actual departure time is 8:10
        // average adaption time should be: 40min between 7:00 and 8:00
        // ==> 40*60 = 2400 --> 2400 seconds

        Assertions.assertEquals(2400.0, adaptionTime, 1e-7);
    }

	@Test
	void testCalcConnectionShares() {
        List<ODConnection> connections = new ArrayList<>();

        // 15-min headway
        ODConnection c0, c1, c2, c3, c4, c5, c6, c7, c8;
        connections.add(c0 = new ODConnection(Time.parseTime("07:50:00"), 600, 60, 150, 0, null));
        connections.add(c1 = new ODConnection(Time.parseTime("08:05:00"), 600, 60, 150, 0, null));
        connections.add(c2 = new ODConnection(Time.parseTime("08:20:00"), 600, 60, 150, 0, null));
        connections.add(c3 = new ODConnection(Time.parseTime("08:35:00"), 600, 60, 150, 0, null));
        connections.add(c4 = new ODConnection(Time.parseTime("08:50:00"), 600, 60, 150, 0, null));
        connections.add(c5 = new ODConnection(Time.parseTime("09:05:00"), 600, 60, 150, 0, null));

        Map<ODConnection, Double> shares = RooftopUtils.calcConnectionShares(connections, Time.parseTime("08:00:00"), Time.parseTime("09:00:00"));
        // there is a departure every 900 seconds, max adaption time would be 450, average of that would be 225.0.
        // every connection should have the same share, i.e. 1/4th = 0.25, but the first and the last only cover part of the time, so they have less
        Assertions.assertEquals(6, shares.size());
        Assertions.assertEquals(0.0 / 60.0, shares.get(c0), 1e-7);
        Assertions.assertEquals(11.5 / 60.0, shares.get(c1), 1e-7);
        Assertions.assertEquals(15.0 / 60.0, shares.get(c2), 1e-7);
        Assertions.assertEquals(15.0 / 60.0, shares.get(c3), 1e-7);
        Assertions.assertEquals(15.0 / 60.0, shares.get(c4), 1e-7);
        Assertions.assertEquals(3.5 / 60.0, shares.get(c5), 1e-7);

        // two special, fast courses
        connections.add(c6 = new ODConnection(Time.parseTime("08:22:00"), 300, 60, 150, 0, null));
        connections.add(c7 = new ODConnection(Time.parseTime("08:48:00"), 300, 60, 150, 0, null));

        connections = RooftopUtils.sortAndFilterConnections(connections, 9 * 3600);
        Assertions.assertEquals(6, connections.size());
        // there should now be departures at 08:05, 08:22, 08:35, 08:48, 09:05

        shares = RooftopUtils.calcConnectionShares(connections, Time.parseTime("08:00:00"), Time.parseTime("09:00:00"));

        Assertions.assertEquals(6, shares.size());
        Assertions.assertEquals(10.0 / 60.0, shares.get(c1), 1e-7);
        Assertions.assertEquals(20.0 / 60.0, shares.get(c6), 1e-7);
        Assertions.assertEquals(8.0 / 60.0, shares.get(c3), 1e-7);
        Assertions.assertEquals(20.0 / 60.0, shares.get(c7), 1e-7);
        Assertions.assertEquals(2.0 / 60.0, shares.get(c5), 1e-7);

        connections.add(c8 = new ODConnection(Time.parseTime("08:15:00"), 300, 60, 150, 0, null));

        connections = RooftopUtils.sortAndFilterConnections(connections, 9 * 3600);
        Assertions.assertEquals(7, connections.size());
        // there should now be departures at 08:05, 08:15, 08:22, 08:35, 08:48, 09:05

        shares = RooftopUtils.calcConnectionShares(connections, Time.parseTime("08:00:00"), Time.parseTime("09:00:00"));

        Assertions.assertEquals(7, shares.size());
        Assertions.assertEquals(6.5 / 60.0, shares.get(c1), 1e-7);
        Assertions.assertEquals(11.0 / 60.0, shares.get(c8), 1e-7);
        Assertions.assertEquals(12.5 / 60.0, shares.get(c6), 1e-7);
        Assertions.assertEquals(8.0 / 60.0, shares.get(c3), 1e-7);
        Assertions.assertEquals(20.0 / 60.0, shares.get(c7), 1e-7);
        Assertions.assertEquals(2.0 / 60.0, shares.get(c5), 1e-7);
    }

	@Test
	void testCalculationShares_noEarlierDeparture() {
        List<ODConnection> connections = new ArrayList<>();

        ODConnection c0, c1;
        connections.add(c0 = new ODConnection(Time.parseTime("07:46:00"), 16080, 360, 125, 5, null));
        connections.add(c1 = new ODConnection(Time.parseTime("08:46:00"), 16080, 360, 125, 5, null));

        Map<ODConnection, Double> shares = RooftopUtils.calcConnectionShares(connections, Time.parseTime("07:00:00"), Time.parseTime("08:00:00"));

        // important: there is no earlier connection, esp. no connection before the start time of 7am.

        double sum = 0;
        for (Map.Entry<ODConnection, Double> e : shares.entrySet()) {
            ODConnection c = e.getKey();
            Double share = e.getValue();
            sum += share;

            System.out.println(Time.writeTime(c.departureTime) + " --> " + share);
        }

        Assertions.assertEquals(1.0, sum, 1e-7);

        Assertions.assertEquals(1.0, shares.get(c0), 1e-7);
        Assertions.assertEquals(0.0, shares.get(c1), 1e-7);
    }

	@Test
	void testCalculationShares_noEarlierDeparture2() {
        List<ODConnection> connections = new ArrayList<>();

        ODConnection c0, c1;
        connections.add(c0 = new ODConnection(Time.parseTime("07:15:00"), 16080, 300, 125, 5, null));
        connections.add(c1 = new ODConnection(Time.parseTime("08:15:00"), 16080, 300, 125, 5, null));

        Map<ODConnection, Double> shares = RooftopUtils.calcConnectionShares(connections, Time.parseTime("07:00:00"), Time.parseTime("08:00:00"));

        // important: there is no earlier connection, esp. no connection before the start time of 7am.

        // access time is 5 min, so actual departure time is 7:10 and 8:10, and the zenith at 7:40, which results in the share 40:20 = 2:1

        double sum = 0;
        for (Map.Entry<ODConnection, Double> e : shares.entrySet()) {
            ODConnection c = e.getKey();
            Double share = e.getValue();
            sum += share;

            System.out.println(Time.writeTime(c.departureTime) + " --> " + share);
        }

        Assertions.assertEquals(1.0, sum, 1e-7);

        Assertions.assertEquals(2.0 / 3.0, shares.get(c0), 1e-7);
        Assertions.assertEquals(1.0 / 3.0, shares.get(c1), 1e-7);
    }

	@Test
	void testCalculationShares_noLaterDeparture() {
        List<ODConnection> connections = new ArrayList<>();

        ODConnection c0, c1;
        connections.add(c0 = new ODConnection(Time.parseTime("06:45:00"), 16080, 300, 125, 5, null));
        connections.add(c1 = new ODConnection(Time.parseTime("07:45:00"), 16080, 300, 125, 5, null));

        Map<ODConnection, Double> shares = RooftopUtils.calcConnectionShares(connections, Time.parseTime("07:00:00"), Time.parseTime("08:00:00"));

        // important: there is no earlier connection, esp. no connection before the start time of 7am.

        // access time is 5 min, so actual departure time is 6:40 and 7:40, and the zenith at 7:10, resulting in the share 10:50 = 1:5

        double sum = 0;
        for (Map.Entry<ODConnection, Double> e : shares.entrySet()) {
            ODConnection c = e.getKey();
            Double share = e.getValue();
            sum += share;

            System.out.println(Time.writeTime(c.departureTime) + " --> " + share);
        }

        Assertions.assertEquals(1.0, sum, 1e-7);

        Assertions.assertEquals(1.0 / 6.0, shares.get(c0), 1e-7);
        Assertions.assertEquals(5.0 / 6.0, shares.get(c1), 1e-7);
    }

	@Test
	void testCalculationShares_noDepartureInRange() {
        List<ODConnection> connections = new ArrayList<>();

        ODConnection c0, c1;
        connections.add(c0 = new ODConnection(Time.parseTime("06:45:00"), 16080, 300, 125, 5, null));
        connections.add(c1 = new ODConnection(Time.parseTime("08:45:00"), 16080, 300, 125, 5, null));

        Map<ODConnection, Double> shares = RooftopUtils.calcConnectionShares(connections, Time.parseTime("07:00:00"), Time.parseTime("08:00:00"));

        // important: there is no earlier connection, esp. no connection before the start time of 7am.

        // access time is 5 min, so actual departure time is 6:40 and 8:40, and the zenith at 7:40, which results in the share 40:20 = 2:1

        double sum = 0;
        for (Map.Entry<ODConnection, Double> e : shares.entrySet()) {
            ODConnection c = e.getKey();
            Double share = e.getValue();
            sum += share;

            System.out.println(Time.writeTime(c.departureTime) + " --> " + share);
        }

        Assertions.assertEquals(1.0, sum, 1e-7);

        Assertions.assertEquals(2.0 / 3.0, shares.get(c0), 1e-7);
        Assertions.assertEquals(1.0 / 3.0, shares.get(c1), 1e-7);
    }

	@Test
	void testCalculationShares_singleDepartureEarly() {
        List<ODConnection> connections = new ArrayList<>();

        ODConnection c0, c1;
        connections.add(c0 = new ODConnection(Time.parseTime("06:15:00"), 16080, 245, 125, 5, null));

        Map<ODConnection, Double> shares = RooftopUtils.calcConnectionShares(connections, Time.parseTime("07:00:00"), Time.parseTime("08:00:00"));
        Assertions.assertEquals(1.0, shares.get(c0), 1e-7);
    }

	@Test
	void testCalculationShares_singleDepartureInRange() {
        List<ODConnection> connections = new ArrayList<>();

        ODConnection c0, c1;
        connections.add(c0 = new ODConnection(Time.parseTime("07:15:00"), 16080, 245, 125, 5, null));

        Map<ODConnection, Double> shares = RooftopUtils.calcConnectionShares(connections, Time.parseTime("07:00:00"), Time.parseTime("08:00:00"));
        Assertions.assertEquals(1.0, shares.get(c0), 1e-7);
    }

	@Test
	void testCalculationShares_singleDepartureLate() {
        List<ODConnection> connections = new ArrayList<>();

        ODConnection c0, c1;
        connections.add(c0 = new ODConnection(Time.parseTime("08:15:00"), 16080, 245, 125, 5, null));

        Map<ODConnection, Double> shares = RooftopUtils.calcConnectionShares(connections, Time.parseTime("07:00:00"), Time.parseTime("08:00:00"));
        Assertions.assertEquals(1.0, shares.get(c0), 1e-7);
    }
}
