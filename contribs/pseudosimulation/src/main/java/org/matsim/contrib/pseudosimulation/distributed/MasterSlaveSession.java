package org.matsim.contrib.pseudosimulation.distributed;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.pseudosimulation.distributed.listeners.events.transit.TransitPerformance;

final class MasterSlaveSession implements SlaveHandlerCoordinator.Handler {
    private final Logger logger =
            LogManager.getLogger(MasterControler.class.getName() + "$SlaveHandler");
    private final Map<String, Plan> plans = new HashMap<>();
    private final ObjectInputStream reader;
    private final ObjectOutputStream writer;
    private final int number;
    private final Context context;
    private double totalIterationTime;
    private List<PersonSerializable> personPool;
    private int targetPopulationSize;
    private CommunicationsMode communicationsMode = CommunicationsMode.TRANSMIT_SCENARIO;
    private int currentPopulationSize;
    private long usedMemory;
    private long maxMemory;
    private int numberOfPlans;
    private int numberOfThreads;
    private boolean readyForNextIteration = true;

    MasterSlaveSession(Socket socket, int number, Context context) throws IOException {
        this(new ObjectOutputStream(socket.getOutputStream()), new ObjectInputStream(socket.getInputStream()),
                number, context);
    }

    MasterSlaveSession(ObjectOutputStream writer, ObjectInputStream reader, int number, Context context) {
        this.writer = writer;
        this.reader = reader;
        this.number = number;
        this.context = context;
    }

    void initialize(Initialization initialization) throws IOException {
        sendNumber(initialization.number());
        sendNumber(initialization.iterationsPerMasterIteration());
        sendNumber(initialization.numberOfPlans());
        sendDouble(initialization.mutationRate());
        sendNumber(initialization.lastIteration());
        sendBoolean(initialization.initialRouting());
        sendBoolean(initialization.quickReplanning());
        sendBoolean(initialization.fullTransitPerformance());
        sendBoolean(initialization.trackGenome());
        sendBoolean(initialization.intelligentRouters());
        sendBoolean(false);
        readMemoryStats();
        numberOfThreads = reader.readInt();
    }

    @Override
    public void run() {
        Runnable completion = context.completion();
        MasterSlaveCommunicationsLoop loop = new MasterSlaveCommunicationsLoop(protocol(), operations(),
                context::failed, completion::run, logger);
        communicationsMode = loop.run(communicationsMode, number);
    }

    private MasterSlaveCommunicationsLoop.Protocol protocol() {
        return new MasterSlaveCommunicationsLoop.Protocol() {
            @Override
            public void writeMode(CommunicationsMode mode) throws IOException {
                writer.writeObject(mode);
            }

            @Override
            public boolean readBoolean() throws IOException {
                return reader.readBoolean();
            }

            @Override
            public void flush() throws IOException {
                writer.flush();
            }

            @Override
            public void reset() throws IOException {
                writer.reset();
            }
        };
    }

    private MasterSlaveCommunicationsLoop.Operations operations() {
        return new MasterSlaveCommunicationsLoop.Operations() {
            public void transmitTravelTimes() throws IOException { MasterSlaveSession.this.transmitTravelTimes(); }
            public void poolPersons() throws IOException, ClassNotFoundException { MasterSlaveSession.this.poolPersons(); }
            public void distributePersons() throws IOException, InterruptedException { MasterSlaveSession.this.distributePersons(); }
            public void transmitPlans() throws IOException, ClassNotFoundException { MasterSlaveSession.this.transmitPlans(); }
            public void readSlaveReadiness() throws IOException { readyForNextIteration = reader.readBoolean(); }
            public void transmitScores() throws IOException, ClassNotFoundException { MasterSlaveSession.this.transmitScores(); }
            public void transmitPerformance() throws IOException { MasterSlaveSession.this.transmitPerformance(); }
            public void transmitInitialPlans() throws IOException { MasterSlaveSession.this.transmitInitialPlans(); }
        };
    }

