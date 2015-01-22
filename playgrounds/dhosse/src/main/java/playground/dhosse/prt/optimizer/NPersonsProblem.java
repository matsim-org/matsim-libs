package playground.dhosse.prt.optimizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;

import playground.dhosse.prt.scheduler.PrtScheduler;
import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.optimizer.TaxiOptimizerConfiguration;
import playground.michalm.taxi.vehreqpath.VehicleRequestPath;
import playground.michalm.taxi.vehreqpath.VehicleRequestPathCost;

public class NPersonsProblem {
	
	private final TaxiOptimizerConfiguration optimConfig;
    private final VehicleRequestPathCost vrpComparator;


    public NPersonsProblem(TaxiOptimizerConfiguration optimConfig)
    {
        this.optimConfig = optimConfig;
        this.vrpComparator = optimConfig.getVrpCost();
    }


    protected class RequestParams{
		
		final Link fromLink;
		final Link toLink;
		final Vehicle vehicle;
		
		public RequestParams(Link fromLink, Link toLink, Vehicle vehicle){
			this.fromLink = fromLink;
			this.toLink = toLink;
			this.vehicle = vehicle;
		}
		
	}
    
    public void scheduleUnplannedRequests(Queue<TaxiRequest> unplannedRequests)
    {
        while (!unplannedRequests.isEmpty()) {
        	List<VehicleRequestPath> requests = new ArrayList<VehicleRequestPath>();
            TaxiRequest req = unplannedRequests.peek();

            VehicleRequestPath best = optimConfig.vrpFinder.findBestVehicleForRequest(req,
                    optimConfig.context.getVrpData().getVehicles(), vrpComparator);

            if (best == null) {
                return;
            }
            
            requests.add(best);

            ((PrtScheduler)optimConfig.scheduler).scheduleRequests(best,requests);
            unplannedRequests.poll();
        }
    }
    
}
