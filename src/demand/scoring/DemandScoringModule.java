package demand.scoring;

import org.matsim.core.controler.listener.ScoringListener;

public interface DemandScoringModule extends ScoringListener{
	
	public void scoreDemandObjects();
}
