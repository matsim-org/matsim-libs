package example.lsp.lspScoring;

import lsp.LSP;
import lsp.functions.LSPInfo;
import lsp.functions.LSPInfoFunction;
import lsp.functions.LSPInfoFunctionValue;
import lsp.scoring.LSPScorer;

/*package-private*/ class TipScorer implements LSPScorer {

	private final TipSimulationTracker tracker;

	/*package-private*/ TipScorer(LSP lsp, TipSimulationTracker tracker) {
		this.tracker = tracker;
	}
	
	@Override
	public double scoreCurrentPlan(LSP lsp) {
		double score = 0;
		for(LSPInfo info : tracker.getInfos()) {
			if(info instanceof TipInfo) {
				LSPInfoFunction function = info.getFunction();
					for(LSPInfoFunctionValue value : function.getValues()) {
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
