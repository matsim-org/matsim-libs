package playground.ciarif.retailers.data;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;

public class LinkRetailersImpl extends LinkImpl  { //AbstractLocation implements BasicLink {

	private static final long serialVersionUID = 1L;
	protected Node from = null;
	protected Node to = null;

	protected int maxFacOnLink = 0;
	protected double length = Double.NaN;
	protected double freespeed = Double.NaN;
	protected double capacity = Double.NaN;
	protected double nofLanes = Double.NaN;

	public LinkRetailersImpl(Link link, NetworkLayer network) {
		super(link.getId(),link.getFromNode(),link.getToNode(),network,link.getLength(),link.getFreespeed(),link.getCapacity(),link.getNumberOfLanes());
	}

	public void setMaxFacOnLink(int max_number_facilities) {
		this.maxFacOnLink = max_number_facilities;
	}
}
