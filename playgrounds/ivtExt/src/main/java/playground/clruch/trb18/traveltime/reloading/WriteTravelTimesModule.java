package playground.clruch.trb18.traveltime.reloading;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

public class WriteTravelTimesModule extends AbstractModule {
    @Override
    public void install() {
        addControlerListenerBinding().to(TravelTimeWriter.class);
    }

    @Provides
    @Singleton
    public TravelTimeWriter provideTravelTimeWriter(OutputDirectoryHierarchy outputDirectoryHierarchy, Network network, @Named("car") TravelTime travelTime, ControlerConfigGroup config) {
        return new TravelTimeWriter(outputDirectoryHierarchy, network, travelTime, config.getWriteEventsInterval(), 300.0, 3600.0 * 30.0);
    }
}
