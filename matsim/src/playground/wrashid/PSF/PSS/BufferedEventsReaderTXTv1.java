package playground.wrashid.PSF.PSS;

import java.util.ArrayList;
import java.util.LinkedList;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderTXTv1;

public class BufferedEventsReaderTXTv1 extends EventsReaderTXTv1 {

	private ArrayList<Event> buffer=new ArrayList<Event>();

	public BufferedEventsReaderTXTv1(EventsManagerImpl events) {
		super(events);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Event createEvent(final EventsManagerImpl events, final double time, final Id agentId, final Id linkId, final int flag,
			final String desc, final String acttype) {

		Event data=  super.createEvent(events, time, agentId, linkId, flag, desc, acttype);

		buffer.add(data);
		
		return data;
	}
	
	public ArrayList<Event> getBuffer(){
		return buffer;
	}

}
