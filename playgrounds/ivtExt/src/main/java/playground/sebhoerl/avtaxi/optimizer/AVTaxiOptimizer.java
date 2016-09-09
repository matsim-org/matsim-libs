package playground.sebhoerl.avtaxi.optimizer;

import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizer;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizerContext;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

public class AVTaxiOptimizer implements TaxiOptimizer {
	final private TaxiOptimizerContext context;
	final private LeastCostPathCalculator router;
	
	final private Vehicle vehicle;
	
	public AVTaxiOptimizer(TaxiOptimizerContext context) {
		this.context = context;
		
		router = new Dijkstra(context.network, context.travelDisutility, context.travelTime);
		vehicle = context.taxiData.getVehicles().values().iterator().next();
	}
	
	@Override
	public void nextLinkEntered(DriveTask driveTask) {}

	@Override
	public void requestSubmitted(Request request) {
		TaxiRequest req = (TaxiRequest) request;
		
		Path path = router.calcLeastCostPath(req.getFromLink().getToNode(), req.getToLink().getToNode(), req.getT0(), null, null);
		VrpPathWithTravelData vrpPath =
		
		vehicle.getSchedule().addTask(new DriveTaskImpl(vrpPath));
	}

	@Override
	public void nextTask(Schedule<? extends Task> schedule) {

	}

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e) {

	}
}
