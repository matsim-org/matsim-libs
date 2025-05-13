package org.matsim.freight.logistics;

import java.util.List;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.GenericStrategyManager;
import org.matsim.core.replanning.GenericStrategyManagerImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.selectors.PlanSelector;

/**
 * Normally, this would be infrastructure that is configurable via the config. Since we ain't there
 * yet, the way to configure this is something like:
 *
 * <pre>
 *         bind( LSPStrategyManager.class ).to( new Provider<LSPStrategyManager>() {
 *                 public LSPStrategyManager.get() {
 *                         LSPStrategyManager manager = new LSPStrategyManagerImpl();
 *                         manager.addStrategy(...)
 *                         ...
 *                         return manager;
 *                 }
 *         }
 * </pre>
 */
public class LSPStrategyManagerImpl implements LSPStrategyManager {
  final GenericStrategyManager<LSPPlan, LSP> delegate = new GenericStrategyManagerImpl<>();

  @Override
  public void addStrategy(
      GenericPlanStrategy<LSPPlan, LSP> strategy, String subpopulation, double weight) {
    delegate.addStrategy(strategy, subpopulation, weight);
  }

  @Override
  public void run(
      Iterable<? extends HasPlansAndId<LSPPlan, LSP>> persons,
      int iteration,
      ReplanningContext replanningContext) {
    delegate.run(persons, iteration, replanningContext);
  }

  @Override
  public void setMaxPlansPerAgent(int maxPlansPerAgent) {
    delegate.setMaxPlansPerAgent(maxPlansPerAgent);
  }

  @Override
  public void addChangeRequest(
      int iteration,
      GenericPlanStrategy<LSPPlan, LSP> strategy,
      String subpopulation,
      double newWeight) {
    delegate.addChangeRequest(iteration, strategy, subpopulation, newWeight);
  }

  @Override
  public void setPlanSelectorForRemoval(PlanSelector<LSPPlan, LSP> planSelector) {
    delegate.setPlanSelectorForRemoval(planSelector);
  }

  @Override
  public List<GenericPlanStrategy<LSPPlan, LSP>> getStrategies(String subpopulation) {
    return delegate.getStrategies(subpopulation);
  }

  @Override
  public List<Double> getWeights(String subpopulation) {
    return delegate.getWeights(subpopulation);
  }
}
