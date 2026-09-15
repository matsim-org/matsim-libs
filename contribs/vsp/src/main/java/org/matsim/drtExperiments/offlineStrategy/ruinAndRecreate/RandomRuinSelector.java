package org.matsim.drtExperiments.offlineStrategy.ruinAndRecreate;

import org.matsim.drtExperiments.basicStructures.FleetSchedules;
import org.matsim.drtExperiments.basicStructures.GeneralRequest;
import org.matsim.drtExperiments.basicStructures.TimetableEntry;

import java.util.*;

public class RandomRuinSelector implements RuinSelector {
    private final Random random;
    private final static double PROPORTION_TO_REMOVE = 0.3;

    public RandomRuinSelector(Random random) {
        this.random = random;
    }

    @Override
    public List<GeneralRequest> selectRequestsToBeRuined(FleetSchedules fleetSchedules) {
        List<GeneralRequest> openRequests = new ArrayList<>();
        for (List<TimetableEntry> timetable : fleetSchedules.vehicleToTimetableMap().values()) {
            timetable.stream().filter(s -> s.getStopType() == TimetableEntry.StopType.PICKUP).forEach(s -> openRequests.add(s.getRequest()));
        }

        Collections.shuffle(openRequests, random);
        int numToRemoved = (int) (openRequests.size() * PROPORTION_TO_REMOVE) + 1;
        int maxRemoval = 1000;
        numToRemoved = Math.min(numToRemoved, maxRemoval);
        numToRemoved = Math.min(numToRemoved, openRequests.size());
        List<GeneralRequest> requestsToBeRuined = new ArrayList<>();
        for (int i = 0; i < numToRemoved; i++) {
            requestsToBeRuined.add(openRequests.get(i));
        }
        return requestsToBeRuined;
    }
}
