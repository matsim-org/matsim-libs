package playground.julia.incomeScoring;

import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;

public class RoadUsedHandler implements LinkLeaveEventHandler{

	static boolean mer;
	int i=3;
	

@Override
public void reset(int iteration) {
	System.out.println("reset...");
}

@Override
public void handleEvent(LinkLeaveEvent event) {
	System.out.println("LinkId"+event.getLinkId().toString());
	if (event.getLinkId().equals(new IdImpl(Integer.toString(i)))){
		mer = true;
	}
}


public static boolean getWasRoadSelected() {
	return mer;
}



}
