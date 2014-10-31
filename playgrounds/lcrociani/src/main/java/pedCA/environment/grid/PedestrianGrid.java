package pedCA.environment.grid;

import java.util.ArrayList;

import pedCA.agents.Agent;

public class PedestrianGrid extends Grid <Agent>{
	
	public PedestrianGrid(int rows, int cols){
		super(rows, cols);		
	}
	
	public void addPedestrian(GridPoint position, Agent pedestrian){
		get(position.getY(),position.getX()).add(pedestrian);
	}
	
	public void removePedestrian(GridPoint position, Agent pedestrian){
		get(position.getY(),position.getX()).remove(pedestrian);
	}
	
	public void moveTo(Agent pedestrian, GridPoint newPos) {
		GridPoint oldPos = pedestrian.getPosition();
		removePedestrian(oldPos, pedestrian);
		addPedestrian(newPos,pedestrian);
	}

	public boolean isOccupied(GridPoint p){
		return get(p).size()>0;
	}
	
	public ArrayList<GridPoint> freePositions(ArrayList<GridPoint> cells){
		ArrayList<GridPoint> result = new ArrayList<GridPoint>();
		for (GridPoint p : cells)
			if (!isOccupied(p))
				result.add(p);
		return result;
	}
	
	@Override
	public void saveCSV(String path) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void loadFromCSV(String fileName) {
		// TODO Auto-generated method stub
		
	}
}
