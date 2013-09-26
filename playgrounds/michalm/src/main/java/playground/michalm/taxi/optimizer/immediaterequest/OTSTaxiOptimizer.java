package playground.michalm.taxi.optimizer.immediaterequest;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.Vehicle;


public class OTSTaxiOptimizer
    extends ImmediateRequestTaxiOptimizer
{
    private final TaxiOptimizationPolicy optimizationPolicy;


    public OTSTaxiOptimizer(VrpData data, boolean destinationKnown, boolean minimizePickupTripTime,
            TaxiOptimizationPolicy optimizationPolicy)
    {
        super(data, destinationKnown, minimizePickupTripTime);
        this.optimizationPolicy = optimizationPolicy;
    }


    @Override
    protected boolean shouldOptimizeBeforeNextTask(Vehicle vehicle, boolean scheduleUpdated)
    {
        if (!scheduleUpdated) {// no changes
            return false;
        }

        return optimizationPolicy.shouldOptimize(vehicle.getSchedule().getCurrentTask());
    }


    @Override
    protected boolean shouldOptimizeAfterNextTask(Vehicle vehicle, boolean scheduleUpdated)
    {
        return false;
    }
}
