package playground.dhosse.prt.optimizer;

import java.util.*;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.LeastCostPathCalculator;

import playground.dhosse.prt.scheduler.PrtScheduler;
import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.optimizer.*;

public class NPersonsProblem {
	
	private final TaxiOptimizerContext optimConfig;
    private static Logger log = Logger.getLogger(NPersonsProblem.class);
    private final LeastCostPathCalculator router;
    private final BestDispatchFinder vrpFinder;


    public NPersonsProblem(TaxiOptimizerContext optimConfig)
    {
        this.optimConfig = optimConfig;
        router = new Dijkstra(optimConfig.context.getScenario().getNetwork(),
                optimConfig.travelDisutility, optimConfig.travelTime);
        
        vrpFinder = new BestDispatchFinder(optimConfig);
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
        	if(optimConfig.context.getVrpData().getVehicles().size()>0&&optimConfig.context.getTime() > 32000){
        		System.out.print("");
        	}
        	List<BestDispatchFinder.Dispatch> requests = new ArrayList<BestDispatchFinder.Dispatch>();
            TaxiRequest req = unplannedRequests.peek();

            BestDispatchFinder.Dispatch best = vrpFinder.findBestVehicleForRequest(req,
                    optimConfig.context.getVrpData().getVehicles().values());

            if (best == null) {
//            	log.info("No vrp found for request " + req.getId().toString() + " at " + 
//            Time.writeTime(optimConfig.context.getTime(), Time.TIMEFORMAT_HHMMSS));
                return;
            }
            
//            log.info("vrp found for request " + req.getId().toString() + " at " + 
//                    Time.writeTime(optimConfig.context.getTime(), Time.TIMEFORMAT_HHMMSS));
            
            requests.add(best);
            
            ((PrtScheduler)optimConfig.scheduler).scheduleRequests(best,requests);
            unplannedRequests.poll();
        }
    }
    
}
