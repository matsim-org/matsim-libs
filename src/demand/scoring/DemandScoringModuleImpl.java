package demand.scoring;

import java.util.Collection;

import org.matsim.core.controler.events.ScoringEvent;

import demand.demandObject.DemandObject;
import demand.demandObject.DemandObjects;


public class DemandScoringModuleImpl implements DemandScoringModule{

	private Collection<DemandObject> demandObjects;
	
	public DemandScoringModuleImpl(DemandObjects demandObjects) {
		this.demandObjects = demandObjects.getDemandObjects().values();	
	}
	
	@Override
	public void notifyScoring(ScoringEvent arg0) {
		scoreDemandObjects();	
	}

	@Override
	public void scoreDemandObjects() {
		for(DemandObject demandObject : demandObjects) {
			demandObject.scoreSelectedPlan();	
		}
	}
}
