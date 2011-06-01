package playground.michalm.vrp.sim;

import org.matsim.api.core.v01.*;
import org.matsim.core.population.*;

import pl.poznan.put.vrp.dynamic.data.model.*;


public class VRPDriverPerson
    extends PersonImpl
{
    private Vehicle vrpVehicle;


    public VRPDriverPerson(final Id id, Vehicle vrpVehicle)
    {
        super(id);
        this.vrpVehicle = vrpVehicle;
    }


    public Vehicle getVrpVehicle()
    {
        return vrpVehicle;
    }
}
