package org.matsim.core.events;

import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class ParallelEventsManagerTest {

    private final Event e = new EventsManagerImplTest.MyEvent(5);

    private EventsManagerImplTest.CountingMyEventHandler handler;

    @Before
    public void setUp() throws Exception {
        handler = new EventsManagerImplTest.CountingMyEventHandler();
    }

    @Test
    public void forgetInit() {

        EventsManager m = EventsUtils.createParallelEventsManager();

        m.addHandler(handler);

        assertThrows(IllegalStateException.class, () -> m.processEvent(e));

        m.initProcessing();
        m.processEvent(e);

        m.finishProcessing();

        assertEquals(1, handler.counter);
    }

    @Test
    public void lateHandler() {

        EventsManager m = EventsUtils.createParallelEventsManager();
        m.initProcessing();

        assertThrows(IllegalStateException.class, () -> m.addHandler(handler));

        m.processEvent(e);

        assertEquals(0, handler.counter);

    }
}