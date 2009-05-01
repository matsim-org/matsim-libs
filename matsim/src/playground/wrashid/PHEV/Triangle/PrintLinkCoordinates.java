package playground.wrashid.PHEV.Triangle;

import org.matsim.api.core.v01.ScenarioLoader;
import org.matsim.core.api.network.Link;

public class PrintLinkCoordinates {

	public static void main(String[] args) throws Exception {
		ScenarioLoader sl = new ScenarioLoader(args[0]);
		sl.loadNetwork();
		
		System.out.println("linkId\tx\ty");
		for (Link link : sl.getScenario().getNetwork().getLinks().values()){
			System.out.println(link.getId().toString() +"\t"+ getXCoordinate(link) +"\t"+  getYCoordinate(link));
		}

	}
	
	public static double getXCoordinate(Link link){
		return (link.getFromNode().getCoord().getX()+ link.getToNode().getCoord().getX())/2;
	}
	
	public static double getYCoordinate(Link link){
		return (link.getFromNode().getCoord().getY()+ link.getToNode().getCoord().getY())/2;
	}
	
}
