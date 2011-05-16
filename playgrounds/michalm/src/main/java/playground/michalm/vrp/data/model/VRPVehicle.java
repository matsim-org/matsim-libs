package playground.michalm.vrp.data.model;

import org.matsim.api.core.v01.network.*;

import pl.poznan.put.vrp.dynamic.data.model.*;


public class VRPVehicle
    extends Vehicle
{
    public VRPRoute getVRPRoute()
    {
        return (VRPRoute)route;
    }


    public Link getCurrentLink(double time)
    {
        // TODO
        return null;
    }
}
