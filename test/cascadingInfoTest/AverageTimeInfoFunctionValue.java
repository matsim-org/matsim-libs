package cascadingInfoTest;

import lsp.functions.InfoFunctionValue;

public class AverageTimeInfoFunctionValue implements InfoFunctionValue<Double>{

	private Double value;
	
	@Override
	public String getName() {
		return "averageTimeInSeconds";
	}
	
	@Override
	public void setValue(Double value) {
		this.value = value;
	}

	@Override
	public Double getValue() {
		return value;
	}
}
