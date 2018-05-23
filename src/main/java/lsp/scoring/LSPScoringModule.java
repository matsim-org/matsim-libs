package lsp.scoring;

import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.listener.ScoringListener;

public interface LSPScoringModule extends ScoringListener{

	public void scoreLSPs(ScoringEvent event);
	
}
