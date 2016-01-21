package org.matsim.core.scoring;


import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;

import javax.inject.Inject;
import java.lang.reflect.Method;

class ExperiencedPlanElementsServiceImpl implements ExperiencedPlanElementsService {


	/**
	 * Simple logging handler for subscriber exceptions.
	 */
	static final class LoggingHandler implements SubscriberExceptionHandler {
		static final LoggingHandler INSTANCE = new LoggingHandler();

		@Override
		public void handleException(Throwable exception, SubscriberExceptionContext context) {
			Logger logger = logger(context);
				logger.error(message(context), exception);
		}

		private static Logger logger(SubscriberExceptionContext context) {
			return Logger.getLogger(EventBus.class.getName() + "." + ExperiencedPlanElementsServiceImpl.class.toString());
		}

		private static String message(SubscriberExceptionContext context) {
			Method method = context.getSubscriberMethod();
			return "Exception thrown by subscriber method "
					+ method.getName() + '(' + method.getParameterTypes()[0].getName() + ')'
					+ " on subscriber " + context.getSubscriber()
					+ " when dispatching event: " + context.getEvent();
		}
	}
	EventBus eventBus = new EventBus(new LoggingHandler());

	@Inject
	ExperiencedPlanElementsServiceImpl(EventsToActivities eventsToActivities, EventsToLegs eventsToLegs) {
		eventsToActivities.setActivityHandler(new EventsToActivities.ActivityHandler() {
			@Override
			public void handleActivity(Id<Person> agentId, Activity activity) {
				eventBus.post(new PersonExperiencedActivity(agentId, activity));
			}
		});
		eventsToLegs.setLegHandler(new EventsToLegs.LegHandler() {
			@Override
			public void handleLeg(Id<Person> agentId, Leg leg) {
				eventBus.post(new PersonExperiencedLeg(agentId, leg));
			}
		});
	}

	@Override
	public void register(Object subscriber) {
		eventBus.register(subscriber);
	}

	@Override
	public void unregister(Object subscriber) {
		eventBus.unregister(subscriber);
	}

}
