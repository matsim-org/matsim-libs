package example.lspAndDemand.requirementsChecking;

import lsp.functions.*;

/*package-private*/ class BlueInfo extends LSPInfo {

	/*package-private*/ BlueInfo() {
//		blueInfoFunction = LSPInfoFunctionUtils.createDefaultInfoFunction();
//		LSPInfoFunctionValue<String> value = LSPInfoFunctionUtils.createInfoFunctionValue("blue" );
//		value.setValue("blue");
//		blueInfoFunction.getValues().add(value);
		this.getAttributes().putAttribute( "blue", null );
	}
	
	@Override
	public String getName() {
		return "blue";
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
