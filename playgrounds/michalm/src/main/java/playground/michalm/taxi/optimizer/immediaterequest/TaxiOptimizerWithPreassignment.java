package playground.michalm.taxi.optimizer.immediaterequest;

import java.io.*;
import java.util.*;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.*;


/**
 * Works similarly to OTS
 * 
 * @author michalm
 */
public class TaxiOptimizerWithPreassignment
    extends ImmediateRequestTaxiOptimizer
{
    private Vehicle[] reqIdToVehMapping;


    public TaxiOptimizerWithPreassignment(VrpData data, final Vehicle[] reqIdToVehMapping)
    {
        super(data, true, false);
        this.reqIdToVehMapping = reqIdToVehMapping;
    }


    @Override
    protected VehicleDrive findBestVehicle(Request req, List<Vehicle> vehicles)
    {
        Vehicle veh = reqIdToVehMapping[req.getId()];
        return super.findBestVehicle(req, Arrays.asList(new Vehicle[] { veh }));
    }


    @Override
    protected boolean shouldOptimizeBeforeNextTask(Vehicle vehicle, boolean scheduleUpdated)
    {
        return false;
    }


    @Override
    protected boolean shouldOptimizeAfterNextTask(Vehicle vehicle, boolean scheduleUpdated)
    {
        return false;
    }


    public static TaxiOptimizerWithPreassignment createOptimizer(VrpData data,
            String reqIdToVehIdFile)
    {
        Scanner scanner = null;
        try {
            scanner = new Scanner(new File(reqIdToVehIdFile));
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        List<Vehicle> vehicles = data.getVehicles();
        Vehicle[] reqIdToVehMapping = new Vehicle[scanner.nextInt()];

        for (int i = 0; i < reqIdToVehMapping.length; i++) {
            reqIdToVehMapping[i] = vehicles.get(scanner.nextInt());
        }

        return new TaxiOptimizerWithPreassignment(data, reqIdToVehMapping);
    }
}
