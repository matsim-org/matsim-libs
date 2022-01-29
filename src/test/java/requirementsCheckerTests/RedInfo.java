package requirementsCheckerTests;

import lsp.functions.*;

public class RedInfo extends LSPInfo {

	private final LSPInfoFunction redInfoFunction;
	
	public RedInfo() {
		redInfoFunction = LSPInfoFunctionUtils.createDefaultInfoFunction();
		LSPInfoFunctionValue<String> value = LSPInfoFunctionUtils.createInfoFunctionValue("red" );
		value.setValue("red");
		redInfoFunction.getValues().add(value);
	}

	@Override
	public String getName() {
		return "red";
	}

	@Override
	public LSPInfoFunction getFunction() {
		return redInfoFunction;
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
