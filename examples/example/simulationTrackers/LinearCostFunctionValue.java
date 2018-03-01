package example.simulationTrackers;

import lsp.functions.InfoFunctionValue;

public class LinearCostFunctionValue implements InfoFunctionValue {

	private String value;
	
	@Override
	public String getName() {
		return "linear";
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
