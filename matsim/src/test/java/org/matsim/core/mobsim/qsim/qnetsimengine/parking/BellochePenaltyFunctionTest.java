package org.matsim.core.mobsim.qsim.qnetsimengine.parking;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.util.Map;

class BellochePenaltyFunctionTest {

    @Test
    void everyLinkFull() {
        BellochePenaltyFunction bellochePenaltyFunction = new BellochePenaltyFunction(0.4, -6);
        double penalty = bellochePenaltyFunction.calculateParkingSearchTime(Map.of(getDummyLinkId(), new ParkingCount(1, 1, 1)));
        Assertions.assertEquals(0.4 * Math.exp(6), penalty); //161.37
    }

    @Test
    void everyLinkEmpty() {
        BellochePenaltyFunction bellochePenaltyFunction = new BellochePenaltyFunction(0.4, -6);
        double penalty = bellochePenaltyFunction.calculateParkingSearchTime(Map.of(getDummyLinkId(), new ParkingCount(0, 1, 1)));
        Assertions.assertEquals(0.4, penalty);
    }

    @Test
    void zeroCapacity() {
        BellochePenaltyFunction bellochePenaltyFunction = new BellochePenaltyFunction(0.4, -6);
        double penalty = bellochePenaltyFunction.calculateParkingSearchTime(Map.of(getDummyLinkId(), new ParkingCount(0, 0, 1)));
        Assertions.assertEquals(0.4 * Math.exp(6), penalty); //161.37
    }

    @Test
    void weightedSum() {
        double occ1 = 3;
        double cap1 = 4;
        double weight1 = 0.5;
        double occ2 = 1;
        double cap2 = 2;
        double weight2 = 0.8;

        BellochePenaltyFunction bellochePenaltyFunction = new BellochePenaltyFunction(0.4, -6);
        double penalty = bellochePenaltyFunction.calculateParkingSearchTime(Map.of(getDummyLinkId(), new ParkingCount(occ1, cap1, weight1), Id.createLinkId("dummyLinkId2"), new ParkingCount(occ2, cap2, weight2)));

        double weightedOcc = occ1 * weight1 + occ2 * weight2;
        double weightedK = cap1 * weight1 + cap2 * weight2;
        double expectedPenalty = 0.4 * Math.exp(6 * (weightedOcc / weightedK));

        Assertions.assertEquals(expectedPenalty, penalty); //161.37
    }

    private static Id<Link> getDummyLinkId() {
        return Id.createLinkId("dummyLinkId");
    }

}
