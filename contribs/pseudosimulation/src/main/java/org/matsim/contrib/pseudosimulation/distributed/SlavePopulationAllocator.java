package org.matsim.contrib.pseudosimulation.distributed;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;

/** Allocates the master population between slaves using performance and memory observations. */
final class SlavePopulationAllocator {

	private final Logger logger;
	private final long bytesPerSlaveBuffer;
	private double planAllocationLimiter;

	SlavePopulationAllocator(Logger logger, double planAllocationLimiter, long bytesPerSlaveBuffer) {
		this.logger = logger;
		this.planAllocationLimiter = planAllocationLimiter;
		this.bytesPerSlaveBuffer = bytesPerSlaveBuffer;
	}

	int[] allocate(double[] totalIterationTime, int[] personsPerSlave,
			long[] maxMemory, long[] usedMemory, long bytesPerPlan,
			long bytesPerPerson, double dampeningFactor, int popSize) {
		int numSlaves = totalIterationTime.length;
		StringBuffer sb = new StringBuffer();
		sb.append("\n");
		sb.append(String.format("\t\t\t%20s:\t%20d\n", "bytesPerPlan", bytesPerPlan));
		sb.append(String.format("\t\t\t%20s:\t%20d\n", "bytesPerPerson", bytesPerPerson));
		String[] lines = {"slave", "totalIterationTime", "personsPerSlave", "maxMemory", "usedMemory"};
		sb.append("\n");
		for (int j = 0; j < 5; j++) {
			sb.append("\t\t\t" + String.format("%20s\t", lines[j]));
			for (int i = 0; i < numSlaves; i++) {
				switch (j) {
					case 0:
						sb.append(String.format("%20s\t", "slave_" + i));
						break;
					case 1:
						sb.append(String.format("%20.3f\t", totalIterationTime[i]));
						break;
					case 2:
						sb.append(String.format("%20d\t", personsPerSlave[i]));
						break;
					case 3:
						sb.append(String.format("%20d\t", maxMemory[i]));
						break;
					case 4:
						sb.append(String.format("%20d\t", usedMemory[i]));
						break;
				}
			}
			sb.append("\n");
		}
		logger.warn(sb.toString());

		Map<Integer, Integer> optimalNumberPerSlave = new HashMap<>();
		double[] timesPerPlan = new double[numSlaves];
		int[] allocation = new int[numSlaves];
		boolean[] fullyAllocated = new boolean[numSlaves];
		long[] overheadMemory = new long[numSlaves];

		Set<Integer> validSlaveIndices = new HashSet<>();
		Set<Integer> newSlaves = new HashSet<>();
		double fastestTimePerPlan = Double.POSITIVE_INFINITY;

		for (int i = 0; i < numSlaves; i++) {
			if (personsPerSlave[i] > 0 && totalIterationTime[i] > 0) {
				timesPerPlan[i] = (totalIterationTime[i] / personsPerSlave[i]);
				if (timesPerPlan[i] < fastestTimePerPlan)
					fastestTimePerPlan = timesPerPlan[i];
			} else
				newSlaves.add(i);
			validSlaveIndices.add(i);
			maxMemory[i] = maxMemory[i] - bytesPerSlaveBuffer;
			overheadMemory[i] = usedMemory[i] - (personsPerSlave[i] * bytesPerPerson);
		}
		fastestTimePerPlan = fastestTimePerPlan > 0 && !Double.valueOf(fastestTimePerPlan).equals(Double.POSITIVE_INFINITY) ? fastestTimePerPlan : 1;
		for (int i : newSlaves)
			timesPerPlan[i] = fastestTimePerPlan;
		int remainder = popSize;
		while (remainder > 0 && validSlaveIndices.size() > 0) {
			Set<Integer> valid = new HashSet<>();
			valid.addAll(validSlaveIndices);
			optimalNumberPerSlave.putAll(getOptimalNumbers(remainder, timesPerPlan, valid));
			for (int i : valid) {
				fullyAllocated[i] = false;
			}
			while (!isAllTrue(fullyAllocated)) {
				for (int i : valid) {
					if (fullyAllocated[i]) {
						continue;
					}
					long maxAvailMemory = (long) (maxMemory[i] - (planAllocationLimiter * (optimalNumberPerSlave.get(i) + allocation[i]) * bytesPerPlan));
					long memoryAvailableForPersons = maxAvailMemory - overheadMemory[i];
					int maxPersonAllocation = (int) (memoryAvailableForPersons / bytesPerPerson);

					if (optimalNumberPerSlave.get(i) > maxPersonAllocation) {
						optimalNumberPerSlave.put(i, optimalNumberPerSlave.get(i) - 1);
						validSlaveIndices.remove(i);
						continue;
					} else {
						if (optimalNumberPerSlave.get(i) > personsPerSlave[i] && optimalNumberPerSlave.get(i) > 10) {
							int dampenedNumber = (int) (((1 - dampeningFactor) * (double) optimalNumberPerSlave.get(i)) + (dampeningFactor * (double) Math.min(personsPerSlave[i], maxPersonAllocation)));
							optimalNumberPerSlave.put(i, dampenedNumber);
						}
					}
					remainder -= optimalNumberPerSlave.get(i);
					fullyAllocated[i] = true;
				}
			}
			for (int i : valid) {
				allocation[i] += optimalNumberPerSlave.get(i);
				if (allocation[i] <= 0) {
					logger.error("Something went wrong during loadBalancing (allocation <=0). Continuing as-is for now...");
					return personsPerSlave;
				}
			}
			if (validSlaveIndices.size() == 0 && remainder > 0) {
				logger.error("All slaves are nearing their maximum memory capacity!! Probably not a sustainable situation...");
				planAllocationLimiter--;
				for (int i = 0; i < numSlaves; i++) {
					validSlaveIndices.add(i);
				}
			}
		}

		sb = new StringBuffer();
		lines = new String[]{"slave", "time per plan", "pax per slave", "allocation", "memUSED_MB", "memAVAIL_MB"};
		sb.append("\n");
		for (int j = 0; j < 6; j++) {
			sb.append("\t\t\t" + String.format("%20s\t", lines[j]));
			for (int i = 0; i < numSlaves; i++) {
				switch (j) {
					case 0:
						sb.append(String.format("%20s\t", "slave_" + i));
						break;
					case 1:
						sb.append(String.format("%20.3f\t", timesPerPlan[i]));
						break;
					case 2:
						sb.append(String.format("%20d\t", personsPerSlave[i]));
						break;
					case 3:
						sb.append(String.format("%20d\t", allocation[i]));
						break;
					case 4:
						sb.append(String.format("%20d\t", usedMemory[i]));
						break;
					case 5:
						sb.append(String.format("%20d\t", maxMemory[i]));
						break;
				}
			}
			sb.append("\n");
		}
		logger.warn(sb.toString());
		return allocation;
	}

	double planAllocationLimiter() {
		return planAllocationLimiter;
	}

	private Map<Integer, Integer> getOptimalNumbers(int popSize, double[] timesPerPlan,
			Set<Integer> validSlaveIndices) {
		Map<Integer, Integer> output = new HashMap<>();
		double sumOfReciprocals = 0.0;
		for (int i : validSlaveIndices) {
			sumOfReciprocals += 1 / timesPerPlan[i];
		}
		int total = 0;
		for (int i : validSlaveIndices) {
			output.put(i, ((int) Math.ceil(popSize / timesPerPlan[i] / sumOfReciprocals)));
			total += output.get(i);
		}
		while (total > popSize) {
			for (int i : validSlaveIndices) {
				output.put(i, output.get(i) - 1);
				total--;
				if (total == popSize)
					break;
			}
		}
		return output;
	}

	private boolean isAllTrue(boolean[] fullyAllocated) {
		for (boolean b : fullyAllocated) {
			if (!b) {
				return false;
			}
		}
		return true;
	}
}
