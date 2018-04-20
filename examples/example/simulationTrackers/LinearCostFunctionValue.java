package example.simulationTrackers;

import lsp.functions.InfoFunctionValue;

public class LinearCostFunctionValue implements InfoFunctionValue<Double> {

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
