package org.matsim.contrib.signals.builder;

import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QSignalsNetworkFactory;

class SignalsQSimModule extends AbstractQSimModule{
    @Override
    protected void configureQSim(){
        this.bind( QNetworkFactory.class ).to( QSignalsNetworkFactory.class ) ;
    }
}
