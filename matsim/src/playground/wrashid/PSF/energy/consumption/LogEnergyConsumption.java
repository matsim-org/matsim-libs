package playground.wrashid.PSF.energy.consumption;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.AgentWait2LinkEventImpl;
import org.matsim.core.events.LinkEnterEventImpl;
import org.matsim.core.events.LinkLeaveEventImpl;
import org.matsim.core.events.handler.AgentWait2LinkEventHandler;
import org.matsim.core.events.handler.LinkEnterEventHandler;
import org.matsim.core.events.handler.LinkLeaveEventHandler;

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
	public void handleEvent(LinkEnterEventImpl event) {
		Id personId = event.getPersonId();

		EnergyConsumption eConsumption = energyConsumption.get(personId);

		eConsumption.setTempEnteranceTimeOfLastLink(event.getTime());
	}

	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	public void handleEvent(LinkLeaveEventImpl event) {
		Id personId = event.getPersonId();

		EnergyConsumption eConsumption = energyConsumption.get(personId);

		double entranceTime = eConsumption.getTempEnteranceTimeOfLastLink();
		double consumption = EnergyConsumptionInfo.getEnergyConsumption(event.getLink(), event.getTime() - entranceTime,
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
	public void handleEvent(AgentWait2LinkEventImpl event) {
		Id personId = event.getPersonId();
		if (!energyConsumption.containsKey(personId)) {
			energyConsumption.put(personId, new EnergyConsumption());
		}
		EnergyConsumption eConsumption = energyConsumption.get(personId);

		eConsumption.setTempEnteranceTimeOfLastLink(event.getTime());
	}

}
