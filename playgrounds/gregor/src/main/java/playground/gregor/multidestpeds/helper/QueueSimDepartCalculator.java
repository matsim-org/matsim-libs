package playground.gregor.multidestpeds.helper;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.config.Module;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;

public class QueueSimDepartCalculator implements LinkLeaveEventHandler{
	
	Map<Id,Double> departs = new HashMap<Id,Double>();
	private double offset = 1;
	
	public QueueSimDepartCalculator(Scenario sc,String eventsFile) {
		Module m = sc.getConfig().getModule("qsim");
		if (m != null) {
			QSimConfigGroup qsim = (QSimConfigGroup)m;
			this.offset  = qsim.getTimeStepSize();
		}
		
		EventsManager mgr = EventsUtils.createEventsManager();
		mgr.addHandler(this);
		new EventsReaderXMLv1(mgr).parse(eventsFile);
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id id = event.getPersonId();
		if (!this.departs.containsKey(id)) {
			this.departs.put(id, event.getTime()-this.offset);
		}
	}
	
	public Map<Id,Double> getQSimDepartures() {
		return this.departs;
	}

}
