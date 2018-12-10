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
        public Configurator( AllowsConfiguration ao ){
            this.signalsModule = new SignalsModule() ;
            ao.addOverridingModule( this.signalsModule ) ;
            ao.addOverridingQSimModule( new SignalsQSimModule() ) ;
        }
        public final void addSignalControllerFactory( String key, Class<? extends SignalControllerFactory> signalControllerFactoryClassName ) {
            signalsModule.addSignalControllerFactory( key, signalControllerFactoryClassName );
            // it is not _totally_ sure that adding this after adding it as an overridingModule will work, but I think that it will.  kai, dec'19
        }
    }

}
