package demand.scoring;

import java.util.Collection;

import org.matsim.core.controler.events.ScoringEvent;

import demand.decoratedLSP.LSPDecorator;
import demand.demandObject.DemandObject;
import demand.demandObject.DemandObjects;


public class MutualScoringModuleImpl implements MutualScoringModule{

	private Collection<DemandObject> demandObjects;
	private Collection<LSPDecorator> lsps;
	
	public MutualScoringModuleImpl(Collection<DemandObject> demandObjects, Collection<LSPDecorator> lsps) {
		this.demandObjects = demandObjects;
		this.lsps = lsps;
	}
	
	@Override
	public void notifyScoring(ScoringEvent arg0) {
		scoreDemandObjects();	
		scoreLSPs();
	}

	@Override
	public void scoreDemandObjects() {
		for(DemandObject demandObject : demandObjects) {
			if(demandObject.getScorer() != null) {
				demandObject.scoreSelectedPlan();	
			}	
		}
	}

	@Override
	public void scoreLSPs() {
		for(LSPDecorator lsp : lsps) {
			if(lsp.getScorer() != null) {
				lsp.scoreSelectedPlan();
			}
		}
		
	}
}
