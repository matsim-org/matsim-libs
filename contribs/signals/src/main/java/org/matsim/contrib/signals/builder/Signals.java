package org.matsim.contrib.signals.builder;

import org.matsim.core.controler.Controler;

public class Signals{
    private Signals(){} // do not instantiate

    public static void configure( Controler c ) {

        c.addOverridingModule( new SignalsModule() );

        c.addOverridingQSimModule( new SignalsQSimModule() );

    }

}
