package pedCA.utility;

public class Constants {
	//General useful constants
	public static final double SQRT2 = Math.sqrt(2);
	
	//Constant for the random seed
	public static long RANDOM_SEED = 42;
	
	//Constants for environment
	public static final int ENV_OBSTACLE = -1;
	public static final int ENV_TACTICAL_DESTINATION = -2;
	public static final int ENV_STAIRS_BORDER = -4;
	public static final int ENV_WALKABLE_CELL = 0;
	public static final double MAX_FF_VALUE = Double.POSITIVE_INFINITY;
	
	//Constants for Conflict Management
	public static final double FRICTION_PROBABILITY = 0.;

	public static final double CELL_SIZE = matsimConnector.utility.Constants.CA_CELL_SIDE;
	public static final double STEP_DURATION = matsimConnector.utility.Constants.CA_STEP_DURATION;

	public static final int SHADOWS_LIFE = 2;

	public static final double SHADOWS_PROBABILITY = 1.;

	public static final int STEP_FOR_BIDIRECTIONAL_SWAPPING = 2;

	public static final double DENSITY_GRID_RADIUS = 1.2;


	
	//Constants for Pedestrian Model
	public static Double KS = 6.0;
	public static Double PHI = 1.0;
}
