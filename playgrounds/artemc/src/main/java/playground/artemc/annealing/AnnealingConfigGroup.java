package playground.artemc.annealing;

import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Map;

/**
 * Created by artemc on 28/1/15.
 */
public class AnnealingConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "annealing";

	final private static String START_PROPORTION = "startProportion";
	private String startProportion = null;

	final private static String END_PROPORTION = "endProportion";
	private String endProportion = null;

	final private static String ANNEAL_TYPE = "annealType";
	private String annealType = null;

	final private static String GEOMETRIC_FACTOR = "geometricFactor";
	private String geoFactor = null;

	final private static String HALF_LIFE = "halfLife";
	private String halfLife = null;

	private static String modName = "SimpleAnnealer";

	static double currentProportion = 0.1;
	static double slope = -1;
	static int currentIter = 0;
	static boolean isGeometric = false;
	static boolean isExponential;
	static boolean annealSwitch = true;

	public AnnealingConfigGroup() {
		super(GROUP_NAME);
	}
	
	@StringGetter(ANNEAL_TYPE)
	public String getAnnealType() {
		return this.annealType;
	}
	@StringSetter(ANNEAL_TYPE)
	public void setAnnealType(final String annealType) {
		this.annealType = annealType;
	}

	@StringGetter(START_PROPORTION)
	public String getStartProportion() {
		return this.startProportion;
	}
	@StringSetter(START_PROPORTION)
	public void setStartProportion(final String startProportion) {
		this.startProportion = startProportion;
	}

	@StringGetter(END_PROPORTION)
	public String getEndProportion() {
		return this.endProportion;
	}
	@StringSetter(END_PROPORTION)
	public void setEndProportion(final String endProportion) {
		this.endProportion = endProportion;
	}

	@Override
	public Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		map.put(ANNEAL_TYPE, " type of annealing used to continuously lower the replanning rates in order to guarantee a smoother conversion and transition from choice generation to choice evaluation phase.");
		map.put(START_PROPORTION," total percentage of agents to be selected for replanning at the start of the simulation.");
		map.put(END_PROPORTION," total percentage of agents to be selected for replanning at the end of the simulation.");
		return map;
	}

}
