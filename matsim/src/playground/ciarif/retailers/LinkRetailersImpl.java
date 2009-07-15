package playground.ciarif.retailers;


import org.matsim.api.basic.v01.network.BasicNode;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.misc.Time;

public class LinkRetailersImpl extends LinkImpl  { //AbstractLocation implements BasicLink {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected BasicNode from = null;
	protected BasicNode to = null;

	protected int maxFacOnLink = 0;
	protected double length = Double.NaN;
	protected double freespeed = Double.NaN;
	protected double capacity = Double.NaN;
	protected double nofLanes = Double.NaN;

	public LinkRetailersImpl(LinkImpl link, NetworkLayer network) {
		super(link.getId(),link.getFromNode(),link.getToNode(),network,link.getLength(),link.getFreespeed(Time.UNDEFINED_TIME),link.getCapacity(Time.UNDEFINED_TIME),link.getNumberOfLanes(Time.UNDEFINED_TIME));
	}

	public void setMaxFacOnLink(int max_number_facilities) {
		this.maxFacOnLink = max_number_facilities;
	}
}	
