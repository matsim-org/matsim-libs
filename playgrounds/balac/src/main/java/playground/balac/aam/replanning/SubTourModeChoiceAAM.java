

package playground.balac.aam.replanning;


import org.matsim.api.core.v01.Scenario;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.population.algorithms.PermissibleModesCalculator;
import org.matsim.population.algorithms.PermissibleModesCalculatorImpl;
import org.matsim.population.algorithms.PlanAlgorithm;

import javax.inject.Provider;

/**
 * Changes the transportation mode of all legs of one randomly chosen subtour in a plan to a randomly chosen
 * different mode given a list of possible modes.
 *
 * A subtour is a consecutive subset of a plan which starts and ends at the same link.
 * 
 * Certain modes are considered only if the choice would not require some resource to appear
 * out of thin air. For example, you can only drive your car back from work if you have previously parked it
 * there. These are called chain-based modes.
 * 
 * The assumption is that each chain-based mode requires one resource (car, bike, ...) and that this
 * resource is initially positioned at home. Home is the location of the first activity in the plan.
 * 
 * If the plan initially violates this constraint, this module may (!) repair it. 
 * 
 * Added parameters used to evaulate scores of pt and walk legs when changing subtours to PT.
 * 
 * @author balac
 * 
 */
public class SubTourModeChoiceAAM extends AbstractMultithreadedModule {
	private PermissibleModesCalculator permissibleModesCalculator;
	private final Provider<TripRouter> tripRouterProvider;

	private final String[] chainBasedModes;
	private final String[] modes;
	private final CharyparNagelScoringParameters params;
	private final double beeLineFactor;
	private final double walkSpeed;
	private final double ptSpeed;
	private Scenario scenario;
	public SubTourModeChoiceAAM(final Scenario scenario, Provider<TripRouter> tripRouterProvider) {
		this( scenario.getConfig().global().getNumberOfThreads(),
				scenario.getConfig().subtourModeChoice().getModes(),
						scenario.getConfig().subtourModeChoice().getChainBasedModes(),
								scenario.getConfig().subtourModeChoice().considerCarAvailability(),
				tripRouterProvider, new CharyparNagelScoringParameters.Builder(scenario.getConfig().planCalcScore(), scenario.getConfig().planCalcScore().getScoringParameters(null), scenario.getConfig().scenario()).build(),
				Double.parseDouble(scenario.getConfig().getModule("planscalcroute").getParams().get("beelineDistanceFactor")),
				Double.parseDouble(scenario.getConfig().getModule("planscalcroute").getParams().get("teleportedModeSpeed_walk")),
				Double.parseDouble(scenario.getConfig().getModule("planscalcroute").getParams().get("teleportedModeSpeed_pt")),
				scenario
		);
	}

	public SubTourModeChoiceAAM(
			final int numberOfThreads,
			final String[] modes,
			final String[] chainBasedModes,
			final boolean considerCarAvailability,
			Provider<TripRouter> tripRouterProvider, final CharyparNagelScoringParameters params,
			double beeLineFactor,
			double walkSpeed,
			double ptSpeed,
			Scenario scenario) {
		super(numberOfThreads);
		this.tripRouterProvider = tripRouterProvider;
		this.scenario = scenario;
		this.modes = modes.clone();
		this.chainBasedModes = chainBasedModes.clone();
		this.params = params;
		this.beeLineFactor = beeLineFactor;
		this.walkSpeed = walkSpeed;
		this.ptSpeed = ptSpeed;
		this.permissibleModesCalculator =
			new PermissibleModesCalculatorImpl(
					this.modes,
					considerCarAvailability);
	}
	
	protected String[] getModes() {
		return modes.clone();
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		final TripRouter tripRouter = tripRouterProvider.get();
		final ChooseRandomLegModeForSubtourAAM chooseRandomLegMode =
				new ChooseRandomLegModeForSubtourAAM(
						tripRouter.getStageActivityTypes(),
						tripRouter.getMainModeIdentifier(),
						this.permissibleModesCalculator,
						this.modes,
						this.chainBasedModes,
						MatsimRandom.getLocalInstance(),
						this.params,
						this.beeLineFactor,
						this.walkSpeed,
						this.ptSpeed,
						this.scenario
						);
		chooseRandomLegMode.setAnchorSubtoursAtFacilitiesInsteadOfLinks( false );
		return chooseRandomLegMode;
	}

	/**
	 * Decides if a person may use a certain mode of transport. Can be used for car ownership.
	 * 
	 */
	public void setPermissibleModesCalculator(PermissibleModesCalculator permissibleModesCalculator) {
		this.permissibleModesCalculator = permissibleModesCalculator;
	}

}
