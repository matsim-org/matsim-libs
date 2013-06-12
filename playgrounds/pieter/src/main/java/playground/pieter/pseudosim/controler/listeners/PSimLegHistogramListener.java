package playground.pieter.pseudosim.controler.listeners;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.corelisteners.LegHistogramListener;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;

public class PSimLegHistogramListener extends LegHistogramListener {

	public PSimLegHistogramListener(EventsManager events, boolean outputGraph) {
		super(events, outputGraph);
		
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if (!MobSimSwitcher.isQSimIteration) {
			super.notifyIterationStarts(event);
		}else{
			return;
		}
		
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		// TODO Auto-generated method stub
		if (!MobSimSwitcher.isQSimIteration) {
			super.notifyIterationEnds(event);
		}else{
			return;
		}
	}

}
