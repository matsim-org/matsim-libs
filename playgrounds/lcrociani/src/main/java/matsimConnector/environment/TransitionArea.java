package matsimConnector.environment;

import java.util.ArrayList;

import matsimConnector.agents.Pedestrian;
import matsimConnector.utility.MathUtility;
import pedCA.environment.grid.GridPoint;
import pedCA.environment.grid.PedestrianGrid;
import pedCA.environment.markers.TacticalDestination;
import pedCA.utility.Lottery;

public class TransitionArea extends PedestrianGrid {

	private int rotation;
	private GridPoint environmentRef;	//coordinates of the top cell of the transition Area in the real grid
	private GridPoint transAreaRef;
	private TacticalDestination destinationRef;
	private ArrayList<GridPoint> positionsForGeneration;
	
	public TransitionArea(int rows, int cols, TacticalDestination destination){
		this(rows, cols, destination.getRotation(),destination.getEnvironmentRef());
		this.destinationRef = destination;
	}
	
	public TransitionArea(int rows, int cols, GridPoint realEnvironmentRef) {
		this(rows,cols,0, realEnvironmentRef);
	}
	
	public TransitionArea(int rows, int cols, int rotation, GridPoint environmentRef) {
		super(rows,cols);
		this.rotation = rotation;
		this.environmentRef = environmentRef;
		this.transAreaRef = new GridPoint(cols-1,0);
		MathUtility.rotate(transAreaRef, this.rotation);
		calculatePositionsForGeneration();
	}
	
	//TODO OPTIMIZE THIS
	public boolean acceptPedestrians(){
		return getFreePositions(positionsForGeneration).size()>0;
	}
	
	public boolean isAtBorder(Pedestrian pedestrian){
		GridPoint position = pedestrian.getPosition();
		return isAtBorder(position) && this.get(position).contains(pedestrian);
	}
	
	public boolean isAtBorder(GridPoint position){
		return position.getX() == getColumns()-1;
	}
	
	//TODO TEST
	public GridPoint convertTAPosToEnvPos(GridPoint tAPosition){
		GridPoint result = new GridPoint(tAPosition.getX(),tAPosition.getY());
		MathUtility.rotate(result, rotation);
		int x_r = result.getX()-transAreaRef.getX()+environmentRef.getX();
		int y_r = result.getY()-transAreaRef.getY()+environmentRef.getY();
		result.setX(x_r); result.setY(y_r);
		return result;
	}
	
	public GridPoint convertEnvPosToTAPos(GridPoint envPosition){
		GridPoint result = new GridPoint(envPosition.getX(),envPosition.getY());
		int x_ta = result.getX()+transAreaRef.getX()-environmentRef.getX();
		int y_ta = result.getY()+transAreaRef.getY()-environmentRef.getY();
		result.setX(x_ta); result.setY(y_ta);
		MathUtility.rotate(result, 360-rotation);
		return result;
	}

	public GridPoint calculateEnterPosition(){
		GridPoint result = Lottery.extractObject(getFreePositions(positionsForGeneration));
		return result;
	}

	public double getSFFValue(GridPoint position, boolean destinationReached) {
		if (destinationReached)
			return position.getX();
		else
			return getColumns()-1-position.getX();
	}
	
	public TacticalDestination getReferenceDestination(){
		return destinationRef;
	}
	
	/**
	 * returns all free positions useful for the generation.
	 * The last column, which is overimposed with the environment, is not considered.
	 * */
	private ArrayList<GridPoint> calculatePositionsForGeneration(){
		positionsForGeneration = new ArrayList<GridPoint>();
		for (int i=0;i<cells.size();i++)
			for (int j=0;j<cells.get(i).size()-1;j++)
				if (get(i,j).size()==0)
					positionsForGeneration.add(new GridPoint(j,i));
		return positionsForGeneration;
	}

	public ArrayList<GridPoint> getPositionsForGeneration() {
			return positionsForGeneration;
	}
}
