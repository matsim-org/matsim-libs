package playground.ciarif.retailers;


import org.matsim.basic.v01.BasicLinkImpl;
import org.matsim.interfaces.basic.v01.network.BasicNode;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.network.NetworkLayer;

public class LinkRetailersImpl extends BasicLinkImpl  { //AbstractLocation implements BasicLink {

	protected BasicNode from = null;
	protected BasicNode to = null;

	protected int maxFacOnLink = 0;
	protected double length = Double.NaN;
	protected double freespeed = Double.NaN;
	protected double capacity = Double.NaN;
	protected double nofLanes = Double.NaN;

	public LinkRetailersImpl(Link link, NetworkLayer network) {
		super (network, link.getId(), link.getFromNode(), link.getToNode());
	}

	public void setMaxFacOnLink(int max_number_facilities) {
		this.maxFacOnLink = max_number_facilities;
	}
}	

	

