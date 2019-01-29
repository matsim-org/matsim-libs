package org.matsim.api.core.v01.population;

import java.util.List;

import org.matsim.api.core.v01.Identifiable;

public interface HasPlansAndId<T extends BasicPlan, I> extends Identifiable<I> {

	/**
	 * Seems that <? extends T> is actually more restrictive than <T>, i.e. we may later switch from 
	 * <? extends T> to <T>, but not the other way around.
	 * <p></p>
	 * Practically, with <? extends T>, you cannot do getPlans().add( new MyPlans() ), where MyPlans is derived from T.
	 */
	public abstract List<? extends T> getPlans();

	/**
	 * adds the plan to the Person's List of plans and
	 * sets the reference to this person in the Plan instance.
	 */
	public abstract boolean addPlan(T p);
	
	public abstract boolean removePlan(T p);

	public abstract T getSelectedPlan();

	public abstract void setSelectedPlan(T selectedPlan);

	public abstract T createCopyOfSelectedPlanAndMakeSelected() ;

}