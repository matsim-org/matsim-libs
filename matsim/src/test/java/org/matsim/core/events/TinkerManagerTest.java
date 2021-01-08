package org.matsim.core.events;


import org.apache.log4j.Logger;
import org.junit.Test;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.BasicEventHandler;

import static org.junit.Assert.assertTrue;

public class TinkerManagerTest {

    private static final Logger log = Logger.getLogger(TinkerManagerTest.class);

    @Test
    public void simpleTest() {

        var manager = new TinkerManager();
        manager.addHandler(new HandlerListeningForAfterSimStepEvents(manager));
        var assertionHandler = new HandlerForAsyncEvents();
        manager.addHandler(assertionHandler);

        manager.initProcessing();
        manager.processEvent(new SomeEvent(1));
        manager.afterSimStep(1);
        manager.processEvent(new AfterSimStepEvent(1));

        // move on to the next timestep. The main thread should now wait for the
        manager.processEvent(new SomeEvent(2));
        manager.finishProcessing();

        assertTrue(assertionHandler.isCaughtEvent());
    }

    private static class SomeEvent extends Event {

        public static final String EVENT_TYPE = "someEvent";
        public SomeEvent(double time) {
            super(time);
        }

        @Override
        public String getEventType() {
            return EVENT_TYPE;
        }
    }

    private static class AfterSimStepEvent extends Event {

        public static final String EVENT_TYPE = "afterSimStep";

        public AfterSimStepEvent(double time) {
            super(time);
        }

        @Override
        public String getEventType() {
            return EVENT_TYPE;
        }
    }

    private static class HandlerListeningForAfterSimStepEvents implements BasicEventHandler {

        private final EventsManager manager;

        private HandlerListeningForAfterSimStepEvents(EventsManager manager) {
            this.manager = manager;
        }

        @Override
        public void handleEvent(Event event) {

            if (event.getEventType().equals(AfterSimStepEvent.EVENT_TYPE)) {
                try {
                    log.info("reaching thread sleep.");
                    Thread.sleep(1000); // long computation
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                log.info("submit event to manager after thread sleep for timestept: " + event.getTime());
                manager.processEvent(new EventThrownAsync(event.getTime()));
            }
        }

        private static class EventThrownAsync extends Event {

            public static final String EVENT_TYPE = "afterSimStepAsync";

            public EventThrownAsync(double time) {
                super(time);
            }

            @Override
            public String getEventType() {
                return EVENT_TYPE;
            }
        }
    }

    private static class HandlerForAsyncEvents implements BasicEventHandler {

        private boolean caughtEvent = false;

        @Override
        public void handleEvent(Event event) {
            if (event.getEventType().equals(HandlerListeningForAfterSimStepEvents.EventThrownAsync.EVENT_TYPE)) {

                this.caughtEvent = true;
            }
        }

        public boolean isCaughtEvent() {
            return caughtEvent;
        }
    }
}