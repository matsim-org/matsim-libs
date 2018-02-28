package demand.decoratedLSP;

import java.util.ArrayList;
import java.util.Collection;

import demand.offer.OfferTransferrer;
import demand.offer.OfferUpdater;
import lsp.LSP;
import lsp.LSPPlan;
import lsp.LogisticsSolution;
import lsp.ShipmentAssigner;

public class LSPPlanWithOfferTransferrer implements LSPPlanDecorator{

	private LSPDecorator lsp;
	private double score;
	private Collection<LogisticsSolution> solutions;
	private ShipmentAssigner assigner; 
	private OfferTransferrer transferrer;
	
	public LSPPlanWithOfferTransferrer() {
		this.solutions = new ArrayList<LogisticsSolution>();
		this.assigner = new DefaultAssigner(this.lsp);
	}
	
	@Override
	public void addSolution(LogisticsSolution solution) {
		try {
			LogisticsSolutionDecorator solutionDecorator = (LogisticsSolutionDecorator) solution;
			this.solutions.add(solutionDecorator);
			solution.setLSP(this.lsp);	
		}
		catch(ClassCastException e) {
			System.out.println("The class " + this.toString() + " expects an LogisticsSolutionDecorator and not any other implementation of LogisticsSolution");
			System.exit(1);
		}
	}

	@Override
	public Collection<LogisticsSolution> getSolutions() {
		return solutions;
	}

	@Override
	public ShipmentAssigner getAssigner() {
		return assigner;
	}

	@Override
	public void setAssigner(ShipmentAssigner assigner) {
		
	}

	@Override
	public void setLSP(LSP lsp) {
		try {
			this.lsp = (LSPDecorator) lsp;
		}
		catch(ClassCastException e) {
			System.out.println("The class " + this.toString() + " expects an LSPDecorator and not any other implementation of LSP");
			System.exit(1);
		}
	}

	@Override
	public LSP getLsp() {
		return lsp;
	}

	@Override
	public void setScore(Double score) {
		this.score = score;
	}

	@Override
	public Double getScore() {
		return score;
	}

	public OfferTransferrer getOfferTransferrer() {
		return transferrer;
	}

	public void setOfferTransferrer(OfferTransferrer offerTransferrer) {
		this.transferrer = offerTransferrer;
		this.transferrer.setLSP(lsp);
	}
	
}
