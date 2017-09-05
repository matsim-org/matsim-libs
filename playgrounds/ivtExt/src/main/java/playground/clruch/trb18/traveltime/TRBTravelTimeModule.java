package playground.clruch.trb18.traveltime;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import playground.sebhoerl.avtaxi.framework.AVModule;

public class TRBTravelTimeModule extends AbstractModule {
    @Override
    public void install() {
        addMobsimListenerBinding().to(TRBTravelTimeTracker.class);
        addEventHandlerBinding().to(TRBTravelTimeTracker.class);
    }

    @Provides @Singleton
    public TRBTravelTimeTracker provideTRBTravelTimeTracker(Network network, @Named(VrpTravelTimeModules.DVRP_ESTIMATED) TravelTime delegate) {
        return new TRBTravelTimeTracker(network, delegate);
    }

    @Provides @Named(AVModule.AV_MODE) @Singleton
    public TravelTime provideTRBTravelTime(TRBTravelTimeTracker tracker) {
        return new TRBTravelTime(tracker);
    }
}
