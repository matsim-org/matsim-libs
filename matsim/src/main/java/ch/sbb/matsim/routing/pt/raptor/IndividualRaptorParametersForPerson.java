package ch.sbb.matsim.routing.pt.raptor;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.functions.ModeUtilityParameters;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Map;

/**
 * An implementation of {@link RaptorParametersForPerson} that returns an
 * individual set of routing parameters, based on
 * {@link ScoringParametersForPerson}.
 *
 * @author sebhoerl / ETHZ
 */
public class IndividualRaptorParametersForPerson implements RaptorParametersForPerson {
	private final Config config;
	private final SwissRailRaptorConfigGroup raptorConfig;
	private final ScoringParametersForPerson parametersForPerson;

	@Inject
	public IndividualRaptorParametersForPerson(Config config, ScoringParametersForPerson parametersForPerson) {
		this.config = config;
		this.raptorConfig = ConfigUtils.addOrGetModule(config, SwissRailRaptorConfigGroup.class);
		this.parametersForPerson = parametersForPerson;
	}

	@Override
	public RaptorParameters getRaptorParameters(Person person) {
		RaptorParameters raptorParameters = RaptorUtils.createParameters(config);
		ScoringParameters scoringParameters = parametersForPerson.getScoringParameters(person);

		double marginalUtilityOfPerforming = scoringParameters.marginalUtilityOfPerforming_s;

		raptorParameters.setMarginalUtilityOfWaitingPt_utl_s(
				scoringParameters.marginalUtilityOfWaitingPt_s - marginalUtilityOfPerforming);

		PlanCalcScoreConfigGroup pcsConfig = config.planCalcScore();

		for (Map.Entry<String, PlanCalcScoreConfigGroup.ModeParams> e : pcsConfig.getModes().entrySet()) {
			String mode = e.getKey();
			ModeUtilityParameters modeParams = scoringParameters.modeParams.get(mode);

			if (modeParams != null) {
				raptorParameters.setMarginalUtilityOfTravelTime_utl_s(mode,
						modeParams.marginalUtilityOfTraveling_s - marginalUtilityOfPerforming);
			}
		}

		ModeUtilityParameters walkParams = scoringParameters.modeParams.get(TransportMode.walk);

		for (String fallbackMode : Arrays.asList(TransportMode.non_network_walk,
				TransportMode.transit_walk)) {
			ModeUtilityParameters modeParams = scoringParameters.modeParams.get(fallbackMode);

			if (modeParams != null) {
				raptorParameters.setMarginalUtilityOfTravelTime_utl_s(fallbackMode,
						walkParams.marginalUtilityOfTraveling_s - marginalUtilityOfPerforming);
			}
		}

		double costPerHour = this.raptorConfig.getTransferPenaltyCostPerTravelTimeHour();
		if (costPerHour == 0.0) {
			// backwards compatibility, use the default utility of line switch
			raptorParameters.setTransferPenaltyFixCostPerTransfer(-scoringParameters.utilityOfLineSwitch);
		} else {
			raptorParameters.setTransferPenaltyFixCostPerTransfer(this.raptorConfig.getTransferPenaltyBaseCost());
		}
		raptorParameters.setTransferPenaltyPerTravelTimeHour(costPerHour);
		raptorParameters.setTransferPenaltyMinimum(this.raptorConfig.getTransferPenaltyMinCost());
		raptorParameters.setTransferPenaltyMaximum(this.raptorConfig.getTransferPenaltyMaxCost());

		return raptorParameters;
	}
}
