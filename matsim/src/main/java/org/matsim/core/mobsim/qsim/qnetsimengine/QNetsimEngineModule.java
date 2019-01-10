package org.matsim.core.mobsim.qsim.qnetsimengine;

import com.google.inject.Inject;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

public class QNetsimEngineModule extends AbstractQSimModule {
	public final static String COMPONENT_NAME = "NetsimEngine";
	
	@Override
	protected void configureQSim() {
		bind(QNetsimEngine.class).asEagerSingleton();
		bind(VehicularDepartureHandler.class).toProvider(QNetsimEngineDepartureHandlerProvider.class).asEagerSingleton();

		if ( this.getConfig().qsim().isUseLanes() ) {
			bind(QNetworkFactory.class).to( QLanesNetworkFactory.class ) ;
		} else {
			bind(QNetworkFactory.class).to( DefaultQNetworkFactory.class ) ;
		}
		
		addNamedComponent(VehicularDepartureHandler.class, COMPONENT_NAME);
		addNamedComponent(QNetsimEngine.class, COMPONENT_NAME);
	}
}
