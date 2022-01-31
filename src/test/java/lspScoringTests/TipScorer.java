package lspScoringTests;

import lsp.LSP;
import lsp.functions.LSPInfo;
import lsp.functions.LSPInfoFunction;
import lsp.functions.LSPInfoFunctionValue;
import lsp.scoring.LSPScorer;

public class TipScorer implements LSPScorer {

	private final TipSimulationTracker tracker;
	
	public TipScorer(LSP lsp, TipSimulationTracker tracker) {
		this.tracker = tracker;
	}
	
	@Override
	public double scoreCurrentPlan(LSP lsp) {
		double score = 0;
		for(LSPInfo info : tracker.getInfos()) {
			if(info.getName().equals("TIPINFO")) {
				LSPInfoFunction function = info.getFunction();
					for(LSPInfoFunctionValue<?> value : function.getValues()) {
						if(value.getName().equals("TIP IN EUR") && value.getValue() instanceof Double) {
							double tipValue = (Double)value.getValue();
							score += tipValue;
						}
					}
			}
		}
		return score;
	}

	@Override
	public void setLSP(LSP lsp) {
		// TODO Auto-generated method stub
		
	}

		
}
