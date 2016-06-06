package playground.dhosse.prt;

import java.util.Collection;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.passenger.*;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.contrib.dvrp.vrpagent.*;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs.LegCreator;
import org.matsim.contrib.taxi.data.TaxiData;
import org.matsim.contrib.taxi.optimizer.*;
import org.matsim.contrib.taxi.passenger.TaxiRequestCreator;
import org.matsim.contrib.taxi.run.*;
import org.matsim.contrib.taxi.scheduler.*;
import org.matsim.contrib.taxi.vrpagent.TaxiActionCreator;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.*;
import org.matsim.core.router.util.*;

import com.google.inject.*;
import com.google.inject.name.Named;

import playground.dhosse.prt.launch.NPersonsActionCreator;
import playground.dhosse.prt.optimizer.PrtOptimizerContext;
import playground.dhosse.prt.passenger.PrtRequestCreator;
import playground.dhosse.prt.scheduler.PrtScheduler;


public class PrtQSimProvider
    implements Provider<Mobsim>
{
    private final MatsimServices matsimServices;
    private final Collection<AbstractQSimPlugin> plugins;

    private final TaxiData taxiData;
    private final TravelTime travelTime;

    private final TaxiConfigGroup taxiCfg;
    private final PrtConfig prtConfig;
    private final TaxiOptimizerFactory optimizerFactory;


    @Inject
    public PrtQSimProvider(MatsimServices matsimServices,
            Collection<AbstractQSimPlugin> plugins, TaxiData taxiData,
            @Named(VrpTravelTimeModules.DVRP_ESTIMATED) TravelTime travelTime, TaxiConfigGroup taxiCfg,
            TaxiOptimizerFactory optimizerFactory)
    {
        this.matsimServices = matsimServices;
        this.plugins = plugins;
        this.taxiData = taxiData;
        this.travelTime = travelTime;
        this.taxiCfg = taxiCfg;
        this.optimizerFactory = optimizerFactory;
        prtConfig = new PrtConfig(taxiCfg);
    }


    @Override
    public Mobsim get()
    {
        if (taxiCfg.isVehicleDiversion() && taxiCfg.isOnlineVehicleTracker()) {
            throw new IllegalStateException("Diversion requires online tracking");
        }

        QSim qSim = QSimUtils.createQSim(matsimServices.getScenario(), matsimServices.getEvents(), plugins);

        TaxiOptimizer optimizer = createTaxiOptimizer(qSim);
        qSim.addQueueSimulationListeners(optimizer);

        PassengerEngine passengerEngine = new PassengerEngine(PrtRequestCreator.MODE, matsimServices.getEvents(),
                new TaxiRequestCreator(), optimizer, taxiData, matsimServices.getScenario().getNetwork());
        qSim.addMobsimEngine(passengerEngine);
        qSim.addDepartureHandler(passengerEngine);

        LegCreator legCreator = taxiCfg.isOnlineVehicleTracker() ? //
                VrpLegs.createLegWithOnlineTrackerCreator(optimizer, qSim.getSimTimer()) : //
                VrpLegs.createLegWithOfflineTrackerCreator(qSim.getSimTimer());

        TaxiActionCreator actionCreator = prtConfig.getVehicleCapacity() > 1
                ? new NPersonsActionCreator(passengerEngine, legCreator,
                        taxiCfg.getPickupDuration())
                : new TaxiActionCreator(passengerEngine, legCreator, taxiCfg.getPickupDuration());
        qSim.addAgentSource(new VrpAgentSource(actionCreator, taxiData, optimizer, qSim));

        return qSim;

    }


    private TaxiOptimizer createTaxiOptimizer(QSim qSim)
    {
        TaxiSchedulerParams schedulerParams = new TaxiSchedulerParams(taxiCfg);
        TravelDisutility travelDisutility = new TimeAsTravelDisutility(travelTime);

        if (prtConfig.getVehicleCapacity() > 1) {
            PrtScheduler scheduler = new PrtScheduler(matsimServices.getScenario(), taxiData, qSim.getSimTimer(),
                    schedulerParams, travelTime, travelDisutility);
            TaxiOptimizerContext optimContext = new PrtOptimizerContext(taxiData, matsimServices,
                    qSim.getSimTimer(), travelTime, travelDisutility, scheduler, prtConfig);
            
            return optimizerFactory.createTaxiOptimizer(optimContext,
                    taxiCfg.getOptimizerConfigGroup());
        }
        else {
            TaxiScheduler scheduler = new TaxiScheduler(matsimServices.getScenario(), taxiData, qSim.getSimTimer(),
                    schedulerParams, travelTime, travelDisutility);

            TaxiOptimizerContext optimContext = new TaxiOptimizerContext(taxiData, matsimServices,
                    qSim.getSimTimer(), travelTime, travelDisutility, scheduler);
            return optimizerFactory.createTaxiOptimizer(optimContext,
                    taxiCfg.getOptimizerConfigGroup());
        }
    }

}
