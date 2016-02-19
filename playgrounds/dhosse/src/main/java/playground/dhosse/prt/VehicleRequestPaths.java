package playground.dhosse.prt;

import org.matsim.contrib.taxi.optimizer.BestDispatchFinder;

public class VehicleRequestPaths
{
    public static final VehicleRequestPathCost TW_COST = new VehicleRequestPathCost() {
        @Override
        public double getCost(BestDispatchFinder.Dispatch vrp)
        {
            return VehicleRequestPaths.getPickupBeginTime(vrp) - vrp.request.getT0();
        }
    };

    public static final VehicleRequestPathCost TP_COST = new VehicleRequestPathCost() {
        @Override
        public double getCost(BestDispatchFinder.Dispatch vrp)
        {
            return VehicleRequestPaths.getPickupBeginTime(vrp) - vrp.path.getDepartureTime();
        }
    };
    
    
    public static double getPickupBeginTime(BestDispatchFinder.Dispatch vrp)
    {
        return Math.max(vrp.request.getT0(), vrp.path.getArrivalTime());
    }
}
