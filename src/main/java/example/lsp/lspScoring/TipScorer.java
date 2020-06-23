package example.lsp.lspScoring;

import lsp.LSP;
import lsp.functions.Info;
import lsp.functions.InfoFunction;
import lsp.functions.InfoFunctionValue;
import lsp.scoring.LSPScorer;

/*package-private*/ class TipScorer implements LSPScorer {

	private LSP lsp;
	private TipSimulationTracker tracker;

	/*package-private*/ TipScorer(LSP lsp, TipSimulationTracker tracker) {
		this.lsp = lsp;
		this.tracker = tracker;
	}
	
	@Override
	public double scoreCurrentPlan(LSP lsp) {
		double score = 0;
		for(Info info : tracker.getInfos()) {
			if(info instanceof TipInfo) {
				InfoFunction function = info.getFunction(); 
					for(InfoFunctionValue value : function.getValues()) {
						if(value.getName() == "TIP IN EUR" && value.getValue() instanceof Double) {
							double trinkgeldValue = (Double) value.getValue();
							score += trinkgeldValue;
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
