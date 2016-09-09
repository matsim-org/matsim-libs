package playground.sebhoerl.avtaxi.framework;

import java.util.Collection;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentSource;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs.LegCreator;
import org.matsim.contrib.taxi.data.TaxiData;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizer;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizerFactory;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.contrib.taxi.run.TaxiQSimProvider;
import org.matsim.contrib.taxi.vrpagent.TaxiActionCreator;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.VehicleType;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import playground.sebhoerl.avtaxi.vrpagent.AVTaxiActionCreator;

public class AVTaxiQSimProvider extends TaxiQSimProvider {
	@Inject
	public AVTaxiQSimProvider(EventsManager eventsManager, Collection<AbstractQSimPlugin> plugins, Scenario scenario,
			TaxiData taxiData, @Named(VrpTravelTimeModules.DVRP_ESTIMATED) TravelTime travelTime, @Named(TaxiModule.TAXI_MODE) VehicleType vehicleType, TaxiOptimizerFactory optimizerFactory) {
		super(eventsManager, plugins, scenario, taxiData, travelTime, vehicleType, optimizerFactory);
	}
	
	protected VrpAgentSource createVrpAgentSource(TaxiOptimizer optimizer, QSim qSim,
	            PassengerEngine passengerEngine, VehicleType vehicleType) {
        LegCreator legCreator = taxiCfg.isOnlineVehicleTracker() ? //
                VrpLegs.createLegWithOnlineTrackerCreator(optimizer, qSim.getSimTimer()) : //
                VrpLegs.createLegWithOfflineTrackerCreator(qSim.getSimTimer());
        
        TaxiActionCreator actionCreator = new TaxiActionCreator(passengerEngine, legCreator,
                taxiCfg.getPickupDuration());
        
        AVTaxiActionCreator avActionCreator = new AVTaxiActionCreator(passengerEngine, legCreator,
                taxiCfg.getPickupDuration(), actionCreator);
        
        return new VrpAgentSource(avActionCreator, taxiData, optimizer, qSim, vehicleType);
	}
}
