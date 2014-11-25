package pedCA.utility;

import java.util.HashMap;
import java.util.Map;

import pedCA.environment.grid.GridPoint;

public class DirectionUtility {
	private static DirectionConverter directionConverter = new DirectionConverter();	
	
	public enum Heading{
		N, NE, NW, S, SE, SW, E, W, X
	}
	
	public static Heading convertGridPointToHeading(GridPoint gp){
		if (gp.getX()>1)
			gp.setX(1);
		else if(gp.getX()<-1)
			gp.setX(-1);
		if (gp.getY()>1)
			gp.setY(1);
		else if(gp.getY()<-1)
			gp.setY(-1);
		return directionConverter.gridPointToHeading.get(gp);
	}
	
	public static GridPoint convertHeadingToGridPoint(Heading h){
		return directionConverter.headingToGridPoint.get(h);
	}
}

class DirectionConverter{
	public final Map <GridPoint, DirectionUtility.Heading> gridPointToHeading = new HashMap<GridPoint, DirectionUtility.Heading>();
	public final Map <DirectionUtility.Heading, GridPoint> headingToGridPoint = new HashMap<DirectionUtility.Heading, GridPoint>();
	
	public DirectionConverter(){ 
		gridPointToHeading.put(new GridPoint(0,0),DirectionUtility.Heading.X);
		headingToGridPoint.put(DirectionUtility.Heading.X, new GridPoint(0,0));
		
		gridPointToHeading.put(new GridPoint(1,0),DirectionUtility.Heading.E);
		headingToGridPoint.put(DirectionUtility.Heading.E, new GridPoint(1,0));
		
		gridPointToHeading.put(new GridPoint(1,-1),DirectionUtility.Heading.SE);
		headingToGridPoint.put(DirectionUtility.Heading.SE, new GridPoint(1,-1));
		
		gridPointToHeading.put(new GridPoint(0,-1),DirectionUtility.Heading.S);
		headingToGridPoint.put(DirectionUtility.Heading.S, new GridPoint(0,-1));
		
		gridPointToHeading.put(new GridPoint(-1,-1),DirectionUtility.Heading.SW);
		headingToGridPoint.put(DirectionUtility.Heading.SW, new GridPoint(-1,-1));
		
		gridPointToHeading.put(new GridPoint(-1,0),DirectionUtility.Heading.W);
		headingToGridPoint.put(DirectionUtility.Heading.W, new GridPoint(-1,0));
		
		gridPointToHeading.put(new GridPoint(-1,1),DirectionUtility.Heading.NW);
		headingToGridPoint.put(DirectionUtility.Heading.NW, new GridPoint(-1,1));
		
		gridPointToHeading.put(new GridPoint(0,1),DirectionUtility.Heading.N);
		headingToGridPoint.put(DirectionUtility.Heading.N, new GridPoint(0,1));
		
		gridPointToHeading.put(new GridPoint(1,1),DirectionUtility.Heading.NE);
		headingToGridPoint.put(DirectionUtility.Heading.NE, new GridPoint(1,1));
	}
 
}
