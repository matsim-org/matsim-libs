package pedCA.agents;

import pedCA.environment.grid.GridPoint;
import pedCA.utility.Lottery;

public class Shadow extends PhysicalObject{
	private final int step;
	private final int duration;
	private final int pedestrianId;
	
	public Shadow(int step, GridPoint position, int pedestrianId, double duration){
		this.step = step;
		this.position = position;
		this.pedestrianId = pedestrianId;
		if (Lottery.simpleExtraction(duration - (int)duration))
			this.duration = (int)duration + 1;
		else
			this.duration = (int)duration;
	}
	
	public int getExpirationTime(){
		return step+duration;
	}
	
	public int getStep() {
		return step;
	}

	public int getDuration() {
		return duration;
	}

	public int getPedestrianId() {
		return pedestrianId;
	}
	
	@Override
	public String toString(){
		return "shadow";
	}
}
