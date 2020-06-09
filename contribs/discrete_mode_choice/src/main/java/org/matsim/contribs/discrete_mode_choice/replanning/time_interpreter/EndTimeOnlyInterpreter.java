package org.matsim.contribs.discrete_mode_choice.replanning.time_interpreter;

import org.matsim.api.core.v01.population.Activity;

/**
 * This TimeInterpreter advances the time along a plan by examining the end time
 * of the next activity. It must be defined.
 * 
 * Note that this behaviour can make the time "jump back" if the duration of a
 * leg exceeds the end time of the following activity. This behaviour can be
 * avoided by setting onlyAdvance to true.
 * 
 * @author sebhoerl
 */
@Deprecated
public class EndTimeOnlyInterpreter extends AbstractTimeInterpreter {
	public EndTimeOnlyInterpreter(double startTime, boolean onlyAdvance) {
		super(startTime, startTime, onlyAdvance);
	}

	private EndTimeOnlyInterpreter(double currentTime, double previousTime, boolean onlyAdvance) {
		super(currentTime, previousTime, onlyAdvance);
	}

	@Override
	public void addActivity(Activity activity) {
		if (activity.getEndTime().isDefined()) {
			advance(activity.getEndTime().seconds());
		} else {
			throw new IllegalStateException("Found activity that has no end time defined");
		}
	}

	@Override
	public TimeInterpreter fork() {
		return new EndTimeOnlyInterpreter(currentTime, previousTime, onlyAdvance);
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
			return new EndTimeOnlyInterpreter(startTime, onlyAdvance);
		}
	}
}
