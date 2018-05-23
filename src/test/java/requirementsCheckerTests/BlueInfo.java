package requirementsCheckerTests;

import lsp.functions.Info;
import lsp.functions.InfoFunction;
import lsp.functions.InfoFunctionImpl;
import lsp.functions.InfoFunctionValue;
import lsp.functions.InfoFunctionValueImpl;

public class BlueInfo extends Info{

	private InfoFunctionImpl blueInfoFunction;
	
	public BlueInfo() {
		blueInfoFunction = new InfoFunctionImpl();
		InfoFunctionValue<String> value = new InfoFunctionValueImpl<String>("blue");
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
