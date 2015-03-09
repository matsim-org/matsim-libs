package playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.definitions;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacility;

import playground.sergioo.passivePlanning2012.population.parallelPassivePlanning.PassivePlannerManager.CurrentTime;
import playground.sergioo.passivePlanning2012.population.parallelPassivePlanning.PassivePlannerManager.MobsimStatus;

public interface SinglePlannerAgent {

	//Methods
	public Plan getPlan();
	public int getPlanElementIndex();
	public void incrementPlanElementIndex();
	public void setRouter(TripRouter tripRouter);
	public int planLegActivityLeg(double startTime, CurrentTime now, Id<ActivityFacility> startFacilityId, double endTime, Id<ActivityFacility> endFacilityId, final MobsimStatus mobSimEnds);
	public void advanceToNextActivity(double now, double penalty);

}
