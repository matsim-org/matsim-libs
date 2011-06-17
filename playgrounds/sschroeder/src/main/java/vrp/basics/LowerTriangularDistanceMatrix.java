package vrp.basics;

import vrp.api.Costs;
import vrp.api.Node;

/**
 * 
 * @author stefan schroeder
 *
 */

public class LowerTriangularDistanceMatrix implements Costs {

	private double[][] distanceMatrix;
	
	public LowerTriangularDistanceMatrix(double[][] distanceMatrix) {
		this.distanceMatrix = distanceMatrix;
	}


	public Double getCost(Node from, Node to) {
		if(from.getMatrixId() < to.getMatrixId()){
			return distanceMatrix[to.getMatrixId()][from.getMatrixId()];
		}
		else{
			return distanceMatrix[from.getMatrixId()][to.getMatrixId()];
		}
	}


	public Double getDistance(Node from, Node to) {
		return getCost(from,to);
	}


	public Double getTime(Node from, Node to) {
		return getCost(from,to);
	}

}
