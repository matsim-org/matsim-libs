package org.matsim.contrib.pseudosimulation.distributed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.pseudosimulation.distributed.listeners.events.transit.TransitPerformance;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;

class SlaveMasterSessionTest {

	@Test
	void readsInitializationInWireOrderAndReportsCapacity() throws Exception {
		Streams streams = streams(output -> writeInitialization(output));
		Context context = new Context();

		SlaveMasterSession.Connected connected = SlaveMasterSession.initialize(
				streams.reader, streams.writer, context, 12);

		SlaveMasterSession.Initialization initialization = connected.initialization();
		assertEquals(7, initialization.number());
		assertEquals(4, initialization.iterationsPerCycle());
		assertEquals(6, initialization.numberOfPlans());
		assertEquals(0.2, initialization.mutationRate());
		assertEquals(80, initialization.lastIteration());
		assertTrue(initialization.initialRouting());
		assertTrue(initialization.fullTransitPerformance());
		assertTrue(initialization.intelligentRouters());

		ObjectInputStream output = streams.outputReader();
		assertEquals(10, output.readLong());
		assertEquals(20, output.readLong());
		assertEquals(3, output.readInt());
		assertEquals(12, output.readInt());
	}

	@Test
	void sendsPerformanceAndAcknowledgesEachCommandInWireOrder() throws Exception {
		Streams streams = streams(output -> {
			writeInitialization(output);
			output.writeObject(CommunicationsMode.TRANSMIT_PERFORMANCE);
			output.writeObject(CommunicationsMode.CONTINUE);
		});
		Context context = new Context();
		context.totalIterationTime = 15.5;
		SlaveMasterSession session = SlaveMasterSession.initialize(streams.reader, streams.writer, context, 12)
				.session();

		assertTrue(session.communicate());

		ObjectInputStream output = streams.outputReader();
		assertEquals(10, output.readLong());
		assertEquals(20, output.readLong());
		assertEquals(3, output.readInt());
		assertEquals(12, output.readInt());
		assertEquals(15.5, output.readDouble());
		assertEquals(0, output.readInt());
		assertEquals(10, output.readLong());
		assertEquals(20, output.readLong());
		assertEquals(3, output.readInt());
		assertTrue(output.readBoolean());
		assertTrue(output.readBoolean());
	}

	private void writeInitialization(ObjectOutputStream output) throws IOException {
		output.writeInt(7);
		output.writeInt(4);
		output.writeInt(6);
		output.writeDouble(0.2);
		output.writeInt(80);
		output.writeBoolean(true);
		output.writeBoolean(false);
		output.writeBoolean(true);
		output.writeBoolean(false);
		output.writeBoolean(true);
		output.writeBoolean(false);
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

	private static final class Context implements SlaveMasterSession.Context {
		private final Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		private double totalIterationTime;

		public Iterable<? extends Person> persons() { return population.getPersons().values(); }
		public Population population() { return population; }
		public int currentIteration() { return 2; }
		public int masterIteration() { return 1; }
		public boolean transitEnabled() { return false; }
		public boolean fullTransitPerformance() { return false; }
		public void updateTravelTimes(int iteration, SerializableLinkTravelTimes times,
				TransitPerformance performance) { }
		public void addPersons(List<PersonSerializable> persons, int masterIteration) { }
		public List<PersonSerializable> takePersons(int difference) { return List.of(); }
		public boolean ready() { return true; }
		public double totalIterationTime() { return totalIterationTime; }
		public int executedPlanCount() { return 5; }
		public int iterationsPerCycle() { return 4; }
		public int populationSize() { return population.getPersons().size(); }
		public long memoryUse() { return 10; }
		public long maximumMemory() { return 20; }
		public int totalNumberOfPlans() { return 3; }
	}
}
