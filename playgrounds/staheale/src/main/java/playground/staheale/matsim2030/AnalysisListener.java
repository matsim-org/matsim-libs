package playground.staheale.matsim2030;

import org.matsim.analysis.CalcLegTimes;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;

public class AnalysisListener implements StartupListener, IterationEndsListener {
	private CalcLegTimes legTimes;
	String filename = "legTimes.txt";
	
	public AnalysisListener () {
		this.legTimes = new CalcLegTimes();
	}

	@Override
	public void notifyStartup(StartupEvent event) { 
		event.getServices().getEvents().addHandler(legTimes);
		this.legTimes.writeStats(filename);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		this.legTimes.reset(event.getIteration());
	}
}
