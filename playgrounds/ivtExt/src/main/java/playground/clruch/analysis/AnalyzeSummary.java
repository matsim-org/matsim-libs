package playground.clruch.analysis;

import java.io.Serializable;

import ch.ethz.idsc.tensor.Tensor;

/**
 * Created by Joel on 28.06.2017.
 */
public class AnalyzeSummary implements Serializable {

    public int numRequests;
    public int numVehicles;

    public String computationTime; // from begin of first iteration till end of the last

    public double occupancyRatio;
    public double distance;
    public double distanceWithCust;
    public double distancePickup;
    public double distanceRebalance;
    public double distanceRatio;
    public Tensor totalDistancesPerVehicle;
    public Tensor distancesWCPerVehicle;
    public Tensor totalWaitTimeQuantile;
    public Tensor totalWaitTimeMean;
    public double maximumWaitTime;

}
