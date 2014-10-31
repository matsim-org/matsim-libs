package scenarios;

import java.util.ArrayList;

import pedCA.environment.grid.EnvironmentGrid;
import pedCA.environment.grid.GridPoint;
import pedCA.environment.markers.Destination;
import pedCA.environment.markers.Start;
import pedCA.environment.markers.TacticalDestination;
import pedCA.environment.network.Coordinates;
import pedCA.utility.Constants;

public class EnvironmentGenerator {

	public static void initCorridor(EnvironmentGrid environment){
		for (int row = 0; row<environment.getRows(); row++)
			for(int col = 0; col<environment.getColumns();col++)
				if (row==0||row==environment.getRows()-1)
					environment.setCellValue(row, col, Constants.ENV_OBSTACLE);
				else
					environment.setCellValue(row, col, Constants.ENV_WALKABLE_CELL);
	}

	public static Destination getCorridorEastDestination(EnvironmentGrid environment){
		ArrayList <GridPoint>cells = generateColumn(new GridPoint(environment.getColumns()-1,1),new GridPoint(environment.getColumns()-1,environment.getRows()-2));	
		return new TacticalDestination(new Coordinates(0.,0.),cells);
	}
	
	public static Destination getCorridorWestDestination(EnvironmentGrid environment){
		ArrayList <GridPoint>cells = generateColumn(new GridPoint(0,1),new GridPoint(0,environment.getRows()-2));	
		return new TacticalDestination(new Coordinates(10.,10.),cells);
	}
	
	public static Start getCorridorEastStart(EnvironmentGrid environment){
		ArrayList <GridPoint>cells = generateColumn(new GridPoint(environment.getColumns()-1,1),new GridPoint(environment.getColumns()-1,environment.getRows()-2));
		return new Start(cells);
	}
	
	public static Start getCorridorWestStart(EnvironmentGrid environment){
		ArrayList <GridPoint>cells = generateColumn(new GridPoint(0,1),new GridPoint(0,environment.getRows()-2));
		return new Start(cells);
	}
	
	public static ArrayList<GridPoint> generateColumn(GridPoint start, GridPoint end){
		ArrayList<GridPoint> result = new ArrayList<GridPoint>();
		for(int i = start.getY(); i<=end.getY();i++)
			result.add(new GridPoint(start.getX(),i));
		return result;
	}
	
	public static ArrayList<GridPoint> generateRow(GridPoint start, GridPoint end){
		ArrayList<GridPoint> result = new ArrayList<GridPoint>();
		for(int j = start.getX(); j<=end.getX();j++)
			result.add(new GridPoint(j,start.getY()));
		return result;
	}
	
}
