package playground.dhosse.prt.request;

import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.router.VrpPathCalculator;
import org.matsim.contrib.dvrp.router.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.Task.TaskStatus;
import org.matsim.contrib.dvrp.util.LinkTimePair;

import playground.dhosse.prt.scheduler.NPersonsPickupStayTask;
import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.schedule.TaxiTask;
import playground.michalm.taxi.schedule.TaxiTask.TaxiTaskType;
import playground.michalm.taxi.scheduler.TaxiScheduler;
import playground.michalm.taxi.vehreqpath.VehicleRequestPath;
import playground.michalm.taxi.vehreqpath.VehicleRequestPathCost;
import playground.michalm.taxi.vehreqpath.VehicleRequestPathFinder;

public class NPersonsVehicleRequestPathFinder extends VehicleRequestPathFinder {

	private final VrpPathCalculator calculator;
	private final TaxiScheduler scheduler;
	private int vehicleCapacity = 4;
	
	public NPersonsVehicleRequestPathFinder(VrpPathCalculator calculator,
			TaxiScheduler scheduler, int vehicleCapacity) {
		super(calculator, scheduler);
		this.calculator = calculator;
		this.scheduler = scheduler;
		this.vehicleCapacity = vehicleCapacity;
	}
	
	@Override
	public VehicleRequestPath findBestVehicleForRequest(TaxiRequest req,
            Iterable<? extends Vehicle> vehicles, VehicleRequestPathCost vrpCost)
    {
		
        VehicleRequestPath bestVrp = null;
        double bestCost = Double.MAX_VALUE;

        for (Vehicle veh : vehicles) {
        	
            VrpPathWithTravelData path = calculateVrpPath(veh, req);

            if (path == null) {
                continue;
            }

            VehicleRequestPath vrp = new VehicleRequestPath(veh, req, path);
            double cost = vrpCost.getCost(vrp);

            if (cost < bestCost) {
                bestVrp = vrp;
                bestCost = cost;
            }
        }

        return bestVrp;
        
    }


	@Override
    public VehicleRequestPath findBestRequestForVehicle(Vehicle veh,
            Iterable<TaxiRequest> unplannedRequests, VehicleRequestPathCost vrpCost)
    {
        VehicleRequestPath bestVrp = null;
        double bestCost = Double.MAX_VALUE;

        for (TaxiRequest req : unplannedRequests) {
            VrpPathWithTravelData path = calculateVrpPath(veh, req);

            if (path == null) {
                continue;
            }

            VehicleRequestPath vrp = new VehicleRequestPath(veh, req, path);
            double cost = vrpCost.getCost(vrp);

            if (cost < bestCost) {
                bestVrp = vrp;
                bestCost = cost;
            }
        }

        return bestVrp;
    }


    private VrpPathWithTravelData calculateVrpPath(Vehicle veh, TaxiRequest req)
    {
    	
    	TaxiTask lastTask = (TaxiTask) Schedules.getLastTask(veh.getSchedule());

    	LinkTimePair departure = null;
    	
    	if(lastTask.getTaxiTaskType().equals(TaxiTaskType.PICKUP)){
    		
    		NPersonsPickupStayTask task = (NPersonsPickupStayTask)lastTask;

    		if(task.getLink().equals(req.getFromLink()) && task.getRequest().getToLink().equals(req.getToLink()) &&
    				task.getRequests().size() < this.vehicleCapacity){
    			
    			double begin = task.getBeginTime();
        		
        		double t0 = req.getT0();
        		
        		if(t0 < begin && task.getStatus()!=TaskStatus.PERFORMED && task.getStatus() != TaskStatus.STARTED){
        			
        			departure = t0 >= veh.getT1() ? null : new LinkTimePair(req.getFromLink(), t0);
        			
        		}
    		}
    		
    	} else if(lastTask.getTaxiTaskType().equals(TaxiTask.TaxiTaskType.STAY)){
    		
    		if(lastTask.getStatus().equals(TaskStatus.STARTED)){
    			departure = scheduler.getEarliestIdleness(veh);
    		}
    			
    	}
    	
        return departure == null ? null : calculator.calcPath(departure.link, req.getFromLink(),
                departure.time);
        
    }

}
