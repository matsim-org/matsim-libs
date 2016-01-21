package pedCA.environment.grid;

import java.util.ArrayList;

import pedCA.agents.Agent;
import pedCA.agents.PhysicalObject;
import pedCA.agents.Shadow;
import pedCA.utility.Constants;

public class PedestrianGrid extends ActiveGrid<PhysicalObject>{
	private ArrayList<Shadow> shadows;
	private final DensityGrid densityGrid;
	
	public PedestrianGrid(int rows, int cols, EnvironmentGrid environmentGrid){
		super(rows, cols);		
		this.shadows = new ArrayList<Shadow>();
		this.densityGrid = new DensityGrid(rows, cols, environmentGrid);
	}
		
	public void moveTo(Agent pedestrian, GridPoint newPos) {
		GridPoint oldPos = pedestrian.getPosition();
		removePedestrian(oldPos, pedestrian);
		//if (Lottery.simpleExtraction(Constants.SHADOWS_PROBABILITY))
			generateShadow(pedestrian);
		addPedestrian(newPos,pedestrian);
	}
	
	public void moveToWithoutShadow(Agent pedestrian, GridPoint newPos) {
		GridPoint oldPos = pedestrian.getPosition();
		removePedestrian(oldPos, pedestrian);
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
	
	public double getPedestrianDensity(GridPoint position){
		return densityGrid.getDensityAt(position);
	}
	
	public void addPedestrian(GridPoint position, Agent pedestrian){
		get(position.getY(),position.getX()).add(pedestrian);
		densityGrid.diffuse(position);
	}
	
	public void removePedestrian(GridPoint position, Agent pedestrian){
		get(position.getY(),position.getX()).remove(pedestrian);
		densityGrid.remove(position);
	}
	
	private void generateShadow(Agent pedestrian) {
		GridPoint position = pedestrian.getPosition();
		//THIS IS FOR THE SHIFT OF THE DENSITY PERCEPTION
		//GridPoint position = pedestrian.getShiftedPosition();
		double pedestrianDensity = getPedestrianDensity(position);
		double shadow_life = Math.pow(pedestrianDensity*.61,1.45)*0.4;//Math.pow(pedestrianDensity*.61,1.43)*0.39;
		shadow_life = shadow_life/Constants.STEP_DURATION;
		Shadow shadow = new Shadow(this.step, position , pedestrian.getID(), shadow_life);
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
	
	public ArrayList<GridPoint> getFreePositions(ArrayList<GridPoint> cells){
		ArrayList<GridPoint> result = new ArrayList<GridPoint>();
		for (GridPoint p : cells)
			if (!isOccupied(p))
				result.add(p);
		return result;
	}

	public Agent getPedestrian(GridPoint neighbour) {
		return (Agent)get(neighbour).get(0);
	}
	
	public boolean containsPedestrian(GridPoint gp){
		return isOccupied(gp) && get(gp).get(0) instanceof Agent;
	}

	public boolean isWalkable(GridPoint shiftedPosition) {
		return densityGrid.neighbourCondition(shiftedPosition.getY(),shiftedPosition.getX());
	}
}