package playground.dhosse.prt.optimizer;

import java.util.*;

import org.matsim.contrib.dvrp.data.Requests;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.optimizer.*;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;

public class PrtNPersonsOptimizer extends AbstractTaxiOptimizer{
	
	public PrtNPersonsOptimizer(TaxiOptimizerContext optimContext) {
		
		super(optimContext, null, new PriorityQueue<TaxiRequest>(100, Requests.T0_COMPARATOR), false, false);
		
	}
	
	@Override
    public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e)
    {
        if (isRequiresReoptimization()) {
            scheduleUnplannedRequests();
            if(this.getUnplannedRequests().size() < 1){
                setRequiresReoptimization(false);
            }
        }
    }

	protected void scheduleUnplannedRequests() {
		
		new NPersonsProblem(getOptimContext()).scheduleUnplannedRequests((Queue<TaxiRequest>)getUnplannedRequests());
		
	}

}
