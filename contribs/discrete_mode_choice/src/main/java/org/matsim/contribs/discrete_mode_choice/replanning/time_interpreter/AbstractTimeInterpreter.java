package org.matsim.contribs.discrete_mode_choice.replanning.time_interpreter;

import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;

/**
 * This class provides an abstract implementation for time interpretation. It
 * knows how to handle legs but leaves the handling of activities for more
 * specific implementations.
 * 
 * There is a "onlyAdvance" flag which decides if the specific implementation is
 * allows to "go back" in time, or if time can only be advanced further.
 * 
 * @author sebhoerl
 */
public abstract class AbstractTimeInterpreter implements TimeInterpreter {
	protected double currentTime;
	protected double previousTime;

	protected final boolean onlyAdvance;

	AbstractTimeInterpreter(double currentTime, double previousTime, boolean onlyAdvance) {
		this.currentTime = currentTime;
		this.previousTime = previousTime;
		this.onlyAdvance = onlyAdvance;

		verify();
	}

	@Override
	public double getCurrentTime() {
		return currentTime;
	}

	@Override
	public double getPreviousTime() {
		return previousTime;
	}

	protected void advance(double updatedTime) {
		this.previousTime = this.currentTime;
		this.currentTime = updatedTime;

		if (onlyAdvance) {
			this.currentTime = Math.max(this.previousTime, this.currentTime);
		}

		verify();
	}

	protected void verify() {
		// Commented this out with the switch to OptionalTime. Not sure if we actually
		// still need this!

		/*
		 * if (Time.isUndefinedTime(currentTime)) { throw new
		 * IllegalStateException("Illegal current time while interpreting times"); }
		 * 
		 * if (Time.isUndefinedTime(previousTime)) { throw new
		 * IllegalStateException("Illegal previous time while interpreting times"); }
		 */

		if (onlyAdvance && currentTime < previousTime) {
			throw new IllegalStateException("Illegal leg/activity duration while interpreting times");
		}
	}

	@Override
	public void setTime(double time) {
		advance(time);
	}

	@Override
	public void addTime(double duration) {
		advance(currentTime + duration);
	}

	@Override
	public void addLeg(Leg leg) {
		if (leg.getRoute() == null) {
			advance(currentTime + leg.getTravelTime().seconds());
		} else {
			advance(currentTime + leg.getRoute().getTravelTime().seconds());
		}
	}

	@Override
	public void addPlanElement(PlanElement element) {
		if (element instanceof Activity) {
			addActivity((Activity) element);
		} else if (element instanceof Leg) {
			addLeg((Leg) element);
		}
	}

	@Override
	public void addPlanElements(List<? extends PlanElement> elements) {
		for (PlanElement element : elements) {
			addPlanElement(element);
		}
	}
}
