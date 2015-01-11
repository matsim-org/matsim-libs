package playground.sergioo.typesPopulation2013.replanning;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.selectors.GenericPlanSelector;
import org.matsim.core.replanning.selectors.WorstPlanForRemovalSelector;
import playground.sergioo.typesPopulation2013.population.PersonImplPops;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class StrategyManagerPops extends StrategyManager {
	
	private final Map<Id<Population>, ArrayList<PlanStrategy>> strategies = new HashMap<Id<Population>, ArrayList<PlanStrategy>>();
	private final Map<Id<Population>, ArrayList<Double>> weights = new HashMap<Id<Population>, ArrayList<Double>>();
	private Map<Id<Population>, Double> totalWeights = new HashMap<Id<Population>, Double>();
	private Map<Id<Population>, Integer> maxPlansPerAgent = new HashMap<Id<Population>, Integer>();
	private Map<Id<Population>, GenericPlanSelector<Plan, Person>> removalPlanSelector = new HashMap<Id<Population>, GenericPlanSelector<Plan, Person>>();
	private final TreeMap<Integer, Map<String, Map<PlanStrategy, Double>>> changeRequests = new TreeMap<Integer, Map<String, Map<PlanStrategy, Double>>>();
	/**
	 * chooses a (weight-influenced) random strategy
	 *
	 * @param person The person for which the strategy should be chosen
	 * @return the chosen strategy
	 */
	public PlanStrategy chooseStrategy(final Person person) {
		double rnd = MatsimRandom.getRandom().nextDouble() * this.totalWeights.get(((PersonImplPops)person).getPopulationId());
		double sum = 0.0;
		for (int i = 0, max = this.weights.get(((PersonImplPops)person).getPopulationId()).size(); i < max; i++) {
			sum += this.weights.get(((PersonImplPops)person).getPopulationId()).get(i).doubleValue();
			if (rnd <= sum) {
				return this.strategies.get(((PersonImplPops)person).getPopulationId()).get(i);
			}
		}
		return null;
	}
	
	@Override
	protected void beforePopulationRunHook(Population population, ReplanningContext replanningContext) {
		for(ArrayList<PlanStrategy> strategies:this.strategies.values())
			for(PlanStrategy strategy:strategies)
				strategy.init(replanningContext);
	}
	
	@Override
	protected void afterRunHook( Population population ) {
		for(ArrayList<PlanStrategy> strategies:this.strategies.values())
			for(PlanStrategy strategy:strategies)
				strategy.finish();
	}
	/**
	 * Adds a strategy to this manager with the specified weight. This weight
	 * compared to the sum of weights of all strategies in this manager defines
	 * the probability this strategy will be used for an agent.
	 *
	 * @param strategy
	 * @param weight
	 */
	public final void addStrategy(final PlanStrategy strategy, final double weight, final Id<Population> populationId) {
		ArrayList<PlanStrategy> strategies = this.strategies.get(populationId);
		if(strategies==null) {
			strategies = new ArrayList<PlanStrategy>();
			this.strategies.put(populationId, strategies);
			this.weights.put(populationId, new ArrayList<Double>());
			this.totalWeights.put(populationId, 0.0);
		}
		strategies.add(strategy);
		this.weights.get(populationId).add(Double.valueOf(weight));
		this.totalWeights.put(populationId, this.totalWeights.get(populationId)+weight);
	}
	/**
	 * removes the specified strategy from this manager
	 *
	 * @param strategy the strategy to be removed
	 * @return true if the strategy was successfully removed from this manager,
	 * 		false if the strategy was not part of this manager and could thus not be removed.
	 */
	public final boolean removeStrategy(final PlanStrategy strategy, final Id<Population> populationId) {
		int idx = this.strategies.get(populationId).indexOf(strategy);
		if (idx != -1) {
			this.strategies.get(populationId).remove(idx);
			double weight = this.weights.get(populationId).remove(idx).doubleValue();
			this.totalWeights.put(populationId, this.totalWeights.get(populationId)-weight);
			return true;
		}
		return false;
	}

	/**
	 * changes the weight of the specified strategy
	 *
	 * @param strategy
	 * @param newWeight
	 * @return true if the strategy is part of this manager and the weight could
	 * 		be changed successfully, false otherwise.
	 */
	public final boolean changeWeightOfStrategy(final PlanStrategy strategy, final double newWeight, final Id<Population> populationId) {
		int idx = this.strategies.get(populationId).indexOf(strategy);
		if (idx != -1) {
			double oldWeight = this.weights.get(populationId).set(idx, Double.valueOf(newWeight)).doubleValue();
			this.totalWeights.put(populationId, this.totalWeights.get(populationId)+(newWeight - oldWeight));
			return true;
		}
		return false;
	}
	/**
	 * Sets the maximal number of plans an agent can memorize. Setting
	 * maxPlansPerAgent to zero means unlimited memory (only limited by RAM).
	 * Agents can have up to maxPlansPerAgent plans plus one additional one with the
	 * currently modified plan they're trying out.
	 *
	 * @param maxPlansPerAgent
	 */
	public final void setMaxPlansPerAgent(final int maxPlansPerAgent, Id<Population> populationId) {
		this.maxPlansPerAgent.put(populationId, maxPlansPerAgent);
	}
	/**
	 * Schedules a {@link #changeStrategy changeStrategy(Strategy, double)} command for a later iteration. The
	 * change will take place before the strategies are applied.
	 *
	 * @param iteration
	 * @param strategy
	 * @param newWeight
	 */
	public final void addChangeRequest(final int iteration, final PlanStrategy strategy, final double newWeight, String populationId) {
		Integer iter = Integer.valueOf(iteration);
		Map<String, Map<PlanStrategy, Double>> iterationRequests = this.changeRequests.get(iter);
		if (iterationRequests==null) {
			iterationRequests = new HashMap<String, Map<PlanStrategy, Double>>(3);
			this.changeRequests.put(iter, iterationRequests);
		}
		Map<PlanStrategy, Double> iterationRequestsPop = iterationRequests.get(populationId);
		if(iterationRequestsPop==null) {
			iterationRequestsPop = new HashMap<PlanStrategy, Double>(3);
			iterationRequests.put(populationId, iterationRequestsPop);
		}
		iterationRequestsPop.put(strategy, Double.valueOf(newWeight));
	}
	/**
	 * Sets a plan selector to be used for choosing plans for removal, if they
	 * have more plans than the specified maximum. This defaults to
	 * {@link WorstPlanForRemovalSelector}.
	 * <p/>
	 * Thoughts about using the logit-type selectors with negative logit model scale parameter:<ul>
	 * <li> Look at one agent.
	 * <li> Assume she has the choice between <i>n</i> different plans.
	 * <li> (Continuous) fraction <i>f(i)</i> of plan <i>i</i> develops as (master equation) 
	 * <blockquote><i>
	 * df(i)/dt = - p(i) * f(i) + 1/n
	 * </i></blockquote>
	 * where <i>p(i)</i> is from the choice model. 
	 * <li> Steady state solution (<i>df/dt=0</i>) <i> f(i) = 1/n * 1/p(i) </i>.
	 * <li> If <i> p(i) = e<sup>-b*U(i)</sup></i>, then <i> f(i) = e<sup>b*U(i)</sup> / n </i>.  Or in words:
	 * <i><b> If you use a logit model with a minus in front of the beta for plans removal, the resulting steady state distribution is
	 * the same logit model with normal beta.</b></i> 
	 * 
	 * </ul>
	 * The implication seems to be: divide the user-configured beta by two, use one half for choice and the other for plans removal.
	 * <p/>
	 * The path size version still needs to be tested (both for choice and for plans removal).
	 *
	 * @param planSelector
	 *
	 * @see #setMaxPlansPerAgent(int)
	 */
	public final void setPlanSelectorForRemoval(final GenericPlanSelector<Plan, Person> planSelector, Id<Population> populationId) {
		Logger.getLogger(this.getClass()).info("setting PlanSelectorForRemoval to " + planSelector.getClass() ) ;
		this.removalPlanSelector.put(populationId, planSelector);
	}

}
