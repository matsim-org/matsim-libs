package playground.pieter.pseudosimulation.controler.listeners;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.corelisteners.LegHistogramListener;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;

/**
 * @author fouriep
 *         <P>
 *         Ideally, leg histograms should only be drawn during qsim iters
 */
class PSimLegHistogramListener extends LegHistogramListener {

	public PSimLegHistogramListener(EventsManager events, OutputDirectoryHierarchy controlerIO, boolean outputGraph) {
		super(events, controlerIO, outputGraph);
		
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if (!MobSimSwitcher.isQSimIteration) {
			super.notifyIterationStarts(event);
		}else{
        }
		
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		// TODO Auto-generated method stub
		if (!MobSimSwitcher.isQSimIteration) {
			super.notifyIterationEnds(event);
		}else{
        }
	}

}
