package org.matsim.core.replanning.choosers;

import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controller;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.ReplanningUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Strategy chooser which ensures that all agents perform innovative strategies in a balanced manner.
 * That is at any time all agents have either performed n, n+1 or n+2 times an innovative strategy. This difference is never larger than 2.
 * The class also tries to balance the number of innovations in a single iteration.
 */
public class BalancedInnovationStrategyChooser<PL extends BasicPlan, AG extends HasPlansAndId<? extends BasicPlan, AG>> implements StrategyChooser<PL, AG> {

	/**
	 * The total number of agents per subpopulation.
	 */
	private final Object2IntMap<String> total = new Object2IntOpenHashMap<>();

	/**
	 * Number of agents already processed this iteration.
	 */
	private final Object2IntMap<String> seen = new Object2IntOpenHashMap<>();

	/**
	 * Number of agents already decided to innovate this iteration.
	 */
	private final Object2IntMap<String> innovated = new Object2IntOpenHashMap<>();

	/**
	 * Agents that are forced to innovate (per subpopulation).
	 */
	private final Object2IntMap<String> carryOver = new Object2IntOpenHashMap<>();

	/**
	 * The set of indices of agents that have performed an innovative strategy.
	 */
	private final Map<String, IntSet> oneAhead = new HashMap<>();

	/**
	 * The set of indices of agents that have performed an innovative strategy two times.
	 */
	private final Map<String, IntSet> twoAhead = new HashMap<>();

	@Inject
	public BalancedInnovationStrategyChooser(Population population) {

		for (Person person : population.getPersons().values()) {
			String key = PopulationUtils.getSubpopulation(person);
			total.mergeInt(key != null ? key : "__none__", 1, Integer::sum);
		}

		for (String s : total.keySet()) {
			oneAhead.put(s, new IntOpenHashSet());
			twoAhead.put(s, new IntOpenHashSet());
			carryOver.put(s, 0);
		}
	}

	/**
	 * Convenience method to bind this strategy chooser to a Guice binder.
	 *
	 * @param binder Guice binder
	 */
	public static void bind(Binder binder) {
		binder.bind(new TypeLiteral<StrategyChooser<Plan, Person>>() {
		}).to(new TypeLiteral<BalancedInnovationStrategyChooser<Plan, Person>>() {
		}).in(Singleton.class);
	}


	/**
	 * Install this strategy chooser in the given controller.
	 */
	public static void install(Controller controller) {
		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				BalancedInnovationStrategyChooser.bind(binder());
			}
		});
	}

	@Override
	public void beforeReplanning(ReplanningContext replanningContext) {
		innovated.clear();
		seen.clear();
	}

	@Override
	public GenericPlanStrategy<PL, AG> chooseStrategy(HasPlansAndId<PL, AG> person, String subpopulation, ReplanningContext replanningContext, StrategyChooser.Weights<PL, AG> weights) {

		// Two separate arrays for innovation and selection strategies weights
		double[] wInno = new double[weights.size()];
		double totalInno = 0;

		double[] wSel = new double[weights.size()];
		double totalSel = 0;

		for (int i = 0; i < weights.size(); i++) {
			if (ReplanningUtils.isOnlySelector(weights.getStrategy(i))) {
				wSel[i] = weights.getWeight(i);
				totalSel += wSel[i];
			} else {
				wInno[i] = weights.getWeight(i);
				totalInno += wInno[i];
			}
		}

		double rnd = MatsimRandom.getRandom().nextDouble() * (totalInno + totalSel);

		// Subpopulation can not be null
		if (subpopulation == null) {
			subpopulation = "__none__";
		}

		IntSet oneAhead = this.oneAhead.get(subpopulation);
		IntSet twoAhead = this.twoAhead.get(subpopulation);

		int id = person.getId().index();

		// Expected number of innovations (this iteration)
		double expected = (seen.getInt(subpopulation) * totalInno) / (totalInno + totalSel);
		double diff = expected - innovated.getInt(subpopulation);

		seen.mergeInt(subpopulation, 1, Integer::sum);

		if (rnd < totalInno) {
			// Agent would innovate

			// Agent has already innovated, force selection and increase carry over
			if (oneAhead.contains(id)) {
				carryOver.mergeInt(subpopulation, 1, Integer::sum);
				return chooseStrategy(totalSel, wSel, weights);
			}

			oneAhead.add(id);
			innovated.mergeInt(subpopulation, 1, Integer::sum);
			advanceStep(subpopulation);

			return chooseStrategy(totalInno, wInno, weights);
		} else {
			// Agent would select

			// Force to innovate if there is carry over
			if (carryOver.getInt(subpopulation) > 0 && !oneAhead.contains(id)) {
				carryOver.mergeInt(subpopulation, -1, Integer::sum);

				oneAhead.add(id);
				innovated.mergeInt(subpopulation, 1, Integer::sum);
				advanceStep(subpopulation);
				return chooseStrategy(totalInno, wInno, weights);
			}

			// If the difference becomes too large, agents are allowed to innovate again
			// some carry overs are always reserved for agent that need to innovate one step
			if (carryOver.getInt(subpopulation) > 20 && diff > 50 && !twoAhead.contains(id)) {
				carryOver.mergeInt(subpopulation, -1, Integer::sum);

				twoAhead.add(id);
				innovated.mergeInt(subpopulation, 1, Integer::sum);
				return chooseStrategy(totalInno, wInno, weights);
			}

			return chooseStrategy(totalSel, wSel, weights);
		}
	}

	private void advanceStep(String subpopulation) {
		IntSet oneAhead = this.oneAhead.get(subpopulation);

		if (oneAhead.size() == total.getInt(subpopulation)) {
			IntSet twoAhead = this.twoAhead.get(subpopulation);

			oneAhead.clear();
			oneAhead.addAll(twoAhead);
			twoAhead.clear();
		}
	}

	private GenericPlanStrategy<PL, AG> chooseStrategy(double total, double[] w, StrategyChooser.Weights<PL, AG> weights) {
		double rnd = MatsimRandom.getRandom().nextDouble() * total;
		double sum = 0.0;
		for (int i = 0, max = w.length; i < max; i++) {
			sum += w[i];
			if (rnd <= sum) {
				return weights.getStrategy(i);
			}
		}
		return null;
	}
}
