package pedCA.utility;

import pedCA.environment.grid.GridPoint;
import pedCA.environment.grid.neighbourhood.Neighbourhood;

public class NeighbourhoodUtility {

	public static Neighbourhood calculateMooreNeighbourhood(GridPoint neighbour){
		Neighbourhood result = new Neighbourhood();
		for(int y=neighbour.getY()-1;y<=neighbour.getY()+1;y++)
			for(int x=neighbour.getX()-1;x<=neighbour.getX()+1;x++)
				result.add(new GridPoint(x, y));
		return result;
	}
	
	public static Neighbourhood calculateVonNeumannNeighbourhood(GridPoint neighbour){
		Neighbourhood result = new Neighbourhood();
		for(int y=neighbour.getY()-1;y<=neighbour.getY()+1;y++)
			for(int x=neighbour.getX()-1;x<=neighbour.getX()+1;x++)
				if(x==neighbour.getX()||y==neighbour.getY())
					result.add(new GridPoint(x, y));
		return result;
	}

}
