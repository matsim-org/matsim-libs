package playground.wrashid.PSF.energy.consumption;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

import java.util.HashMap;

/*
 * During driving energy is being "consumed", log that for each vehicle and leg.
 * - for this we need AverageSpeedEnergyConsumption for each link (later more inputs: e.g. maximum speed allowed on the road)
 * - get also a class for AverageSpeedEnergyConsumption here...
 * => we need to assign a different such curve to each agent (we need to put this attribute to the agent)
 * 
 */
public class LogEnergyConsumption implements LinkEnterEventHandler, LinkLeaveEventHandler, VehicleEntersTrafficEventHandler,
VehicleLeavesTrafficEventHandler {

	private static final Logger log = Logger.getLogger(LogEnergyConsumption.class);
	Controler controler;
	HashMap<Id<Person>, EnergyConsumption> energyConsumption = new HashMap<>();
	
	private Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler() ;	

	/*
	 * Register the time, when the vehicle entered the link (non-Javadoc)
	 * 
	 * @see org.matsim.core.events.handler.LinkEnterEventHandler#handleEvent(org.matsim.core.events.LinkEnterEvent)
	 */
	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id<Person> personId = delegate.getDriverOfVehicle( event.getVehicleId() ) ;

		if (!energyConsumption.containsKey(personId)) {
			energyConsumption.put(personId, new EnergyConsumption());
		}
		
		EnergyConsumption eConsumption = energyConsumption.get(personId);

		eConsumption.setTempEnteranceTimeOfLastLink(event.getTime());
	}

	@Override
	public void reset(int iteration) {
		energyConsumption = new HashMap<>();
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id<Person> personId = delegate.getDriverOfVehicle( event.getVehicleId() ) ;

		EnergyConsumption eConsumption = energyConsumption.get(personId);

		double entranceTime = eConsumption.getTempEnteranceTimeOfLastLink();
        Link link = controler.getScenario().getNetwork().getLinks().get(event.getLinkId());
		double consumption = EnergyConsumptionInfo.getEnergyConsumption(link, event.getTime() - entranceTime,
				EnergyConsumptionInfo.getVehicleType(personId));
		eConsumption.addEnergyConsumptionLog(new LinkEnergyConsumptionLog(event.getLinkId(), eConsumption
				.getTempEnteranceTimeOfLastLink(), event.getTime(), consumption));

	}

	public LogEnergyConsumption(Controler controler) {
		super();
		this.controler = controler;
	}

	public HashMap<Id<Person>, EnergyConsumption> getEnergyConsumption() {
		return energyConsumption;
	}

	/*
	 * For JDEQSim this the starting point of the simulation, when the agent
	 * waits to enter the first link
	 * 
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.core.events.handler.AgentWait2LinkEventHandler#handleEvent(org.matsim.core.events.AgentWait2LinkEvent)
	 */
	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		this.delegate.handleEvent(event);
		
		Id<Person> personId = event.getPersonId();
		if (!energyConsumption.containsKey(personId)) {
			energyConsumption.put(personId, new EnergyConsumption());
		}
		EnergyConsumption eConsumption = energyConsumption.get(personId);

		eConsumption.setTempEnteranceTimeOfLastLink(event.getTime());
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		this.delegate.handleEvent(event);
	}

}
