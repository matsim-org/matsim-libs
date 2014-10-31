package matsimConnector.environment;

import matsimConnector.agents.Pedestrian;
import matsimConnector.utility.MathUtility;
import pedCA.environment.grid.GridPoint;
import pedCA.environment.grid.PedestrianGrid;
import pedCA.utility.RandomExtractor;

public class TransitionArea extends PedestrianGrid {

	private int rotation;
	private GridPoint environmentRef;	//coordinates of the top cell of the transition Area in the real grid
	private GridPoint transAreaRef;
	
	public TransitionArea(int rows, int cols, GridPoint realEnvironmentRef) {
		this(rows,cols,0, realEnvironmentRef);
		
	}
	
	public TransitionArea(int rows, int cols, int rotation, GridPoint environmentRef) {
		super(rows,cols);
		this.rotation = rotation;
		this.environmentRef = environmentRef;
		this.transAreaRef = new GridPoint(cols-1,0);
		MathUtility.rotate(transAreaRef, this.rotation);
	}
	
	public boolean acceptPedestrians(){
		//TODO VERIFY IF IT CAN HOST OTHER PEDS
		return true;
	}
	
	public boolean isAtBorder(Pedestrian pedestrian){
		GridPoint position = pedestrian.getPosition();
		return position.getX() == getColumns() && this.get(position).contains(pedestrian);
	}
	
	//TODO TEST
	public GridPoint convertTAPosToEnvPos(GridPoint tAPosition){
		MathUtility.rotate(tAPosition, rotation);
		int x_r = tAPosition.getX()-transAreaRef.getX()+environmentRef.getX();
		int y_r = tAPosition.getY()-transAreaRef.getY()+environmentRef.getY();
		GridPoint result = new GridPoint(x_r,y_r);
		return result;
	}
	
	public GridPoint convertEnvPosToTAPos(GridPoint envPosition){
		int x_ta = envPosition.getX()+transAreaRef.getX()-environmentRef.getX();
		int y_ta = envPosition.getY()+transAreaRef.getY()-environmentRef.getY();
		GridPoint result = new GridPoint(x_ta,y_ta);
		MathUtility.rotate(result, 360-rotation);
		return result;
	}

	public GridPoint calculateEnterPosition(){
		return new GridPoint(RandomExtractor.nextInt(getColumns()),RandomExtractor.nextInt(getRows()));
	}

	public double getSFFValue(GridPoint position, boolean destinationReached) {
		if (destinationReached)
			return position.getX();
		else
			return getColumns()-1-position.getX();
	}
	
}
