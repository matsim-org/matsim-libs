/*
 * Copyright (C) 2023 MOIA GmbH - All Rights Reserved
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.simulatedannealing;

import org.matsim.core.config.Config;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 *
 * Utility class that outputs iteration specific results.
 *
 * @author nkuehnel
 */
public final class SimulatedAnnealingAnalysis<T> implements IterationStartsListener {

	private final MatsimServices matsimServices;
	private boolean headerWritten = false;
	private final String runId;
	private final SimulatedAnnealing<T> simulatedAnnealing;
	private static final String notAvailableString = "NA";

	private final String delimiter;

	public SimulatedAnnealingAnalysis(Config config, MatsimServices matsimServices, SimulatedAnnealing<T> simulatedAnnealing) {
		this.matsimServices = matsimServices;
		this.runId = Optional.ofNullable(config.controller().getRunId()).orElse(notAvailableString);
		this.simulatedAnnealing = simulatedAnnealing;

		this.delimiter = config.global().getDefaultDelimiter();
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if(simulatedAnnealing.getCurrentState().isPresent()) {
			SimulatedAnnealing.SimulatedAnnealingIteration<T> lastIteration = simulatedAnnealing.getCurrentState().get();
			String summarizeAnnealing = new StringJoiner(delimiter)
					.add("" + lastIteration.accepted().getCost().orElse(Double.NaN))
					.add("" + lastIteration.current().getCost().orElse(Double.NaN))
					.add("" + lastIteration.best().getCost().orElse(Double.NaN))
					.add("" + lastIteration.temperature())
					.toString();
			writeIterationAnnealingStats(summarizeAnnealing, event.getIteration());
		}
	}


	/**
	 * @param it iteration
	 */
	private void writeIterationAnnealingStats(String summarizeAnnealing, int it) {
		try (var bw = getAppendingBufferedWriter("simulatedAnnealing_" + simulatedAnnealing.get().getClass().getSimpleName(), ".csv")) {
			if (!headerWritten) {
				headerWritten = true;
				bw.write(line("runId", "iteration", "acceptedCost", "currentCost", "bestCost", "temperature"));
			}
			bw.write(runId + delimiter + it + delimiter + summarizeAnnealing);
			bw.newLine();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	private String line(Object... cells) {
		return Arrays.stream(cells).map(Object::toString).collect(Collectors.joining(delimiter, "", "\n"));
	}

	private BufferedWriter getAppendingBufferedWriter(String prefix, String extension) {
		return IOUtils.getAppendingBufferedWriter(
				matsimServices.getControlerIO().getOutputFilename(prefix + "_" + extension));
	}
}

