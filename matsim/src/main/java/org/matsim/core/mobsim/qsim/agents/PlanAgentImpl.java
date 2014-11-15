package org.matsim.core.mobsim.qsim.agents;

import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.population.PlanImpl;

public final class PlanAgentImpl implements PlanAgent {

	private int currentPlanElementIndex = 0;
	private Plan plan;
	private boolean firstTimeToGetModifiablePlan = true;

	public PlanAgentImpl(Plan plan2) {
		this.plan = plan2 ;
	}
	
	@Override
	public final PlanElement getCurrentPlanElement() {
		return this.plan.getPlanElements().get(this.currentPlanElementIndex);
	}

	@Override
	public final PlanElement getNextPlanElement() {
		if ( this.currentPlanElementIndex < this.plan.getPlanElements().size() ) {
			return this.plan.getPlanElements().get( this.currentPlanElementIndex+1 ) ;
		} else {
			return null ;
		}
	}


	/* default */ final int getCurrentPlanElementIndex() {
		return currentPlanElementIndex;
	}

	@Override
	public final Plan getCurrentPlan() {
		return plan;
	}

	private final void setPlan(Plan plan) {
		this.firstTimeToGetModifiablePlan = true ;
		this.plan = plan;
	}

	/**
	 * Returns a modifiable Plan for use by WithinDayAgentUtils in this package.
	 * This agent retains the copied plan and forgets the original one.  However, the original plan remains in the population file
	 * (and will be scored).  This is deliberate behavior!
	 */
	final Plan getModifiablePlan() {
		if (firstTimeToGetModifiablePlan) {
			firstTimeToGetModifiablePlan = false ;
			PlanImpl newPlan = new PlanImpl(this.getCurrentPlan().getPerson());
			newPlan.copyFrom(this.getCurrentPlan());
			this.setPlan(newPlan);
		}
		return this.getCurrentPlan();
	}


	void advancePlan() {
		this.currentPlanElementIndex++ ;
	}

}