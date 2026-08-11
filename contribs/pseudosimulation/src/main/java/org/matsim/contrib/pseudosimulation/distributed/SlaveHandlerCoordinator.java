package org.matsim.contrib.pseudosimulation.distributed;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.Logger;

final class SlaveHandlerCoordinator {
    private static final long POLL_INTERVAL_MILLISECONDS = 10;

    private final Logger logger;
    private final WorkerStarter workerStarter;
    private final Sleeper sleeper;
    private AtomicInteger activeThreads = new AtomicInteger();
    private boolean somethingWentWrong;

    private SlaveHandlerCoordinator(Logger logger, WorkerStarter workerStarter, Sleeper sleeper) {
        this.logger = logger;
        this.workerStarter = workerStarter;
        this.sleeper = sleeper;
    }

    static SlaveHandlerCoordinator production(Logger logger) {
        return new SlaveHandlerCoordinator(logger, (worker, name) -> {
            Thread thread = new Thread(worker);
            thread.setName(name);
            thread.start();
        }, Thread::sleep);
    }

    static SlaveHandlerCoordinator testing(Logger logger, WorkerStarter workerStarter, Sleeper sleeper) {
        return new SlaveHandlerCoordinator(logger, workerStarter, sleeper);
    }

    void start(Collection<? extends Handler> handlers, CommunicationsMode mode) {
        if (activeThreads.get() > 0) {
            logger.warn("All slaveHandlers have not finished previous operation but they are being asked to " + mode);
        }
        activeThreads = new AtomicInteger(handlers.size());
        for (Handler handler : handlers) {
            handler.setCommunicationsMode(mode);
            workerStarter.start(handler, "slave_" + handler.slaveNumber() + ":" + mode);
        }
    }

    void waitForCompletion() {
        logger.warn("Waiting for " + activeThreads.get() + " slaveHandlers");
        while (activeThreads.get() > 0) {
            try {
                sleeper.sleep(POLL_INTERVAL_MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException();
            }
        }
        if (somethingWentWrong) {
            logger.error("Something went wrong. Exiting.");
            throw new RuntimeException();
        }
        logger.warn("All slaveHandlers done.");
    }

    Runnable completion() {
        AtomicInteger counter = activeThreads;
        return counter::decrementAndGet;
    }

    void failed() {
        somethingWentWrong = true;
    }

    int activeThreadCount() {
        return activeThreads.get();
    }

    interface Handler extends Runnable {
        void setCommunicationsMode(CommunicationsMode mode);

        int slaveNumber();
    }

    interface WorkerStarter {
        void start(Runnable worker, String name);
    }

    interface Sleeper {
        void sleep(long milliseconds) throws InterruptedException;
    }
}
