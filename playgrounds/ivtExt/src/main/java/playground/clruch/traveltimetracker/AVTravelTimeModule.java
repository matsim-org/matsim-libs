package playground.clruch.traveltimetracker;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import playground.sebhoerl.avtaxi.framework.AVModule;

public class AVTravelTimeModule extends AbstractModule {
    
    @Override
    public void install() {
        bind(AVTravelTimeTracker.class).asEagerSingleton();
        addEventHandlerBinding().to(AVTravelTimeTracker.class).asEagerSingleton();

        bind(TravelTime.class).annotatedWith(Names.named(AVModule.AV_MODE))
                .to(AVTravelTime.class);
    }

    @Provides @Singleton
    private AVTravelTime provideAVTravelTime(AVTravelTimeTracker travelTimeTracker, @Named("car") TravelTime delegate) {
        return new AVTravelTime(travelTimeTracker, delegate);
    }
    
}
