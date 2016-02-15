package matsimConnector.utility;

import java.util.ArrayList;
import java.util.List;

public class Constants {

	public static final String CA_MOBSIM_MODE = "MobsimCA";
	public static final String CA_LINK_MODE = "walkCA";
	public static final String WALK_LINK_MODE = "walk";
	public static final String CAR_LINK_MODE = "car";
	public static final String TO_Q_LINK_MODE = "CA->Q";
	public static final String TO_CA_LINK_MODE = "Q->CA";
	
	public static final double CA_CELL_SIDE = 0.4;
	public static final double CA_STEP_DURATION = .3;
	public static final Double PEDESTRIAN_SPEED = CA_CELL_SIDE/CA_STEP_DURATION;
	
	/** this is for the generation of the fundamental diagram of the CA: pedestrian will be kept inside the
	 * CAEnvironment until this time (in seconds). Keep to 0 if you want to run normal simulation.**/
	public static Double CA_TEST_END_TIME = 0.; //1200.;
	
	public static double SIMULATION_DURATION = 20000;
	public static int SIMULATION_ITERATIONS = 50;
	public static boolean SAVE_FRAMES = false;
	
	/** name to use to add CAScenario to a matsim scenario as a scenario element **/ 
	public static final String CASCENARIO_NAME = "CAScenario";
	
	public static final Double FLOPW_CAP_PER_METER_WIDTH = 1.2;
	
	public static final Double TRANSITION_AREA_LENGTH = CA_CELL_SIDE*25;
	public static final Double FAKE_LINK_WIDTH = 10.;
//	public static final Double FAKE_LINK_WIDTH = 1.2;
	public static final Double CA_LINK_LENGTH = 10.;
	public static final Double TRANSITION_LINK_LENGTH = TRANSITION_AREA_LENGTH/2.;
	public static final int TRANSITION_AREA_COLUMNS = (int)(TRANSITION_AREA_LENGTH/CA_CELL_SIDE);
	
	
	public static boolean MARGINAL_SOCIAL_COST_OPTIMIZATION = true;
	
	public static String PATH;
	static {
		String OS = System.getProperty("os.name").toLowerCase();
		if (OS.indexOf("win") >= 0)
			PATH = "C:/Users/Luca/Documents/uni/Dottorato/Juelich/developing_stuff/Test";
		else
			PATH = "/tmp/TestCA";
	}
	public static final String RESOURCE_PATH = "src/main/resources";
	public static final String DEBUG_TEST_PATH = PATH+"/debug";
	public static final String FD_TEST_PATH = PATH+"/FD/";
	public static String INPUT_PATH = DEBUG_TEST_PATH+"/input";
	public static String OUTPUT_PATH = DEBUG_TEST_PATH+"/output";
	public static final String COORDINATE_SYSTEM = "EPSG:3395";
	public static String ENVIRONMENT_FILE = "ABMUS_PG_station_separated.csv";
	public static boolean BRAESS_WL = false;
	public static boolean VIS = true;
	
	public static List<String> stairsLinks;
	static{
		stairsLinks = new ArrayList<String>();
	}
}
