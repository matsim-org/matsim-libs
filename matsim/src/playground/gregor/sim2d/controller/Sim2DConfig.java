package playground.gregor.sim2d.controller;

//TODO make a config group instead of using this hard coded static stuff!!!
@Deprecated
public class Sim2DConfig {

	public static double STATIC_FORCE_RESOLUTION = 0.25;
	public static final double TIME_STEP_SIZE = 0.1;
	
	public static final double Bp = 1.0;
	public static final double Bw = 1;
	public static final double App = 600.;
	public static final double Apw = 600.;
	public static final double tau = 1;
}
