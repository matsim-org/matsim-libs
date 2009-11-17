package org.matsim.api.core.v01.population;

import java.io.Serializable;
import java.util.List;

import org.matsim.core.api.internal.MatsimPopulationObject;

public interface Plan extends Serializable, MatsimPopulationObject {

	public List<PlanElement> getPlanElements();

	public void addLeg(final Leg leg);

	public void addActivity(final Activity act);

	public boolean isSelected();
	
	public void setSelected(boolean selected);

	public void setScore(Double score);
	
	public Double getScore();

	public Person getPerson();
	/**
	 * Sets the reference to the person in the BasicPlan instance.
	 * This is done automatically if using Person.addPlan(). Make
	 * sure that the bidirectional reference is set correctly if
	 * you are using this method!.
	 */
	public void setPerson(Person person);
	
}
