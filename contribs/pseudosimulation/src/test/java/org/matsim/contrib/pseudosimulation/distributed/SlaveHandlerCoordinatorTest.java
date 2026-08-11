package org.matsim.contrib.pseudosimulation.distributed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Test;

class SlaveHandlerCoordinatorTest {

    @Test
    void assignsModesNamesThreadsAndWaitsForTheirCompletion() {
        RecordingStarter starter = new RecordingStarter();
        List<Long> sleeps = new ArrayList<>();
        SlaveHandlerCoordinator coordinator = coordinator(starter, milliseconds -> {
            sleeps.add(milliseconds);
            starter.runNext();
        });
        RecordingHandler first = new RecordingHandler(3, coordinator);
        RecordingHandler second = new RecordingHandler(8, coordinator);

        coordinator.start(List.of(first, second), CommunicationsMode.TRANSMIT_PERFORMANCE);
        coordinator.waitForCompletion();

        assertEquals(CommunicationsMode.TRANSMIT_PERFORMANCE, first.mode);
        assertEquals(CommunicationsMode.TRANSMIT_PERFORMANCE, second.mode);
        assertEquals(List.of("slave_3:TRANSMIT_PERFORMANCE", "slave_8:TRANSMIT_PERFORMANCE"), starter.names);
        assertEquals(1, first.runs);
        assertEquals(1, second.runs);
        assertEquals(List.of(10L, 10L), sleeps);
        assertEquals(0, coordinator.activeThreadCount());
    }

    @Test
    void acceptsAnEmptyHandlerSetWithoutSleeping() {
        RecordingStarter starter = new RecordingStarter();
        List<Long> sleeps = new ArrayList<>();
        SlaveHandlerCoordinator coordinator = coordinator(starter, sleeps::add);

        coordinator.start(List.of(), CommunicationsMode.CONTINUE);
        coordinator.waitForCompletion();

        assertEquals(List.of(), starter.names);
        assertEquals(List.of(), sleeps);
        assertEquals(0, coordinator.activeThreadCount());
    }

    @Test
    void replacesTheActiveCounterWhenOperationsOverlap() {
        RecordingStarter starter = new RecordingStarter();
        SlaveHandlerCoordinator coordinator = coordinator(starter, milliseconds -> {
        });
        RecordingHandler oldHandler = new RecordingHandler(1, coordinator);
        RecordingHandler newFirst = new RecordingHandler(2, coordinator);
        RecordingHandler newSecond = new RecordingHandler(3, coordinator);

        coordinator.start(List.of(oldHandler), CommunicationsMode.POOL_PERSONS);
        Runnable oldCompletion = coordinator.completion();
        coordinator.start(List.of(newFirst, newSecond), CommunicationsMode.DISTRIBUTE_PERSONS);
        oldCompletion.run();

        assertEquals(2, coordinator.activeThreadCount());
        assertEquals(CommunicationsMode.POOL_PERSONS, oldHandler.mode);
        assertEquals(CommunicationsMode.DISTRIBUTE_PERSONS, newFirst.mode);
        assertEquals(CommunicationsMode.DISTRIBUTE_PERSONS, newSecond.mode);
    }

    @Test
    void preservesFailureStateAndAcceptsTheLegacyDoubleDecrement() {
        RecordingStarter starter = new RecordingStarter();
        SlaveHandlerCoordinator coordinator = coordinator(starter, milliseconds -> {
        });

        coordinator.start(List.of(new RecordingHandler(4, coordinator)), CommunicationsMode.TRANSMIT_SCORES);
        coordinator.failed();
        Runnable completion = coordinator.completion();
        completion.run();
        completion.run();

        assertEquals(-1, coordinator.activeThreadCount());
        assertThrows(RuntimeException.class, coordinator::waitForCompletion);
    }

    @Test
    void turnsAnInterruptedPollIntoTheExistingRuntimeFailure() {
        RecordingStarter starter = new RecordingStarter();
        List<Long> sleeps = new ArrayList<>();
        SlaveHandlerCoordinator coordinator = coordinator(starter, milliseconds -> {
            sleeps.add(milliseconds);
            throw new InterruptedException("interrupted wait");
        });
        coordinator.start(List.of(new RecordingHandler(5, coordinator)), CommunicationsMode.WAIT);

        assertThrows(RuntimeException.class, coordinator::waitForCompletion);

        assertEquals(List.of(10L), sleeps);
        assertEquals(1, coordinator.activeThreadCount());
    }

    private SlaveHandlerCoordinator coordinator(RecordingStarter starter, SlaveHandlerCoordinator.Sleeper sleeper) {
        return SlaveHandlerCoordinator.testing(LogManager.getLogger(SlaveHandlerCoordinatorTest.class), starter, sleeper);
    }

    private static final class RecordingStarter implements SlaveHandlerCoordinator.WorkerStarter {
        private final List<Runnable> workers = new ArrayList<>();
        private final List<String> names = new ArrayList<>();

        @Override
        public void start(Runnable worker, String name) {
            workers.add(worker);
            names.add(name);
        }

        private void runNext() {
            workers.remove(0).run();
        }
    }

    private static final class RecordingHandler implements SlaveHandlerCoordinator.Handler {
        private final int number;
        private final SlaveHandlerCoordinator coordinator;
        private CommunicationsMode mode;
        private int runs;

        private RecordingHandler(int number, SlaveHandlerCoordinator coordinator) {
            this.number = number;
            this.coordinator = coordinator;
        }

        @Override
        public void run() {
            runs++;
            coordinator.completion().run();
        }

        @Override
        public void setCommunicationsMode(CommunicationsMode mode) {
            this.mode = mode;
        }

        @Override
        public int slaveNumber() {
            return number;
        }
    }
}
