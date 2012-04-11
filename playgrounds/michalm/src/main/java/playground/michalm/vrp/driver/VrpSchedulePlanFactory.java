package playground.michalm.vrp.driver;

import org.matsim.api.core.v01.population.Plan;

import pl.poznan.put.vrp.dynamic.data.model.Vehicle;
import playground.michalm.dynamic.*;
import playground.michalm.vrp.data.MatsimVrpData;


public class VrpSchedulePlanFactory
    implements DynPlanFactory
{
    private Vehicle vehicle;
    private MatsimVrpData data;


    public VrpSchedulePlanFactory(Vehicle vehicle, MatsimVrpData data)
    {
        this.vehicle = vehicle;
        this.data = data;
    }


    @Override
    public Plan create(DynAgent agent)
    {
        return new VrpSchedulePlan(vehicle, data);
    }
}
