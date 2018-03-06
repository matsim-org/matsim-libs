package testMutualreplanningWithOfferUpdate;

import lsp.functions.InfoFunctionValue;

public class FixedCostFunctionValue implements InfoFunctionValue {

	private String value;
		
	@Override
	public String getName() {
		return "fixed";
	}

	@Override
	public Class<?> getDataType() {
		return Double.class;
	}

	@Override
	public String getValue() {
		return value;
	}

	@Override
	public void setValue(String value) {
		this.value = value;
	}

}
