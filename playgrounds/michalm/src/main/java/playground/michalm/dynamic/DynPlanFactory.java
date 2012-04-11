package playground.michalm.dynamic;

import org.matsim.api.core.v01.population.Plan;


public interface DynPlanFactory
{
    Plan create(DynAgent agent);
}
