package playground.dhosse.prt;

import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.optimizer.BestDispatchFinder;

public class VehicleRequestPaths
{
    public static final VehicleRequestPathCost TW_COST = new VehicleRequestPathCost() {
        @Override
        public double getCost(BestDispatchFinder.Dispatch<TaxiRequest> vrp)
        {
            return VehicleRequestPaths.getPickupBeginTime(vrp) - vrp.destination.getT0();
        }
    };

    public static final VehicleRequestPathCost TP_COST = new VehicleRequestPathCost() {
        @Override
        public double getCost(BestDispatchFinder.Dispatch<TaxiRequest> vrp)
        {
            return VehicleRequestPaths.getPickupBeginTime(vrp) - vrp.path.getDepartureTime();
        }
    };
    
    
    public static double getPickupBeginTime(BestDispatchFinder.Dispatch<TaxiRequest> vrp)
    {
        return Math.max(vrp.destination.getT0(), vrp.path.getArrivalTime());
    }
}
