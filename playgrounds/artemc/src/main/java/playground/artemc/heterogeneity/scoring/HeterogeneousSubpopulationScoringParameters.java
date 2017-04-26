package playground.artemc.heterogeneity.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.scoring.functions.ActivityUtilityParameters;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.pt.PtConstants;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.utils.objectattributes.ObjectAttributes;
import playground.artemc.heterogeneity.scoring.paramterBuilders.ProportionalHeterogeneityScoringParametersBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by artemc on 24/06/16.
 */
public class HeterogeneousSubpopulationScoringParameters implements ScoringParametersForPerson {

	private final PlanCalcScoreConfigGroup config;
	private final ScenarioConfigGroup scConfig;
	private final TransitConfigGroup transitConfigGroup;
	private final ObjectAttributes personAttributes;
	private final String subpopulationAttributeName;
	private final Map<String, ScoringParameters> params = new HashMap<>();

	private final Scenario scenario;
	private final String heterogeneityType;

	// Save indvidual scoring parametrs in a map in order to prevents re-generation of person parameters each call/iteration.
	private Map<Id<Person>,ScoringParameters> individualScoringParamters = new HashMap<>();
	// private Map<Id<Person>,String> personToSubpopulation = new HashMap<>();

	@javax.inject.Inject
	public HeterogeneousSubpopulationScoringParameters(Scenario scenario) {

		this.scenario = scenario;
		this.config = scenario.getConfig().planCalcScore();
		this.scConfig = scenario.getConfig().scenario();
		this.transitConfigGroup = scenario.getConfig().transit();
		this.personAttributes = scenario.getPopulation().getPersonAttributes();
		this.subpopulationAttributeName = scenario.getConfig().plans().getSubpopulationAttributeName();

		//TODO Change this
		this.heterogeneityType = "hetero";

	}

	@Override
	public ScoringParameters getScoringParameters(Person person) {

		final String subpopulation = (String) personAttributes.getAttribute(person.getId().toString(),
				                                                                   subpopulationAttributeName);

		if (!this.params.containsKey(subpopulation)) {
			/* lazy initialization of params. not strictly thread safe, as different threads could
			 * end up with different params-object, although all objects will have the same
			 * values in them due to using the same config. Still much better from a memory performance
			 * point of view than giving each ScoringFunction its own copy of the params.
			 */
			ScoringParameters.Builder builder = new ScoringParameters.Builder(this.config, this.config.getScoringParameters(subpopulation), scConfig);
			if (transitConfigGroup.isUseTransit()) {
				// yyyy this should go away somehow. :-)

				PlanCalcScoreConfigGroup.ActivityParams transitActivityParams = new PlanCalcScoreConfigGroup.ActivityParams(PtConstants.TRANSIT_ACTIVITY_TYPE);
				transitActivityParams.setTypicalDuration(120.0);
				transitActivityParams.setOpeningTime(0.) ;
				transitActivityParams.setClosingTime(0.) ;
				ActivityUtilityParameters.Builder modeParamsBuilder = new ActivityUtilityParameters.Builder(transitActivityParams);
				modeParamsBuilder.setScoreAtAll(false);
				builder.setActivityParameters(PtConstants.TRANSIT_ACTIVITY_TYPE, modeParamsBuilder);
			}

			this.params.put(subpopulation, builder.build());
		}

		/* If person contains incomeFactor attribute and heterogeneous scoring is enabled, generate individual
         * scoring parameters
		 */
		if(person.getCustomAttributes().containsKey("incomeAlphaFactor") && !heterogeneityType.equals("homo")) {

			if (individualScoringParamters.containsKey(person.getId())) return individualScoringParamters.get(person.getId());

			ScoringParameters parameters = params.get(subpopulation);

			if(heterogeneityType.equals("hetero")) {
				ProportionalHeterogeneityScoringParametersBuilder heterogeneousScoringParametersBuilder = new ProportionalHeterogeneityScoringParametersBuilder(scenario);

				parameters = heterogeneousScoringParametersBuilder.buildIncomeBasedScoringParameters(person);
			}

		individualScoringParamters.put(person.getId(), parameters);
			return parameters;
		}
		else {
			return this.params.get(subpopulation);
		}
	}



}
