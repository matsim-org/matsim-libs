package playground.jbischoff.taxi.vehicles;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.transEnergySim.vehicles.api.BatteryElectricVehicle;


public class ElectricTaxi 
{

    /**
     * A wrapping class combining both the Vehicle from TransengerySim and DVRP taxi. It may or may
     * not be a BEV (mixed fleets)
     * 
     * @param args
     */
    private final Id<ElectricTaxi> vehicleId;
    private final BatteryElectricVehicle bev;
    private final Vehicle veh;
    private final boolean isElectric;


    public ElectricTaxi(BatteryElectricVehicle bev, Vehicle veh)
    {
        this.bev = bev;
        this.veh = veh;
        this.vehicleId = Id.create(bev.getId(),ElectricTaxi.class);
        this.isElectric = true;
    }


    public Id<ElectricTaxi> getVehicleId()
    {
        return vehicleId;
    }


    public BatteryElectricVehicle getBev()
    {
        return bev;
    }


    public Vehicle getVeh()
    {
        return veh;
    }


    public boolean isElectric()
    {
        return isElectric;
    }

}
