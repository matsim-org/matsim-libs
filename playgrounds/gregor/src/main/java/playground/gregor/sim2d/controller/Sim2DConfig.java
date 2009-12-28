package playground.gregor.sim2d.controller;

//TODO make a config group instead of using this hard coded static stuff!!!
@Deprecated
public class Sim2DConfig {

	public static double STATIC_FORCE_RESOLUTION = 0.05;
	public static final double TIME_STEP_SIZE = 0.1;
	
	public static final double Bp = 1.0;
	public static final double Bw = 0.75;
	public static final double App = 600.;
	public static final double Apw = 100.;
	public static final double tau = 1;
	
	public static final String STATIC_FORCE_FIELD_FILE = "../../inputs/networks/staticForceField.xml.gz";
	public static final boolean LOAD_STATIC_FORCE_FIELD_FROM_FILE = false;
}
