package ch.sbb.matsim.contrib.railsim.prototype.analysis;

import ch.sbb.matsim.contrib.railsim.prototype.TrainEntersLink;
import ch.sbb.matsim.contrib.railsim.prototype.TrainLeavesLink;
import ch.sbb.matsim.contrib.railsim.prototype.TrainPathEntersLink;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.vehicles.Vehicle;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.Stack;

/**
 * @author ihab
 */
public class TrainEventsReader extends MatsimXmlParser {

	private static final String EVENT = "event";

	private final EventsManager eventsManager;

	public TrainEventsReader(EventsManager events) {
		super();
		this.eventsManager = events;
		setValidating(false); // events-files have no DTD, thus they cannot validate
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (EVENT.equals(name)) {
			startEvent(atts);
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		// ignore characters to prevent OutOfMemoryExceptions
		/* the events-file only contains empty tags with attributes,
		 * but without the dtd or schema, all whitespace between tags is handled
		 * by characters and added up by super.characters, consuming huge
		 * amount of memory when large events-files are read in.
		 */
	}

	private void startEvent(final Attributes attributes) {

		String eventType = attributes.getValue("type");

		Double time = 0.0;
		Id<Link> linkId = null;
		Id<Vehicle> vehicleId = null;

		if (TrainEntersLink.EVENT_TYPE.equals(eventType)) {
			for (int i = 0; i < attributes.getLength(); i++) {
				if (attributes.getQName(i).equals("time")) {
					time = Double.parseDouble(attributes.getValue(i));
				} else if (attributes.getQName(i).equals("type")) {
					eventType = attributes.getValue(i);
				} else if (attributes.getQName(i).equals(TrainEntersLink.ATTRIBUTE_LINK)) {
					linkId = Id.create((attributes.getValue(i)), Link.class);
				} else if (attributes.getQName(i).equals(TrainEntersLink.ATTRIBUTE_VEHICLE)) {
					vehicleId = Id.create((attributes.getValue(i)), Vehicle.class);
				} else {
					throw new RuntimeException("Unknown event attribute. Aborting...");
				}
			}
			this.eventsManager.processEvent(new TrainEntersLink(time, linkId, vehicleId));

		} else if (TrainLeavesLink.EVENT_TYPE.equals(eventType)) {
			for (int i = 0; i < attributes.getLength(); i++) {
				if (attributes.getQName(i).equals("time")) {
					time = Double.parseDouble(attributes.getValue(i));
				} else if (attributes.getQName(i).equals("type")) {
					eventType = attributes.getValue(i);
				} else if (attributes.getQName(i).equals(TrainEntersLink.ATTRIBUTE_LINK)) {
					linkId = Id.create((attributes.getValue(i)), Link.class);
				} else if (attributes.getQName(i).equals(TrainEntersLink.ATTRIBUTE_VEHICLE)) {
					vehicleId = Id.create((attributes.getValue(i)), Vehicle.class);
				} else {
					throw new RuntimeException("Unknown event attribute. Aborting...");
				}
			}
			this.eventsManager.processEvent(new TrainLeavesLink(time, linkId, vehicleId));

		} else if (TrainPathEntersLink.EVENT_TYPE.equals(eventType)) {
			for (int i = 0; i < attributes.getLength(); i++) {
				if (attributes.getQName(i).equals("time")) {
					time = Double.parseDouble(attributes.getValue(i));
				} else if (attributes.getQName(i).equals("type")) {
					eventType = attributes.getValue(i);
				} else if (attributes.getQName(i).equals(TrainEntersLink.ATTRIBUTE_LINK)) {
					linkId = Id.create((attributes.getValue(i)), Link.class);
				} else if (attributes.getQName(i).equals(TrainEntersLink.ATTRIBUTE_VEHICLE)) {
					vehicleId = Id.create((attributes.getValue(i)), Vehicle.class);
				} else {
					throw new RuntimeException("Unknown event attribute. Aborting...");
				}
			}
			this.eventsManager.processEvent(new TrainPathEntersLink(time, linkId, vehicleId));

		} else {
			// do not process other event types
		}
	}
}
