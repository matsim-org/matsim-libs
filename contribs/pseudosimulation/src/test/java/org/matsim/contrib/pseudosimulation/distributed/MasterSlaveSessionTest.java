package org.matsim.contrib.pseudosimulation.distributed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.pseudosimulation.distributed.listeners.events.transit.TransitPerformance;

class MasterSlaveSessionTest {

    @Test
    void writesInitializationValuesInProtocolOrderAndReadsSlaveCapacity() throws Exception {
        Streams streams = streams(output -> {
            output.writeLong(101);
            output.writeLong(202);
            output.writeInt(3);
            output.writeInt(8);
        });
        MasterSlaveSession session = new MasterSlaveSession(streams.writer, streams.reader, 4, new Context());

        session.initialize(new MasterSlaveSession.Initialization(4, 6, 7, 0.25, 120,
                true, false, true, false, true));

        ObjectInputStream output = streams.outputReader();
        assertEquals(4, output.readInt());
        assertEquals(6, output.readInt());
        assertEquals(7, output.readInt());
        assertEquals(0.25, output.readDouble());
        assertEquals(120, output.readInt());
        assertEquals(true, output.readBoolean());
        assertEquals(false, output.readBoolean());
        assertEquals(true, output.readBoolean());
        assertEquals(false, output.readBoolean());
        assertEquals(true, output.readBoolean());
        assertEquals(false, output.readBoolean());
        assertEquals(101, session.usedMemory());
        assertEquals(202, session.maxMemory());
        assertEquals(3, session.numberOfPlans());
        assertEquals(8, session.numberOfThreads());
    }

    @Test
    void receivesPerformanceAndMemoryStateBeforeFinalAcknowledgement() throws Exception {
        Streams streams = streams(output -> {
            output.writeDouble(12.5);
            output.writeInt(42);
            output.writeLong(1000);
            output.writeLong(2000);
            output.writeInt(84);
            output.writeBoolean(true);
        });
        Context context = new Context();
        MasterSlaveSession session = new MasterSlaveSession(streams.writer, streams.reader, 9, context);
        session.setCommunicationsMode(CommunicationsMode.TRANSMIT_PERFORMANCE);

        session.run();

        ObjectInputStream output = streams.outputReader();
        assertEquals(CommunicationsMode.TRANSMIT_PERFORMANCE, output.readObject());
        assertEquals(12.5, session.totalIterationTime());
        assertEquals(42, session.currentPopulationSize());
        assertEquals(1000, session.usedMemory());
        assertEquals(2000, session.maxMemory());
        assertEquals(84, session.numberOfPlans());
        assertEquals(1, context.completions);
        assertFalse(context.failed);
    }

    @Test
    void sendsInitialPersonsThenTransitionsToContinueInWireOrder() throws Exception {
        Streams streams = streams(output -> {
            output.writeBoolean(true);
            output.writeBoolean(true);
        });
        Context context = new Context();
        context.iteration = 17;
        MasterSlaveSession session = new MasterSlaveSession(streams.writer, streams.reader, 2, context);
        session.setPersons(new ArrayList<>());

        session.run();

        ObjectInputStream output = streams.outputReader();
        assertEquals(CommunicationsMode.TRANSMIT_SCENARIO, output.readObject());
        assertEquals(17, output.readInt());
        assertEquals(List.of(), output.readObject());
        assertEquals(CommunicationsMode.CONTINUE, output.readObject());
        assertEquals(0, session.currentPopulationSize());
        assertEquals(1, context.completions);
    }

    private Streams streams(OutputWriter input) throws IOException {
        ByteArrayOutputStream inputBytes = new ByteArrayOutputStream();
        try (ObjectOutputStream inputWriter = new ObjectOutputStream(inputBytes)) {
            input.write(inputWriter);
        }
        return new Streams(new ObjectInputStream(new ByteArrayInputStream(inputBytes.toByteArray())));
    }

    private interface OutputWriter {
        void write(ObjectOutputStream output) throws IOException;
    }

    private static final class Streams {
        private final ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
        private final ObjectOutputStream writer;
        private final ObjectInputStream reader;

        private Streams(ObjectInputStream reader) throws IOException {
            this.reader = reader;
            writer = new ObjectOutputStream(outputBytes);
        }

        private ObjectInputStream outputReader() throws IOException {
            writer.flush();
            return new ObjectInputStream(new ByteArrayInputStream(outputBytes.toByteArray()));
        }
    }

    private static final class Context implements MasterSlaveSession.Context {
        private int iteration;
        private int completions;
        private boolean failed;

        public int currentIteration() { return iteration; }
        public Population population() { return null; }
        public SerializableLinkTravelTimes linkTravelTimes() { return null; }
        public boolean transitEnabled() { return false; }
        public boolean fullTransitPerformanceTransmission() { return false; }
        public TransitPerformance transitPerformance() { return null; }
        public List<PersonSerializable> takePersons(int difference) { return List.of(); }
        public void recordScores(int iteration, int slavePopulation, int masterPopulation, double[] scores) { }
        public Runnable completion() { return () -> completions++; }
        public void failed() { failed = true; }
    }
}
