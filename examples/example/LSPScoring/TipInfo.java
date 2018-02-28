package example.LSPScoring;

import lsp.functions.Info;
import lsp.functions.InfoFunction;

public class TipInfo extends Info{

	private TipInfoFunction function;
	private String name = "TRINKGELDINFO";
	
	public TipInfo (TipInfoFunction function) {
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

}
