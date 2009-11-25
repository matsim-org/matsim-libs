package playground.mzilske.pt.queuesim;

import java.util.Collection;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.queuesim.DriverAgent;
import org.matsim.core.mobsim.queuesim.QueueLink;
import org.matsim.core.mobsim.queuesim.Simulation;
import org.matsim.core.network.NetworkLayer;
import org.matsim.pt.queuesim.TransitQueueVehicle;
import org.matsim.pt.queuesim.TransitStopAgentTracker;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.BasicVehicle;
import org.matsim.vehicles.BasicVehicles;


public class TransitQueueSimulation extends org.matsim.pt.queuesim.TransitQueueSimulation {

	public TransitQueueSimulation(ScenarioImpl scenario, EventsManager events) {
		super(scenario, events);
	}

	protected TransitStopAgentTracker getAgentTracker() {
		return agentTracker;
	}

	@Override
	protected void createVehiclesAndDrivers(TransitSchedule thisSchedule,
			TransitStopAgentTracker thisAgentTracker) {
		BasicVehicles vehicles = ((ScenarioImpl) this.scenario).getVehicles();
		ReconstructingUmlaufBuilder reconstructingUmlaufBuilder = new ReconstructingUmlaufBuilder((NetworkLayer) scenario.getNetwork(),((ScenarioImpl) this.scenario).getTransitSchedule().getTransitLines().values(), ((ScenarioImpl) this.scenario).getVehicles());
		Collection<Umlauf> umlaeufe = reconstructingUmlaufBuilder.build();
		for (Umlauf umlauf : umlaeufe) {
			BasicVehicle basicVehicle = vehicles.getVehicles().get(umlauf.getVehicleId());
			createAndScheduleVehicleAndDriver(umlauf, basicVehicle,
					thisAgentTracker);
		}
	}

	private void createAndScheduleVehicleAndDriver(Umlauf umlauf,
			BasicVehicle vehicle, TransitStopAgentTracker thisAgentTracker) {
		UmlaufDriver driver = new UmlaufDriver(umlauf, thisAgentTracker, this);
		TransitQueueVehicle veh = new TransitQueueVehicle(vehicle, 5);
		veh.setDriver(driver);
		driver.setVehicle(veh);
		QueueLink qlink = this.network.getQueueLink(driver
				.getCurrentLeg().getRoute().getStartLinkId());
		qlink.addParkedVehicle(veh);

		this.scheduleActivityEnd(driver);
		Simulation.incLiving();
	}

	@Override
	protected void scheduleActivityEnd(DriverAgent agent) {
		super.scheduleActivityEnd(agent);
	}
	
}
