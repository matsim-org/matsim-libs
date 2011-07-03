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
    private VRPSchedulePlan vrpSchedulePlan;


    private VRPVehicleAgent(Person driver, Netsim simulation, Vehicle vrpVehicle, MATSimVRPData data)
    {
        super(driver, simulation);

        this.vrpVehicle = vrpVehicle;

        vrpSchedulePlan = new VRPSchedulePlan(driver, vrpVehicle.schedule, data);
        driver.addPlan(vrpSchedulePlan);
    }


    @Override
    public Id chooseNextLinkId()
    {
        // should depend on the vrpSchedulePlan:

        return super.chooseNextLinkId();
    }


    public Vehicle getVrpVehicle()
    {
        return vrpVehicle;
    }


    public VRPSchedulePlan getVrpSchedulePlan()
    {
        return vrpSchedulePlan;
    }


    @Override
    public VRPSchedulePlan getSelectedPlan()
    {
        return vrpSchedulePlan;
    }
}
