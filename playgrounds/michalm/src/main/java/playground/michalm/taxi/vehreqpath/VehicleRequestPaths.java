package playground.michalm.taxi.vehreqpath;

import java.util.Comparator;


public class VehicleRequestPaths
{
    public static final Comparator<VehicleRequestPath> TW_COMPARATOR = new Comparator<VehicleRequestPath>() {
        @Override
        public int compare(VehicleRequestPath vrp1, VehicleRequestPath vrp2)
        {
            double tw1 = calculatePickupBeginTime(vrp1) - vrp1.request.getT0();
            double tw2 = calculatePickupBeginTime(vrp2) - vrp2.request.getT0();
            return Double.compare(tw1, tw2);
        }
    };

    public static final Comparator<VehicleRequestPath> TP_COMPARATOR = new Comparator<VehicleRequestPath>() {
        @Override
        public int compare(VehicleRequestPath vrp1, VehicleRequestPath vrp2)
        {
            double tw1 = calculatePickupBeginTime(vrp1) - vrp1.path.getDepartureTime();
            double tw2 = calculatePickupBeginTime(vrp2) - vrp2.path.getDepartureTime();
            return Double.compare(tw1, tw2);
        }
    };


    public static double calculatePickupBeginTime(VehicleRequestPath vrp)
    {
        return Math.max(vrp.request.getT0(), vrp.path.getArrivalTime());
    }
}
