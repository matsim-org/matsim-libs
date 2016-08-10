package org.matsim.contrib.carsharing.control.listeners;

import java.util.Collection;
import java.util.List;

import javax.inject.Singleton;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.carsharing.manager.CarsharingManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.withinday.replanning.identifiers.tools.ActivityReplanningMap;

import com.google.inject.Inject;

@Singleton
public class CarsharingMobsimListener implements MobsimBeforeSimStepListener{

	private ActivityReplanningMap activityReplanningMap;
	private CarsharingManager carsharingManager;
	
	@Inject
	public void ActivityReplanningMap(ActivityReplanningMap activityReplanningMap, CarsharingManager carsharingManager) {
		
		this.activityReplanningMap = activityReplanningMap;
		this.carsharingManager = carsharingManager;
	}
	
	
	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
		
		Collection<MobsimAgent> agentsEndingActivities = activityReplanningMap.getActivityEndingAgents(e.getSimulationTime());
		
		for(MobsimAgent ma : agentsEndingActivities) {
			
			Plan plan = WithinDayAgentUtils.getModifiablePlan( ma ) ; 
			int nextElementIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(ma) + 1;
			
			PlanElement pe = plan.getPlanElements().get(nextElementIndex);
			if (carsharingLeg(pe)) {
				
				List<PlanElement> newTrip = carsharingManager.reserveAndrouteCarsharingTrip(plan.getPerson(), plan, "freefloating", nextElementIndex, e.getSimulationTime());
				if (newTrip == null)
					ma.setStateToAbort(e.getSimulationTime());
				else {
					List<PlanElement> planElements = plan.getPlanElements();
	
					planElements.remove(pe);
					planElements.addAll(nextElementIndex, newTrip);
				}
			}			
		}
		
		
	}

	private boolean carsharingLeg(PlanElement pe) {
		String mode = ((Leg)pe).getMode();
		if (mode.equals("freefloating"))
			return true;
		
		return false;
	}

}
