package scenarios;

import java.io.IOException;

import pedCA.context.Context;
import pedCA.environment.grid.EnvironmentGrid;
import pedCA.environment.markers.MarkerConfiguration;
import pedCA.environment.markers.Start;

public class ContextGenerator {
	public static void createAndSaveBidCorridorContext(String path, int rows, int cols, int populationSize){
		Context context = getBidCorridorContext(rows, cols, populationSize);
		try {
			context.saveConfiguration(path);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static Context getBidCorridorContext(int rows, int cols, int populationSize) {
		EnvironmentGrid environmentGrid = new EnvironmentGrid(rows, cols);
		EnvironmentGenerator.initCorridorWithObstacles(environmentGrid);
		MarkerConfiguration markerConfiguration = new MarkerConfiguration();
		markerConfiguration.addDestination(EnvironmentGenerator.getCorridorEastDestination(environmentGrid));
		markerConfiguration.addDestination(EnvironmentGenerator.getCorridorWestDestination(environmentGrid));
		Start startW = EnvironmentGenerator.getCorridorWestStart(environmentGrid);
		startW.setTotalPedestrians(populationSize);
		Start startE = EnvironmentGenerator.getCorridorEastStart(environmentGrid);
		startE.setTotalPedestrians(populationSize);
		markerConfiguration.addStart(startE);
		markerConfiguration.addStart(startW);
		return new Context(environmentGrid, markerConfiguration);
	}

	public static Context getCorridorContext(int rows, int cols, int populationSize){
		EnvironmentGrid environmentGrid = new EnvironmentGrid(rows, cols);
		EnvironmentGenerator.initCorridorWithWalls(environmentGrid);
		MarkerConfiguration markerConfiguration = new MarkerConfiguration();
		markerConfiguration.addDestination(EnvironmentGenerator.getCorridorEastDestination(environmentGrid));
		Start start = EnvironmentGenerator.getCorridorWestStart(environmentGrid);
		start.setTotalPedestrians(populationSize);
		markerConfiguration.addStart(start);
		return new Context(environmentGrid, markerConfiguration);
	}
}
