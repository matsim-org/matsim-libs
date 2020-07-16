package example.lsp.lspScoring;

import lsp.functions.LSPInfo;
import lsp.functions.LSPInfoFunction;

/*package-private*/ class TipInfo extends LSPInfo {

	private LSPInfoFunction function;
	private String name = "TIPINFO";

	/*package-private*/ TipInfo (LSPInfoFunction function) {
		this.function = function;
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public LSPInfoFunction getFunction() {
		return function;
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
