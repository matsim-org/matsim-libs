package playground.gregor.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;

public class Histogram implements AgentArrivalEventHandler{

	private boolean firstStage = true;
	Map<Id,AgentArrivalEvent> aes = new HashMap<Id,AgentArrivalEvent>();
	private final BufferedWriter w;

	public Histogram(BufferedWriter w) {
		this.w = w;
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		if (this.firstStage) {
			this.aes.put(event.getPersonId(), event);
		} else {
			AgentArrivalEvent e = this.aes.get(event.getPersonId());
			double diff = event.getTime() - e.getTime();
			try {
				this.w.append(diff + "\n");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
		}
	}
	
	public void startSecondStage() {
		this.firstStage = false;
	}
	
	public static void main(String[] args) { 

		String events = "/Users/laemmel/devel/crisscross/input/events_walk2d.xml.gz";
		String eventsT = "/Users/laemmel/devel/crisscross/output/ITERS/it.0/0.events.xml.gz";
//		String eventsT = "/Users/laemmel/devel/crisscross/output/ITERS/it.0/0.events.xml.gz";
		EventsManager mgr = EventsUtils.createEventsManager();

		BufferedWriter w = null;
		try {
			w = new BufferedWriter(new FileWriter(new File("/Users/laemmel/devel/crisscross/dbg/hist")));
			w.append("diff\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Histogram h = new Histogram(w);
		mgr.addHandler(h);
		new EventsReaderXMLv1(mgr).parse(events);
		h.startSecondStage();
		new EventsReaderXMLv1(mgr).parse(eventsT);
		
		
		try {
			w.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}


}
