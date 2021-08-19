package org.matsim.core.utils.time_interpreter;

import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansConfigGroup.RoutingDepartureTimeInterpration;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class TimeInterpreterModule extends AbstractModule {
	@Override
	public void install() {
	}

	@Provides
	@Singleton
	public TimeInterpreter.Factory provideTimeInterpreterFactory(Config config) {
		double startTime = config.qsim().getStartTime().orElse(0.0);
		boolean onlyAdvance = config.plans().getRoutingDepartureTimeInterpration()
				.equals(RoutingDepartureTimeInterpration.cumulative);

		switch (config.plans().getActivityDurationInterpretation()) {
		case endTimeOnly:
			return new EndTimeOnlyInterpreter.Factory(startTime, onlyAdvance);
		case minOfDurationAndEndTime:
			return new MinimumEndTimeAndDurationInterpreter.Factory(startTime, onlyAdvance);
		case tryEndTimeThenDuration:
			return new EndTimeThenDurationInterpreter.Factory(startTime, onlyAdvance);
		default:
			throw new IllegalStateException();
		}
	}
}
