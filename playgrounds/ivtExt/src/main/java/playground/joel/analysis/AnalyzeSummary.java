package playground.joel.analysis;

import ch.ethz.idsc.tensor.Tensor;

import java.io.Serializable;

/**
 * Created by Joel on 28.06.2017.
 */
public class AnalyzeSummary implements Serializable {
    // public CoreAnalysis coreAnalysis;
    // public DistanceAnalysis distanceAnalysis;

    public int numRequests;
    public int numVehicles;

    public double occupancyRatio ;
    public double distance ;
    public double distanceWithCust;
    public double distancePickup;
    public double distanceRebalance;
    public double distanceRatio;
    public Tensor totalWaitTimeQuantile;
    public Tensor totalWaitTimeMean;
    public double maximumWaitTime;

}
