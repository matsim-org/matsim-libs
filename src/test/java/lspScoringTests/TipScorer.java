package lspScoringTests;

import lsp.LSP;
import lsp.functions.Info;
import lsp.functions.InfoFunction;
import lsp.functions.InfoFunctionValue;
import lsp.scoring.LSPScorer;

public class TipScorer implements LSPScorer {

	private LSP lsp;
	private TipSimulationTracker tracker;
	
	public TipScorer(LSP lsp, TipSimulationTracker tracker) {
		this.lsp = lsp;
		this.tracker = tracker;
	}
	
	@Override
	public double scoreCurrentPlan(LSP lsp) {
		double score = 0;
		for(Info info : tracker.getInfos()) {
			if(info.getName() == "TIPINFO") {
				InfoFunction function = info.getFunction(); 
					for(InfoFunctionValue<?> value : function.getValues()) {
						if(value.getName() == "TIP IN EUR" && value.getValue() instanceof Double) {
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
