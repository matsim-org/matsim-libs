package org.matsim.application.automatedCalibration;

/**
 * The standard distance grouping used in many survey and statistics.
 * The distance grouping is as follows: 0-1000, 1000-2000, 2000-5000, 5000-10000, 10000-20000, 20000+
 */
public class StandardDistanceGrouping implements DistanceGrouping {
    private final String[] distanceGroupings = new String[]
            {"below 1km", "1km - 2km", "2km - 5km", "5km - 10km", "10km - 20km", "more than 20km"};

    @Override
    public String assignDistanceGroup(double distance) {
        if (distance < 1000) {
            return distanceGroupings[0];
        } else if (distance < 2000) {
            return distanceGroupings[1];
        } else if (distance < 5000) {
            return distanceGroupings[2];
        } else if (distance < 10000) {
            return distanceGroupings[3];
        } else if (distance < 20000) {
            return distanceGroupings[4];
        }
        return distanceGroupings[5];
    }

    @Override
    public String[] getDistanceGroupings() {
        return distanceGroupings;
    }
}
