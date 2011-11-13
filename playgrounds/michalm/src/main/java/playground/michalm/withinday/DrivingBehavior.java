package playground.michalm.withinday;

public interface DrivingBehavior {

	void doSimStep(DrivingWorld drivingWorld);
	
	void drivingEnded(DrivingWorld drivingWorld);
}
