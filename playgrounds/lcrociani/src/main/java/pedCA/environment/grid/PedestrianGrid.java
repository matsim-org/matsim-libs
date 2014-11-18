package pedCA.environment.grid;

import java.util.ArrayList;

import pedCA.agents.Agent;
import pedCA.agents.PhysicalObject;
import pedCA.agents.Shadow;
import pedCA.utility.Constants;
import pedCA.utility.Lottery;

public class PedestrianGrid extends ActiveGrid<PhysicalObject>{
	private ArrayList<Shadow> shadows;
	
	public PedestrianGrid(int rows, int cols){
		super(rows, cols);		
		this.shadows = new ArrayList<Shadow>();
	}
		
	public void moveTo(Agent pedestrian, GridPoint newPos) {
		GridPoint oldPos = pedestrian.getPosition();
		removePedestrian(oldPos, pedestrian);
		if (Lottery.simpleExtraction(Constants.SHADOWS_PROBABILITY))
			generateShadow(pedestrian);
		addPedestrian(newPos,pedestrian);
	}
	
	@Override
	protected void updateGrid() {
		for (int i=0;i<shadows.size();i++)
			if (step>=shadows.get(i).getExpirationTime()){
				removeShadow(shadows.get(i));
				i--;
			}
	}
	
	public void addPedestrian(GridPoint position, Agent pedestrian){
		get(position.getY(),position.getX()).add(pedestrian);
	}
	
	public void removePedestrian(GridPoint position, Agent pedestrian){
		get(position.getY(),position.getX()).remove(pedestrian);
	}
	
	private void generateShadow(Agent pedestrian) {
		GridPoint position = pedestrian.getPosition();
		Shadow shadow = new Shadow(this.step, position , pedestrian.getID());
		get(position.getY(),position.getX()).add(shadow);
		shadows.add(shadow);
	}
	
	public void removeShadow(Shadow shadow) {
		GridPoint position = shadow.getPosition();
		get(position.getY(),position.getX()).remove(shadow);
		shadows.remove(shadow);
	}

	public boolean isOccupied(int i, int j){
		return get(i,j).size()>0;
	}
	
	public boolean isOccupied(GridPoint p){
		return get(p).size()>0;
	}
	
	public ArrayList<GridPoint> getAllFreePositions(){
		ArrayList<GridPoint> result = new ArrayList<GridPoint>();
		for (int i=0;i<cells.size();i++)
			for (int j=0;j<cells.get(i).size();j++)
				if (!isOccupied(i,j))
					result.add(new GridPoint(j,i));
		return result;
	}
	
	public ArrayList<GridPoint> getFreePositions(ArrayList<GridPoint> cells){
		ArrayList<GridPoint> result = new ArrayList<GridPoint>();
		for (GridPoint p : cells)
			if (!isOccupied(p))
				result.add(p);
		return result;
	}
}