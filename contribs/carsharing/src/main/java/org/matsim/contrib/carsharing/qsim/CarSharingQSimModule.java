package org.matsim.contrib.carsharing.qsim;

import org.matsim.contrib.carsharing.manager.CarsharingManagerInterface;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.components.QSimComponentAnnotationsRegistry;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class CarSharingQSimModule extends AbstractQSimModule {
	public final static String COMPONENT_NAME = "Carsharing";

	private final CarsharingSupplyInterface carsharingSupply;
	private final CarsharingManagerInterface carsharingManager;

	public CarSharingQSimModule(CarsharingSupplyInterface carsharingSupply,
			CarsharingManagerInterface carsharingManager) {
		this.carsharingSupply = carsharingSupply;
		this.carsharingManager = carsharingManager;
	}

	@Override
	protected void configureQSim() {
		this.addQSimComponentBinding( COMPONENT_NAME ).to( ParkCSVehicles.class ) ;
	}

	@Provides
	@Singleton
	ParkCSVehicles provideParkCSVehicles(QSim qsim) {
		return new ParkCSVehicles(qsim, carsharingSupply);
	}

	@Provides
	@Singleton
	AgentFactory provideAgentFactory(Netsim netsim) {
		return new CSAgentFactory(netsim, carsharingManager);
	}
	
	static public void configureComponents( QSimComponentAnnotationsRegistry components ) {
		components.addNamedAnnotation(COMPONENT_NAME );
	}
}
