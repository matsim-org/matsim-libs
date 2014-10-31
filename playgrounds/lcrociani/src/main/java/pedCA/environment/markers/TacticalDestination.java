package pedCA.environment.markers;

import java.util.ArrayList;

import pedCA.environment.grid.GridPoint;
import pedCA.environment.network.Coordinates;
import pedCA.utility.Constants;

public class TacticalDestination extends Destination  {

	private static final long serialVersionUID = 1L;
	private final Coordinates coordinates;
	private double width;
	//for the transition area
	private int rotation = -1;
	private GridPoint environmentRef;
	
	public TacticalDestination(Coordinates coordinates, ArrayList<GridPoint> cells) {
		super(cells);
		this.coordinates = coordinates;
		calculateWidth();
		calculateRotationAndRef();
	}

	//TODO test!!!
	private void calculateRotationAndRef() {
		GridPoint first = getCells().get(0);
		GridPoint second = getCells().get(1);
		GridPoint last = getCells().get(getCells().size()-1);
		if (first.getX()==second.getX() && first.getX() == 0){
			rotation = 0;
			environmentRef = first;
		}
		else if(first.getX()==second.getX() && first.getX() != 0){ //equal x but at the end of the environment grid
			rotation = 180;
			environmentRef = last; 
		}
		else if(first.getY()==second.getY() && first.getY() == 0){
			rotation = 90;
			environmentRef = last;
		}
		else if(first.getY()==second.getY() && first.getX() != 0) {//equal y but at the end of the environment grid
			rotation = 270;
			environmentRef = first;
		}		
	}

	public int getID(){
		return getLevel();
	}

	public Coordinates getCoordinates(){
		return coordinates;
	}
	
	/**
	 * TODO: till now the width is calculated by only considering
	 * cases where the destination represent a perfectly horizontal 
	 * or vertical set of cells
	 * */
	private void calculateWidth(){
		width = cells.size()*Constants.CELL_SIZE;
	}

	public double getWidth() {
		return width;
	}
	
	public GridPoint getEnvironmentRef(){
		return environmentRef;
	}
	
	public int getRotation(){
		return rotation;
	}
}
