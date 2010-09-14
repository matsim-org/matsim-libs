package playground.mmoyo.PTRouter;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;

public class PTLink extends LinkImpl{
	private TransitRoute transitRoute;
	private TransitLine transitLine;
	private Link plainLink;
	private double travelTime;
	private byte aliasType ;   //1= access, 2= standard, 3= transfer 4= detTransfer 5= Egress
	
	public PTLink(final Id id, final Node from, final Node to, final NetworkImpl network, final String type) {
		super(id, from, to, network, CoordUtils.calcDistance(from.getCoord(), to.getCoord()), 99, 9999 , 99);
		this.setType(type);
	
		if 	(type.equals(PTValues.ACCESS_STR )) 		{this.aliasType=1;} 
		else if (type.equals(PTValues.STANDARD_STR)) 	{this.aliasType=2;}
		else if (type.equals(PTValues.TRANSFER_STR)) 	{this.aliasType=3;}
		else if (type.equals(PTValues.DETTRANSFER_STR))	{this.aliasType=4;}
		else if (type.equals(PTValues.EGRESS_STR)) 		{this.aliasType=5;}
		else 	{aliasType=0;} 
		
		network.addLink(this);
	}

	public double getTravelTime() {
		return travelTime;
	}

	public void setTravelTime(final double travelTime) {
		this.travelTime = travelTime;
	}

	public byte getAliasType() {
		return this.aliasType;
	}

	public void setAliasType(final byte aliasType) {
		this.aliasType = aliasType;
	}

	public double getWalkTime(){
		return this.getLength() * PTValues.AV_WALKING_SPEED;
	}

	public TransitRoute getTransitRoute() {
		return transitRoute;
	}

	public void setTransitRoute(TransitRoute transitRoute) {
		this.transitRoute = transitRoute;
	}

	public TransitLine getTransitLine() {
		return transitLine;
	}

	public void setTransitLine(TransitLine transitLine) {
		this.transitLine = transitLine;
	}

	public Link getPlainLink() {
		return plainLink;
	}

	public void setPlainLink(Link plainLink) {
		this.plainLink = plainLink;
	}

	

}