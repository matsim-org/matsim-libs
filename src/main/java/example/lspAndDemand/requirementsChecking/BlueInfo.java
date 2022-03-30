package example.lspAndDemand.requirementsChecking;

import lsp.LSPInfo;

/*package-private*/ class BlueInfo extends LSPInfo {

	/*package-private*/ BlueInfo() {
		this.getAttributes().putAttribute( "blue", null );
	}
	
	@Override
	public String getName() {
		return "blue";
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
