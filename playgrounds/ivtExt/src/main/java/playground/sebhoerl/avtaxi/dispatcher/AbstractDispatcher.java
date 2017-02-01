package playground.sebhoerl.avtaxi.dispatcher;

import org.matsim.core.api.experimental.events.EventsManager;

import playground.sebhoerl.avtaxi.dispatcher.utils.SingleRideAppender;

public abstract class AbstractDispatcher implements AVDispatcher {
    protected final SingleRideAppender appender;
    protected final EventsManager eventsManager;

    public AbstractDispatcher(EventsManager eventsManager, SingleRideAppender appender) {
	this.appender = appender;
	this.eventsManager = eventsManager;
    }

}
