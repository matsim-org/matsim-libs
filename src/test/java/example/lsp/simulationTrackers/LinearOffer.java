package example.lsp.simulationTrackers;

import demand.decoratedLSP.LSPDecorator;
import demand.decoratedLSP.LogisticsSolutionDecorator;
import demand.offer.Offer;
import demand.offer.OfferVisitor;
import lsp.functions.LSPInfo;

import java.util.Random;


public class LinearOffer implements Offer{

	private LSPDecorator  lsp;
	private LogisticsSolutionDecorator solution;
	private final String type;
	private double fix;
	private double linear;
	
	public LinearOffer(LogisticsSolutionDecorator solution) {
		this.lsp =  solution.getLSP();
		this.solution = solution;
		this.type = "linear";
		Random random = new Random(1);
		fix = random.nextDouble() * 10;
		linear = random.nextDouble() * 10;
	}
	
	@Override
	public LSPDecorator getLsp() {
		return lsp;
	}

	@Override
	public LogisticsSolutionDecorator getSolution() {
		return solution;
	}

	@Override
	public String getType() {
		return type;
	}

	public double getFix() {
		return fix;
	}

	public void setFix(double fix) {
		this.fix = fix;
	}

	public double getLinear() {
		return linear;
	}

	public void setLinear(double linear) {
		this.linear = linear;
	}

	@Override
	public void accept(OfferVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public void update() {
		for(LSPInfo info : solution.getInfos()) {
			if(info instanceof CostInfo ) {
				CostInfo costInfo = (CostInfo) info;
				this.fix = costInfo.getFixedCost();
				this.linear = costInfo.getVariableCost();
			}
		}
	}

	@Override
	public void setLSP(LSPDecorator lsp) {
		this.lsp = lsp;
	}

	@Override
	public void setSolution(LogisticsSolutionDecorator solution) {
		this.solution = solution;
	}

}
