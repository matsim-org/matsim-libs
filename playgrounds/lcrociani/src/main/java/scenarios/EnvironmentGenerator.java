package scenarios;

import java.util.ArrayList;

import matsimConnector.utility.MathUtility;
import pedCA.environment.grid.EnvironmentGrid;
import pedCA.environment.grid.GridPoint;
import pedCA.environment.grid.neighbourhood.Neighbourhood;
import pedCA.environment.markers.Destination;
import pedCA.environment.markers.FinalDestination;
import pedCA.environment.markers.MarkerConfiguration;
import pedCA.environment.markers.Start;
import pedCA.environment.markers.TacticalDestination;
import pedCA.environment.network.Coordinates;
import pedCA.utility.Constants;
import pedCA.utility.NeighbourhoodUtility;

public class EnvironmentGenerator {

	public static void initCorridor(EnvironmentGrid environment){
		for (int row = 0; row<environment.getRows(); row++)
			for(int col = 0; col<environment.getColumns();col++)
				environment.setCellValue(row, col, Constants.ENV_WALKABLE_CELL);
	}
	
	public static void initCorridorWithWalls(EnvironmentGrid environment){
		for (int row = 0; row<environment.getRows(); row++)
			for(int col = 0; col<environment.getColumns();col++)
				if ((row==0||row==environment.getRows()-1))
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
		return new FinalDestination(generateCoordinates(cells),cells);
	}

	public static Destination getCorridorWestDestination(EnvironmentGrid environment){
		ArrayList <GridPoint>cells = generateColumn(new GridPoint(0,0),new GridPoint(0,environment.getRows()-1));	
		return new FinalDestination(generateCoordinates(cells),cells);
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

	public static MarkerConfiguration generateBorderDestinations(EnvironmentGrid environmentGrid) {
		MarkerConfiguration markerConfiguration = new MarkerConfiguration();
		boolean found = false;
		ArrayList<GridPoint> cells = null;
		for (int i=0;i<environmentGrid.getRows();i+=environmentGrid.getRows()-1){
			for (int j=0;j<environmentGrid.getColumns();j++){
				GridPoint cell = new GridPoint(j, i);
				if(environmentGrid.belongsToExit(cell) && !found){
					found = true;
					cells = new ArrayList<GridPoint>();
					cells.add(cell);
				}else if(environmentGrid.belongsToExit(cell) && found){
					cells.add(cell);
				}else if (found){
					if (j>1) //skip corner
						markerConfiguration.addDestination(new FinalDestination(generateCoordinates(cells), cells));
					found = false;
				}
			}
			if (found){
				if (cells.size()>1) //skip corner for the moment
					markerConfiguration.addDestination(new FinalDestination(generateCoordinates(cells), cells));
				found = false;
			}
		}
		cells = null;
		for (int j=0;j<environmentGrid.getColumns();j+=environmentGrid.getColumns()-1){
			for (int i=0;i<environmentGrid.getRows();i++){
				GridPoint cell = new GridPoint(j, i);
				if(environmentGrid.belongsToExit(cell) && !found){
					found = true;
					cells = new ArrayList<GridPoint>();
					cells.add(cell);
				}else if(environmentGrid.belongsToExit(cell) && found){
					cells.add(cell);
				}else if (found){
					if (i==1){ //add corner if it has not been added before
						if	(!markerConfiguration.getDestinations().get(markerConfiguration.getDestinations().size()-1).getCells().contains(cells.get(0)))
							markerConfiguration.addDestination(new FinalDestination(generateCoordinates(cells), cells));
					}else
						markerConfiguration.addDestination(new FinalDestination(generateCoordinates(cells), cells));
					found = false;
				}
			}
			if (found){
				markerConfiguration.addDestination(new FinalDestination(generateCoordinates(cells), cells));
				found = false;
			}
		}
		return markerConfiguration;
	}
	
	public static void addTacticalDestinations(MarkerConfiguration markerConfiguration, EnvironmentGrid environmentGrid){
		ArrayList<GridPoint> consideredCells = new ArrayList<GridPoint>();
		ArrayList<GridPoint> cells = null;
		for (int i=0;i<environmentGrid.getRows();i++){
			for (int j=0;j<environmentGrid.getColumns();j++){
				GridPoint cell = new GridPoint(j, i);
				if (environmentGrid.belongsToTacticalDestination(cell) && !consideredCells.contains(cell)){
					cells = new ArrayList<GridPoint>();
					cells.add(cell);
					consideredCells.add(cell);
					Neighbourhood neighbourhood = NeighbourhoodUtility.calculateVonNeumannNeighbourhood(cell);
					GridPoint difference = null;
					for (GridPoint neighbour : neighbourhood.getObjects()){
						if (!neighbour.equals(cell) && environmentGrid.belongsToTacticalDestination(neighbour)){
							difference = MathUtility.gridPointDifference(cell, neighbour);
							break;
						}
					}
					if (difference != null){
						GridPoint neighbour = MathUtility.gridPointDifference(cell, difference);
						do{
							cell = neighbour;
							cells.add(cell);
							consideredCells.add(cell);
							neighbour = MathUtility.gridPointDifference(cell, difference);
						}while (environmentGrid.belongsToTacticalDestination(neighbour));
					}
					TacticalDestination tacticalDestination = new TacticalDestination(generateCoordinates(cells), cells, environmentGrid.isStairsBorder(cells.get(0)));
					markerConfiguration.addDestination(tacticalDestination);
				}
			}
		}
	}
}
