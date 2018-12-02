package org.matsim.core.mobsim.qsim.qnetsimengine;

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

		//		bindNamedComponent(componentClass, name).to(componentClass);
		this.addQSimComponentBinding( COMPONENT_NAME ).to( VehicularDepartureHandler.class ) ;
		//		bindNamedComponent(componentClass, name).to(componentClass);
		this.addQSimComponentBinding( COMPONENT_NAME ).to( QNetsimEngine.class ) ;
	}
}
