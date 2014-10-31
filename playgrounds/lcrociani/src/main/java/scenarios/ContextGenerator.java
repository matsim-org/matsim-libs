package scenarios;

import pedCA.context.Context;
import pedCA.environment.grid.EnvironmentGrid;
import pedCA.environment.markers.MarkerConfiguration;
import pedCA.environment.markers.Start;

public class ContextGenerator {
	public static Context getCorridorContext(int rows, int cols, int populationSize){
		EnvironmentGrid environmentGrid = new EnvironmentGrid(rows, cols);
		EnvironmentGenerator.initCorridor(environmentGrid);
		MarkerConfiguration markerConfiguration = new MarkerConfiguration();
		markerConfiguration.addDestination(EnvironmentGenerator.getCorridorEastDestination(environmentGrid));
		Start start = EnvironmentGenerator.getCorridorWestStart(environmentGrid);
		start.setTotalPedestrians(populationSize);
		markerConfiguration.addStart(start);
		return new Context(environmentGrid, markerConfiguration);
	}
}
