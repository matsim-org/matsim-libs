package example.lsp.lspScoring;

import lsp.LSPInfo;

/*package-private*/ class TipInfo extends LSPInfo {

	/*package-private*/ TipInfo() {
	}
	
	@Override
	public String getName() {
		String name = "TIPINFO";
		return name;
	}

	@Override
	public double getFromTime() {
		return 0;
	}

	@Override
	public double getToTime() {
		return Double.MAX_VALUE;
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
