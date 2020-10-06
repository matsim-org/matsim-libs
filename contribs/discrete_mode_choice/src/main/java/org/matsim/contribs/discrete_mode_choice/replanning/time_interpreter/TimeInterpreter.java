package org.matsim.contribs.discrete_mode_choice.replanning.time_interpreter;

import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;

/**
 * This interfaces defines a utility that allows to track the time of day while
 * looping through plan elements. MATSim uses different implementations of this
 * process to make sense of activity end times and maximum activity durations.
 * 
 * @author sebhoerl
 */
public interface TimeInterpreter {
	double getCurrentTime();

	double getPreviousTime();

	void addLeg(Leg leg);

	void addActivity(Activity activity);

	void addPlanElement(PlanElement element);

	void addPlanElements(List<? extends PlanElement> elements);

	void setTime(double time);

	void addTime(double duration);

	TimeInterpreter fork();

	static interface Factory {
		TimeInterpreter createTimeInterpreter();
	}
}
