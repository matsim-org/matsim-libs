package playground.gregor.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;

public class LinkLeaveCurve implements LinkLeaveEventHandler {
	int time = -1;
	int arrived = 0;
	private final BufferedWriter w;
	
	public LinkLeaveCurve(BufferedWriter w) {
		this.w = w;
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (!event.getLinkId().toString().equals("5") && !event.getLinkId().toString().equals("17")) {
			return;
		}
		int intTime = (int) (event.getTime() + 0.5);
		
		if (intTime > this.time) {
			dump();
			this.time = intTime;
		}
		this.arrived++;
	}
	
	private void dump() {
		try {
			this.w.append(this.time + "\t" + this.arrived + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}

		
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}
	
	public static void main(String [] args) {
		String events = "/Users/laemmel/devel/gr90/output/ITERS/it.0/0.events.xml.gz";
//		String events = "/Users/laemmel/devel/gr90/input/events.xml";
		EventsManager mgr = EventsUtils.createEventsManager();
		
		BufferedWriter w = null;
		try {
			w = new BufferedWriter(new FileWriter(new File("/Users/laemmel/devel/gr90/dbg/pq_walk2d")));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		LinkLeaveCurve arrv = new LinkLeaveCurve(w);
		mgr.addHandler(arrv);
		new EventsReaderXMLv1(mgr).parse(events);
		try {
			w.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
