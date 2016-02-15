package playground.dhosse.prt.optimizer;

import java.util.PriorityQueue;
import java.util.Queue;

import org.matsim.contrib.dvrp.data.Requests;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;

import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.optimizer.AbstractTaxiOptimizer;
import playground.michalm.taxi.optimizer.TaxiOptimizerContext;

public class PrtNPersonsOptimizer extends AbstractTaxiOptimizer{
	
	public PrtNPersonsOptimizer(TaxiOptimizerContext optimConfig) {
		
		super(optimConfig, new PriorityQueue<TaxiRequest>(100, Requests.T0_COMPARATOR), false);
		
	}
	
	@Override
    public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e)
    {
        if (requiresReoptimization) {
            scheduleUnplannedRequests();
            if(this.unplannedRequests.size() < 1){
                requiresReoptimization = false;
            }
        }
    }

	protected void scheduleUnplannedRequests() {
		
		new NPersonsProblem(optimContext).scheduleUnplannedRequests((Queue<TaxiRequest>)unplannedRequests);
		
	}

}
