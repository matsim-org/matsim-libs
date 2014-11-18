package pedCA.agents;

import pedCA.environment.grid.GridPoint;

public abstract class PhysicalObject {
	protected GridPoint position;
	
	public GridPoint getPosition() {
		return position;
	}
}
