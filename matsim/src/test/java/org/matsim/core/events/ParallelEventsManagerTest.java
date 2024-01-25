package org.matsim.core.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ParallelEventsManagerTest {

    private final Event e = new EventsManagerImplTest.MyEvent(5);

    private EventsManagerImplTest.CountingMyEventHandler handler;

    @BeforeEach
    public void setUp() throws Exception {
        handler = new EventsManagerImplTest.CountingMyEventHandler();
    }

	@Test
	void forgetInit() {

        EventsManager m = EventsUtils.createParallelEventsManager();

        m.addHandler(handler);

        assertThrows(IllegalStateException.class, () -> m.processEvent(e));

        m.initProcessing();
        m.processEvent(e);

        m.finishProcessing();

        assertEquals(1, handler.counter);
    }

	@Test
	void lateHandler() {

        EventsManager m = EventsUtils.createParallelEventsManager();
        m.initProcessing();

        assertThrows(IllegalStateException.class, () -> m.addHandler(handler));

        m.processEvent(e);

        assertEquals(0, handler.counter);

    }
}
