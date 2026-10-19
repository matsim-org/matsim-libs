package org.matsim.contrib.pseudosimulation.distributed;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.pseudosimulation.distributed.instrumentation.scorestats.SlaveScoreStatsCalculator;
import org.matsim.contrib.pseudosimulation.distributed.listeners.events.transit.TransitPerformance;

/** Owns one slave-side connection to the master and its wire protocol. */
final class SlaveMasterSession {

	private final ObjectInputStream reader;
	private final ObjectOutputStream writer;
	private final Context context;
	private final Logger logger;

	private SlaveMasterSession(ObjectInputStream reader, ObjectOutputStream writer, Context context, int number) {
		this.reader = reader;
		this.writer = writer;
		this.context = context;
		this.logger = LogManager.getLogger("SLAVE_" + number);
	}

	static Connected connect(SlaveConfigPreparer.SlaveConnectionSettings settings, Context context)
			throws IOException, InterruptedException {
		Socket socket = null;
		while (socket == null) {
			try {
				socket = new Socket(settings.hostname(), settings.port());
			} catch (ConnectException exception) {
				Thread.sleep(1000);
			}
		}
		ObjectInputStream reader = new ObjectInputStream(socket.getInputStream());
		ObjectOutputStream writer = new ObjectOutputStream(socket.getOutputStream());
		return initialize(reader, writer, context, settings.numberOfThreads());
	}

	static Connected initialize(ObjectInputStream reader, ObjectOutputStream writer, Context context,
			int numberOfThreads) throws IOException {
		int number = reader.readInt();
		Initialization initialization = new Initialization(number, reader.readInt(), reader.readInt(),
				reader.readDouble(), reader.readInt(), reader.readBoolean(), reader.readBoolean(),
				reader.readBoolean(), reader.readBoolean(), reader.readBoolean(), reader.readBoolean());
		SlaveMasterSession session = new SlaveMasterSession(reader, writer, context, number);
		session.writeMemoryStats();
		writer.writeInt(numberOfThreads);
		writer.flush();
		return new Connected(session, initialization);
	}

	Logger logger() {
		return logger;
	}

	boolean communicate() {
		SlaveCommunicationsLoop loop = new SlaveCommunicationsLoop(
				() -> (CommunicationsMode) reader.readObject(), acknowledger(), operations(), Thread::sleep,
				Runtime.getRuntime()::halt, logger);
		return loop.run();
	}

	private SlaveCommunicationsLoop.Acknowledger acknowledger() {
		return new SlaveCommunicationsLoop.Acknowledger() {
			@Override
			public void acknowledge() throws IOException {
				writer.writeBoolean(true);
				writer.flush();
			}

			@Override
			public void reset() throws IOException {
				writer.reset();
			}
		};
	}

	private SlaveCommunicationsLoop.Operations operations() {
		return new SlaveCommunicationsLoop.Operations() {
			public void distributePersons() throws IOException, ClassNotFoundException { receivePersons(); }
			public void transmitTravelTimes() throws IOException, ClassNotFoundException { receiveTravelTimes(); }
			public void poolPersons() throws IOException { sendPersons(); }
			public void transmitPlans() throws IOException { sendPlans(); }
			public void transmitSlaveStatus() throws IOException { writer.writeBoolean(context.ready()); }
			public void transmitScores() throws IOException { sendScores(); }
			public void transmitPerformance() throws IOException { sendPerformance(); }
		};
	}

	private void sendPlans() throws IOException {
		Map<String, PlanSerializable> plans = new HashMap<>();
		for (Person person : context.persons()) {
			PlanSerializable plan = new PlanSerializable(person.getSelectedPlan());
			plan.pSimScore = plan.getScore() == null ? 0 : plan.getScore();
			plans.put(person.getId().toString(), plan);
		}
		logger.warn("Sending " + plans.size() + " plans...");
		writer.writeInt(context.currentIteration());
		writer.writeInt(context.masterIteration());
		writer.writeObject(plans);
		logger.warn("Sending completed.");
	}

	private void receiveTravelTimes() throws IOException, ClassNotFoundException {
		logger.warn("RECEIVING travel times...");
		int masterIteration = reader.readInt();
		SerializableLinkTravelTimes travelTimes = (SerializableLinkTravelTimes) reader.readObject();
		TransitPerformance performance = null;
		if (context.transitEnabled() && context.fullTransitPerformance()) {
			performance = (TransitPerformance) reader.readObject();
		}
		context.updateTravelTimes(masterIteration, travelTimes, performance);
		logger.warn("RECEIVING travel times completed. Master at iteration number " + masterIteration);
	}

	private void sendPerformance() throws IOException {
		if (context.totalIterationTime() > 0) {
			logger.warn("Spent a total of " + context.totalIterationTime() + " running "
					+ context.executedPlanCount() + " person plans for " + context.iterationsPerCycle()
					+ " PSim iterations.");
		}
		writer.writeDouble(context.totalIterationTime());
		writer.writeInt(context.populationSize());
		writeMemoryStats();
	}

	private void receivePersons() throws IOException, ClassNotFoundException {
		int masterIteration = reader.readInt();
		List<PersonSerializable> persons = SerializedObjectReader.readList(reader);
		context.addPersons(persons, masterIteration);
		logger.warn("Received " + persons.size() + " persons. Master.currentIteration = " + masterIteration);
	}

	private void sendPersons() throws IOException {
		logger.warn("Load balancing...");
		int difference = reader.readInt();
		logger.warn("Received " + difference + " as lb instr from master");
		List<PersonSerializable> persons = difference > 0 ? context.takePersons(difference) : new ArrayList<>();
		writer.writeObject(persons);
		logger.warn("Sent " + persons.size() + " pax to master");
	}

	private void sendScores() throws IOException {
		writer.writeObject(new SlaveScoreStatsCalculator().calculateScoreStats(context.population()));
	}

	private void writeMemoryStats() throws IOException {
		writer.writeLong(context.memoryUse());
		writer.writeLong(context.maximumMemory());
		writer.writeInt(context.totalNumberOfPlans());
	}

	record Connected(SlaveMasterSession session, Initialization initialization) { }

	record Initialization(int number, int iterationsPerCycle, int numberOfPlans, double mutationRate,
			int lastIteration, boolean initialRouting, boolean quickReplanning, boolean fullTransitPerformance,
			boolean trackGenome, boolean intelligentRouters, boolean diversityGeneratingPlanSelection) { }

	interface Context {
		Iterable<? extends Person> persons();
		org.matsim.api.core.v01.population.Population population();
		int currentIteration();
		int masterIteration();
		boolean transitEnabled();
		boolean fullTransitPerformance();
		void updateTravelTimes(int masterIteration, SerializableLinkTravelTimes travelTimes,
				TransitPerformance performance);
		void addPersons(List<PersonSerializable> persons, int masterIteration);
		List<PersonSerializable> takePersons(int difference);
		boolean ready();
		double totalIterationTime();
		int executedPlanCount();
		int iterationsPerCycle();
		int populationSize();
		long memoryUse();
		long maximumMemory();
		int totalNumberOfPlans();
	}
}
