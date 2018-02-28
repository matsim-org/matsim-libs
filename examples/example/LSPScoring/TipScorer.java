package example.LSPScoring;

import org.matsim.api.core.v01.population.HasPlansAndId;

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
			if(info.getName() == "TRINKGELDINFO") {
				InfoFunction function = info.getFunction(); 
					for(InfoFunctionValue value : function.getValues()) {
						if(value.getName() == "TRINKGELD IN EUR") {
							double trinkgeldValue = Double.parseDouble(value.getValue());
							score += trinkgeldValue;
						}
					}
			}
		}
		return score;
	}

		
}
