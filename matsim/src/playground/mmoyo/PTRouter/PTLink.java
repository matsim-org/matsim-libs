package playground.mmoyo.PTRouter;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.network.BasicNode;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.api.core.v01.network.Link;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitLine;

public class PTLink extends LinkImpl{
	private TransitRoute transitRoute;
	private TransitLine transitLine;
	private double travelTime;
	private byte aliasType;   //1= access, 2= standard, 3= transfer 4= detTransfer 5= Egress
	private Link plainLink;
	PTValues ptValues = new PTValues();
	
	public PTLink(final Id id, final BasicNode from, final BasicNode to, final NetworkLayer network, final String type) {
		super(id, from, to, network, 0, 10, 9999 , 1);
		this.setLength(CoordUtils.calcDistance(from.getCoord(), to.getCoord()));
		this.setType(type);

		if 		(type.equals("Access")) 	{aliasType=1;} 
		else if (type.equals("Standard")) 	{aliasType=2;}
		else if (type.equals("Transfer")) 	{aliasType=3;}
		else if (type.equals("DetTransfer")){aliasType=4;}
		else if (type.equals("Egress")) 	{aliasType=5;}
		else 				{aliasType=0;}
		network.addLink(this);///
	}

	public TransitRoute getTransitRoute() {
		return transitRoute;
	}

	public void setTransitRoute(TransitRoute transitRoute) {
		this.transitRoute = transitRoute;
	}

	public double getTravelTime() {
		return travelTime;
	}

	public void setTravelTime(double travelTime) {
		this.travelTime = travelTime;
	}

	public byte getAliasType() {
		return this.aliasType;
	}

	public void setAliasType(byte aliasType) {
		this.aliasType = aliasType;
	}

	public Link getPlainLink() {
		return plainLink;
	}

	public void setPlainLink(Link plainLink) {
		this.plainLink = plainLink;
	}

	public TransitLine getTransitLine() {
		return transitLine;
	}

	public void setTransitLine(TransitLine transitLine) {
		this.transitLine = transitLine;
	}

	public double getWalkTime(){
		return this.length * ptValues.getAV_WALKING_SPEED();
	}

}