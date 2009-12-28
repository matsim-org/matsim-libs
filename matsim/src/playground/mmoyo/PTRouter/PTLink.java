package playground.mmoyo.PTRouter;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;

public class PTLink extends LinkImpl{
	private TransitRoute transitRoute;
	private TransitLine transitLine;
	private Link plainLink;
	private double travelTime;
	private double walkTime = Double.NaN;
	private byte aliasType ;   //1= access, 2= standard, 3= transfer 4= detTransfer 5= Egress
	double avWalkSpeed = 1/1.34;//new PTValues().AV_WALKING_SPEED;    //use static variable
	
	public PTLink(final Id id, final Node from, final Node to, final NetworkLayer network, final String type) {
		super(id, from, to, network, 0, 10, 9999 , 1);
		this.setType(type);
		this.setLength(this.getEuklideanDistance());
		network.addLink(this);
	}

	public double getTravelTime() {
		return travelTime;
	}

	public void setTravelTime(final double travelTime) {
		this.travelTime = travelTime;
	}

	public byte getAliasType() {
		if 		(type.equals("Access")) 	{aliasType=1;} 
		else if (type.equals("Standard")) 	{aliasType=2;}
		else if (type.equals("Transfer")) 	{aliasType=3;}
		else if (type.equals("DetTransfer")){aliasType=4;}
		else if (type.equals("Egress")) 	{aliasType=5;}
		else 				{aliasType=0;}
		
		return this.aliasType;
	}

	public void setAliasType(final byte aliasType) {
		this.aliasType = aliasType;
	}

	public double getWalkTime(){
		if (aliasType!=2){ this.walkTime = this.getLength() * avWalkSpeed;}
		return this.walkTime;
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