package playground.mmoyo.PTRouter;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.network.BasicNode;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.api.core.v01.network.Link;
import org.matsim.transitSchedule.api.TransitRoute;

public class PTLink extends LinkImpl{
	TransitRoute transitRoute;
	double travelTime;
	byte aliasType;   //0= access, 1= standard, 2= transfer 3= detTransfer 4= Egress
	Link plainLink;
	
	public PTLink(Id id, BasicNode from, BasicNode to, NetworkLayer network, String type) {
		super(id, from, to, network, 0, 1.0, 1.0 , 1);
		this.length = CoordUtils.calcDistance(from.getCoord(), to.getCoord());
		this.type= type;
		if (type.equals("Access")) aliasType=0; 
		if (type.equals("Standard")) aliasType=1;
		if (type.equals("Transfer")) aliasType=2;
		if (type.equals("DetTransfer")) aliasType=3;
		if (type.equals("Egress")) aliasType=4;
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
	

}