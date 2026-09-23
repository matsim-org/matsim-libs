package org.matsim.contrib.pseudosimulation.distributed;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.Logger;

/** Coordinates a master load-balance cycle around the numeric allocation policy. */
final class MasterLoadBalancingCoordinator {

	private final Sessions sessions;
	private final Operations operations;
	private final TargetAllocator allocator;
	private final Logger logger;
	private final long scenarioMemoryUse;
	private final double dampeningFactor;
	private final List<PersonSerializable> personPool = new ArrayList<>();
	private long bytesPerPlan;
	private long bytesPerPerson;

	MasterLoadBalancingCoordinator(Sessions sessions, Operations operations, TargetAllocator allocator,
			Logger logger, long scenarioMemoryUse, long bytesPerPlan, long bytesPerPerson,
			double dampeningFactor) {
		this.sessions = sessions;
		this.operations = operations;
		this.allocator = allocator;
		this.logger = logger;
		this.scenarioMemoryUse = scenarioMemoryUse;
		this.bytesPerPlan = bytesPerPlan;
		this.bytesPerPerson = bytesPerPerson;
		this.dampeningFactor = dampeningFactor;
	}

	void balance() {
		operations.waitForSlaves();
		operations.admitReadySessions();
		if (sessions.size() < 2) {
			return;
		}
		operations.start(CommunicationsMode.TRANSMIT_PERFORMANCE);
		operations.waitForSlaves();
		updateMemoryEstimates();
		personPool.clear();
		logger.warn("About to start load balancing.");

		Set<Integer> valid = new TreeSet<>(sessions.ids());
		Set<Integer> invalid = new TreeSet<>();
		for (Participant participant : sessions.all()) {
			if (!participant.ready()) {
				valid.remove(participant.number());
				invalid.add(participant.number());
				participant.targetPopulationSize(0);
			}
		}

		double[] iterationTimes = new double[valid.size()];
		int[] populationSizes = new int[valid.size()];
		long[] usedMemory = new long[valid.size()];
		long[] maximumMemory = new long[valid.size()];
		int index = 0;
		for (int number : valid) {
			Participant participant = sessions.get(number);
			iterationTimes[index] = participant.totalIterationTime();
			populationSizes[index] = participant.currentPopulationSize();
			usedMemory[index] = participant.usedMemory();
			maximumMemory[index] = participant.maximumMemory();
			index++;
		}

		int[] targets = allocator.allocate(iterationTimes, populationSizes, maximumMemory, usedMemory,
				bytesPerPlan, bytesPerPerson, dampeningFactor, operations.populationSize());
		index = 0;
		for (int number : valid) {
			sessions.get(number).targetPopulationSize(targets[index++]);
		}

		operations.start(CommunicationsMode.POOL_PERSONS);
		operations.waitForSlaves();
		for (Participant participant : sessions.all()) {
			personPool.addAll(participant.persons());
		}
		logger.warn("Distributing persons between  slaveHandlerTreeMap");
		for (int number : invalid) {
			operations.terminate(number);
		}
		operations.start(CommunicationsMode.DISTRIBUTE_PERSONS);
	}

	synchronized List<PersonSerializable> takePersons(int difference) {
		List<PersonSerializable> persons = new ArrayList<>();
		if (difference < 0) {
			for (int i = 0; i > difference; i--) {
				persons.add(personPool.remove(0));
			}
		}
		return persons;
	}

	long bytesPerPlan() {
		return bytesPerPlan;
	}

	long bytesPerPerson() {
		return bytesPerPerson;
	}

	private void updateMemoryEstimates() {
		int plans = 0;
		long populationMemory = 0;
		for (Participant participant : sessions.all()) {
			plans += participant.numberOfPlans();
			populationMemory += participant.usedMemory() - scenarioMemoryUse;
		}
		if (plans > 0) {
			bytesPerPlan = populationMemory / plans;
			bytesPerPerson = populationMemory / operations.populationSize();
		}
	}

	interface Participant {
		int number();
		boolean ready();
		int numberOfPlans();
		double totalIterationTime();
		int currentPopulationSize();
		long usedMemory();
		long maximumMemory();
		Collection<? extends PersonSerializable> persons();
		void targetPopulationSize(int value);
	}

	interface Sessions {
		int size();
		Set<Integer> ids();
		Collection<? extends Participant> all();
		Participant get(int number);
	}

	interface Operations {
		void waitForSlaves();
		void admitReadySessions();
		void start(CommunicationsMode mode);
		int populationSize();
		void terminate(int number);
	}

	interface TargetAllocator {
		int[] allocate(double[] iterationTimes, int[] populationSizes, long[] maximumMemory,
				long[] usedMemory, long bytesPerPlan, long bytesPerPerson, double dampeningFactor,
				int populationSize);
	}
}
