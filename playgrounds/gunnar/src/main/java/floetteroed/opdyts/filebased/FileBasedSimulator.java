package floetteroed.opdyts.filebased;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.List;

import floetteroed.opdyts.SimulatorState;
import floetteroed.opdyts.searchalgorithms.Simulator;
import floetteroed.opdyts.trajectorysampling.TrajectorySampler;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class FileBasedSimulator implements Simulator<FileBasedDecisionVariable> {

	// -------------------- CONSTANTS --------------------

	private final String advanceSimulationCommand;

	private final String newStateFileName;

	// -------------------- CONSTRUCTION --------------------

	public FileBasedSimulator(final String advanceSimulationCommand,
			final String newStateFileName) {
		this.advanceSimulationCommand = advanceSimulationCommand;
		this.newStateFileName = newStateFileName;
	}

	// -------------------- INTERNALS --------------------

	private void advanceSimulation() {
		final Process proc;
		final int exitVal;
		try {
			proc = Runtime.getRuntime().exec(this.advanceSimulationCommand);
			exitVal = proc.waitFor();
			if (exitVal != 0) {
				throw new RuntimeException(
						"Simulation terminated with exit code " + exitVal + ".");
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private FileBasedSimulatorState loadNewState() {
		final List<Double> numbers = new LinkedList<>();
		try {
			String line;
			final BufferedReader reader = new BufferedReader(new FileReader(
					this.newStateFileName));
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				numbers.add(Double.parseDouble(line));
			}
			reader.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return new FileBasedSimulatorState(numbers.get(0), new Vector(
				numbers.subList(1, numbers.size())));
	}

	// -------------------- IMPLEMENTATION OF Simulator --------------------

	@Override
	public SimulatorState run(
			TrajectorySampler<FileBasedDecisionVariable> evaluator) {
		return this.run(evaluator, null);
	}

	@Override
	public SimulatorState run(
			TrajectorySampler<FileBasedDecisionVariable> evaluator,
			SimulatorState initialState) {
		evaluator.initialize();
		FileBasedSimulatorState newState = null;
		while (!evaluator.foundSolution()) {
			this.advanceSimulation();
			newState = this.loadNewState();
			evaluator.afterIteration(newState);
		}
		return newState;
	}
}
