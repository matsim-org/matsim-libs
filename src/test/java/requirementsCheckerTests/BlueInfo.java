package requirementsCheckerTests;

import lsp.functions.*;

public class BlueInfo extends Info{

	private InfoFunction blueInfoFunction;
	
	public BlueInfo() {
		blueInfoFunction = InfoFunctionUtils.createDefaultInfoFunction();
		InfoFunctionValue<String> value = InfoFunctionUtils.createInfoFunctionValue("blue" );
		value.setValue("blue");
		blueInfoFunction.getValues().add(value);
	}
	
	@Override
	public String getName() {
		return "blue";
	}

	@Override
	public InfoFunction getFunction() {
		return blueInfoFunction;
	}

	@Override
	public double getFromTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getToTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void update() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setName(String name) {
		// TODO Auto-generated method stub
		
	}

}
