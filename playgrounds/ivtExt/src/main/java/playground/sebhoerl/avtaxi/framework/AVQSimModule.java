package playground.sebhoerl.avtaxi.framework;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentSource;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.vehicles.VehicleType;
import playground.sebhoerl.avtaxi.config.AVConfig;
import playground.sebhoerl.avtaxi.data.AVData;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatchmentListener;
import playground.sebhoerl.avtaxi.passenger.AVRequestCreator;
import playground.sebhoerl.avtaxi.schedule.AVOptimizer;
import playground.sebhoerl.avtaxi.vrpagent.AVActionCreator;

import javax.inject.Named;

public class AVQSimModule extends com.google.inject.AbstractModule {
    final private AVConfig config;
    final private QSim qsim;

    public AVQSimModule(AVConfig config, QSim qsim) {
        this.config = config;
        this.qsim = qsim;
    }

    @Override
    protected void configure() {
        bind(Double.class).annotatedWith(Names.named("pickupDuration")).toInstance(config.getTimingParameters().getPickupDurationPerStop());
        bind(AVOptimizer.class);
        bind(AVActionCreator.class);
        bind(AVRequestCreator.class);
        bind(AVDispatchmentListener.class);
    }

    @Provides @Singleton
    public PassengerEngine providePassengerEngine(EventsManager events, AVRequestCreator requestCreator, AVOptimizer optimizer, AVData data, Network network) {
        return new PassengerEngine(
                AVModule.AV_MODE,
                events,
                requestCreator,
                optimizer,
                data,
                network
        );
    }

    @Provides @Singleton
    VrpLegs.LegCreator provideLegCreator() {
        return VrpLegs.createLegWithOfflineTrackerCreator(qsim.getSimTimer());
    }

    @Provides @Singleton
    public VrpAgentSource provideAgentSource(AVActionCreator actionCreator, AVData data, AVOptimizer optimizer, @Named(AVModule.AV_MODE) VehicleType vehicleType) {
        return new VrpAgentSource(actionCreator, data, optimizer, qsim, vehicleType);
    }
}
