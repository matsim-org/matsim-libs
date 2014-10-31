package matsimConnector.utility;

public class Constants {

	public static final Object CA_MOBSIM_MODE = "MobsimCA";
	public static final String CA_LINK_MODE = "walkCA";
	public static final String TO_Q_LINK_MODE = "CA->Q";
	public static final String TO_CA_LINK_MODE = "Q->CA";
	
	public static final double CA_CELL_SIDE = 0.4;
	public static final double CA_STEP_DURATION = 0.3;
	public static final Double PEDESTRIAN_SPEED = CA_CELL_SIDE/CA_STEP_DURATION;
	
	/** name to use to add CAScenario to a matsim scenario as a scenario element **/ 
	public static final String CASCENARIO_NAME = "CAScenario";
	
	public static Double FLOPW_CAP_PER_METER_WIDTH = 1.2;
	
	public static final Double TRANSITION_AREA_LENGTH = 2.;
	public static final Double FAKE_LINK_WIDTH = 5.;
	public static final Double TRANSITION_LINK_LENGTH = TRANSITION_AREA_LENGTH/2.;
	public static final int TRANSITION_AREA_COLUMNS = (int)(TRANSITION_AREA_LENGTH/CA_CELL_SIDE);
	
}
