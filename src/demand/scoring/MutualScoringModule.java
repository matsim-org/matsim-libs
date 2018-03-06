package demand.scoring;

import org.matsim.core.controler.listener.ScoringListener;

public interface MutualScoringModule extends ScoringListener{
	
	public void scoreDemandObjects();
	public void scoreLSPs();
}
