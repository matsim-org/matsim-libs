package playground.michalm.taxi.optimizer.fifo;

import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.router.VrpPathCalculator;

import playground.michalm.taxi.vehreqpath.VehicleRequestPathFinder;


public class TaxiOptimizerConfiguration
{
    public final MatsimVrpContext context;
    public final ImmediateRequestParams params;

    public final VrpPathCalculator calculator;
    public final TaxiScheduler scheduler;
    public final VehicleRequestPathFinder vrpFinder;


    public TaxiOptimizerConfiguration(MatsimVrpContext context, ImmediateRequestParams params,
            VrpPathCalculator calculator, TaxiScheduler scheduler,
            VehicleRequestPathFinder vrpFinder)
    {
        this.context = context;
        this.params = params;

        this.calculator = calculator;
        this.scheduler = scheduler;
        this.vrpFinder = vrpFinder;
    }
}
