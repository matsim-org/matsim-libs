package playground.wrashid.parkingSearch.ca.matlabInfra;

import java.util.Random;

import org.matsim.api.core.v01.Id;

public class Agent {

	Id<Agent> id;
	double tripStartTime;
	String routeTo;
	String actType;
	double actDur;
	String routeAway;
	
	Route tmpRoute;
	double actStartTime;
	
	public Agent(Id<Agent> id,double tripStartTime){
		tmpRoute=new Route();
		this.tripStartTime=tripStartTime;
		this.id=id;
	}
	
	public Agent(Id<Agent> id, double tripStartTime, String routeTo, String actType, double actDur, String routeAway){
		this.id = id;
		this.tripStartTime = tripStartTime;
		this.routeTo = routeTo;
		this.actType = actType;
		this.actDur = actDur;
		this.routeAway = routeAway;		
	}
	
	public String getXMLString(org.matsim.api.core.v01.network.Network network){
		Random rand=new Random();
		
		StringBuffer stringBuffer = new StringBuffer();
		
		stringBuffer.append("\t<agent>\n");

		stringBuffer.append("\t\t<id>" + id + "#" + rand.nextLong() + "</id>\n");
		stringBuffer.append("\t\t<tripStartTime>" + tripStartTime + "</tripStartTime>\n");
		stringBuffer.append("\t\t<route_to>" + routeTo + "</route_to>\n");
		stringBuffer.append("\t\t<actType>" + actType + "</actType>\n");
		stringBuffer.append("\t\t<actDur>" + actDur + "</actDur>\n");
		stringBuffer.append("\t\t<route_away>" + routeAway + "</route_away>\n");
		
		stringBuffer.append("\t</agent>\n");
		
		return stringBuffer.toString();
	}

	public String getActType() {
		return actType;
	}	
}
