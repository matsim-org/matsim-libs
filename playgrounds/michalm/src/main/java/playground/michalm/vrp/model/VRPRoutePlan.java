package playground.michalm.vrp.model;

import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.*;

import pl.poznan.put.vrp.dynamic.data.model.Route;


public class VRPRoutePlan
    extends PlanImpl
    implements Plan
{
    Route vrpRoute;


    public VRPRoutePlan(Person p, Route vrpRoute)
    {
        super(p);
        this.vrpRoute = vrpRoute;
    }
}
