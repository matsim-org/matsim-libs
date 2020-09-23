package example.lsp.simulationTrackers;

import lsp.functions.LSPInfoFunctionValue;

/*package-private*/ class FixedCostFunctionValue implements LSPInfoFunctionValue<Double> {

	private double value;

	@Override
	public String getName() {
		return "fixed";
	}

	@Override
	public Double getValue() {
		return value;
	}

	@Override
	public void setValue(Double value) {
		this.value = value;	
	}
		
}
