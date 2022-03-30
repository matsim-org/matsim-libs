package example.lsp.lspScoring;

import lsp.LSP;
import lsp.LSPInfo;
import lsp.scoring.LSPScorer;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.util.Map;

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
				Attributes function = info.getAttributes();
					for(  Map.Entry value : function.getAsMap().entrySet() ) {
						if(value.getKey().equals("TIP IN EUR") && value.getValue() instanceof Double) {
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
