package pedCA.utility;

import java.util.HashMap;
import java.util.Map;

import pedCA.environment.grid.GridPoint;

public class DirectionUtility {
	private static DirectionConverter directionConverter = new DirectionConverter();	
	
	public enum Heading{
		N, NE, NO, S, SE, SO, E, O, X
	}
	
	public static Heading convertGridPointToHeading(GridPoint gp){
		return directionConverter.directions.get(gp);
	}
	
}

class DirectionConverter{
	public final Map <GridPoint, DirectionUtility.Heading> directions = new HashMap<GridPoint, DirectionUtility.Heading>();
	
	public DirectionConverter(){ 
		//TODO generate the map
	
	}
 
}
