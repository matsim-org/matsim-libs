package playground.christoph.controler;

import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunctionFactory;

import playground.christoph.router.costcalculators.SystemOptimalTravelCostCalculator;
import playground.christoph.scoring.OnlyTimeDependentScoringFunctionFactory;

/*
 * Uses a Scoring Function that gets the Facilities Opening Times from 
 * the Facilities instead of the Config File.
 */
public class PaperControler extends Controler{

	public PaperControler(String[] args)
	{
		super(args);
	}

	@Override
	protected ScoringFunctionFactory loadScoringFunctionFactory()
	{
//		return new CharyparNagelOpenTimesScoringFunctionFactory(this.config.charyparNagelScoring());
//		return new CharyparNagelScoringFunctionFactory(this.config.charyparNagelScoring());
		
		// Use a Scoring Function, that only scores the travel times!
		return new OnlyTimeDependentScoringFunctionFactory();
	}

	protected void setUp()
	{
		super.setUp();

		// Do Iterations and use the default internal TravelTimeCalculator
//		this.travelCostCalculator = new OnlyTimeDependentTravelCostCalculator(this.getTravelTimeCalculator());
		this.setTravelCostCalculatorFactory(new TravelCostCalculatorFactory() {

			@Override
			public PersonalizableTravelCost createTravelCostCalculator(
					TravelTime timeCalculator,
					CharyparNagelScoringConfigGroup cnScoringGroup) {
				return new SystemOptimalTravelCostCalculator(PaperControler.this.getTravelTimeCalculator());
			}
			
		});

		// Update the Calculators in the Replanning Modules!
		this.strategyManager = loadStrategyManager();
		
//		SocialCostCalculatorMultiLink sc = new SocialCostCalculatorMultiLink(this.network,this.config.travelTimeCalculator().getTraveltimeBinSize(), this.travelTimeCalculator, this.population);
//		
//		this.events.addHandler(sc);
//		this.getQueueSimulationListener().add(sc);
//		this.travelCostCalculator = new MarginalTravelCostCalculatorII(this.travelTimeCalculator, sc);
//		this.strategyManager = loadStrategyManager();
//		this.addControlerListener(sc);
	}

	
	public static void main(final String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println();
		} else {
			final PaperControler controler = new PaperControler(args);
			controler.setOverwriteFiles(true);
			controler.run();
		}
		System.exit(0);
	}
}
