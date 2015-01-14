package playground.dhosse.prt.optimizer;

import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import org.matsim.contrib.dvrp.data.Requests;

import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.optimizer.AbstractTaxiOptimizer;
import playground.michalm.taxi.optimizer.TaxiOptimizerConfiguration;

public class PrtMPOptimizer extends AbstractTaxiOptimizer{
	
	public PrtMPOptimizer(TaxiOptimizerConfiguration optimConfig) {
		
		super(optimConfig, new PriorityQueue<TaxiRequest>(100, Requests.T0_COMPARATOR));
		
	}

	protected void scheduleUnplannedRequests() {
		
		new MPProblem(optimConfig).scheduleUnplannedRequests((Queue<TaxiRequest>)unplannedRequests);
		
	}

}
