package org.matsim.contrib.freightReceiver.replanning;

import com.google.inject.Provider;
import org.matsim.contrib.freightReceiver.ReceiverReplanningType;

/**
 * A single entry point for receiver replanning.
 */
public class ReceiverReplanningUtils {

	public static Provider<ReceiverStrategyManager> createStrategyManager(ReceiverReplanningType replanningType) {
		return switch (replanningType) {
			case serviceTime -> new ServiceTimeStrategyManagerProvider();
			case timeWindow -> new TimeWindowStrategyManagerFactory();
			case orderFrequency -> new OrderFrequencyStrategyManager();
			case afterHoursTimeWindow -> null;
		};
	}
}

