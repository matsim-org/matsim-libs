package org.matsim.contrib.pseudosimulation.distributed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Test;

class DynamicSlaveRegistryTest {

    @Test
    void acceptsInitializesAndPreparesSlavesBeforeDrainingOnlyReadyOnes() {
        List<String> events = new ArrayList<>();
        List<Long> sleeps = new ArrayList<>();
        AtomicInteger nextId = new AtomicInteger(4);
        AtomicBoolean lock = new AtomicBoolean(false);
        Deque<Socket> sockets = new ArrayDeque<>(List.of(new Socket(), new Socket()));
        DynamicSlaveRegistry<FakeSlave>[] holder = registryHolder();
        holder[0] = registry(4321, port -> {
            events.add("open:" + port);
            return () -> {
                events.add("accept");
                return sockets.removeFirst();
            };
        }, nextId::getAndIncrement, (slave, id) -> {
            events.add("initialize:" + id + ":pending=" + holder[0].pendingCount());
            if (id == 5) {
                holder[0].kill();
            }
        }, slave -> {
            slave.prepared = true;
            events.add("prepare:" + slave.number);
        }, milliseconds -> sleeps.add(milliseconds), lock);

        holder[0].run();
        TreeMap<Integer, FakeSlave> ready = holder[0].drainReadySlaves();

        assertEquals(List.of("open:4321", "accept", "initialize:4:pending=1", "prepare:4",
                "accept", "initialize:5:pending=2", "prepare:5"), events);
        assertEquals(List.of(1000L, 1000L), sleeps);
        assertEquals(List.of(4), new ArrayList<>(ready.keySet()));
        assertTrue(ready.get(4).prepared);
        assertEquals(0, holder[0].pendingCount());
        assertFalse(holder[0].isAccessingMap());
        assertFalse(holder[0].isAccepting());
    }

    @Test
    void waitsForTheMapLockBeforePublishingAnAcceptedSlave() {
        List<Long> sleeps = new ArrayList<>();
        AtomicBoolean lock = new AtomicBoolean(true);
        DynamicSlaveRegistry<FakeSlave>[] holder = registryHolder();
        holder[0] = registry(1, port -> () -> new Socket(), () -> 9, (slave, id) -> holder[0].kill(),
                slave -> {
                }, milliseconds -> {
                    sleeps.add(milliseconds);
                    if (milliseconds == 10) {
                        lock.set(false);
                    }
                }, lock);

        holder[0].run();

        assertEquals(List.of(10L, 1000L), sleeps);
        assertEquals(1, holder[0].pendingCount());
        assertFalse(holder[0].isAccessingMap());
    }

    @Test
    void opensTheConnectionSourceButDoesNotAcceptAfterEarlyShutdown() {
        List<String> events = new ArrayList<>();
        DynamicSlaveRegistry<FakeSlave> registry = registry(7654, port -> {
            events.add("open:" + port);
            return () -> {
                events.add("accept");
                return new Socket();
            };
        }, () -> 1, (slave, id) -> {
        }, slave -> {
        }, milliseconds -> {
        }, new AtomicBoolean(false));

        registry.kill();
        registry.run();

        assertEquals(List.of("open:7654"), events);
        assertEquals(0, registry.pendingCount());
    }

    @Test
    void catchesConnectionFailuresAndStopsTheAcceptanceLoop() {
        DynamicSlaveRegistry<FakeSlave> registry = registry(1, port -> () -> {
            throw new IOException("broken accept");
        }, () -> 1, (slave, id) -> {
        }, slave -> {
        }, milliseconds -> {
        }, new AtomicBoolean(false));

        registry.run();

        assertEquals(0, registry.pendingCount());
        assertTrue(registry.isAccepting());
    }

    @Test
    void preservesTheLockedPendingEntryWhenInitializationFails() {
        List<String> events = new ArrayList<>();
        DynamicSlaveRegistry<FakeSlave> registry = registry(1, port -> () -> new Socket(), () -> 6,
                (slave, id) -> {
                    events.add("initialize");
                    throw new IOException("broken initialization");
                }, slave -> events.add("prepare"), milliseconds -> events.add("sleep"),
                new AtomicBoolean(false));

        registry.run();

        assertEquals(List.of("initialize"), events);
        assertEquals(1, registry.pendingCount());
        assertTrue(registry.isAccessingMap());
    }

    @Test
    void catchesInterruptedRegistrationWhileRetainingTheExistingLockState() {
        AtomicBoolean lock = new AtomicBoolean(true);
        DynamicSlaveRegistry<FakeSlave> registry = registry(1, port -> () -> new Socket(), () -> 3,
                (slave, id) -> {
                }, slave -> {
                }, milliseconds -> {
                    throw new InterruptedException("interrupted registration");
                }, lock);

        registry.run();

        assertEquals(0, registry.pendingCount());
        assertTrue(registry.isAccessingMap());
        assertTrue(registry.isAccepting());
    }

    @Test
    void interruptedBatchPollingPrintsAndRetries() {
        AtomicBoolean lock = new AtomicBoolean(false);
        DynamicSlaveRegistry<FakeSlave>[] holder = registryHolder();
        holder[0] = registry(1, port -> () -> new Socket(), () -> 2, (slave, id) -> holder[0].kill(),
                slave -> {
                }, milliseconds -> {
                    if (milliseconds == 10) {
                        lock.set(false);
                        throw new InterruptedException("interrupted drain");
                    }
                }, lock);
        holder[0].run();
        lock.set(true);

        TreeMap<Integer, FakeSlave> drained = holder[0].drainReadySlaves();

        assertEquals(List.of(2), new ArrayList<>(drained.keySet()));
        assertFalse(holder[0].isAccessingMap());
    }

    private DynamicSlaveRegistry<FakeSlave> registry(int port,
            DynamicSlaveRegistry.ConnectionSourceFactory connectionSourceFactory,
            java.util.function.IntSupplier idSupplier,
            DynamicSlaveRegistry.SlaveInitializer<FakeSlave> initializer,
            DynamicSlaveRegistry.SlavePreparer<FakeSlave> preparer,
            DynamicSlaveRegistry.Sleeper sleeper, AtomicBoolean lock) {
        return DynamicSlaveRegistry.testing(port, connectionSourceFactory, (socket, id) -> {
            return new FakeSlave(id, id != 5);
        }, idSupplier, initializer, preparer, slave -> slave.ready, slave -> slave.number, sleeper,
                LogManager.getLogger(DynamicSlaveRegistryTest.class), lock);
    }

    @SuppressWarnings("unchecked")
    private DynamicSlaveRegistry<FakeSlave>[] registryHolder() {
        return (DynamicSlaveRegistry<FakeSlave>[]) new DynamicSlaveRegistry<?>[1];
    }

    private static final class FakeSlave {
        private final int number;
        private final boolean ready;
        private boolean prepared;

        private FakeSlave(int number, boolean ready) {
            this.number = number;
            this.ready = ready;
        }
    }
}
