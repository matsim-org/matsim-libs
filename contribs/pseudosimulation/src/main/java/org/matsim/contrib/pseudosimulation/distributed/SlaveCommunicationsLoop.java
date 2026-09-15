package org.matsim.contrib.pseudosimulation.distributed;

import java.io.IOException;

import org.apache.logging.log4j.Logger;

/** Executes one slave command batch, ending when the master sends {@link CommunicationsMode#CONTINUE}. */
final class SlaveCommunicationsLoop {

	private final CommandReader commandReader;
	private final Acknowledger acknowledger;
	private final Operations operations;
	private final Sleeper sleeper;
	private final ProcessTerminator processTerminator;
	private final Logger logger;

	SlaveCommunicationsLoop(CommandReader commandReader, Acknowledger acknowledger, Operations operations,
			Sleeper sleeper, ProcessTerminator processTerminator, Logger logger) {
		this.commandReader = commandReader;
		this.acknowledger = acknowledger;
		this.operations = operations;
		this.sleeper = sleeper;
		this.processTerminator = processTerminator;
		this.logger = logger;
	}

	boolean run() {
		CommunicationsMode communicationsMode = CommunicationsMode.WAIT;
		logger.warn("Initializing communications...");
		try {
			while (!communicationsMode.equals(CommunicationsMode.CONTINUE)) {
				communicationsMode = commandReader.read();
				dispatch(communicationsMode);
				acknowledger.acknowledge();
			}
			acknowledger.reset();
		} catch (ClassNotFoundException | IOException | InterruptedException e) {
			e.printStackTrace();
			logger.error("Something went wrong. Exiting.");
			return false;
		}
		logger.warn("Communications completed.");
		return true;
	}

	private void dispatch(CommunicationsMode communicationsMode)
			throws IOException, ClassNotFoundException, InterruptedException {
		switch (communicationsMode) {
			case TRANSMIT_SCENARIO, DISTRIBUTE_PERSONS:
				operations.distributePersons();
				break;
			case TRANSMIT_TRAVEL_TIMES:
				operations.transmitTravelTimes();
				break;
			case POOL_PERSONS:
				operations.poolPersons();
				break;
			case TRANSMIT_PLANS_TO_MASTER:
				operations.transmitPlans();
				operations.transmitSlaveStatus();
				break;
			case TRANSMIT_SCORES:
				operations.transmitScores();
				break;
			case TRANSMIT_PERFORMANCE:
				operations.transmitPerformance();
				break;
			case CONTINUE:
				break;
			case WAIT:
				sleeper.sleep(10);
				break;
			case DIE:
				logger.error("Got the kill signal from MASTER. Bye.");
				processTerminator.halt(0);
				break;
		}
	}

	@FunctionalInterface
	interface CommandReader {
		CommunicationsMode read() throws IOException, ClassNotFoundException;
	}

	interface Acknowledger {
		void acknowledge() throws IOException;

		void reset() throws IOException;
	}

	interface Operations {
		void distributePersons() throws IOException, ClassNotFoundException;

		void transmitTravelTimes() throws IOException, ClassNotFoundException;

		void poolPersons() throws IOException;

		void transmitPlans() throws IOException, ClassNotFoundException;

		void transmitSlaveStatus() throws IOException;

		void transmitScores() throws IOException;

		void transmitPerformance() throws IOException;
	}

	@FunctionalInterface
	interface Sleeper {
		void sleep(long milliseconds) throws InterruptedException;
	}

	@FunctionalInterface
	interface ProcessTerminator {
		void halt(int status);
	}
}
