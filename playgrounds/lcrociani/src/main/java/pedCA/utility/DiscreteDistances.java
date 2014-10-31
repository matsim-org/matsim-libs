package pedCA.utility;

import pedCA.environment.grid.GridPoint;
import pedCA.output.Log;

public class DiscreteDistances {
	private int radius;
	private Double[][] discreteDistances;
	
	public DiscreteDistances(int radius){
		this.radius = radius;
		initMatrix();
	}
	
	private void initMatrix(){
		int side = radius*2+1;
		discreteDistances = new Double[side][side];
		GridPoint center = new GridPoint(radius,radius);
		for(int i=0;i<discreteDistances.length;i++)
			for(int j=0;j<discreteDistances[i].length;j++)
				discreteDistances[i][j] = Distances.EuclideanDistance(center,new GridPoint(j,i));
	}
	
	public double getDistance(GridPoint center,GridPoint neighbour){
		try{
			return discreteDistances[neighbour.getY()-center.getY()+radius][neighbour.getX()-center.getX()+radius];
		}catch(IndexOutOfBoundsException e){
			Log.warning("DiscreteDistances.getDistance() - neighbour out of the range of precalculated distances");
			return Distances.EuclideanDistance(center, neighbour);
		}
	}
}
