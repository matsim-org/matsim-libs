package playground.michalm.vrp.supply;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.ptproject.qsim.agents.*;
import org.matsim.ptproject.qsim.interfaces.*;

import pl.poznan.put.vrp.dynamic.data.model.*;
import playground.michalm.vrp.data.*;


public class VRPVehicleAgent
    extends ExperimentalBasicWithindayAgent
{
    private Vehicle vrpVehicle;
    private VRPRoutePlan vrpRoutePlan;


    private VRPVehicleAgent(Person driver, Netsim simulation, Vehicle vrpVehicle, MATSimVRPData data)
    {
        super(driver, simulation);

        this.vrpVehicle = vrpVehicle;

        vrpRoutePlan = new VRPRoutePlan(driver, vrpVehicle.schedule, data);
        driver.addPlan(vrpRoutePlan);
    }


    @Override
    public Id chooseNextLinkId()
    {
        // should depend on the vrpRoutePlan:

        return super.chooseNextLinkId();
    }


    public Vehicle getVrpVehicle()
    {
        return vrpVehicle;
    }


    public VRPRoutePlan getVrpRoutePlan()
    {
        return vrpRoutePlan;
    }


    @Override
    public VRPRoutePlan getSelectedPlan()
    {
        return vrpRoutePlan;
    }
}
