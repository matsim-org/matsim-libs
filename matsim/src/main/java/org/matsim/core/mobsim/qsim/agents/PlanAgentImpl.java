package org.matsim.core.mobsim.qsim.agents;

import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.population.PlanImpl;

public class PlanAgentImpl implements PlanAgent {

	private int currentPlanElementIndex = 0;
	private Plan plan;
	private boolean firstTimeToGetModifiablePlan = true;

	public PlanAgentImpl(Plan plan2) {
		this.plan = plan2 ;
	}
	

	/**
	 * Convenience method delegating to person's selected plan
	 * @return list of {@link Activity}s and {@link Leg}s of this agent's plan
	 */
	protected List<PlanElement> getPlanElements() {
		return this.getCurrentPlan().getPlanElements();
	}

	@Override
	public final PlanElement getCurrentPlanElement() {
		return this.getPlanElements().get(this.currentPlanElementIndex);
	}

	@Override
	public final PlanElement getNextPlanElement() {
		if ( this.currentPlanElementIndex < this.getPlanElements().size() ) {
			return this.getPlanElements().get( this.currentPlanElementIndex+1 ) ;
		} else {
			return null ;
		}
	}


	/* default */ final int getCurrentPlanElementIndex() {
		return currentPlanElementIndex;
	}

	/* default */ final void setCurrentPlanElementIndex(int currentPlanElementIndex) {
		this.currentPlanElementIndex = currentPlanElementIndex;
	}

	@Override
	public final Plan getCurrentPlan() {
		return plan;
	}

	private final void setPlan(Plan plan) {
		this.plan = plan;
	}

	/**
	 * Returns a modifiable Plan for use by WithinDayAgentUtils in this package.
	 * This agent retains the copied plan and forgets the original one.
	 */
	protected final Plan getModifiablePlan() {
		if (firstTimeToGetModifiablePlan) {
			firstTimeToGetModifiablePlan = false ;
			PlanImpl newPlan = new PlanImpl(this.getCurrentPlan().getPerson());
			newPlan.copyFrom(this.getCurrentPlan());
			this.setPlan(newPlan);
		}
		return this.getCurrentPlan();
	}

}