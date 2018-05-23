package demand.scoring;

import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.listener.ScoringListener;

public interface MutualScoringModule extends ScoringListener{
	
	public void scoreDemandObjects(ScoringEvent event);
	public void scoreLSPs(ScoringEvent event);
}
