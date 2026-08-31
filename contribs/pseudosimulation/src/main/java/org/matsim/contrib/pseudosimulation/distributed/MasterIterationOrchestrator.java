package org.matsim.contrib.pseudosimulation.distributed;

import java.util.function.BooleanSupplier;

/** Preserves the command and barrier ordering for the master MATSim lifecycle. */
final class MasterIterationOrchestrator {

	private final Operations operations;

	MasterIterationOrchestrator(Operations operations) {
		this.operations = operations;
	}

	void startup(boolean initialRouting, boolean parallel, BooleanSupplier loadBalanceRequired) {
		operations.start(CommunicationsMode.TRANSMIT_SCENARIO);
		if (!initialRouting) {
			return;
		}
		operations.waitForSlaves();
		operations.start(CommunicationsMode.TRANSMIT_PLANS_TO_MASTER);
		operations.waitForSlaves();
		operations.mergePlans();
		operations.applyInitialPlans();
		if (loadBalanceRequired.getAsBoolean()) {
			operations.loadBalance();
		}
		if (parallel) {
			operations.waitForSlaves();
			operations.start(CommunicationsMode.CONTINUE);
		}
	}

	void iterationStarts(int iteration, int innovationEndsAtIteration, boolean parallel) {
		if (innovationHasEnded(iteration, innovationEndsAtIteration)) {
			return;
		}
		operations.waitForSlaves();
		if (parallel) {
			operations.start(CommunicationsMode.TRANSMIT_PLANS_TO_MASTER);
		}
	}

	void afterMobsim(int iteration, int innovationEndsAtIteration, boolean parallel,
			BooleanSupplier loadBalanceRequired) {
		if (innovationHasEnded(iteration, innovationEndsAtIteration)) {
			return;
		}
		if (iteration == innovationEndsAtIteration) {
			operations.start(CommunicationsMode.DIE);
			return;
		}
		if (parallel) {
			operations.waitForSlaves();
			operations.mergePlans();
			operations.waitForSlaves();
			operations.start(CommunicationsMode.TRANSMIT_SCORES);
			operations.waitForSlaves();
		}
		if (loadBalanceRequired.getAsBoolean()) {
			operations.loadBalance();
		}
		operations.waitForSlaves();
		operations.updateTravelTimes();
		operations.start(CommunicationsMode.TRANSMIT_TRAVEL_TIMES);
		if (!parallel) {
			operations.waitForSlaves();
			operations.start(CommunicationsMode.TRANSMIT_PLANS_TO_MASTER);
			operations.waitForSlaves();
			operations.mergePlans();
			operations.waitForSlaves();
			operations.start(CommunicationsMode.TRANSMIT_SCORES);
			operations.waitForSlaves();
		}
	}

	void shutdown() {
		operations.stopRegistry();
		operations.start(CommunicationsMode.DIE);
	}

	private boolean innovationHasEnded(int iteration, int innovationEndsAtIteration) {
		return innovationEndsAtIteration > 0 && iteration > innovationEndsAtIteration;
	}

	interface Operations {
		void start(CommunicationsMode mode);
		void waitForSlaves();
		void mergePlans();
		void applyInitialPlans();
		void loadBalance();
		void updateTravelTimes();
		void stopRegistry();
	}
}
