package playground.michalm.taxi.optimizer.fifo;

import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.router.VrpPathCalculator;

import playground.michalm.taxi.scheduler.TaxiScheduler;
import playground.michalm.taxi.vehreqpath.VehicleRequestPathFinder;


public class TaxiOptimizerConfiguration
{
    public final MatsimVrpContext context;

    public final VrpPathCalculator calculator;
    public final TaxiScheduler scheduler;
    public final VehicleRequestPathFinder vrpFinder;

    public final Boolean minimizePickupTripTime;


    public TaxiOptimizerConfiguration(MatsimVrpContext context, VrpPathCalculator calculator,
            TaxiScheduler scheduler, VehicleRequestPathFinder vrpFinder,
            Boolean minimizePickupTripTime)
    {
        this.context = context;

        this.calculator = calculator;
        this.scheduler = scheduler;
        this.vrpFinder = vrpFinder;

        this.minimizePickupTripTime = minimizePickupTripTime;
    }
}
