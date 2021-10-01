package org.matsim.core.utils.timing;

import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.utils.misc.OptionalTime;

/**
 * This is a helper class which makes it possible to track time along a plan
 * while taking into account the system configuration with respect to leg and
 * activity end time calculation.
 * 
 * @author sebhoerl
 */
public class TimeTracker {
	private final TimeInterpretation timeInterpretation;
	private OptionalTime currentTime;

	public TimeTracker(TimeInterpretation timeInterpretation) {
		this.timeInterpretation = timeInterpretation;
		this.currentTime = OptionalTime.defined(timeInterpretation.getSimulationStartTime());
	}

	public OptionalTime setTime(double time) {
		currentTime = OptionalTime.defined(time);
		return currentTime;
	}
	
	public OptionalTime addDuration(double duration) {
		if (currentTime.isUndefined()) {
			throw new IllegalStateException("Cannot add element as current time is undefined");
		}
		
		currentTime = OptionalTime.defined(currentTime.seconds() + duration);
		return currentTime;
	}

	public OptionalTime getTime() {
		return currentTime;
	}

	public OptionalTime addElement(PlanElement element) {
		if (currentTime.isUndefined()) {
			throw new IllegalStateException("Cannot add element as current time is undefined");
		}

		currentTime = timeInterpretation.decideOnElementEndTime(element, currentTime.seconds());
		return currentTime;
	}

	public OptionalTime addElements(List<? extends PlanElement> elements) {
		for (PlanElement element : elements) {
			addElement(element);
		}

		return currentTime;
	}

	public OptionalTime addActivity(Activity activity) {
		return addElement(activity);
	}

	public OptionalTime addLeg(Leg leg) {
		return addElement(leg);
	}
}
