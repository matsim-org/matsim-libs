package org.matsim.contrib.pseudosimulation.distributed;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import org.apache.logging.log4j.Logger;

final class DynamicSlaveRegistry<S> implements Runnable {
    private static final long LOCK_POLL_MILLISECONDS = 10;
    private static final long REGISTRATION_DELAY_MILLISECONDS = 1000;

    private final int port;
    private final ConnectionSourceFactory connectionSourceFactory;
    private final SlaveFactory<S> slaveFactory;
    private final IntSupplier idSupplier;
    private final SlaveInitializer<S> initializer;
    private final SlavePreparer<S> preparer;
    private final Predicate<S> readyForNextIteration;
    private final ToIntFunction<S> slaveNumber;
    private final Sleeper sleeper;
    private final Logger logger;
    private final AtomicBoolean accessingMap;
    private TreeMap<Integer, S> pendingSlaves = new TreeMap<>();
    private boolean acceptSlaves = true;

    private DynamicSlaveRegistry(int port, ConnectionSourceFactory connectionSourceFactory,
                                 SlaveFactory<S> slaveFactory, IntSupplier idSupplier,
                                 SlaveInitializer<S> initializer, SlavePreparer<S> preparer,
                                 Predicate<S> readyForNextIteration, ToIntFunction<S> slaveNumber,
                                 Sleeper sleeper, Logger logger, AtomicBoolean accessingMap) {
        this.port = port;
        this.connectionSourceFactory = connectionSourceFactory;
        this.slaveFactory = slaveFactory;
        this.idSupplier = idSupplier;
        this.initializer = initializer;
        this.preparer = preparer;
        this.readyForNextIteration = readyForNextIteration;
        this.slaveNumber = slaveNumber;
        this.sleeper = sleeper;
        this.logger = logger;
        this.accessingMap = accessingMap;
    }

    static <S> DynamicSlaveRegistry<S> production(int port, SlaveFactory<S> slaveFactory,
                                                   IntSupplier idSupplier, SlaveInitializer<S> initializer,
                                                   SlavePreparer<S> preparer, Predicate<S> readyForNextIteration,
                                                   ToIntFunction<S> slaveNumber, Logger logger) {
        return new DynamicSlaveRegistry<>(port, serverPort -> {
            ServerSocket server = new ServerSocket(serverPort);
            return server::accept;
        }, slaveFactory, idSupplier, initializer, preparer, readyForNextIteration, slaveNumber,
                Thread::sleep, logger, new AtomicBoolean(false));
    }

    static <S> DynamicSlaveRegistry<S> testing(int port, ConnectionSourceFactory connectionSourceFactory,
                                                SlaveFactory<S> slaveFactory, IntSupplier idSupplier,
                                                SlaveInitializer<S> initializer, SlavePreparer<S> preparer,
                                                Predicate<S> readyForNextIteration, ToIntFunction<S> slaveNumber,
                                                Sleeper sleeper, Logger logger, AtomicBoolean accessingMap) {
        return new DynamicSlaveRegistry<>(port, connectionSourceFactory, slaveFactory, idSupplier, initializer,
                preparer, readyForNextIteration, slaveNumber, sleeper, logger, accessingMap);
    }

    @Override
    public void run() {
        try {
            ConnectionSource connectionSource = connectionSourceFactory.open(port);
            while (acceptSlaves) {
                Socket socket = connectionSource.accept();
                int id = idSupplier.getAsInt();
                logger.warn("Slave accepted.");
                S slave = slaveFactory.create(socket, id);
                waitForMap();
                accessingMap.set(true);
                pendingSlaves.put(id, slave);
                initializer.initialize(slave, id);
                preparer.prepare(slave);
                accessingMap.set(false);
                sleeper.sleep(REGISTRATION_DELAY_MILLISECONDS);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void kill() {
        acceptSlaves = false;
    }

    TreeMap<Integer, S> drainReadySlaves() {
        while (accessingMap.get()) {
            try {
                sleeper.sleep(LOCK_POLL_MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        accessingMap.set(true);
        List<Integer> slavesToDrop = new ArrayList<>();
        for (S slave : pendingSlaves.values()) {
            if (!readyForNextIteration.test(slave)) {
                slavesToDrop.add(slaveNumber.applyAsInt(slave));
            }
        }
        for (int id : slavesToDrop) {
            pendingSlaves.remove(id);
        }
        TreeMap<Integer, S> slaveBatch = pendingSlaves;
        pendingSlaves = new TreeMap<>();
        accessingMap.set(false);
        return slaveBatch;
    }

    int pendingCount() {
        return pendingSlaves.size();
    }

    boolean isAccepting() {
        return acceptSlaves;
    }

    boolean isAccessingMap() {
        return accessingMap.get();
    }

    private void waitForMap() throws InterruptedException {
        while (accessingMap.get()) {
            sleeper.sleep(LOCK_POLL_MILLISECONDS);
        }
    }

    interface ConnectionSourceFactory {
        ConnectionSource open(int port) throws IOException;
    }

    interface ConnectionSource {
        Socket accept() throws IOException;
    }

    interface SlaveFactory<S> {
        S create(Socket socket, int id) throws IOException;
    }

    interface SlaveInitializer<S> {
        void initialize(S slave, int id) throws IOException;
    }

    interface SlavePreparer<S> {
        void prepare(S slave);
    }

    interface Sleeper {
        void sleep(long milliseconds) throws InterruptedException;
    }
}
