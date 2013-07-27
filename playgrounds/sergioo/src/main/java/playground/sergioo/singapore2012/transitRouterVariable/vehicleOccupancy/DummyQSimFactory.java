package playground.sergioo.singapore2012.transitRouterVariable.vehicleOccupancy;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimFactory;

public class DummyQSimFactory extends QSimFactory implements MobsimFactory {
	
	private VehicleOccupancyCalculator vehicleOccupancyCalculator;
	
	public DummyQSimFactory(VehicleOccupancyCalculator vehicleOccupancyCalculator) {
		this.vehicleOccupancyCalculator = vehicleOccupancyCalculator;
	}
	@Override
	public QSim createMobsim(Scenario sc, EventsManager eventsManager) {
		QSim mobsim = ((QSim)super.createMobsim(sc, eventsManager));
		vehicleOccupancyCalculator.setPtEngine(mobsim.getTransitEngine());
		return mobsim;
	}
	
}
