package pedCA.environment.markers;

import java.util.ArrayList;

import pedCA.environment.grid.GridPoint;
import pedCA.environment.network.Coordinates;
import pedCA.utility.Constants;

public class TacticalDestination extends Destination {

	private static final long serialVersionUID = 1L;
	private double width;
	private final Coordinates coordinates;

	public TacticalDestination(Coordinates coordinates, ArrayList<GridPoint> cells) {
		super(cells);
		this.coordinates = coordinates;
		calculateWidth();
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
	
	public int getID(){
		return getLevel();
	}
	
}
