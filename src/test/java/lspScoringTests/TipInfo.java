package lspScoringTests;

import lsp.functions.LSPInfo;
import lsp.functions.LSPInfoFunction;

public class TipInfo extends LSPInfo {

	private LSPInfoFunction function;
	private String name = "TIPINFO";
	
	public TipInfo (LSPInfoFunction function) {
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
