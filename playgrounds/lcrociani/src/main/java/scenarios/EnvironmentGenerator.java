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
				environment.setCellValue(row, col, Constants.ENV_WALKABLE_CELL);
	}
	
	public static void initCorridorWithWalls(EnvironmentGrid environment){
		for (int row = 0; row<environment.getRows(); row++)
			for(int col = 0; col<environment.getColumns();col++)
				if ((col!=0&&col!=environment.getColumns()-1)&&(row==0||row==environment.getRows()-1))
					environment.setCellValue(row, col, Constants.ENV_OBSTACLE);
				else
					environment.setCellValue(row, col, Constants.ENV_WALKABLE_CELL);
	}
	
	public static void initCorridorWithObstacles(EnvironmentGrid environment){
		initCorridor(environment);
		environment.setCellValue(environment.getRows()/2, environment.getColumns()/2, Constants.ENV_OBSTACLE);
	}

	public static Destination getCorridorEastDestination(EnvironmentGrid environment){
		ArrayList <GridPoint>cells = generateColumn(new GridPoint(environment.getColumns()-1,0),new GridPoint(environment.getColumns()-1,environment.getRows()-1));	
		return new TacticalDestination(generateCoordinates(cells),cells);
	}

	public static Destination getCorridorWestDestination(EnvironmentGrid environment){
		ArrayList <GridPoint>cells = generateColumn(new GridPoint(0,0),new GridPoint(0,environment.getRows()-1));	
		return new TacticalDestination(generateCoordinates(cells),cells);
	}
	
	public static Start getCorridorEastStart(EnvironmentGrid environment){
		ArrayList <GridPoint>cells = generateColumn(new GridPoint(environment.getColumns()-1,0),new GridPoint(environment.getColumns()-1,environment.getRows()-1));
		return new Start(cells);
	}
	
	public static Start getCorridorWestStart(EnvironmentGrid environment){
		ArrayList <GridPoint>cells = generateColumn(new GridPoint(0,0),new GridPoint(0,environment.getRows()-1));
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
	
	public static Coordinates generateCoordinates(ArrayList<GridPoint> cells) {
		return generateCoordinates(cells, new GridPoint(0,0));
	}
		
	public static Coordinates generateCoordinates(ArrayList<GridPoint> cells, GridPoint shift) {
		Coordinates result = calculateCentroid(cells);
		result.setX(result.getX()+shift.getX());
		result.setY(result.getY()+shift.getY());
		return result;
	}

	public static Coordinates calculateCentroid(ArrayList<GridPoint> cells) {
		Coordinates result = new Coordinates(0,0);
		for (GridPoint point : cells){
			result.setX(result.getX()+(point.getX()*Constants.CELL_SIZE)+Constants.CELL_SIZE/2);
			result.setY(result.getY()+(point.getY()*Constants.CELL_SIZE)+Constants.CELL_SIZE/2);
		}
		result.setX(result.getX()/cells.size());
		result.setY(result.getY()/cells.size());
		return result;
	}
}
