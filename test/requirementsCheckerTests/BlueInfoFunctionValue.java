package requirementsCheckerTests;

import java.awt.Color;

import lsp.functions.InfoFunctionValue;

public class BlueInfoFunctionValue implements InfoFunctionValue{

	@Override
	public String getName() {
		return "blue";
	}

	@Override
	public Class<?> getDataType() {
		return Color.class;
	}

	@Override
	public String getValue() {
		return "blue";
	}

	@Override
	public void setValue(String value) {
		// TODO Auto-generated method stub
		
	}

}
