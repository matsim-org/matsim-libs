package org.matsim.contrib.signals.builder;

import org.matsim.contrib.signals.controller.SignalControllerFactory;
import org.matsim.core.controler.AllowsConfiguration;

public class Signals{

    public static void configure( AllowsConfiguration ao ){

        ao.addOverridingModule( new SignalsModule() );

        ao.addOverridingQSimModule( new SignalsQSimModule() );
    }

    public static class Configurator{
        private final SignalsModule signalsModule;
        private Configurator( SignalsModule signalsModule ){
            this.signalsModule = signalsModule;
        }
        public final void addSignalControllerFactory( String key, Class<? extends SignalControllerFactory> signalControllerFactoryClassName ) {
            signalsModule.addSignalControllerFactory( key, signalControllerFactoryClassName );
        }
    }

}
