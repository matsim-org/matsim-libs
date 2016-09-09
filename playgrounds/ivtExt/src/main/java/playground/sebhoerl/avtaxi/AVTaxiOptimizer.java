package playground.sebhoerl.avtaxi;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.optimizer.AbstractTaxiOptimizer;
import org.matsim.contrib.taxi.optimizer.AbstractTaxiOptimizerParams;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizerContext;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.LeastCostPathCalculator;

public class AVTaxiOptimizer extends AbstractTaxiOptimizer {
	final private Queue<Vehicle> availableVehicles = new LinkedList<>();
	final private TaxiOptimizerContext context;
	final private LeastCostPathCalculator pathCalculator;

	public AVTaxiOptimizer(TaxiOptimizerContext optimContext, AbstractTaxiOptimizerParams params) {
		super(optimContext, params, new LinkedList<TaxiRequest>(), true);
		this.context = optimContext;optimContext.
		
		this.pathCalculator = new Dijkstra(optimContext.network, optimContext.travelDisutility, optimContext.travelTime);
	}

	@Override
	protected void scheduleUnplannedRequests() {
		Queue<TaxiRequest> requests = (Queue<TaxiRequest>) unplannedRequests;
		
		while (!requests.isEmpty() && availableVehicles.size() > 0) {
			TaxiRequest request = requests.poll();
			Vehicle vehicle = availableVehicles.poll();
			
			Path path = pathCalculator.calcLeastCostPath(fromNode, toNode, starttime, person, vehicle)
			
			VrpPathWithTravelData path = new VrpPathWithTravelDataImpl(
					optimContext.timer.getTimeOfDay(),
					);
			
			pathCalculator.calcLeastCostPath(fromNode, toNode, starttime, person, vehicle)
			
			context.scheduler.scheduleRequest(vehicle, request, vrpPath);
		}
	}
}
