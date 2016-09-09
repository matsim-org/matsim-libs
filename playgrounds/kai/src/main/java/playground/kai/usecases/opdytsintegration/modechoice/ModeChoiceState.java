package playground.kai.usecases.opdytsintegration.modechoice;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.scoring.ExperiencedPlansService;

import floetteroed.utilities.math.Vector;
import opdytsintegration.MATSimState;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
 class ModeChoiceState extends MATSimState {

	private ExperiencedPlansService epService;
	private StageActivityTypes stageActivities;
	private MainModeIdentifier mmIdent;

	ModeChoiceState(final Population population, final Vector vectorRepresentation, ExperiencedPlansService epService, 
			StageActivityTypes stageActivities, MainModeIdentifier mmIdent) {
		super(population, vectorRepresentation);
		this.epService = epService;
		this.stageActivities = stageActivities;
		this.mmIdent = mmIdent;
	}

	final Map<Id<Person>, Plan> getExperiencedPlans() {
		return epService.getExperiencedPlans() ;
	}

	final StageActivityTypes getStageActivities() {
		return this.stageActivities;
	}

	final MainModeIdentifier getMainModeIdentifier() {
		return this.mmIdent;
	}

}
