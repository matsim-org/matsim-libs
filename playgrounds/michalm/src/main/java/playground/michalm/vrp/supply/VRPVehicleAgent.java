package playground.michalm.vrp.supply;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.*;
import org.matsim.ptproject.qsim.agents.*;
import org.matsim.ptproject.qsim.interfaces.*;

import pl.poznan.put.vrp.dynamic.data.model.*;


public class VRPVehicleAgent
    extends ExperimentalBasicWithindayAgent
{
    private Vehicle vrpVehicle;
    private VRPRoutePlan vrpRoutePlan;


    public VRPVehicleAgent(Person driver, Netsim simulation, Vehicle vrpVehicle)
    {
        super(driver, simulation);

        this.vrpVehicle = vrpVehicle;
        vrpRoutePlan = new VRPRoutePlan(driver, vrpVehicle.route);
    }


    @Override
    public Id chooseNextLinkId()
    {
        //should depend on the vrpRoutePlan:
        
        
        return super.chooseNextLinkId();
    }


    @Override
    public Plan getExecutedPlan()
    {
        return vrpRoutePlan;
    }

}
