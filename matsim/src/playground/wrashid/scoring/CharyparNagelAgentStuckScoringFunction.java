package playground.wrashid.scoring;

import org.apache.log4j.Logger;
import org.matsim.interfaces.basic.v01.BasicLeg;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.interfaces.core.v01.Route;
import org.matsim.population.ActUtilityParameters;
import org.matsim.scoring.CharyparNagelScoringFunction;
import org.matsim.scoring.CharyparNagelScoringParameters;
import org.matsim.utils.misc.Time;

import playground.wrashid.scoring.interfaces.AgentStuckScoringFunction;
import playground.wrashid.scoring.interfaces.BasicScoringFunction;
import playground.wrashid.scoring.interfaces.LegScoringFunction;

public class CharyparNagelAgentStuckScoringFunction implements AgentStuckScoringFunction, BasicScoringFunction {

	protected final Person person;
	protected final Plan plan;

	protected double score;
	private double lastTime;
	private int index; // the current position in plan.actslegs
	private double firstActTime;
	private final int lastActIndex;

	private static final double INITIAL_LAST_TIME = 0.0;
	private static final int INITIAL_INDEX = 0;
	private static final double INITIAL_FIRST_ACT_TIME = Time.UNDEFINED_TIME;
	private static final double INITIAL_SCORE = 0.0;

	private static int firstLastActWarning = 0;

	/** The parameters used for scoring */
	protected final CharyparNagelScoringParameters params;

	private static final Logger log = Logger.getLogger(CharyparNagelScoringFunction.class);

	public CharyparNagelAgentStuckScoringFunction(final Plan plan, final CharyparNagelScoringParameters params) {
		this.params = params;
		this.reset();

		this.plan = plan;
		this.person = this.plan.getPerson();
		this.lastActIndex = this.plan.getActsLegs().size() - 1;
	}

	public void reset() {
		this.lastTime = INITIAL_LAST_TIME;
		this.index = INITIAL_INDEX;
		this.firstActTime = INITIAL_FIRST_ACT_TIME;
		this.score = INITIAL_SCORE;
	}

	public void agentStuck(final double time) {
		this.lastTime = time;
		this.score += getStuckPenalty();
	}

	public void finish() {

	}

	public double getScore() {
		return this.score;
	}

	private double getStuckPenalty() {
		return this.params.abortedPlanScore;
	}

}
