package org.matsim.contrib.pseudosimulation.distributed;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.TreeMap;
import java.util.function.IntSupplier;

final class MasterStartupBootstrap<S, R extends Runnable> {
    private static final long INITIAL_REGISTRATION_DELAY_MILLISECONDS = 10;
    private static final String REGISTRY_THREAD_NAME = "HYDRA";

    private final int port;
    private final int initialSlaveCount;
    private final ConnectionSourceFactory connectionSourceFactory;
    private final SlaveFactory<S> slaveFactory;
    private final SlaveInitializer<S> slaveInitializer;
    private final RegistryFactory<R> registryFactory;
    private final IdSequence ids;
    private final Sleeper sleeper;
    private final ThreadStarter threadStarter;
    private final AcceptanceReporter acceptanceReporter;

    private MasterStartupBootstrap(int port, int initialSlaveCount,
            ConnectionSourceFactory connectionSourceFactory, SlaveFactory<S> slaveFactory,
            SlaveInitializer<S> slaveInitializer, RegistryFactory<R> registryFactory,
            IdSequence ids, Sleeper sleeper, ThreadStarter threadStarter,
            AcceptanceReporter acceptanceReporter) {
        this.port = port;
        this.initialSlaveCount = initialSlaveCount;
        this.connectionSourceFactory = connectionSourceFactory;
        this.slaveFactory = slaveFactory;
        this.slaveInitializer = slaveInitializer;
        this.registryFactory = registryFactory;
        this.ids = ids;
        this.sleeper = sleeper;
        this.threadStarter = threadStarter;
        this.acceptanceReporter = acceptanceReporter;
    }

    static <S, R extends Runnable> MasterStartupBootstrap<S, R> production(int port, int initialSlaveCount,
            SlaveFactory<S> slaveFactory, SlaveInitializer<S> slaveInitializer,
            RegistryFactory<R> registryFactory, IdSequence ids) {
        return new MasterStartupBootstrap<>(port, initialSlaveCount, serverPort -> {
            ServerSocket server = new ServerSocket(serverPort);
            return new ConnectionSource() {
                @Override
                public Socket accept() throws IOException {
                    return server.accept();
                }

                @Override
                public void close() throws IOException {
                    server.close();
                }
            };
        }, slaveFactory, slaveInitializer, registryFactory, ids, Thread::sleep, (task, name) -> {
            Thread thread = new Thread(task);
            thread.setName(name);
            thread.start();
        }, (accepted, expected) -> System.out.println(
                "Slave " + accepted + " out of an initial " + expected + " accepted.\n"));
    }

    static <S, R extends Runnable> MasterStartupBootstrap<S, R> testing(int port, int initialSlaveCount,
            ConnectionSourceFactory connectionSourceFactory, SlaveFactory<S> slaveFactory,
            SlaveInitializer<S> slaveInitializer, RegistryFactory<R> registryFactory,
            IdSequence ids, Sleeper sleeper, ThreadStarter threadStarter,
            AcceptanceReporter acceptanceReporter) {
        return new MasterStartupBootstrap<>(port, initialSlaveCount, connectionSourceFactory, slaveFactory,
                slaveInitializer, registryFactory, ids, sleeper, threadStarter, acceptanceReporter);
    }

    Result<S, R> start() throws IOException, InterruptedException {
        TreeMap<Integer, S> initialSlaves = new TreeMap<>();
        try (ConnectionSource connections = connectionSourceFactory.open(port)) {
            for (int accepted = 1; accepted <= initialSlaveCount; accepted++) {
                Socket socket = connections.accept();
                int id = ids.current();
                acceptanceReporter.accepted(accepted, initialSlaveCount);
                S slave = slaveFactory.create(socket, id);
                initialSlaves.put(id, slave);
                slaveInitializer.initialize(slave, id, true);
                ids.advance();
                sleeper.sleep(INITIAL_REGISTRATION_DELAY_MILLISECONDS);
            }
        }

        R registry = registryFactory.create(ids::take);
        threadStarter.start(registry, REGISTRY_THREAD_NAME);
        return new Result<>(initialSlaves, registry);
    }

    record Result<S, R extends Runnable>(TreeMap<Integer, S> initialSlaves, R registry) { }

    interface ConnectionSourceFactory {
        ConnectionSource open(int port) throws IOException;
    }

    interface ConnectionSource extends AutoCloseable {
        Socket accept() throws IOException;
        @Override void close() throws IOException;
    }

    interface SlaveFactory<S> {
        S create(Socket socket, int id) throws IOException;
    }

    interface SlaveInitializer<S> {
        void initialize(S slave, int id, boolean initialRouting) throws IOException;
    }

    interface RegistryFactory<R> {
        R create(IntSupplier idSupplier);
    }

    interface IdSequence {
        int current();
        void advance();

        default int take() {
            int id = current();
            advance();
            return id;
        }
    }

    interface Sleeper {
        void sleep(long milliseconds) throws InterruptedException;
    }

    interface ThreadStarter {
        void start(Runnable task, String name);
    }

    interface AcceptanceReporter {
        void accepted(int accepted, int expected);
    }
}
