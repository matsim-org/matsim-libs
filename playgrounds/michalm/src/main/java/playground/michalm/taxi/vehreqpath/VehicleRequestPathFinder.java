package playground.michalm.taxi.vehreqpath;

import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.router.*;
import org.matsim.contrib.dvrp.util.LinkTimePair;

import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.scheduler.TaxiScheduler;


public class VehicleRequestPathFinder
{
    private final VrpPathCalculator calculator;
    private final TaxiScheduler scheduler;


    public VehicleRequestPathFinder(VrpPathCalculator calculator, TaxiScheduler scheduler)
    {
        this.calculator = calculator;
        this.scheduler = scheduler;
    }


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
        LinkTimePair departure = scheduler.getImmediateDiversionOrEarliestIdleness(veh);
        return departure == null ? //
                null : calculator.calcPath(departure.link, req.getFromLink(), departure.time);
    }
}
