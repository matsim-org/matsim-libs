package org.matsim.freight.receiver.replanning;

import com.google.inject.Provider;
import org.matsim.freight.receiver.ReceiverReplanningType;

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

