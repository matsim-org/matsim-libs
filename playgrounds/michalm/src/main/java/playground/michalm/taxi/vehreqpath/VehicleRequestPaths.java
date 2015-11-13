package playground.michalm.taxi.vehreqpath;

import java.util.Comparator;


public class VehicleRequestPaths
{
    //    public static final Predicate<VehicleRequestPath> BELOW_TRIP_TIME_LIMIT = new Predicate<>() {
    //        @Override
    //        public boolean apply(VehicleRequestPath vrp)
    //        {
    //            return vrp.path.getTravelCost();
    //        }
    //    };

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

    public static final Comparator<VehicleRequestPath> TW_COMPARATOR = new Comparator<VehicleRequestPath>() {
        @Override
        public int compare(VehicleRequestPath vrp1, VehicleRequestPath vrp2)
        {
            return Double.compare(TW_COST.getCost(vrp1), TW_COST.getCost(vrp2));
        }
    };

    public static final Comparator<VehicleRequestPath> TP_COMPARATOR = new Comparator<VehicleRequestPath>() {
        @Override
        public int compare(VehicleRequestPath vrp1, VehicleRequestPath vrp2)
        {
            return Double.compare(TP_COST.getCost(vrp1), TP_COST.getCost(vrp2));
        }
    };


    public static double getPickupBeginTime(VehicleRequestPath vrp)
    {
        return Math.max(vrp.request.getT0(), vrp.path.getArrivalTime());
    }
}
