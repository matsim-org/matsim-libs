package optdyts.algorithms;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import optdyts.DecisionVariable;
import optdyts.surrogatesolutions.SurrogateSolution;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class SearchStatisticsTracker<U extends DecisionVariable> {

	// -------------------- CONSTANTS --------------------

	private final String fileName;

	// -------------------- CONSTRUCTION AND INITIALIZATION --------------------

	public SearchStatisticsTracker(final String fileName) {
		this.fileName = fileName;

		try {
			final BufferedWriter writer = new BufferedWriter(new FileWriter(
					this.fileName, false));
			writer.write("equilibriumGap2\t");
			writer.write("maxEquilibriumGap2\t");
			writer.write("converged\t");
			writer.write("Q\t");
			writer.write("M\t");
			writer.write("u\t");
			writer.write("alpha\t");
			writer.write("...");
			writer.newLine();
			writer.flush();
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	// -------------------- SETTERS & GETTERS --------------------

	public String getFileName() {
		return this.fileName;
	}

	// -------------------- FILE WRITING --------------------

	public void writeToFile(final SurrogateSolution<?, U> surrogateSolution) {

		try {
			final BufferedWriter writer = new BufferedWriter(new FileWriter(
					this.fileName, true));

			writer.write(Double.toString(surrogateSolution
					.getEstimatedExpectedGap2()) + "\t");
			writer.write(Double.toString(surrogateSolution
					.getConvergenceNoiseVariance()) + "\t");
			writer.write(Boolean.toString(surrogateSolution.isConverged())
					+ "\t");
			writer.write(Double.toString(surrogateSolution
					.getInterpolatedObjectiveFunctionValue()) + "\t");
			writer.write(Integer.toString(surrogateSolution.size()) + "\t");
			for (U decisionVariable : surrogateSolution.getDecisionVariables()) {
				writer.write(decisionVariable + "\t");
				writer.write(surrogateSolution.getAlphaSum(decisionVariable)
						+ "\t");
			}
			writer.newLine();
			writer.flush();
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
