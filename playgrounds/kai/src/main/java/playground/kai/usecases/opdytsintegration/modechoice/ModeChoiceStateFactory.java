package playground.kai.usecases.opdytsintegration.modechoice;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scoring.ExperiencedPlansService;

import floetteroed.utilities.math.Vector;
import opdytsintegration.MATSimState;
import opdytsintegration.MATSimStateFactory;

/**
 * 
 * @author Kai Nagel based on Gunnar Flötteröd
 *
 */
public class ModeChoiceStateFactory implements MATSimStateFactory<ModeChoiceDecisionVariable> {
	
	private Controler controler = null;
	
	@Override
	public void registerControler(final Controler controler1) {
		this.controler = controler1;
	}
	
	@Override public MATSimState newState(Population population, Vector stateVector, ModeChoiceDecisionVariable decisionVariable) {
		ExperiencedPlansService epService = controler.getInjector().getInstance( ExperiencedPlansService.class ) ;
		StageActivityTypes stageActivities = controler.getInjector().getInstance( TripRouter.class ).getStageActivityTypes() ;
		MainModeIdentifier mmIdent = controler.getInjector().getInstance( TripRouter.class ).getMainModeIdentifier() ;
		/*
		 * Kai, my suggestion would be extract here whatever you need from the Controler and to put
		 * it into the ModeChoiceState. However, be aware that the state object must be thereof
		 * independent of the controler state (because the Controler keeps changing its state but the
		 * ModeChoiceState is a snapshot of a past state). Bullet-proof solution is to compute the 
		 * objective function value here and to pass it as a number to the ModeChoiceState. Gunnar
		 */		
		return new ModeChoiceState(population, stateVector, epService, stageActivities, mmIdent);
	}

}
