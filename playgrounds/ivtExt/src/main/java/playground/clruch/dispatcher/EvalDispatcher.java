package playground.clruch.dispatcher;

import org.matsim.core.api.experimental.events.EventsManager;

import playground.sebhoerl.avtaxi.dispatcher.utils.SingleRideAppender;

public class EvalDispatcher extends UniversalDispatcher {

    public EvalDispatcher(EventsManager eventsManager, SingleRideAppender appender) {
        super(eventsManager, appender);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void redispatch(double now) {
        // TODO Auto-generated method stub

    }

}
