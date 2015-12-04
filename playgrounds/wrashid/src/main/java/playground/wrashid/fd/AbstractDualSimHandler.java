package playground.wrashid.fd;

import java.util.HashSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.lib.obj.TwoKeyHashMapWithDouble;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;


public abstract class AbstractDualSimHandler implements LinkLeaveEventHandler,
		LinkEnterEventHandler, PersonArrivalEventHandler,
		VehicleEntersTrafficEventHandler , VehicleLeavesTrafficEventHandler {
	
	Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler() ;

	public abstract boolean isJDEQSim();

	public abstract boolean isLinkPartOfStudyArea(Id linkId);

	public abstract void processLeaveLink(Id linkId, Id personId, double enterTime, double leaveTime);

	// personId
	private HashSet<Id> agentsTravellingOnLinks = new HashSet<Id>();

	// linkId, personId
	private TwoKeyHashMapWithDouble<Id, Id> linkEnterTime = new TwoKeyHashMapWithDouble<Id, Id>();

	@Override
	public void reset(int iteration) {
		this.delegate.reset(iteration);
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id<Person> driverId = delegate.getDriverOfVehicle( event.getVehicleId() ) ;
		
		if (isLinkPartOfStudyArea(event.getLinkId())) {
			if (agentsTravellingOnLinks.contains(driverId)) {
				processLeaveLink(event.getLinkId(), driverId,
						linkEnterTime.get(event.getLinkId(), driverId), event.getTime());
			}
		}
		agentsTravellingOnLinks.remove(driverId);
		linkEnterTime.removeValue(event.getLinkId(), driverId);
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (isLinkPartOfStudyArea(event.getLinkId())) {
			if (agentsTravellingOnLinks.contains(event.getPersonId())) {
				if (!isJDEQSim()) {
					processLeaveLink(event.getLinkId(), event.getPersonId(),
							linkEnterTime.get(event.getLinkId(), event.getPersonId()), event.getTime());
				}
			}
		}
		agentsTravellingOnLinks.remove(event.getPersonId());
		linkEnterTime.removeValue(event.getLinkId(), event.getPersonId());
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id<Person> driverId = delegate.getDriverOfVehicle( event.getVehicleId() ) ;
		agentsTravellingOnLinks.add(driverId);
		linkEnterTime.put(event.getLinkId(), driverId,
				event.getTime());
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		this.delegate.handleEvent(event);
		if (isJDEQSim()) {
			agentsTravellingOnLinks.add(event.getPersonId());
			linkEnterTime.put(event.getLinkId(), event.getPersonId(),
					event.getTime());
		} else {
			agentsTravellingOnLinks.remove(event.getPersonId());
		}
	}
	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		this.delegate.handleEvent(event);
	}
}
