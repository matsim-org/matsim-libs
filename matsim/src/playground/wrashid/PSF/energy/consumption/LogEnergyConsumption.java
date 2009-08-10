package playground.wrashid.PSF.energy.consumption;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.events.BasicLinkEnterEvent;
import org.matsim.api.basic.v01.events.BasicLinkLeaveEvent;
import org.matsim.api.basic.v01.events.handler.BasicLinkEnterEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicLinkLeaveEventHandler;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.ActivityEndEvent;
import org.matsim.core.events.ActivityStartEvent;
import org.matsim.core.events.LinkEnterEvent;
import org.matsim.core.events.LinkLeaveEvent;
import org.matsim.core.events.handler.ActivityEndEventHandler;
import org.matsim.core.events.handler.ActivityStartEventHandler;
import org.matsim.core.events.handler.LinkEnterEventHandler;
import org.matsim.core.events.handler.LinkLeaveEventHandler;
import org.matsim.core.mobsim.jdeqsim.Scheduler;

import playground.wrashid.PSF.parking.ParkLog;
import playground.wrashid.PSF.parking.ParkingTimes;

/*
 * During driving energy is being "consumed", log that for each vehicle and leg.
 * - for this we need AverageSpeedEnergyConsumption for each link (later more inputs: e.g. maximum speed allowed on the road)
 * - get also a class for AverageSpeedEnergyConsumption here...
 * => we need to assign a different such curve to each agent (we need to put this attribute to the agent)
 * 
 */
public class LogEnergyConsumption implements LinkEnterEventHandler, LinkLeaveEventHandler {

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
		// TODO Auto-generated method stub

	}

	public void handleEvent(LinkLeaveEvent event) {
		Id personId = event.getPersonId();

		EnergyConsumption eConsumption = energyConsumption.get(personId);

		if (eConsumption != null) {
			/*
			 * this is not the first time we are departing, which means that the
			 * car was parked before
			 */
			double entranceTime=eConsumption.getTempEnteranceTimeOfLastLink();
			double consumption=EnergyConsumptionInfo.getEnergyConsumption(event.getLink(), event.getTime()-entranceTime, EnergyConsumptionInfo.getVehicleType(personId));
			eConsumption.addEnergyConsumptionLog(new LinkEnergyConsumptionLog(event.getLinkId(),eConsumption.getTempEnteranceTimeOfLastLink(),event.getTime(),consumption));
		} else {
			log.error("Some thing is wrong, as the vehicle is leaving a link which he did not enter...");
		}

	}

	public LogEnergyConsumption(Controler controler) {
		super();
		this.controler = controler;
	}

}
