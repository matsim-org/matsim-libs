/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** */

package org.matsim.contrib.drt.run.benchmark.scenario;

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

	private double sampleDepartureTime() {
		if (!timeDependent) return random.nextDouble() * 86400;
		double peak = random.nextDouble() < 0.7 ? 8 * 3600 : 17 * 3600;
		double stdDev = random.nextDouble() < 0.7 ? 1.5 * 3600 : 2 * 3600;
		double sample;
		do { sample = peak + random.nextGaussian() * stdDev; } while (sample < 0 || sample > 86400);
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
