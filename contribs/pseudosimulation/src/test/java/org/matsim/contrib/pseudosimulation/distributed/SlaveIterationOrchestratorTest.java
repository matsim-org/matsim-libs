package org.matsim.contrib.pseudosimulation.distributed;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.junit.jupiter.api.Test;

class SlaveIterationOrchestratorTest {

    @Test
    void initialRoutingRecordsStartupTimeAndCommunicatesBeforePreparingTheIteration() {
        Fixture fixture = new Fixture(100, 135, 140);
        fixture.orchestrator.configure(true, 3);

        fixture.orchestrator.iterationStarts(7);

        assertEquals(List.of("communicate", "activateTravelTime", "initializePlanCatcher"), fixture.events);
        assertEquals(35.0, fixture.orchestrator.totalIterationTime());
        assertEquals(7, fixture.orchestrator.currentIteration());
    }

    @Test
    void communicatesOnlyAfterEachCompletedCycle() {
        Fixture fixture = new Fixture(0, 10, 20, 20, 30, 30, 40, 40, 50, 50);
        fixture.orchestrator.configure(false, 2);

        fixture.orchestrator.iterationStarts(0);
        fixture.orchestrator.iterationStarts(1);
        fixture.orchestrator.iterationStarts(2);
        fixture.orchestrator.iterationStarts(3);
        fixture.orchestrator.iterationStarts(4);

        assertEquals(1, fixture.events.stream().filter("communicate"::equals).count());
        assertEquals(30.0, fixture.orchestrator.totalIterationTime());
        assertEquals(40.0, fixture.orchestrator.sumIterationTimes());
    }

    @Test
    void startupFailureIsRememberedAndHaltedAtTheNextScheduledCommunication() {
        Fixture fixture = new Fixture(0, 5, 10);
        fixture.communicationResult = false;
        fixture.orchestrator.configure(true, 4);

        fixture.orchestrator.startup();
        fixture.communicationResult = true;
        fixture.orchestrator.iterationStarts(0);

        assertEquals(List.of("communicate", "communicate", "halt", "activateTravelTime",
                "initializePlanCatcher"), fixture.events);
    }

    @Test
    void shutdownSendsPlansAndThenWaitsForTheKillSignal() {
        Fixture fixture = new Fixture(0, 8, 20, 20);
        fixture.orchestrator.configure(false, 5);
        fixture.orchestrator.iterationStarts(0);
        fixture.orchestrator.iterationStarts(1);
        fixture.events.clear();

        fixture.orchestrator.shutdown();

        assertEquals(List.of("communicate", "communicate"), fixture.events);
        assertEquals(12.0, fixture.orchestrator.totalIterationTime());
    }

    @Test
    void receivedPopulationResetsAccumulatedTimesButNotTheCurrentStartTime() {
        Fixture fixture = new Fixture(0, 10, 10, 25, 40, 50);
        fixture.orchestrator.configure(false, 3);
        fixture.orchestrator.iterationStarts(0);
        fixture.orchestrator.iterationStarts(1);

        fixture.orchestrator.resetIterationTimes();
        fixture.orchestrator.iterationStarts(2);

        assertEquals(15.0, fixture.orchestrator.sumIterationTimes());
    }

    private static final class Fixture implements SlaveIterationOrchestrator.Operations {
        private final List<String> events = new ArrayList<>();
        private final Deque<Long> times = new ArrayDeque<>();
        private final SlaveIterationOrchestrator orchestrator;
        private boolean communicationResult = true;

        private Fixture(long... times) {
            for (long time : times) {
                this.times.add(time);
            }
            orchestrator = new SlaveIterationOrchestrator(this, this.times::removeFirst);
        }

        public boolean communicate() {
            events.add("communicate");
            return communicationResult;
        }

        public void halt() {
            events.add("halt");
        }

        public void activateTravelTime() {
            events.add("activateTravelTime");
        }

        public void initializePlanCatcher() {
            events.add("initializePlanCatcher");
        }
    }
}