    private void transmitPlans() throws IOException, ClassNotFoundException {
        plans.clear();
        logger.warn("Waiting to receive plans from slave number " + number);
        int slaveIteration = reader.readInt();
        int timesIteration = reader.readInt();
        logger.warn(String.format("Plan signature: M%03dP%03dT%03d ", context.currentIteration() + 1,
                slaveIteration, timesIteration));
        logger.warn("(M = iteration for execution on master,P = PSim iteration when plan came from on slave, T = travel time iteration from master used to generate plan on slave)");
        Map<String, PlanSerializable> serialPlans = SerializedObjectReader.readMap(reader);
        logger.warn("RECEIVED " + serialPlans.size() + " plans from slave number " + number);
        for (Entry<String, PlanSerializable> entry : serialPlans.entrySet()) {
            plans.put(entry.getKey(), entry.getValue().getPlan(context.population()));
        }
        currentPopulationSize = plans.size();
    }

    private void transmitPerformance() throws IOException {
        totalIterationTime = reader.readDouble();
        currentPopulationSize = reader.readInt();
        readMemoryStats();
    }

    private void transmitTravelTimes() throws IOException {
        logger.warn("About to send travel times to slave number " + number);
        writer.writeInt(context.currentIteration());
        writer.writeObject(context.linkTravelTimes());
        if (context.transitEnabled() && context.fullTransitPerformanceTransmission()) {
            writer.writeObject(context.transitPerformance());
        }
        writer.flush();
        logger.warn("SENT travel times to slave number " + number);
    }

    private void poolPersons() throws IOException, ClassNotFoundException {
        logger.warn("Trying to receive persons from slave " + number);
        logger.warn("Currently has " + currentPopulationSize + " persons, target is " + targetPopulationSize);
        personPool = new ArrayList<>();
        writer.writeInt(currentPopulationSize - targetPopulationSize);
        writer.flush();
        personPool = SerializedObjectReader.readList(reader);
    }

    private void distributePersons() throws IOException {
        logger.warn("Distributing persons to slave" + number);
        writer.writeInt(context.currentIteration());
        writer.writeObject(context.takePersons(currentPopulationSize - targetPopulationSize));
        writer.flush();
    }

    private void transmitInitialPlans() throws IOException {
        writer.writeInt(context.currentIteration());
        writer.writeObject(personPool);
        writer.flush();
        currentPopulationSize = personPool.size();
    }

    private void transmitScores() throws IOException, ClassNotFoundException {
        context.recordScores(context.currentIteration(), currentPopulationSize,
                context.population().getPersons().size(), (double[]) reader.readObject());
    }

    private void sendNumber(int value) throws IOException { writer.writeInt(value); writer.flush(); }
    private void sendDouble(double value) throws IOException { writer.writeDouble(value); writer.flush(); }
    private void sendBoolean(boolean value) throws IOException { writer.writeBoolean(value); writer.flush(); }

    private void readMemoryStats() throws IOException {
        usedMemory = reader.readLong();
        maxMemory = reader.readLong();
        numberOfPlans = reader.readInt();
    }

    @Override public void setCommunicationsMode(CommunicationsMode mode) { communicationsMode = mode; }
    @Override public int slaveNumber() { return number; }
    int numberOfPlans() { return numberOfPlans; }
    int numberOfThreads() { return numberOfThreads; }
    boolean readyForNextIteration() { return readyForNextIteration; }
    void setTargetPopulationSize(int value) { targetPopulationSize = value; }
    int currentPopulationSize() { return currentPopulationSize; }
    double totalIterationTime() { return totalIterationTime; }
    long usedMemory() { return usedMemory; }
    long maxMemory() { return maxMemory; }
    Map<String, Plan> plans() { return plans; }
    Collection<? extends PersonSerializable> persons() { return personPool; }
    void setPersons(List<PersonSerializable> persons) { personPool = persons; }

    record Initialization(int number, int iterationsPerMasterIteration, int numberOfPlans, double mutationRate,
                          int lastIteration, boolean initialRouting, boolean quickReplanning,
                          boolean fullTransitPerformance, boolean trackGenome, boolean intelligentRouters) { }

    interface Context {
        int currentIteration();
        Population population();
        SerializableLinkTravelTimes linkTravelTimes();
        boolean transitEnabled();
        boolean fullTransitPerformanceTransmission();
        TransitPerformance transitPerformance();
        List<PersonSerializable> takePersons(int difference);
        void recordScores(int iteration, int slavePopulation, int masterPopulation, double[] scores);
        Runnable completion();
        void failed();
    }
}
