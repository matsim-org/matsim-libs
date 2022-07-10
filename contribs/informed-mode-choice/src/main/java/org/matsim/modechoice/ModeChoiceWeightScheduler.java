package org.matsim.modechoice;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;


/**
 * Annealer for mode choice weights.
 */
public final class ModeChoiceWeightScheduler implements StartupListener, IterationStartsListener {

	private static final Logger log = LogManager.getLogger(ModeChoiceWeightScheduler.class);

	private double startBeta;
	private double currentBeta;
	private int iterations;

	private InformedModeChoiceConfigGroup.Schedule anneal;

	@Override
	public void notifyStartup(StartupEvent event) {

		Config config = event.getServices().getConfig();
		InformedModeChoiceConfigGroup imc = ConfigUtils.addOrGetModule(config, InformedModeChoiceConfigGroup.class);

		startBeta = currentBeta = imc.getInvBeta();
		anneal = imc.getAnneal();

		// The first iteration does not do any replanning
		iterations = config.controler().getLastIteration() - 1;
		double disableInnovation = config.strategy().getFractionOfIterationsToDisableInnovation();
		if (disableInnovation > 0 && disableInnovation < 1)
			iterations *= disableInnovation;

		if (anneal != InformedModeChoiceConfigGroup.Schedule.off)
			log.info("Annealing over {} iterations", iterations);
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {

		if (anneal == InformedModeChoiceConfigGroup.Schedule.off || event.getIteration() == 0)
			return;

		// anneal target is 0, iterations are offset by 1 because first iteration does not do replanning
		currentBeta = Math.max(0, startBeta - ( (event.getIteration() - 1)  * startBeta / iterations));
		log.info("Setting invBeta parameter to {} in iteration {}", currentBeta, event.getIteration());
	}

	public double getInvBeta() {
		return currentBeta;
	}
}
