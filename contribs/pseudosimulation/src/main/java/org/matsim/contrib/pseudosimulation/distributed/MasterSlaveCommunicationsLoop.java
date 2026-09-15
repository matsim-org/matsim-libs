package org.matsim.contrib.pseudosimulation.distributed;

import java.io.IOException;

import org.apache.logging.log4j.Logger;

final class MasterSlaveCommunicationsLoop {
    private final Protocol protocol;
    private final Operations operations;
    private final FailureHandler failureHandler;
    private final ThreadCounter threadCounter;
    private final Logger logger;

    MasterSlaveCommunicationsLoop(Protocol protocol, Operations operations, FailureHandler failureHandler,
                                  ThreadCounter threadCounter, Logger logger) {
        this.protocol = protocol;
        this.operations = operations;
        this.failureHandler = failureHandler;
        this.threadCounter = threadCounter;
        this.logger = logger;
    }

    CommunicationsMode run(CommunicationsMode communicationsMode, int slaveNumber) {
        try {
            logger.warn("SlaveHandler " + slaveNumber + " entering comms mode: " + communicationsMode);
            protocol.writeMode(communicationsMode);
            protocol.flush();
            switch (communicationsMode) {
                case TRANSMIT_TRAVEL_TIMES:
                    operations.transmitTravelTimes();
                    protocol.readBoolean();
                    communicationsMode = CommunicationsMode.CONTINUE;
                    continueCommunications();
                    break;
                case POOL_PERSONS:
                    operations.poolPersons();
                    break;
                case DISTRIBUTE_PERSONS:
                    operations.distributePersons();
                    break;
                case TRANSMIT_PLANS_TO_MASTER:
                    protocol.reset();
                    operations.transmitPlans();
                    operations.readSlaveReadiness();
                    break;
                case TRANSMIT_SCORES:
                    operations.transmitScores();
                    break;
                case TRANSMIT_PERFORMANCE:
                    operations.transmitPerformance();
                    break;
                case TRANSMIT_SCENARIO:
                    operations.transmitInitialPlans();
                    protocol.readBoolean();
                    communicationsMode = CommunicationsMode.CONTINUE;
                    continueCommunications();
                    break;
                case DIE:
                    return communicationsMode;
                case CONTINUE:
                case WAIT:
                    break;
            }
            protocol.readBoolean();
        } catch (IOException | InterruptedException | IndexOutOfBoundsException | ClassNotFoundException e) {
            e.printStackTrace();
            failureHandler.failed();
            threadCounter.decrement();
        }
        threadCounter.decrement();
        logger.warn("SlaveHandler " + slaveNumber + " leaving comms mode: " + communicationsMode);
        return communicationsMode;
    }

    private void continueCommunications() throws IOException {
        protocol.writeMode(CommunicationsMode.CONTINUE);
        protocol.flush();
    }

    interface Protocol {
        void writeMode(CommunicationsMode mode) throws IOException;

        boolean readBoolean() throws IOException;

        void flush() throws IOException;

        void reset() throws IOException;
    }

    interface Operations {
        void transmitTravelTimes() throws IOException;

        void poolPersons() throws IOException, ClassNotFoundException;

        void distributePersons() throws IOException, InterruptedException;

        void transmitPlans() throws IOException, ClassNotFoundException;

        void readSlaveReadiness() throws IOException;

        void transmitScores() throws IOException, ClassNotFoundException;

        void transmitPerformance() throws IOException;

        void transmitInitialPlans() throws IOException;
    }

    interface FailureHandler {
        void failed();
    }

    interface ThreadCounter {
        void decrement();
    }
}
