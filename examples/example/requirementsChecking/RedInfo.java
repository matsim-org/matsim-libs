package example.requirementsChecking;

import lsp.functions.Info;
import lsp.functions.InfoFunction;
import lsp.functions.InfoFunctionImpl;
import lsp.functions.InfoFunctionValue;
import lsp.functions.InfoFunctionValueImpl;

public class RedInfo extends Info{

private InfoFunctionImpl redInfoFunction;
	
	public RedInfo() {
		redInfoFunction = new InfoFunctionImpl();
		InfoFunctionValue<String> value = new InfoFunctionValueImpl<String>("red");
		value.setValue("red");
		redInfoFunction.getValues().add(value);
	}

	@Override
	public String getName() {
		return "red";
	}

	@Override
	public InfoFunction getFunction() {
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
