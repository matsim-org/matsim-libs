package playground.gregor.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;

public class DeparturelCurve implements AgentDepartureEventHandler{

	int time = 0;
	int arrived = 0;
	private final BufferedWriter w;
	
	public DeparturelCurve(BufferedWriter w) {
		this.w = w;
	}
	
	@Override
	public void handleEvent(AgentDepartureEvent event) {
		int intTime = (int) (event.getTime() + 0.5);
		
		if (intTime > this.time) {
			this.time = intTime;
			dump();
		}
		this.arrived++;
	}
	
	public void dump() {
		try {
			this.w.append(this.time/3600. + "\t" + this.arrived + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}

		
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}
	
	public static void main(String [] args) {
//		String base = "/Users/laemmel/svn/runs-svn/run1400";
		String base = "/Users/laemmel/devel/ICEM//input_dirac";
		String events = base + "/output/ITERS/it.500/500.events.xml.gz";
		EventsManager mgr = EventsUtils.createEventsManager();
		
		BufferedWriter w = null;
		try {
			w = new BufferedWriter(new FileWriter(new File(base + "/analysis/departure")));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		DeparturelCurve arrv = new DeparturelCurve(w);
		mgr.addHandler(arrv);
		new EventsReaderXMLv1(mgr).parse(events);
		arrv.dump();
		try {
			w.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}





}
