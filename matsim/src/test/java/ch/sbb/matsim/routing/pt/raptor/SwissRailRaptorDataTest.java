/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2018.
 */

package ch.sbb.matsim.routing.pt.raptor;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author mrieser / SBB
 */
public class SwissRailRaptorDataTest {

    @Test
    public void testTransfersFromSchedule() {
        Fixture f = new Fixture();
        f.init();

        f.config.transitRouter().setMaxBeelineWalkConnectionDistance(100);
        RaptorStaticConfig raptorConfig = RaptorUtils.createStaticConfig(f.config);
        SwissRailRaptorData data = SwissRailRaptorData.create(f.schedule, raptorConfig, f.network);

        // check there is no transfer between stopFacility 19 and stopFacility 9
        Id<TransitStopFacility> stopId5 = Id.create(5, TransitStopFacility.class);
        Id<TransitStopFacility> stopId9 = Id.create(9, TransitStopFacility.class);
        Id<TransitStopFacility> stopId18 = Id.create(18, TransitStopFacility.class);
        Id<TransitStopFacility> stopId19 = Id.create(19, TransitStopFacility.class);
        for (SwissRailRaptorData.RTransfer t : data.transfers) {
            TransitStopFacility fromStop = data.routeStops[t.fromRouteStop].routeStop.getStopFacility();
            TransitStopFacility toStop = data.routeStops[t.toRouteStop].routeStop.getStopFacility();
            if (fromStop.getId().equals(stopId19) && toStop.getId().equals(stopId9)) {
                Assert.fail("There should not be any transfer between stop facilities 19 and 9.");
            }
        }

        // add a previously inexistant transfer

        f.schedule.getMinimalTransferTimes().set(stopId19, stopId9, 345);
        SwissRailRaptorData data2 = SwissRailRaptorData.create(f.schedule, raptorConfig, f.network);
        int foundTransferCount = 0;
        for (SwissRailRaptorData.RTransfer t : data2.transfers) {
            TransitStopFacility fromStop = data2.routeStops[t.fromRouteStop].routeStop.getStopFacility();
            TransitStopFacility toStop = data2.routeStops[t.toRouteStop].routeStop.getStopFacility();
            if (fromStop.getId().equals(stopId19) && toStop.getId().equals(stopId9)) {
                foundTransferCount++;
            }
        }
        Assert.assertEquals("wrong number of transfers between stop facilities 19 and 9.", 1, foundTransferCount);
        Assert.assertEquals("number of transfers should have incrased.", data.transfers.length + 1, data2.transfers.length);

        // assign a high transfer time to a "default" transfer
        f.schedule.getMinimalTransferTimes().set(stopId5, stopId18, 456);
        SwissRailRaptorData data3 = SwissRailRaptorData.create(f.schedule, raptorConfig, f.network);
        boolean foundCorrectTransfer = false;
        for (SwissRailRaptorData.RTransfer t : data3.transfers) {
            TransitStopFacility fromStop = data3.routeStops[t.fromRouteStop].routeStop.getStopFacility();
            TransitStopFacility toStop = data3.routeStops[t.toRouteStop].routeStop.getStopFacility();
            if (fromStop.getId().equals(stopId5) && toStop.getId().equals(stopId18)) {
                Assert.assertEquals("transfer has wrong transfer time.", 456, t.transferTime, 0.0);
                foundCorrectTransfer = true;
            }
        }
        Assert.assertTrue("did not find overwritten transfer", foundCorrectTransfer);
        Assert.assertEquals("number of transfers should have stayed the same.", data2.transfers.length, data3.transfers.length);

        // assign a low transfer time to a "default" transfer
        f.schedule.getMinimalTransferTimes().set(stopId5, stopId18, 0.2);
        SwissRailRaptorData data4 = SwissRailRaptorData.create(f.schedule, raptorConfig, f.network);
        foundCorrectTransfer = false;
        for (SwissRailRaptorData.RTransfer t : data4.transfers) {
            TransitStopFacility fromStop = data4.routeStops[t.fromRouteStop].routeStop.getStopFacility();
            TransitStopFacility toStop = data4.routeStops[t.toRouteStop].routeStop.getStopFacility();
            if (fromStop.getId().equals(stopId5) && toStop.getId().equals(stopId18)) {
                Assert.assertEquals("transfer has wrong transfer time.", 0.2, t.transferTime, 0.0);
                foundCorrectTransfer = true;
            }
        }
        Assert.assertTrue("did not find overwritten transfer", foundCorrectTransfer);
        Assert.assertEquals("number of transfers should have stayed the same.", data2.transfers.length, data4.transfers.length);
    }

}