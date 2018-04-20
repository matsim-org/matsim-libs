package lspScoringTests;

import lsp.functions.Info;
import lsp.functions.InfoFunction;

public class TipInfo extends Info{

	private InfoFunction function;
	private String name = "TIPINFO";
	
	public TipInfo (InfoFunction function) {
		this.function = function;
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public InfoFunction getFunction() {
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
