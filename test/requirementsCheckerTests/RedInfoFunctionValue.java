package requirementsCheckerTests;

import java.awt.Color;

import lsp.functions.InfoFunctionValue;

public class RedInfoFunctionValue implements InfoFunctionValue{

	@Override
	public String getName() {
		return "red";
	}

	@Override
	public Class<?> getDataType() {
		return Color.class;
	}

	@Override
	public String getValue() {
		return "red";
	}

	@Override
	public void setValue(String value) {
		// TODO Auto-generated method stub
		
	}

}
