package playground.dhosse.prt;

import playground.michalm.taxi.vehreqpath.VehicleRequestPath;

public class VehicleRequestPaths
{
    public static final VehicleRequestPathCost TW_COST = new VehicleRequestPathCost() {
        @Override
        public double getCost(VehicleRequestPath vrp)
        {
            return VehicleRequestPaths.getPickupBeginTime(vrp) - vrp.request.getT0();
        }
    };

    public static final VehicleRequestPathCost TP_COST = new VehicleRequestPathCost() {
        @Override
        public double getCost(VehicleRequestPath vrp)
        {
            return VehicleRequestPaths.getPickupBeginTime(vrp) - vrp.path.getDepartureTime();
        }
    };
    
    
    public static double getPickupBeginTime(VehicleRequestPath vrp)
    {
        return Math.max(vrp.request.getT0(), vrp.path.getArrivalTime());
    }
}
