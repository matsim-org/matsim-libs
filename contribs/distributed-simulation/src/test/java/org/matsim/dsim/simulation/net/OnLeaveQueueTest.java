package org.matsim.dsim.simulation.net;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class OnLeaveQueueTest {

    @Test
    public void compose() {
        var handler1WasCalled = new AtomicBoolean(false);
        SimLink.OnLeaveQueue handler1 = (_, _, _) -> {
            handler1WasCalled.set(true);
            return SimLink.OnLeaveQueueInstruction.MoveToBuffer;
        };

        var handler2WasCalled = new AtomicBoolean(false);
        SimLink.OnLeaveQueue handler2 = (_, _, _) -> {
            handler2WasCalled.set(true);
            return SimLink.OnLeaveQueueInstruction.RemoveVehicle;
        };

        var handler3WasCalled = new AtomicBoolean(false);
        SimLink.OnLeaveQueue handler3 = (_, _, _) -> {
            handler3WasCalled.set(true);
            return SimLink.OnLeaveQueueInstruction.MoveToBuffer;
        };

        var handler = handler1.compose(handler2.compose(handler3));
        var result = handler.apply(null, null, 0);

        assertFalse(handler1WasCalled.get());
        assertTrue(handler2WasCalled.get());
        assertTrue(handler3WasCalled.get());
        assertEquals(SimLink.OnLeaveQueueInstruction.RemoveVehicle, result);
    }
}
