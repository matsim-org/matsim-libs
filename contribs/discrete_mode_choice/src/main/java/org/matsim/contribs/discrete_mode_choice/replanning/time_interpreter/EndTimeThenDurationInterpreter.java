package org.matsim.contribs.discrete_mode_choice.replanning.time_interpreter;

import org.matsim.api.core.v01.population.Activity;

/**
 * This TimeInterpreter first checks whehther the end time of an activity is
 * set. If it exists, the new time along the plan is set to this time. If not,
 * the maximum activity duration is added to the current time. It is not allowed
 * than neither end time nor maximum duration are defined.
 * 
 * Note that this behaviour can make the time "jump back" if the duration of a
 * leg exceeds the end time of the following activity. This behaviour can be
 * avoided by setting onlyAdvance to true.
 * 
 * @author sebhoerl
 */
public class EndTimeThenDurationInterpreter extends AbstractTimeInterpreter {
	public EndTimeThenDurationInterpreter(double startTime, boolean onlyAdvance) {
		super(startTime, startTime, onlyAdvance);
	}

	private EndTimeThenDurationInterpreter(double currentTime, double previousTime, boolean onlyAdvance) {
		super(currentTime, previousTime, onlyAdvance);
	}

	@Override
	public void addActivity(Activity activity) {
		if (activity.getEndTime().isDefined()) {
			advance(activity.getEndTime().seconds());
		} else if (activity.getMaximumDuration().isDefined()) {
			advance(currentTime + activity.getMaximumDuration().seconds());
		} else {
			throw new IllegalStateException("Found activity having neither an end time nor a maximum duration");
		}
	}

	@Override
	public TimeInterpreter fork() {
		return new EndTimeThenDurationInterpreter(currentTime, previousTime, onlyAdvance);
	}

	static public class Factory implements TimeInterpreter.Factory {
		private final double startTime;
		private final boolean onlyAdvance;

		public Factory(double startTime, boolean onlyAdvance) {
			this.startTime = startTime;
			this.onlyAdvance = onlyAdvance;
		}

		@Override
		public TimeInterpreter createTimeInterpreter() {
			return new EndTimeThenDurationInterpreter(startTime, onlyAdvance);
		}
	}
}
