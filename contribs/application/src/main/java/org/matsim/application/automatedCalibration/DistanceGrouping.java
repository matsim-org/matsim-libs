package org.matsim.application.automatedCalibration;

public interface DistanceGrouping {
    /**
     * Assign the distance group of a trip based on distance (in meter)
     */
    String assignDistanceGroup(double distance);

    String[] getDistanceGroupings();

}
