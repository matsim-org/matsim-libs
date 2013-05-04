package playground.jbischoff.taxi.optimizer;
import java.util.Arrays;
import java.util.List;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.Request;
import pl.poznan.put.vrp.dynamic.data.model.Vehicle;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.immediaterequest.TaxiOptimizationPolicy;
import playground.jbischoff.taxi.optimizer.BestVehicleFinder.BestVehicle;


public class NOSRankTaxiOptimizer extends RankTaxiOptimizer{

	    public static RankTaxiOptimizerFactory createFactory(final boolean straightLineDistance,
	            TaxiOptimizationPolicy policy)
	    {
	        return new AbstractRankTaxiOptimizerFactory(policy) {

	            @Override
	            public RankTaxiOptimizer create(VrpData data)
	            {
	                return new NOSRankTaxiOptimizer(data, straightLineDistance);
	            }
	        };
	    }


	    private final VrpData data;
	    private final BestIdleVehicleFinder idleVehicleFinder;
	    private final ClosestInTimeVehicleFinder bestVehicleFinder;

	    private int nextReqToServeIdx = 0;


	    public NOSRankTaxiOptimizer(final VrpData data, final boolean straightLineDistance)
	    {
	        super(data, null);

	        this.data = data;
	        idleVehicleFinder = new BestIdleVehicleFinder(data, straightLineDistance);
	        bestVehicleFinder =  new ClosestInTimeVehicleFinder(data);
	    }


	    @Override
	    public void optimize()
	    {
	        // if I knew the reason (i.e. new Request) then I could make a queue........
	        // this however requires refactoring of TaxiSimEngine and TaxiOptimizer.......

	        List<Request> reqs = data.getRequests();

	        if (nextReqToServeIdx == reqs.size()) {
	            return;
	        }

	        Request req = reqs.get(nextReqToServeIdx);
	        Vehicle veh = idleVehicleFinder.findBestVehicle(req);

	        if (veh == null) {
	            return;
	        }

	        BestVehicle bestVehicle = bestVehicleFinder.findBestVehicle(req, Arrays.asList(veh));
	        assignToBestVehicle(bestVehicle, req);

	        nextReqToServeIdx++;
	    }
	}


