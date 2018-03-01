package example.requirementsChecking;

import lsp.functions.Info;
import lsp.functions.InfoFunction;
import lsp.functions.InfoFunctionImpl;

public class RedInfo extends Info{

	private InfoFunctionImpl redInfoFunction;
	
	public RedInfo() {
		redInfoFunction = new InfoFunctionImpl();
		redInfoFunction.getValues().add(new RedInfoFunctionValue());
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

}
