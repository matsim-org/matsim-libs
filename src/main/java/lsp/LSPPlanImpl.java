
package lsp;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.population.BasicPlan;

/* package-private */ class LSPPlanImpl implements LSPPlan{

	private LSP lsp;
	private double score;
	private Collection<LogisticsSolution> solutions;
	private ShipmentAssigner assigner;
	
	LSPPlanImpl() {
		this.solutions = new ArrayList<>();
	}
	
	public void addSolution (LogisticsSolution solution) {
		this.solutions.add(solution);
		solution.setLSP(this.lsp);
	}
	
	public Collection<LogisticsSolution> getSolutions() {
		return solutions;
	}

	public ShipmentAssigner getAssigner() {
		return assigner;
	}
	
	public void setAssigner(ShipmentAssigner assigner) {
		this.assigner = assigner;
		this.assigner.setLSP(this.lsp);
	}

	@Override
	public Double getScore() {
		return score;
	}

	@Override
	public void setScore(Double score) {
		this.score = score;
	}

	public void setLSP(LSP lsp) {
		this.lsp = lsp;
		if(assigner != null) {
			this.assigner.setLSP(lsp);
		}
		for(LogisticsSolution solution : solutions) {
			solution.setLSP(lsp);
		}
	}
	
	public LSP getLsp() {
		return lsp;
	}
	
}
