
package lsp;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.population.BasicPlan;

/* package-private */ class LSPPlanImpl implements LSPPlan{

	private LSP lsp;
	private double score;
	private final Collection<LogisticsSolution> solutions;
	private ShipmentAssigner assigner;
	
	LSPPlanImpl() {
		this.solutions = new ArrayList<>();
	}
	
	@Override public LSPPlan addSolution( LogisticsSolution solution ) {
		this.solutions.add(solution);
		solution.setLSP(this.lsp);
		return this;
	}
	
	@Override public Collection<LogisticsSolution> getSolutions() {
		return solutions;
	}

	@Override public ShipmentAssigner getAssigner() {
		return assigner;
	}
	
	@Override public LSPPlan setAssigner( ShipmentAssigner assigner ) {
		this.assigner = assigner;
		this.assigner.setLSP(this.lsp);
		return this;
	}

	@Override
	public Double getScore() {
		return score;
	}

	@Override
	public void setScore(Double score) {
		this.score = score;
	}

	@Override public LSPPlan setLSP( LSP lsp ) {
		this.lsp = lsp;
		if(assigner != null) {
			this.assigner.setLSP(lsp);
		}
		for(LogisticsSolution solution : solutions) {
			solution.setLSP(lsp);
		}
		return this;
	}
	
	@Override public LSP getLsp() {
		return lsp;
	}
	
}
