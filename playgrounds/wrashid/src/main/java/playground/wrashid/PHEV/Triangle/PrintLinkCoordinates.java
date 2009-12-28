package playground.wrashid.PHEV.Triangle;

import org.matsim.core.network.LinkImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;

public class PrintLinkCoordinates {

	public static void main(String[] args) throws Exception {
		ScenarioLoaderImpl sl = new ScenarioLoaderImpl(args[0]);
		sl.loadNetwork();
		
		System.out.println("linkId\tx\ty");
		for (LinkImpl link : sl.getScenario().getNetwork().getLinks().values()){
			System.out.println(link.getId().toString() +"\t"+ getXCoordinate(link) +"\t"+  getYCoordinate(link));
		}

	}
	
	public static double getXCoordinate(LinkImpl link){
		return (link.getFromNode().getCoord().getX()+ link.getToNode().getCoord().getX())/2;
	}
	
	public static double getYCoordinate(LinkImpl link){
		return (link.getFromNode().getCoord().getY()+ link.getToNode().getCoord().getY())/2;
	}
	
}
