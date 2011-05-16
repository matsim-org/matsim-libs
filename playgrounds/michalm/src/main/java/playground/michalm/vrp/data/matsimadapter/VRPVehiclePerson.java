package playground.michalm.vrp.data.matsimadapter;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.population.*;

import pl.poznan.put.vrp.dynamic.data.model.*;


public class VRPVehiclePerson
    implements Person
{
    public Vehicle vehicle;
    public Person person;


    public Id getId()
    {
        return person.getId();
    }


    public List< ? extends Plan> getPlans()
    {
        return person.getPlans();
    }


    public void setId(Id id)
    {
        person.setId(id);
    }


    public boolean addPlan(Plan p)
    {
        return person.addPlan(p);
    }


    public Plan getSelectedPlan()
    {
        return person.getSelectedPlan();
    }


    public Map<String, Object> getCustomAttributes()
    {
        return person.getCustomAttributes();
    }

}
