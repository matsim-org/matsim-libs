package org.matsim.contrib.pseudosimulation.distributed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;

import org.junit.jupiter.api.Test;

class MasterStartupBootstrapTest {

    @Test
    void registersInitialSlavesBeforeStartingTheDynamicRegistry() throws Exception {
        List<String> events = new ArrayList<>();
        Deque<Socket> sockets = new ArrayDeque<>(List.of(new Socket(), new Socket()));
        AtomicInteger nextId = new AtomicInteger(7);
        FakeRegistry registry = new FakeRegistry();
        MasterStartupBootstrap<FakeSlave, FakeRegistry> bootstrap = MasterStartupBootstrap.testing(
                4321, 2, port -> {
                    events.add("open:" + port);
                    return new MasterStartupBootstrap.ConnectionSource() {
                        public Socket accept() {
                            events.add("accept");
                            return sockets.removeFirst();
                        }

                        public void close() {
                            events.add("close");
                        }
                    };
                }, (socket, id) -> {
                    events.add("create:" + id);
                    return new FakeSlave(id);
                }, (slave, id, initialRouting) -> events.add(
                        "initialize:" + id + ":routing=" + initialRouting), idSupplier -> {
                            events.add("registry:" + idSupplier.getAsInt());
                            return registry;
                        }, ids(nextId), milliseconds -> events.add("sleep:" + milliseconds),
                (task, name) -> events.add("start:" + name),
                (accepted, expected) -> events.add("report:" + accepted + "/" + expected));

        MasterStartupBootstrap.Result<FakeSlave, FakeRegistry> result = bootstrap.start();

        assertEquals(List.of("open:4321", "accept", "report:1/2", "create:7",
                "initialize:7:routing=true", "sleep:10", "accept", "report:2/2", "create:8",
                "initialize:8:routing=true", "sleep:10", "close", "registry:9", "start:HYDRA"), events);
        assertEquals(List.of(7, 8), new ArrayList<>(result.initialSlaves().keySet()));
        assertSame(registry, result.registry());
        assertEquals(10, nextId.get());
    }

    @Test
    void closesTheInitialServerAndDoesNotAdvanceTheIdWhenInitializationFails() {
        List<String> events = new ArrayList<>();
        AtomicInteger nextId = new AtomicInteger(3);
        MasterStartupBootstrap<FakeSlave, FakeRegistry> bootstrap = MasterStartupBootstrap.testing(
                1, 1, port -> new MasterStartupBootstrap.ConnectionSource() {
                    public Socket accept() {
                        return new Socket();
                    }

                    public void close() {
                        events.add("close");
                    }
                }, (socket, id) -> new FakeSlave(id), (slave, id, initialRouting) -> {
                    throw new IOException("broken initialization");
                }, idSupplier -> {
                    events.add("registry");
                    return new FakeRegistry();
                }, ids(nextId), milliseconds -> events.add("sleep"),
                (task, name) -> events.add("start"), (accepted, expected) -> events.add("report"));

        IOException failure = assertThrows(IOException.class, bootstrap::start);

        assertEquals("broken initialization", failure.getMessage());
        assertEquals(List.of("report", "close"), events);
        assertEquals(3, nextId.get());
    }

    private MasterStartupBootstrap.IdSequence ids(AtomicInteger nextId) {
        return new MasterStartupBootstrap.IdSequence() {
            public int current() {
                return nextId.get();
            }

            public void advance() {
                nextId.incrementAndGet();
            }
        };
    }

    private record FakeSlave(int id) { }

    private static final class FakeRegistry implements Runnable {
        @Override
        public void run() { }
    }
}
