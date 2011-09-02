package playground.sergioo.NetworksMatcher.kernel.core;


import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

public class MatchingComposedLink extends ComposedLink {
	
	
	//Attributes
	
	private boolean fromMatched = false;
	private boolean toMatched = false;
	
	
	//Methods
	
	public MatchingComposedLink(Link link, Network network) {
		super(link, network);
	}
	
	public MatchingComposedLink(Id id, Node from, Node to, Network network) {
		super(id, from, to, network);
	}
	
	public boolean isFromMatched() {
		return fromMatched;
	}

	public boolean isToMatched() {
		return toMatched;
	}
	
	public void setFromMatched(boolean fromMatched) {
		this.fromMatched = fromMatched;
	}

	public void setToMatched(boolean toMatched) {
		this.toMatched = toMatched;
	}

	public void applyProperties(MatchingComposedLink fullLink) {
		//TODO
	}


}
