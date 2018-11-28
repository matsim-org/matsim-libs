package org.matsim.contrib.signals.builder;

import org.matsim.core.controler.AllowsOverriding;

public class SignalsBuilder{

    public static void configure( AllowsOverriding ao ) {

        ao.addOverridingModule( new SignalsModule() );

        ao.addOverridingQSimModule( new SignalsQSimModule() );

    }

}
