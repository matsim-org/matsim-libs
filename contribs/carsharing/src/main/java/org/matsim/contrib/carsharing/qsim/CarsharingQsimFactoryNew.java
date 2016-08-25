package org.matsim.contrib.carsharing.qsim;

import javax.inject.Inject;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.carsharing.manager.CarSharingVehiclesNew;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;

import com.google.inject.Provider;
/** 
 * 
 * @author balac
 */
public class CarsharingQsimFactoryNew implements Provider<Mobsim>{

	@Inject Scenario scenario;
	@Inject EventsManager eventsManager;
	@Inject CarSharingVehiclesNew carsharingData;
	@Override
	public Mobsim get() {
		final QSim qsim = QSimUtils.createDefaultQSim(scenario, eventsManager);

		ParkCSVehicles parkSource = new ParkCSVehicles( qsim,
				carsharingData);
		qsim.addAgentSource(parkSource);
		return qsim;
	}

}
