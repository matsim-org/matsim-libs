/**
 * org.matsim.contrib.atap
 * 
 * Copyright (C) 2025 by Gunnar Flötteröd (VTI, LiU).
 * 
 * VTI = Swedish National Road and Transport Institute
 * LiU = Linköping University, Sweden
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either 
 * version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>. See also COPYING and WARRANTY file.
 */
package se.vti.atap.matsim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.util.TravelTime;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class KernelPopulationDistance extends AbstractPopulationDistance {

	// -------------------- INNER CLASSES --------------------

	private class LinkEntry {

		final Id<Person> personId;

		final double time_s;

		LinkEntry(final Id<Person> personId, final double time_s) {
			this.personId = personId;
			this.time_s = time_s;
		}
	}

	// -------------------- CONSTANTS --------------------

	private final double flowCapacityFactor;
	private final double kernelHalfTime_s;
	private final double kernelThreshold;

//	private GreedoConfigGroup greedoConfig;

	// -------------------- MEMBERS --------------------

//	private final Map<Id<Person>, Map<Id<Person>, Double>> personId2personId2aCoeff = new LinkedHashMap<>();
	private final ConcurrentHashMap<Id<Person>, ConcurrentHashMap<Id<Person>, Double>> personId2personId2aCoeff = new ConcurrentHashMap<>();

	private final Set<Id<Link>> consideredLinkIds;

	// -------------------- CONSTRUCTION --------------------

	KernelPopulationDistance(final Plans pop1, final Plans pop2, final Scenario scenario,
			final Map<String, ? extends TravelTime> mode2travelTime) {
		this.flowCapacityFactor = scenario.getConfig().qsim().getFlowCapFactor();
		final ATAPConfigGroup greedoConfig = ConfigUtils.addOrGetModule(scenario.getConfig(),
				ATAPConfigGroup.class);
		this.kernelHalfTime_s = greedoConfig.getKernelHalftime_s();
		this.kernelThreshold = greedoConfig.getKernelThreshold();

		// >>> NEW >>>

		this.consideredLinkIds = new LinkedHashSet<>(
				1 + (int) (greedoConfig.getLinkShareInDistance() * scenario.getNetwork().getLinks().size()));
		final List<Id<Link>> allLinkIds = new ArrayList<>(scenario.getNetwork().getLinks().keySet().stream().toList());
		Collections.shuffle(allLinkIds);
		for (int i = 0; i < greedoConfig.getLinkShareInDistance() * scenario.getNetwork().getLinks().size(); i++) {
			this.consideredLinkIds.add(allLinkIds.get(i));
		}

		// <<< NEW <<<

		final ConcurrentHashMap<Link, CopyOnWriteArrayList<LinkEntry>> link2entries1 = this.plans2linkEntries(pop1,
				scenario, mode2travelTime);
		final ConcurrentHashMap<Link, CopyOnWriteArrayList<LinkEntry>> link2entries2 = this.plans2linkEntries(pop2,
				scenario, mode2travelTime);

		this.updateCoeffsParallel(link2entries1, link2entries1, 1.0); // K(x,x) terms
		this.updateCoeffsParallel(link2entries1, link2entries2, -2.0); // K(x,y) terms
		this.updateCoeffsParallel(link2entries2, link2entries2, 1.0); // K(y,y) terms

		System.gc(); // link2entries no longer needed
	}

	// -------------------- INTERNALS --------------------

	private int noNetworkRouteWarningCnt = 0;

	private List<Leg> extractNetworkLegs(final Plan plan) {
		final List<Leg> legs = new ArrayList<>(plan.getPlanElements().size() / 2);
		for (PlanElement pE : plan.getPlanElements()) {
			if (pE instanceof Leg) {
				final Leg leg = (Leg) pE;
				if ((leg.getRoute() != null) && (leg.getRoute() instanceof NetworkRoute)) {
					legs.add(leg);
				} else if (this.noNetworkRouteWarningCnt < 10) {
					LogManager.getLogger(this.getClass())
							.warn("Person " + plan.getPerson().getId() + " has a selected plan with "
									+ (leg.getRoute() == null ? "no route"
											: ("a route that is not of type NetworkRoute but "
													+ leg.getRoute().getClass().getSimpleName())));
					this.noNetworkRouteWarningCnt++;
					if (this.noNetworkRouteWarningCnt == 10) {
						LogManager.getLogger(this.getClass()).warn("Suppressing further warnings of this type.");
					}
				}
			}
		}
		return legs;
	}

	private List<Link> allRelevantLinksAsList(final NetworkRoute route, final Network network) {
		final List<Link> result = new ArrayList<>(route.getLinkIds().size() + 2);
		// route start and end leg are not included in getLinkIds()
		if (this.consideredLinkIds.contains(route.getStartLinkId())) {
			result.add(network.getLinks().get(route.getStartLinkId()));
		}
		route.getLinkIds().stream().filter(id -> this.consideredLinkIds.contains(id))
				.forEach(id -> result.add(network.getLinks().get(id)));
		if (this.consideredLinkIds.contains(route.getEndLinkId())) {
			result.add(network.getLinks().get(route.getEndLinkId()));
		}
		return result;
	}

	private ConcurrentHashMap<Link, CopyOnWriteArrayList<LinkEntry>> plans2linkEntries(final Plans plans,
			final Scenario scenario, Map<String, ? extends TravelTime> mode2travelTime) {
		this.noNetworkRouteWarningCnt = 0;
		final Map<Link, List<LinkEntry>> tmpResult = new LinkedHashMap<>();
		for (Id<Person> personId : plans.getPersonIdView()) {
			for (Leg leg : this.extractNetworkLegs(plans.getSelectedPlan(personId))) {
				final TravelTime travelTime = mode2travelTime.get(leg.getMode());
				double time_s = leg.getDepartureTime().seconds();
				for (Link link : this.allRelevantLinksAsList((NetworkRoute) leg.getRoute(), scenario.getNetwork())) {
					tmpResult.computeIfAbsent(link, l -> new ArrayList<>()).add(new LinkEntry(personId, time_s));
					time_s += travelTime.getLinkTravelTime(link, time_s, null, null);
				}
			}
		}

		final ConcurrentHashMap<Link, CopyOnWriteArrayList<LinkEntry>> result = new ConcurrentHashMap<>();
		for (Map.Entry<Link, List<LinkEntry>> e : tmpResult.entrySet()) {
			result.put(e.getKey(), new CopyOnWriteArrayList<>(e.getValue()));
		}
		return result;
	}

//	private void updateCoeffs(final Map<Link, List<LinkEntry>> link2entries1,
//			final Map<Link, List<LinkEntry>> link2entries2, final double fact) {
//		for (Map.Entry<Link, List<LinkEntry>> e : link2entries1.entrySet()) {
//			final Link link = e.getKey();
//
//			final double flowCap_veh_s = this.flowCapacityFactor * link.getCapacity() / link.getCapacityPeriod();
//			final double kernelMu_1_s = Math.log(2.0) / kernelHalfTime_s;
//			final double linkFact = fact * kernelMu_1_s / flowCap_veh_s;
//
//			final List<LinkEntry> entries1 = e.getValue();
//			final List<LinkEntry> entries2 = link2entries2.computeIfAbsent(link, l -> Collections.emptyList());
//			if (entries1.size() > 0 && entries2.size() > 0) {
//				for (LinkEntry entry1 : entries1) {
//					for (LinkEntry entry2 : entries2) {
//						final double muTimesDelta = kernelMu_1_s * Math.abs(entry1.time_s - entry2.time_s);
//						final double kernel = Math.exp(-muTimesDelta) * (1.0 + muTimesDelta);
//						if (kernel >= kernelThreshold) {
//							this.addCoefficient(entry1.personId, entry2.personId, linkFact * kernel);
//						}
//					}
//				}
//			}
//		}
//	}

//	private void addCoefficient(final Id<Person> personId1, final Id<Person> personId2, final double addend) {
//		this.personId2personId2aCoeff.computeIfAbsent(personId1, id -> new ConcurrentHashMap<>()).compute(personId2,
//				(id, coeff) -> coeff == null ? addend : coeff + addend);
//	}

	private class JobProcessor implements Runnable {

		private final BlockingQueue<Job> allJobs;
		private final double fact;
		private final ConcurrentHashMap<Link, CopyOnWriteArrayList<LinkEntry>> link2entries2;

		JobProcessor(BlockingQueue<Job> allJobs, double fact,
				ConcurrentHashMap<Link, CopyOnWriteArrayList<LinkEntry>> link2entries2) {
			this.allJobs = allJobs;
			this.fact = fact;
			this.link2entries2 = link2entries2;
		}

		@Override
		public void run() {
			try {
				while (true) {
					Job job = this.allJobs.take();
					if (job == Job.TERMINATE) {
						break;
					}
					this.process(job);
				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		void process(Job job) {
			final Link link = job.entry.getKey();

			final double flowCap_veh_s = flowCapacityFactor * link.getCapacity() / link.getCapacityPeriod();
			final double kernelMu_1_s = Math.log(2.0) / kernelHalfTime_s;
			final double linkFact = this.fact * kernelMu_1_s / flowCap_veh_s;

			final CopyOnWriteArrayList<LinkEntry> entries1 = job.entry.getValue();
			final List<LinkEntry> entries2 = this.link2entries2.get(link);
			if (entries1.size() > 0 && entries2 != null && entries2.size() > 0) {
				for (LinkEntry entry1 : entries1) {
					for (LinkEntry entry2 : entries2) {
						final double muTimesDelta = kernelMu_1_s * Math.abs(entry1.time_s - entry2.time_s);
						final double kernel = Math.exp(-muTimesDelta) * (1.0 + muTimesDelta);
						if (kernel >= kernelThreshold) {
							final double addend = linkFact * kernel;
							personId2personId2aCoeff.computeIfAbsent(entry1.personId, id -> new ConcurrentHashMap<>())
									.compute(entry2.personId, (id, coeff) -> coeff == null ? addend : coeff + addend);

						}
					}
				}
			}
		}

	}

	private static class Job {

		static Job TERMINATE = new Job(null);

		private final Map.Entry<Link, CopyOnWriteArrayList<LinkEntry>> entry;

		Job(Map.Entry<Link, CopyOnWriteArrayList<LinkEntry>> entry) {
			this.entry = entry;
		}

	}

	private void updateCoeffsParallel(final ConcurrentHashMap<Link, CopyOnWriteArrayList<LinkEntry>> link2entries1,
			final ConcurrentHashMap<Link, CopyOnWriteArrayList<LinkEntry>> link2entries2, final double fact) {

		final int threadCnt = Runtime.getRuntime().availableProcessors();
		final BlockingQueue<Job> jobQueue = new LinkedBlockingQueue<>(100 * threadCnt);
		final List<Thread> threads = new ArrayList<>();

		try {
			for (int i = 0; i < threadCnt; i++) {
				final JobProcessor jobProcessor = new JobProcessor(jobQueue, fact, link2entries2);
				final Thread thread = new Thread(jobProcessor);
				threads.add(thread);
				thread.start();
			}
			for (Map.Entry<Link, CopyOnWriteArrayList<LinkEntry>> e : link2entries1.entrySet()) {
				jobQueue.put(new Job(e));
			}
			for (int i = 0; i < threads.size(); i++) {
				jobQueue.put(Job.TERMINATE);
			}
			for (Thread thread : threads) {
				thread.join();
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	// --------------- OVERRIDING of PopulationDistance ---------------

	@Override
	ConcurrentHashMap<Id<Person>, ConcurrentHashMap<Id<Person>, Double>> getPersonId2personId2aCoeff() {
		return this.personId2personId2aCoeff;
	}

	@Override
	double getACoefficient(final Id<Person> personId1, final Id<Person> personId2) {
		if (this.personId2personId2aCoeff.containsKey(personId1)) {
			return this.personId2personId2aCoeff.get(personId1).getOrDefault(personId2, 0.0);
		} else {
			return 0.0;
		}
	}
}
