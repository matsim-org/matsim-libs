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
	 * Samples a departure time with realistic demand distribution.
	 * <p>
	 * Distribution breakdown:
	 * <ul>
	 *   <li>5% uniform background demand throughout the day</li>
	 *   <li>55% morning rush (6:00-10:00) with trapezoidal distribution</li>
	 *   <li>40% evening rush (15:00-19:00) with trapezoidal distribution</li>
	 * </ul>
	 * <p>
	 * The trapezoidal distribution creates a plateau effect, avoiding
	 * extreme concentration of requests at single time points.
	 */
	private double sampleDepartureTime() {
		if (!timeDependent) return random.nextDouble() * 86400;

		double rand = random.nextDouble();

		// 5% uniform background demand throughout the day
		if (rand < 0.05) {
			return random.nextDouble() * 86400;
		}

		// 55% morning rush hour (6:00 - 10:00)
		if (rand < 0.60) {
			return sampleTrapezoidal(6 * 3600, 7 * 3600, 9 * 3600, 10 * 3600);
		}

		// 40% evening rush hour (15:00 - 19:00)
		return sampleTrapezoidal(15 * 3600, 16 * 3600, 18 * 3600, 19 * 3600);
	}

	/**
	 * Samples from a trapezoidal distribution.
	 * <p>
	 * The distribution has linear ramps at the edges and a flat plateau in the middle:
	 * <pre>
	 *       ___________
	 *      /           \
	 *     /             \
	 * ___/               \___
	 *   a    b       c    d
	 * </pre>
	 *
	 * @param a start of ramp-up
	 * @param b start of plateau
	 * @param c end of plateau
	 * @param d end of ramp-down
	 * @return sampled time value
	 */
	private double sampleTrapezoidal(double a, double b, double c, double d) {
		double rampUp = b - a;
		double plateau = c - b;
		double rampDown = d - c;

		// Area under each section (assuming height = 1 for plateau)
		double areaRampUp = 0.5 * rampUp;
		double areaPlateau = plateau;
		double areaRampDown = 0.5 * rampDown;
		double totalArea = areaRampUp + areaPlateau + areaRampDown;

		double r = random.nextDouble() * totalArea;

		if (r < areaRampUp) {
			// Ramp-up section: inverse of triangular CDF
			return a + Math.sqrt(r * 2 * rampUp);
		} else if (r < areaRampUp + areaPlateau) {
			// Plateau section: uniform
			return b + (r - areaRampUp);
		} else {
			// Ramp-down section: inverse of triangular CDF
			double remaining = r - areaRampUp - areaPlateau;
			return d - Math.sqrt((areaRampDown - remaining) * 2 * rampDown);
		}
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
