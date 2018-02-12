package cascadingInfoTest;

import lsp.functions.InfoFunctionValue;

public class AverageTimeInfoFunctionValue implements InfoFunctionValue{

	private String value;
	
	@Override
	public String getName() {
		return "agerageTimeInSeconds";
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
