package playground.michalm.vrp.model;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.mobsim.framework.*;
import org.matsim.ptproject.qsim.agents.*;
import org.matsim.ptproject.qsim.interfaces.*;

import pl.poznan.put.vrp.dynamic.data.model.*;


public class VRPVehicleAgent
    extends ExperimentalBasicWithindayAgent
    implements PersonDriverAgent
{
    Vehicle vrpVehicle;
    VRPRoutePlan vrpRoutePlan;


    public VRPVehicleAgent(Person p, Netsim simulation, Vehicle vrpVehicle)
    {
        super(p, simulation);
        
        this.vrpVehicle = vrpVehicle;
        vrpRoutePlan = new VRPRoutePlan(p, vrpVehicle.route);
    }


    @Override
    public Id chooseNextLinkId()
    {

        // TODO Auto-generated method stub
        return super.chooseNextLinkId();
    }
}
