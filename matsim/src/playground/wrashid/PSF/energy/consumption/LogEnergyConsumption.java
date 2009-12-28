package playground.wrashid.PSF.energy.consumption;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentWait2LinkEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.controler.Controler;

/*
 * During driving energy is being "consumed", log that for each vehicle and leg.
 * - for this we need AverageSpeedEnergyConsumption for each link (later more inputs: e.g. maximum speed allowed on the road)
 * - get also a class for AverageSpeedEnergyConsumption here...
 * => we need to assign a different such curve to each agent (we need to put this attribute to the agent)
 * 
 */
public class LogEnergyConsumption implements LinkEnterEventHandler, LinkLeaveEventHandler, AgentWait2LinkEventHandler {

	private static final Logger log = Logger.getLogger(LogEnergyConsumption.class);
	Controler controler;
	HashMap<Id, EnergyConsumption> energyConsumption = new HashMap<Id, EnergyConsumption>();

	/*
	 * Register the time, when the vehicle entered the link (non-Javadoc)
	 * 
	 * @see org.matsim.core.events.handler.LinkEnterEventHandler#handleEvent(org.matsim.core.events.LinkEnterEvent)
	 */
	public void handleEvent(LinkEnterEvent event) {
		Id personId = event.getPersonId();

		if (!energyConsumption.containsKey(personId)) {
			energyConsumption.put(personId, new EnergyConsumption());
		}
		
		EnergyConsumption eConsumption = energyConsumption.get(personId);

		eConsumption.setTempEnteranceTimeOfLastLink(event.getTime());
	}

	public void reset(int iteration) {
		energyConsumption = new HashMap<Id, EnergyConsumption>();
	}

	public void handleEvent(LinkLeaveEvent event) {
		Id personId = event.getPersonId();

		EnergyConsumption eConsumption = energyConsumption.get(personId);

		double entranceTime = eConsumption.getTempEnteranceTimeOfLastLink();
		Link link = controler.getNetwork().getLinks().get(event.getLinkId());
		double consumption = EnergyConsumptionInfo.getEnergyConsumption(link, event.getTime() - entranceTime,
				EnergyConsumptionInfo.getVehicleType(personId));
		eConsumption.addEnergyConsumptionLog(new LinkEnergyConsumptionLog(event.getLinkId(), eConsumption
				.getTempEnteranceTimeOfLastLink(), event.getTime(), consumption));

	}

	public LogEnergyConsumption(Controler controler) {
		super();
		this.controler = controler;
	}

	public HashMap<Id, EnergyConsumption> getEnergyConsumption() {
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
	public void handleEvent(AgentWait2LinkEvent event) {
		Id personId = event.getPersonId();
		if (!energyConsumption.containsKey(personId)) {
			energyConsumption.put(personId, new EnergyConsumption());
		}
		EnergyConsumption eConsumption = energyConsumption.get(personId);

		eConsumption.setTempEnteranceTimeOfLastLink(event.getTime());
	}

}
