package org.matsim.api.core.v01.population;

import java.util.List;

import org.matsim.api.core.v01.Identifiable;

public interface HasPlansAndId<T extends BasicPlan> extends Identifiable {

	public abstract List<? extends T> getPlans();

	/**
	 * adds the plan to the Person's List of plans and
	 * sets the reference to this person in the Plan instance.
	 */
	public abstract boolean addPlan(T p);

	public abstract T getSelectedPlan();

	public abstract void setSelectedPlan(T selectedPlan);
	
//	@Deprecated
//	public abstract T copySelectedPlan() ;

}