package pedCA.engine;

import java.util.ArrayList;

import pedCA.context.Context;
import pedCA.environment.grid.ActiveGrid;
import pedCA.environment.grid.PedestrianGrid;

@SuppressWarnings("rawtypes")
public class GridsUpdater {
	private ArrayList<ActiveGrid> activeGrids;
	
	public GridsUpdater(Context context) {
		this.activeGrids = new ArrayList<ActiveGrid>();
		for (PedestrianGrid pedestrianGrid : context.getPedestrianGrids())
			activeGrids.add(pedestrianGrid);
	}

	public void step(){
		for (ActiveGrid activeGrid : activeGrids){
			activeGrid.update();
		}
	}
	
}
