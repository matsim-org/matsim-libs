package playground.andreas.intersection.tl;

import org.matsim.network.Link;
import org.matsim.utils.geometry.shared.Coord;

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
		return myCalculateAngle.calculateLeftLane();		
	}

	private Link calculateLeftLane() {
		
		Coord coordInLink = getVector(this.inLink);
		double thetaInLink = Math.atan2(coordInLink.getY(), coordInLink.getX());
		
		Link leftLane = null;
		double thetaLeftLane = Double.NEGATIVE_INFINITY;
				
		for (Link outLink : this.inLink.getToNode().getOutLinks().values()) {
			
			if (!(outLink.getToNode().equals(this.inLink.getFromNode()))){
				
				Coord coordOutLink = getVector(outLink);
				double thetaOutLink = Math.atan2(coordOutLink.getY(), coordOutLink.getX());
				
				double thetaDiff = thetaOutLink - thetaInLink;
				
				if (thetaDiff < -Math.PI){
					thetaDiff += 2 * Math.PI;
				} else if (thetaDiff > Math.PI){
					thetaDiff -= 2 * Math.PI;
				} else {

				}
				
				if (thetaDiff > thetaLeftLane){
					thetaLeftLane = thetaDiff;
					leftLane = outLink;
				}				
			}			
		}
		System.out.println("t LeftLane: " + thetaLeftLane);
		return leftLane;
	}
	
	private Coord getVector(Link link){
		double x = link.getToNode().getCoord().getX() - link.getFromNode().getCoord().getX();
		double y = link.getToNode().getCoord().getY() - link.getFromNode().getCoord().getY();		
		return new Coord(x, y);
	}
	
}
