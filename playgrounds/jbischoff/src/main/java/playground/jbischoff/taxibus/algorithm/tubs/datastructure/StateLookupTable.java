package playground.jbischoff.taxibus.algorithm.tubs.datastructure;

import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

public class StateLookupTable implements IterationEndsListener, StateSpace {

	
	
	StateLookupTable(double startTime, double endTime, double binSizeInSeconds, double maximumTourDuration){
		
		
	}
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public double getValue(double time, double slack) {
		
		return 0;
	}

	@Override
	public double getCurrentLastArrivalTime(double now) {
		// TODO Auto-generated method stub
		return 8*3600;
	}

}
