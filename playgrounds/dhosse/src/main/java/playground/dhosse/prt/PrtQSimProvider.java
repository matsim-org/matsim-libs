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
    private final Scenario scenario;
    private final EventsManager events;
    private final Collection<AbstractQSimPlugin> plugins;

    private final TaxiData taxiData;
    private final TravelTime travelTime;

    private final TaxiConfigGroup taxiCfg;
    private final PrtConfig prtConfig;
    private final TaxiOptimizerFactory optimizerFactory;


    @Inject
    public PrtQSimProvider(Scenario scenario, EventsManager events,
            Collection<AbstractQSimPlugin> plugins, TaxiData taxiData,
            @Named(VrpTravelTimeModules.DVRP) TravelTime travelTime, TaxiConfigGroup taxiCfg,
            TaxiOptimizerFactory optimizerFactory)
    {
        this.scenario = scenario;
        this.events = events;
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

        QSim qSim = QSimUtils.createQSim(scenario, events, plugins);

        TaxiOptimizer optimizer = createTaxiOptimizer(qSim);
        qSim.addQueueSimulationListeners(optimizer);

        PassengerEngine passengerEngine = new PassengerEngine(PrtRequestCreator.MODE, events,
                new TaxiRequestCreator(), optimizer, taxiData, scenario.getNetwork());
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
            PrtScheduler scheduler = new PrtScheduler(scenario, taxiData, qSim.getSimTimer(),
                    schedulerParams, travelTime, travelDisutility);
            TaxiOptimizerContext optimContext = new PrtOptimizerContext(taxiData, scenario,
                    qSim.getSimTimer(), travelTime, travelDisutility, scheduler, prtConfig);
            
            return optimizerFactory.createTaxiOptimizer(optimContext,
                    taxiCfg.getOptimizerConfigGroup());
        }
        else {
            TaxiScheduler scheduler = new TaxiScheduler(scenario, taxiData, qSim.getSimTimer(),
                    schedulerParams, travelTime, travelDisutility);

            TaxiOptimizerContext optimContext = new TaxiOptimizerContext(taxiData, scenario,
                    qSim.getSimTimer(), travelTime, travelDisutility, scheduler);
            return optimizerFactory.createTaxiOptimizer(optimContext,
                    taxiCfg.getOptimizerConfigGroup());
        }
    }

}
