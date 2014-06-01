package playground.vbmh.vmEV;

import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;

public class EVHandler implements LinkLeaveEventHandler, LinkEnterEventHandler {
	private EVControl evControl = new EVControl();
	
	
	
	public EVControl getEvControl() {
		return evControl;
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		// TODO Auto-generated method stub
		
		evControl.linkLeave(event);

	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		// TODO Auto-generated method stub
		
		evControl.linkEnter(event);
		
	}

}
