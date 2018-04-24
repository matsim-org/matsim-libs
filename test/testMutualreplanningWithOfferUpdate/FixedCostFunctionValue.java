package testMutualreplanningWithOfferUpdate;

import lsp.functions.InfoFunctionValue;

public class FixedCostFunctionValue implements InfoFunctionValue<Double> {

	private Double value;
		
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
