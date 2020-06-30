package example.lsp.simulationTrackers;

import lsp.functions.InfoFunctionValue;

/*package-private*/ class LinearCostFunctionValue implements InfoFunctionValue<Double> {

	private Double value;

	@Override
	public String getName() {
		return "linear";
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
