package optdyts.logging;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import optdyts.DecisionVariable;
import optdyts.SimulatorState;
import optdyts.surrogatesolutions.SurrogateSolution;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class SearchStatisticsWriter<X extends SimulatorState, U extends DecisionVariable> {

	// -------------------- CONSTANTS --------------------

	private final String fileName;

	private final List<SearchStatistic<X, U>> statistics = new ArrayList<SearchStatistic<X, U>>();

	// -------------------- CONSTRUCTION AND INITIALIZATION --------------------

	public SearchStatisticsWriter(final String fileName) {
		this.fileName = fileName;
	}

	// -------------------- SETTERS & GETTERS --------------------

	public void addStandards() {
		this.statistics.add(new AbsoluteEquilibriumGap<X, U>());
		this.statistics.add(new MaximumAbsoluteEquilibriumGap<X, U>());
		this.statistics.add(new IsConverged<X, U>());
		this.statistics.add(new EquivalentAveragingSolutions<X, U>());
		this.statistics.add(new InterpolatedObjectiveFunctionValue<X, U>());
		this.statistics.add(new SolutionSize<X, U>());
		this.statistics.add(new AllSolutions<X, U>("\t"));
	}

	public void addSearchStatistic(final SearchStatistic<X, U> statistic) {
		this.statistics.add(statistic);
	}

	public String getFileName() {
		return this.fileName;
	}

	// -------------------- FILE WRITING --------------------

	private boolean wroteHeader = false;

	public void writeToFile(final SurrogateSolution<X, U> surrogateSolution) {

		try {
			final BufferedWriter writer;
			if (!this.wroteHeader) {
				writer = new BufferedWriter(
						new FileWriter(this.fileName, false));
				for (SearchStatistic<X, U> stat : this.statistics) {
					writer.write(stat.label() + "\t");
				}
				writer.newLine();
				this.wroteHeader = true;
			} else {
				writer = new BufferedWriter(new FileWriter(this.fileName, true));
			}
			for (SearchStatistic<X, U> stat : this.statistics) {
				writer.write(stat.value(surrogateSolution) + "\t");
			}
			writer.newLine();
			writer.flush();
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
