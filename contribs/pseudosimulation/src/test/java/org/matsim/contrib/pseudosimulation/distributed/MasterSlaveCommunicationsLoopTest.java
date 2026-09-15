package org.matsim.contrib.pseudosimulation.distributed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Test;

class MasterSlaveCommunicationsLoopTest {

    @Test
    void dispatchesEverySimpleModeWithTheProtocolAcknowledgement() {
        assertSimpleDispatch(CommunicationsMode.POOL_PERSONS, "pool");
        assertSimpleDispatch(CommunicationsMode.DISTRIBUTE_PERSONS, "distribute");
        assertSimpleDispatch(CommunicationsMode.TRANSMIT_SCORES, "scores");
        assertSimpleDispatch(CommunicationsMode.TRANSMIT_PERFORMANCE, "performance");
        assertSimpleDispatch(CommunicationsMode.CONTINUE, null);
        assertSimpleDispatch(CommunicationsMode.WAIT, null);
    }

    @Test
    void performsTravelTimeHandshakeBeforeTheFinalAcknowledgement() {
        Fixture fixture = new Fixture();

        CommunicationsMode result = fixture.loop().run(CommunicationsMode.TRANSMIT_TRAVEL_TIMES, 7);

        assertEquals(CommunicationsMode.CONTINUE, result);
        assertEquals(List.of("write:TRANSMIT_TRAVEL_TIMES", "flush", "travel-times", "read-boolean",
                "write:CONTINUE", "flush", "read-boolean", "decrement"), fixture.events);
    }

    @Test
    void performsScenarioHandshakeBeforeTheFinalAcknowledgement() {
        Fixture fixture = new Fixture();

        CommunicationsMode result = fixture.loop().run(CommunicationsMode.TRANSMIT_SCENARIO, 7);

        assertEquals(CommunicationsMode.CONTINUE, result);
        assertEquals(List.of("write:TRANSMIT_SCENARIO", "flush", "initial-plans", "read-boolean",
                "write:CONTINUE", "flush", "read-boolean", "decrement"), fixture.events);
    }

    @Test
    void resetsBeforeReceivingPlansAndReadsReadinessBeforeAcknowledgement() {
        Fixture fixture = new Fixture();

        CommunicationsMode result = fixture.loop().run(CommunicationsMode.TRANSMIT_PLANS_TO_MASTER, 7);

        assertEquals(CommunicationsMode.TRANSMIT_PLANS_TO_MASTER, result);
        assertEquals(List.of("write:TRANSMIT_PLANS_TO_MASTER", "flush", "reset", "plans", "readiness",
                "read-boolean", "decrement"), fixture.events);
    }

    @Test
    void dieReturnsWithoutAcknowledgingDecrementingOrLeavingLogBookkeeping() {
        Fixture fixture = new Fixture();

        CommunicationsMode result = fixture.loop().run(CommunicationsMode.DIE, 7);

        assertEquals(CommunicationsMode.DIE, result);
        assertEquals(List.of("write:DIE", "flush"), fixture.events);
        assertFalse(fixture.failed);
    }

    @Test
    void preservesContinueModeAndDoubleDecrementWhenItsFlushFails() {
        Fixture fixture = new Fixture();
        fixture.protocol.failOnFlush = 2;

        CommunicationsMode result = fixture.loop().run(CommunicationsMode.TRANSMIT_SCENARIO, 7);

        assertEquals(CommunicationsMode.CONTINUE, result);
        assertEquals(List.of("write:TRANSMIT_SCENARIO", "flush", "initial-plans", "read-boolean",
                "write:CONTINUE", "flush", "failed", "decrement", "decrement"), fixture.events);
        assertTrue(fixture.failed);
    }

    @Test
    void handlesCheckedOperationFailureBeforeTheFinalAcknowledgement() {
        Fixture fixture = new Fixture();
        fixture.operations.poolFailure = new ClassNotFoundException("broken persons");

        CommunicationsMode result = fixture.loop().run(CommunicationsMode.POOL_PERSONS, 7);

        assertEquals(CommunicationsMode.POOL_PERSONS, result);
        assertEquals(List.of("write:POOL_PERSONS", "flush", "pool", "failed", "decrement", "decrement"),
                fixture.events);
        assertTrue(fixture.failed);
    }

