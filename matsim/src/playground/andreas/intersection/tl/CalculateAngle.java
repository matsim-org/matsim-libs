package playground.andreas.intersection.tl;

import java.util.TreeMap;

import org.matsim.network.Link;
import org.matsim.utils.geometry.CoordImpl;

/**
 * 
 * @author aneumann
 *
 */
public class CalculateAngle {

	private Link inLink;

	private CalculateAngle(Link inLink){		
		this.inLink = inLink;
	}
	
	/**
	 * Calculates the most 'left' outLink for a given inLink.
	 * That's the link a driver would refer to when turning left (no u-turn),
	 * even if there is only one link going to the right.
	 * 
	 * @param inLink The inLink given
	 * @return outLink, or null if there is only one outLink back to the inLinks fromNode.
	 */
	public static Link getLeftLane(Link inLink){
		
		CalculateAngle myCalculateAngle = new CalculateAngle(inLink);
		TreeMap<Double, Link> result = myCalculateAngle.calculateOutLinksSortedByAngle();

		if (result.size() == 0){
			return null;
		} else {
			return result.get(result.firstKey());
		}
	}
	
	/**
	 * Calculates the orientation of the outLinks for a given inLink
	 * beginning from the right. The most 'left' outLink comes last.
	 * backLink is ignored
	 * 
	 * @param inLink The inLink given
	 * @return Collection of outLinks, or an empty collection, if there is only
	 * one outLink back to the inLinks fromNode.
	 */
	public static TreeMap<Double, Link> getOutLinksSortedByAngle(Link inLink){
		CalculateAngle myCalculateAngle = new CalculateAngle(inLink);
		return myCalculateAngle.calculateOutLinksSortedByAngle();
	}
	
	private TreeMap<Double, Link> calculateOutLinksSortedByAngle() {
		
		CoordImpl coordInLink = getVector(this.inLink);
		double thetaInLink = Math.atan2(coordInLink.getY(), coordInLink.getX());
		
		TreeMap<Double, Link> leftLane = new TreeMap<Double, Link>();
						
		for (Link outLink : this.inLink.getToNode().getOutLinks().values()) {
			
			if (!(outLink.getToNode().equals(this.inLink.getFromNode()))){
				
				CoordImpl coordOutLink = getVector(outLink);
				double thetaOutLink = Math.atan2(coordOutLink.getY(), coordOutLink.getX());
				
				double thetaDiff = thetaOutLink - thetaInLink;
				
				if (thetaDiff < -Math.PI){
					thetaDiff += 2 * Math.PI;
				} else if (thetaDiff > Math.PI){
					thetaDiff -= 2 * Math.PI;
				}
				
				leftLane.put(Double.valueOf(-thetaDiff), outLink);
				
			}			
		}
		
		return leftLane;
	}	
	
	private CoordImpl getVector(Link link){
		double x = link.getToNode().getCoord().getX() - link.getFromNode().getCoord().getX();
		double y = link.getToNode().getCoord().getY() - link.getFromNode().getCoord().getY();		
		return new CoordImpl(x, y);
	}
	
}
