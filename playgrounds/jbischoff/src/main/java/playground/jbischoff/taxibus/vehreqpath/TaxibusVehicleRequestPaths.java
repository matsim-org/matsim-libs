package playground.jbischoff.taxibus.vehreqpath;

import java.util.Comparator;


public class TaxibusVehicleRequestPaths
{

    public static final TaxibusVehicleRequestPathCost TW_COST = new TaxibusVehicleRequestPathCost() {
        @Override
        public double getCost(TaxibusVehicleRequestPath vrp)
        {
            return TaxibusVehicleRequestPaths.getPickupBeginTime(vrp) - vrp.getT0();
        }
    };

    public static final TaxibusVehicleRequestPathCost TP_COST = new TaxibusVehicleRequestPathCost() {
        @Override
        public double getCost(TaxibusVehicleRequestPath vrp)
        {
            return TaxibusVehicleRequestPaths.getPickupBeginTime(vrp) - vrp.path.get(0).getDepartureTime();
        }
    };

   

    public static double getPickupBeginTime(TaxibusVehicleRequestPath vrp)
    {
        return Math.max(vrp.getT0(), vrp.getArrivalTime());
    }
}
