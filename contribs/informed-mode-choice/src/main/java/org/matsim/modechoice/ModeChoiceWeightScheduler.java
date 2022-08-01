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

	/**
	 * Starting beta = t_0
	 */
	private double startBeta;
	private double currentBeta;

	/**
	 * Number of iterations
	 */
	private int n;

	private InformedModeChoiceConfigGroup.Schedule anneal;

	@Override
	public void notifyStartup(StartupEvent event) {

		Config config = event.getServices().getConfig();
		InformedModeChoiceConfigGroup imc = ConfigUtils.addOrGetModule(config, InformedModeChoiceConfigGroup.class);

		startBeta = currentBeta = imc.getInvBeta();
		anneal = imc.getAnneal();

		// The first iteration does not do any replanning
		n = config.controler().getLastIteration() - 1;
		double disableInnovation = config.strategy().getFractionOfIterationsToDisableInnovation();
		if (disableInnovation > 0 && disableInnovation < 1)
			n *= disableInnovation;

		if (anneal != InformedModeChoiceConfigGroup.Schedule.off)
			log.info("Annealing over {} iterations", n);
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {

		if (anneal == InformedModeChoiceConfigGroup.Schedule.off || event.getIteration() == 0)
			return;

		// anneal target is 0, iterations are offset by 1 because first iteration does not do replanning
		switch (anneal) {
			case linear -> currentBeta = linear(startBeta, n, event.getIteration() - 1);
			case quadratic -> currentBeta = quadratic(startBeta, n, event.getIteration() - 1);
			case cubic -> currentBeta = cubic(startBeta, n, event.getIteration() - 1);
			case exponential -> currentBeta = exponential(startBeta, n, event.getIteration() - 1);
			case trigonometric -> currentBeta = trigonometric(startBeta, n, event.getIteration() - 1);
			default -> throw new IllegalStateException("Unknown annealing schedule");
		}

		// Never fall below 0
		currentBeta = currentBeta < 0 ? 0 : currentBeta;

		log.info("Setting invBeta parameter to {} in iteration {}", currentBeta, event.getIteration());
	}

	public double getInvBeta() {
		return currentBeta;
	}

	// https://nathanrooy.github.io/posts/2020-05-14/simulated-annealing-with-python/

	public static double linear(double t_0, double n, double k) {
		return t_0 * ((n - k) / n);
	}

	public static double quadratic(double t_0, double n, double k) {
		return t_0 * Math.pow((n - k) / n, 2);
	}

	public static double cubic(double t_0, double n, double k) {
		return t_0 * Math.pow((n - k) / n, 3);
	}

	public static double exponential(double t_0, double n, double k) {
		return t_0 * Math.pow(0.95, k);
	}

	public static double fast(double t_0, double n, double k) {
		return t_0 / (k + 1);
	}

	public static double trigonometric(double t_0, double n, double k) {
		return 0.5 * t_0 * (1 + Math.cos(k * Math.PI / n));
	}

}
