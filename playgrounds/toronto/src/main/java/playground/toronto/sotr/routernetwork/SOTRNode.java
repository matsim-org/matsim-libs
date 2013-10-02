package playground.toronto.sotr.routernetwork;

import java.util.ArrayList;
import java.util.List;

import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.toronto.sotr.routernetwork.SOTRLink;

public class SOTRNode {

	private TransitStopFacility stop;
		
	protected List<SOTRLink> incomingLinks;
	protected List<SOTRLink> outgoingLinks;
	
	protected SOTRNode(final TransitStopFacility stop){
		this.stop = stop;
		
		this.incomingLinks = new ArrayList<SOTRLink>(4);
		this.outgoingLinks = new ArrayList<SOTRLink>(4);
	}
	
	public Iterable<SOTRLink> getIncomingLinks() {return this.incomingLinks;}
	public Iterable<SOTRLink> getOutgoingLinks() {return this.outgoingLinks;}
	public TransitStopFacility getStop() {return this.stop;}
	
	
}
