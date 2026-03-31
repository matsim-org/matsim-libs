/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** */

package org.matsim.contrib.drt.extension.benchmark.scenario;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.gbl.MatsimRandom;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates a synthetic DRT population for benchmarks.
 *
 * @author Steffen Axer
 */
public class PopulationGenerator {

	private final Random random = MatsimRandom.getLocalInstance();
	private final boolean timeDependent;
	private final double sigma;

	public PopulationGenerator(boolean timeDependent, double sigma) {
		this.timeDependent = timeDependent;
		this.sigma = sigma;
	}

	public void generate(int numberOfAgents, Scenario scenario) {
		Population population = scenario.getPopulation();
		List<Link> links = new ArrayList<>(scenario.getNetwork().getLinks().values());

		double centerX = links.stream().mapToDouble(l -> l.getCoord().getX()).average().orElse(0);
		double centerY = links.stream().mapToDouble(l -> l.getCoord().getY()).average().orElse(0);

		double[] weights = new double[links.size()];
		double totalWeight = 0;
		for (int i = 0; i < links.size(); i++) {
			double dx = links.get(i).getCoord().getX() - centerX;
			double dy = links.get(i).getCoord().getY() - centerY;
			weights[i] = Math.exp(-(dx * dx + dy * dy) / (2 * sigma * sigma));
			totalWeight += weights[i];
		}

		double[] cumulative = new double[weights.length];
		double sum = 0;
		for (int i = 0; i < weights.length; i++) {
			sum += weights[i] / totalWeight;
			cumulative[i] = sum;
		}

		addActivityParams(scenario);

		for (int i = 0; i < numberOfAgents; i++) {
			Link origin = selectLink(links, cumulative);
			Link destination;
			do { destination = selectLink(links, cumulative); } while (destination.equals(origin));

			createAgent(population, i, origin, destination, sampleDepartureTime());
		}
	}

	private void addActivityParams(Scenario scenario) {
		var scoring = scenario.getConfig().scoring();
		if (scoring.getActivityParams("home") == null) {
			scoring.addActivityParams(new ScoringConfigGroup.ActivityParams("home").setTypicalDuration(8 * 3600));
		}
		if (scoring.getActivityParams("work") == null) {
			scoring.addActivityParams(new ScoringConfigGroup.ActivityParams("work").setTypicalDuration(8 * 3600));
		}
	}

	private Link selectLink(List<Link> links, double[] cumulative) {
		double r = random.nextDouble();
		for (int i = 0; i < cumulative.length; i++) {
			if (r <= cumulative[i]) return links.get(i);
		}
		return links.getLast();
	}

	/**
	 * Samples a departure time with realistic Gaussian demand distribution.
	 * <p>
	 * Distribution breakdown:
	 * <ul>
	 *   <li>5% uniform background demand throughout the day</li>
	 *   <li>55% morning rush centered at 8:00 with Gaussian distribution (σ = 45 min)</li>
	 *   <li>40% evening rush centered at 17:30 with Gaussian distribution (σ = 50 min)</li>
	 * </ul>
	 * <p>
	 * The Gaussian distribution creates a natural bell curve, which better reflects
	 * empirical traffic patterns compared to trapezoidal distributions.
	 */
	private double sampleDepartureTime() {
		if (!timeDependent) return random.nextDouble() * 86400;

		double rand = random.nextDouble();

		// 5% uniform background demand throughout the day
		if (rand < 0.05) {
			return random.nextDouble() * 86400;
		}

		// 55% morning rush hour: peak at 8:00, std dev 45 min
		if (rand < 0.60) {
			return sampleGaussian(8 * 3600, 45 * 60, 5 * 3600, 11 * 3600);
		}

		// 40% evening rush hour: peak at 17:30, std dev 50 min
		return sampleGaussian(17.5 * 3600, 50 * 60, 14 * 3600, 21 * 3600);
	}

	/**
	 * Samples from a truncated Gaussian (normal) distribution.
	 * <p>
	 * The distribution follows a bell curve centered at peakTime:
	 * <pre>
	 *           *
	 *          ***
	 *         *****
	 *        *******
	 *       *********
	 *      ***********
	 * ____*************____
	 *    min  peak   max
	 * </pre>
	 *
	 * @param peakTime center of the peak (in seconds)
	 * @param stdDev   standard deviation (in seconds)
	 * @param minTime  earliest allowed time (truncation bound)
	 * @param maxTime  latest allowed time (truncation bound)
	 * @return sampled time value, guaranteed to be within [minTime, maxTime]
	 */
	private double sampleGaussian(double peakTime, double stdDev, double minTime, double maxTime) {
		double sample;
		do {
			sample = peakTime + random.nextGaussian() * stdDev;
		} while (sample < minTime || sample > maxTime);
		return sample;
	}

	private void createAgent(Population pop, int idx, Link origin, Link dest, double departure) {
		Person person = pop.getFactory().createPerson(Id.createPersonId("drt_" + idx));
		Plan plan = pop.getFactory().createPlan();

		Activity home = pop.getFactory().createActivityFromLinkId("home", origin.getId());
		home.setEndTime(departure);
		plan.addActivity(home);
		plan.addLeg(pop.getFactory().createLeg("drt"));
		plan.addActivity(pop.getFactory().createActivityFromLinkId("work", dest.getId()));

		person.addPlan(plan);
		pop.addPerson(person);
	}
}
