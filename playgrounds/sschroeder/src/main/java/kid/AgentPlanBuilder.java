package kid;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.population.algorithms.PlanAlgorithm;

public class AgentPlanBuilder {
	
	private Plan plan;

	private boolean lastElementWasActivity = false;
	
	private boolean driverSet = false;
	
	private PlanAlgorithm router;
	
	public AgentPlanBuilder(PlanAlgorithm router) {
		plan = new PlanImpl();
		this.router = router;
	}
	
	public void setDriverId(Id driverId) {
		plan.setPerson(new PersonImpl(driverId));
		driverSet = true;
	}

	public void scheduleActivity(String name, Id location, Integer depTime){
		if(lastElementWasActivity){
			throw new IllegalStateException("cannot schedule activity because last element has already been an activity");
		}
		Activity act = new ActivityImpl(name, location);
		if(depTime != null){
			act.setEndTime(depTime);
		}
		plan.addActivity(act);
		lastElementWasActivity = true;
	}
	
	public void scheduleLeg(){
		if(!lastElementWasActivity){
			throw new IllegalStateException("I must be an activity, but actually I am a leg");
		}
		plan.addLeg(new LegImpl(TransportMode.car));
		lastElementWasActivity = false;
	}
	
	public Plan build(){
		if(!driverSet){
			throw new IllegalStateException("driverId has not been set");
		}
		if(!lastElementWasActivity){
			throw new IllegalStateException("there is still an activity missing");
		}
		router.run(plan);
		return plan;
	}

}
