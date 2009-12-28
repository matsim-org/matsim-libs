package playground.wrashid.PSF.PSS;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderTXTv1;

public class BufferedEventsReaderTXTv1 extends EventsReaderTXTv1 {

	private ArrayList<Event> buffer=new ArrayList<Event>();

	public BufferedEventsReaderTXTv1(EventsManager events) {
		super(events);
	}

	@Override
	public Event createEvent(final double time, final Id agentId, final Id linkId, final int flag, final String desc,
			final String acttype) {

		Event data=  super.createEvent(time, agentId, linkId, flag, desc, acttype);

		buffer.add(data);
		
		return data;
	}
	
	public ArrayList<Event> getBuffer(){
		return buffer;
	}

}
