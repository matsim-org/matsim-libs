package playground.michalm.vrp.data.matsimadapter;

import java.util.*;

import org.matsim.api.core.v01.population.*;

import pl.poznan.put.vrp.dynamic.data.model.Route;


public class VRPRoutePlan
    implements Plan
{
    public Route vrpRoute;
    public Plan plan;


    public VRPRoutePlan()
    {

    }


    public List<PlanElement> getPlanElements()
    {
        return plan.getPlanElements();
    }


    public void addLeg(Leg leg)
    {
        plan.addLeg(leg);
    }


    public void addActivity(Activity act)
    {
        plan.addActivity(act);
    }


    public boolean isSelected()
    {
        return plan.isSelected();
    }


    public void setScore(Double score)
    {
        plan.setScore(score);
    }


    public Double getScore()
    {
        return plan.getScore();
    }


    public Person getPerson()
    {
        return plan.getPerson();
    }


    public void setPerson(Person person)
    {
        plan.setPerson(person);
    }


    public Map<String, Object> getCustomAttributes()
    {
        return plan.getCustomAttributes();
    }
}
