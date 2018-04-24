package testLSPWithCostTracker;

import lsp.functions.Info;
import lsp.functions.InfoFunction;

public class CostInfo extends Info {

	private CostInfoFunction costFunction;
	
	public CostInfo() {
		this.costFunction = new CostInfoFunction();
	}
	
	
	@Override
	public String getName() {
		return "cost_function";
	}

	@Override
	public InfoFunction getFunction() {
		return costFunction;
	}

	@Override
	public double getFromTime() {
		return 0;
	}

	@Override
	public double getToTime() {
		return Double.MAX_VALUE;
	}


	@Override
	public void update() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void setName(String name) {
		// TODO Auto-generated method stub
		
	}

}
