package pedCA.environment.grid;

import java.io.IOException;

import pedCA.environment.grid.neighbourhood.PedestrianFootprint;
import pedCA.utility.Constants;
import pedCA.utility.Distances;


public class DensityGrid extends Grid<Double> {

	private final PedestrianFootprint pedestrianFootprint;
	
	public DensityGrid(int rows, int cols) {
		super(rows, cols);
		this.pedestrianFootprint = new PedestrianFootprint(Constants.DENSITY_GRID_RADIUS);
	}

	protected void diffuse(GridPoint position){
		for (GridPoint shift : pedestrianFootprint.getValuesMap().keySet()){
			GridPoint positionToWrite = Distances.gridPointDifference(position, shift);
			if (neighbourCondition(positionToWrite.getY(), positionToWrite.getX())){
				Double oldValue =  get(positionToWrite).get(0);
				if (oldValue == null)
					get(positionToWrite).add(pedestrianFootprint.getValuesMap().get(shift));
				else
					get(positionToWrite).set(0, oldValue + pedestrianFootprint.getValuesMap().get(shift));
			}
		}
	}
	
	protected void remove(GridPoint position){
		for (GridPoint shift : pedestrianFootprint.getValuesMap().keySet()){
			GridPoint positionToWrite = Distances.gridPointDifference(position, shift);
			if (neighbourCondition(positionToWrite.getY(), positionToWrite.getX())){
				Double oldValue =  get(positionToWrite).get(0);
				get(positionToWrite).set(0, oldValue - pedestrianFootprint.getValuesMap().get(shift));
			}
		}
	}
	
	public double getDensityAt(GridPoint position){
		double deltaArea = 0;
		double cellArea = Math.pow(Constants.CELL_SIZE, 2);
		for (GridPoint shift : pedestrianFootprint.getValuesMap().keySet()){
			GridPoint positionToWrite = Distances.gridPointDifference(position, shift);
			if (!neighbourCondition(positionToWrite.getY(), positionToWrite.getX())){
				deltaArea+=cellArea;
			}
		}
		double densityValue = get(position).get(0);
		double footprintArea = pedestrianFootprint.getArea();
		densityValue = densityValue*footprintArea/(footprintArea-deltaArea);
		return densityValue;
	}
	
	@Override
	protected void loadFromCSV(String fileName) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void saveCSV(String path) throws IOException {
		// TODO Auto-generated method stub
		
	}

	

}
