package playground.wrashid.parkingSearch.ca.matlabInfra;

import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.core.network.NetworkImpl;

public class Agent {

	Id id;
	double tripStartTime;
	Route routeTo;
	String actType;
	double actDur;
	Route routeAway;
	
	Route tmpRoute;
	double actStartTime;
	
	public Agent(Id id,double tripStartTime){
		tmpRoute=new Route();
		this.tripStartTime=tripStartTime;
		this.id=id;
	}
	
	public String getXMLString(NetworkImpl network){
		Random rand=new Random();
		
		StringBuffer stringBuffer = new StringBuffer();
		
		stringBuffer.append("\t<agent>\n");

		stringBuffer.append("\t\t<id>" + id + "#" + rand.nextLong() + "</id>\n");
		stringBuffer.append("\t\t<tripStartTime>" + tripStartTime + "</tripStartTime>\n");
		stringBuffer.append("\t\t<route_to>" + routeTo==null?"":routeTo.getNodeString(network) + "</route_to>\n");
		stringBuffer.append("\t\t<actType>" + actType + "</actType>\n");
		stringBuffer.append("\t\t<actDur>" + actDur + "</actDur>\n");
		stringBuffer.append("\t\t<route_away>" + routeAway==null?"":routeAway.getNodeString(network) + "</route_away>\n");
		
		stringBuffer.append("\t</agent>\n");
		
		return stringBuffer.toString();
	}
	
	
}