    @Test
    void handlesInterruptedDistributionBeforeTheFinalAcknowledgement() {
        Fixture fixture = new Fixture();
        fixture.operations.distributionFailure = new InterruptedException("interrupted distribution");

        fixture.loop().run(CommunicationsMode.DISTRIBUTE_PERSONS, 7);

        assertEquals(List.of("write:DISTRIBUTE_PERSONS", "flush", "distribute", "failed", "decrement",
                "decrement"), fixture.events);
        assertTrue(fixture.failed);
    }

    @Test
    void handlesUncheckedBoundsFailureLikeTheOriginalLoop() {
        Fixture fixture = new Fixture();
        fixture.operations.runtimeFailure = new IndexOutOfBoundsException("broken payload");

        fixture.loop().run(CommunicationsMode.TRANSMIT_SCORES, 7);

        assertEquals(List.of("write:TRANSMIT_SCORES", "flush", "scores", "failed", "decrement", "decrement"),
                fixture.events);
        assertTrue(fixture.failed);
    }

    private void assertSimpleDispatch(CommunicationsMode mode, String operation) {
        Fixture fixture = new Fixture();

        CommunicationsMode result = fixture.loop().run(mode, 7);

        assertEquals(mode, result);
        List<String> expected = new ArrayList<>(List.of("write:" + mode, "flush"));
        if (operation != null) {
            expected.add(operation);
        }
        expected.add("read-boolean");
        expected.add("decrement");
        assertEquals(expected, fixture.events);
        assertFalse(fixture.failed);
    }

    private static final class Fixture {
        private final List<String> events = new ArrayList<>();
        private final RecordingProtocol protocol = new RecordingProtocol(events);
        private final RecordingOperations operations = new RecordingOperations(events);
        private boolean failed;

        private MasterSlaveCommunicationsLoop loop() {
            return new MasterSlaveCommunicationsLoop(protocol, operations, () -> {
                failed = true;
                events.add("failed");
            }, () -> events.add("decrement"), LogManager.getLogger(MasterSlaveCommunicationsLoopTest.class));
        }
    }

    private static final class RecordingProtocol implements MasterSlaveCommunicationsLoop.Protocol {
        private final List<String> events;
        private int flushes;
        private int failOnFlush = -1;

        private RecordingProtocol(List<String> events) {
            this.events = events;
        }

        @Override
        public void writeMode(CommunicationsMode mode) {
            events.add("write:" + mode);
        }

        @Override
        public boolean readBoolean() {
            events.add("read-boolean");
            return true;
        }

        @Override
        public void flush() throws IOException {
            events.add("flush");
            flushes++;
            if (flushes == failOnFlush) {
                throw new IOException("broken flush");
            }
        }

        @Override
        public void reset() {
            events.add("reset");
        }
    }

    private static final class RecordingOperations implements MasterSlaveCommunicationsLoop.Operations {
        private final List<String> events;
        private ClassNotFoundException poolFailure;
        private InterruptedException distributionFailure;
        private RuntimeException runtimeFailure;

        private RecordingOperations(List<String> events) {
            this.events = events;
        }

        @Override
        public void transmitTravelTimes() {
            record("travel-times");
        }

        @Override
        public void poolPersons() throws ClassNotFoundException {
            record("pool");
            if (poolFailure != null) {
                throw poolFailure;
            }
        }

        @Override
        public void distributePersons() throws InterruptedException {
            record("distribute");
            if (distributionFailure != null) {
                throw distributionFailure;
            }
        }

        @Override
        public void transmitPlans() {
            record("plans");
        }

        @Override
        public void readSlaveReadiness() {
            record("readiness");
        }

        @Override
        public void transmitScores() {
            record("scores");
        }

        @Override
        public void transmitPerformance() {
            record("performance");
        }

        @Override
        public void transmitInitialPlans() {
            record("initial-plans");
        }

        private void record(String event) {
            events.add(event);
            if (runtimeFailure != null) {
                throw runtimeFailure;
            }
        }
    }
}
