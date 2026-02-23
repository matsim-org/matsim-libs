package org.matsim.core.mobsim.qsim.pt;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.vehicles.Vehicle;

import java.util.Map;

public class PersonEntersPtVehicleEvent extends PersonEntersVehicleEvent {

	public static final String EVENT_TYPE = "PersonEntersPtVehicle";

	private final Id<TransitLine> transitLine;
	private final Id<TransitRoute> transitRoute;

	public PersonEntersPtVehicleEvent(double time, Id<Person> personId, Id<Vehicle> vehicleId, Id<TransitLine> transitLine, Id<TransitRoute> transitRoute) {
		super(time, personId, vehicleId);
		this.transitLine = transitLine;
		this.transitRoute = transitRoute;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public Map<String, String> getAttributes() {
		var atts = super.getAttributes();
		atts.put("transitLine", transitLine.toString());
		atts.put("transitRoute", transitRoute.toString());
		return atts;
	}

	@Override
	public void writeAsXML(StringBuilder out) {
		// PersonEntersVehicle only calls writeXMLStart and writeXMLEnd and nothing else
		// all attributes of PersonEntersVehicle are handled by the base Event class.
		writeXMLStart(out);
		out.append(" transitLine=\"").append(transitLine).append("\"");
		out.append(" transitRoute=\"").append(transitRoute).append("\"");
		writeXMLEnd(out);
	}
}
