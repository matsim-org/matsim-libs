package playground.michalm.taxi.vehreqpath;

import java.util.Comparator;

import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.router.*;
import org.matsim.contrib.dvrp.util.LinkTimePair;

import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.scheduler.TaxiScheduler;

import com.google.common.base.Function;
import com.google.common.collect.*;


public class VehicleRequestPathFinder
{
    private final VrpPathCalculator calculator;
    private final TaxiScheduler scheduler;


    public VehicleRequestPathFinder(VrpPathCalculator calculator, TaxiScheduler scheduler)
    {
        this.calculator = calculator;
        this.scheduler = scheduler;
    }


    public Function<Vehicle, VehicleRequestPath> vehToVRPathTransformation(final TaxiRequest req)
    {
        return new Function<Vehicle, VehicleRequestPath>() {
            public VehicleRequestPath apply(Vehicle veh)
            {
                VrpPathWithTravelData path = calculateVrpPath(veh, req);
                return path == null ? null : new VehicleRequestPath(veh, req, path);
            }
        };
    }


    public Function<TaxiRequest, VehicleRequestPath> reqToVRPathTransformation(final Vehicle veh)
    {
        return new Function<TaxiRequest, VehicleRequestPath>() {
            public VehicleRequestPath apply(TaxiRequest req)
            {
                VrpPathWithTravelData path = calculateVrpPath(veh, req);
                return path == null ? null : new VehicleRequestPath(veh, req, path);
            }
        };
    }


    public VehicleRequestPath findBestVehicleForRequest(TaxiRequest req,
            Iterable<Vehicle> vehicles, Comparator<VehicleRequestPath> vrpComparator)
    {
        if (Iterables.isEmpty(vehicles)) {
            return null;
        }
        
        Function<Vehicle, VehicleRequestPath> transformation = vehToVRPathTransformation(req);
        Iterable<VehicleRequestPath> vrps = Iterables.transform(vehicles, transformation);
        
        return Ordering.from(vrpComparator).nullsLast().min(vrps);
    }


    public VehicleRequestPath findBestRequestForVehicle(Vehicle veh,
            Iterable<TaxiRequest> unplannedRequests, Comparator<VehicleRequestPath> vrpComparator)
    {
        if (Iterables.isEmpty(unplannedRequests)) {
            return null;
        }
        
        Function<TaxiRequest, VehicleRequestPath> transformation = reqToVRPathTransformation(veh);
        Iterable<VehicleRequestPath> vrps = Iterables.transform(unplannedRequests, transformation);

        return Ordering.from(vrpComparator).nullsLast().min(vrps);
    }


    public VrpPathWithTravelData calculateVrpPath(Vehicle veh, TaxiRequest req)
    {
        LinkTimePair departure = scheduler.getEarliestIdleness(veh);
        return departure == null ? null : calculator.calcPath(departure.link, req.getFromLink(),
                departure.time);
    }
}
