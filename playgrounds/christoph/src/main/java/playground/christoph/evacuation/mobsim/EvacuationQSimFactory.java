package playground.christoph.evacuation.mobsim;

import org.matsim.core.router.util.TravelTime;

import playground.christoph.multimodal.mobsim.MultiModalMobsimFactory;
import playground.christoph.withinday.mobsim.WithinDayQSimFactory;

public class EvacuationQSimFactory extends MultiModalMobsimFactory {

	/*
	 * Override the QSimFactory of the MultiModalMobsimFactory.
	 * Should not be necessary anymore if the Within Day Modules are
	 * moved to the core...
	 */
	public EvacuationQSimFactory(TravelTime travelTime) {
		super(travelTime);
		
		super.mobSimFactory = new WithinDayQSimFactory();
		super.parallelMobSimFactory = new WithinDayQSimFactory();
	}
}
