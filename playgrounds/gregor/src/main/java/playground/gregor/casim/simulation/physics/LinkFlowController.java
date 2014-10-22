package playground.gregor.casim.simulation.physics;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.gbl.MatsimRandom;

public class LinkFlowController implements LinkLeaveEventHandler, LinkEnterEventHandler{

	private List<CALinkDynamic> controlled;
	private Id<Link> id1;
	private Id<Link> id2;
	private double area;
	private int onLink;
	
	boolean triggered = false;
	private double initDens = MatsimRandom.getRandom().nextDouble()*CANetworkDynamic.RHO_HAT/2;
	private double red =0.1;

	public LinkFlowController(Id<Link> id1, Id<Link> id2, Link l, List<CALinkDynamic> controlled) {
		this.controlled = controlled;
		this.id1 = id1;
		this.id2 = id2;
		this.area = l.getLength() * l.getCapacity();
		
	}
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (event.getLinkId() == id1 || event.getLinkId() == id2) {
			this.onLink++;
		}

		
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (!triggered) {
			for (CALinkDynamic c : controlled) {
				c.changeWidth(0.24);
				System.out.println(c.getWidth());
			}
			triggered = true;
		}
		if (event.getLinkId() == id1 || event.getLinkId() == id2) {
			this.onLink--;
		}
		
	}

}
