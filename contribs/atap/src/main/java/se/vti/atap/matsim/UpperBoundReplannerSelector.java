/**
 * se.vti.atap
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import se.vti.utils.misc.fileio.Hacks;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class UpperBoundReplannerSelector extends AbstractReplannerSelector {

	private class JobProcessor implements Runnable {

		private final ConcurrentHashMap<Id<Person>, ConcurrentHashMap<Id<Person>, Double>> personId2personId2aCoeff;

		private final List<Id<Person>> myPersonIds = new ArrayList<>();
		private final ConcurrentHashMap<Id<Person>, Double> personId2bParam;

		JobProcessor(ConcurrentHashMap<Id<Person>, Double> personId2bParam,
				ConcurrentHashMap<Id<Person>, ConcurrentHashMap<Id<Person>, Double>> personId2personId2aCoeff) {
			this.personId2bParam = personId2bParam;
			this.personId2personId2aCoeff = personId2personId2aCoeff;
		}

		void addPersonId(Id<Person> personId) {
			this.myPersonIds.add(personId);
		}

		private double computeASum(Id<Person> personId1, Id<Person> personId2) {
			double aSum = 0.0;
			if (this.personId2personId2aCoeff.containsKey(personId1)) {
				aSum += this.personId2personId2aCoeff.get(personId1).getOrDefault(personId2, 0.0);
			}
			if (this.personId2personId2aCoeff.containsKey(personId2)) {
				aSum += this.personId2personId2aCoeff.get(personId2).getOrDefault(personId1, 0.0);
			}
			return aSum;
		}

		private List<Id<Person>> replannerIds = null;

		void prepareInit(List<Id<Person>> replannerIds) {
			this.replannerIds = replannerIds;
		}

		private void init() {
			for (Id<Person> personId : this.myPersonIds) {
				double b = 0.0;
				for (Id<Person> replannerId : this.replannerIds) {
					b += this.computeASum(personId, replannerId);
				}
				this.personId2bParam.put(personId, b);
			}
			this.replannerIds = null;
		}

		private double deltaSign;

		private Id<Person> candidateId;

		void prepareRun(double deltaSign, Id<Person> candidateId) {
			if (this.replannerIds != null) {
				throw new RuntimeException();
			}
			this.deltaSign = deltaSign;
			this.candidateId = candidateId;
		}

		private void update() {
			for (Id<Person> personId : this.myPersonIds) {
				final double aSum = this.computeASum(personId, this.candidateId);
				this.personId2bParam.compute(personId, (id, b2) -> b2 + this.deltaSign * aSum);
			}
		}

		@Override
		public void run() {
			if (this.replannerIds != null) {
				this.init();
			} else {
				this.update();
			}
		}
	}

	// -------------------- CONSTANTS --------------------

	private final double eps = 1e-8;

	private final ATAPConfigGroup greedoConfig;

	private final boolean logReplanningProcess = true;

	private final ATAPConfigGroup.DistanceTransformation distanceTransformation;

	// -------------------- MEMBERS --------------------

	private PopulationDistance populationDistance = null;

	private Double initialGap = null;

	// -------------------- CONSTRUCTION --------------------

	UpperBoundReplannerSelector(final ATAPConfigGroup greedoConfig) {
		super(greedoConfig.newIterationToTargetReplanningRate());
		this.distanceTransformation = greedoConfig.newDistanceTransformation();
		this.greedoConfig = greedoConfig;
	}

	// -------------------- INTERNALS --------------------

	private double effectiveEta(final double currentGap) {
			return this.getTargetReplanningRate();
	}

	private double _Q(final double _G, final double _D2, final double epsilon, final double _D2max) {
		final double transformedD = this.distanceTransformation.transform(Math.sqrt(_D2), Math.sqrt(_D2max));
		return (_G - epsilon) / Math.max(this.eps, transformedD);
	}

	// --------------- OVERRIDING OF AbstractReplannerSelector ---------------

	@Override
	void setDistanceToReplannedPopulation(final PopulationDistance populationDistance) {
		this.populationDistance = populationDistance;
	}

	@Override
	Set<Id<Person>> selectReplannersHook(Map<Id<Person>, Double> personId2gap) {

		// Start with a maximum amount of replanning gap.
		final Set<Id<Person>> replannerIds = personId2gap.entrySet().stream().filter(e -> e.getValue() > 0.0)
				.map(e -> e.getKey()).collect(Collectors.toSet());
		if (replannerIds.size() == 0) {
			return Collections.emptySet();
		}

		// >>>>>>>>>> PARALLEL >>>>>>>>>>

		final List<Id<Person>> allPersonIds = new ArrayList<>(personId2gap.keySet());

		final ConcurrentHashMap<Id<Person>, Double> personId2bParam = new ConcurrentHashMap<>(personId2gap.size());

		final int threadCnt = Runtime.getRuntime().availableProcessors();
		final List<JobProcessor> jobProcessors = new ArrayList<>(threadCnt);
		for (int i = 0; i < threadCnt; i++) {
			jobProcessors.add(new JobProcessor(personId2bParam, this.populationDistance.getPersonId2personId2aCoeff()));
		}

		int processorIndex = 0;
		for (Id<Person> personId : allPersonIds) {
			jobProcessors.get(processorIndex++).addPersonId(personId);
			if (processorIndex >= jobProcessors.size()) {
				processorIndex = 0;
			}
		}

		List<Thread> threads = new ArrayList<>();
		try {
			for (int i = 0; i < threadCnt; i++) {
				final JobProcessor jobProcessor = jobProcessors.get(i);
				jobProcessor.prepareInit(new ArrayList<>(replannerIds));
				final Thread thread = new Thread(jobProcessor);
				threads.add(thread);
				thread.start();
			}
			for (Thread thread : threads) {
				thread.join();
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		// <<<<<<<<<< PARALLEL <<<<<<<<<<

		final String logFile = "exact-replanning.log";
		if (this.logReplanningProcess) {
			Hacks.append2file(logFile, "strictly positive gaps: "
					+ ((double) personId2gap.size()) / ((double) personId2gap.size()) + "\n");
			Hacks.append2file(logFile, "G(lambda)\tD(lambda)\tQ(lambda)\n");
		}

		final double _Gall = personId2gap.entrySet().stream().mapToDouble(e -> e.getValue()).sum();
		final double _D2all = 0.5 * personId2bParam.entrySet().stream().mapToDouble(e -> e.getValue()).sum();

		if (this.initialGap == null) {
			this.initialGap = _Gall;
		}

		double _G = replannerIds.stream().mapToDouble(r -> personId2gap.get(r)).sum();
		double _D2 = 0.5 * replannerIds.stream().mapToDouble(r -> personId2bParam.get(r)).sum();
		final double _D2max = _D2;

		boolean switched = true;

		while (switched) {

			if (this.logReplanningProcess) {
				Hacks.append2file(logFile, _G + "\t" + Math.sqrt(_D2) + "\t"
						+ this._Q(_G, _D2, this.effectiveEta(_Gall) * _Gall, _D2max) + "\n");
			}

			switched = false;
			if (this.greedoConfig.getShuffleBeforeReplannerSelection()) {
				Collections.shuffle(allPersonIds);
			}

			for (Id<Person> candidateId : allPersonIds) {

				final double candidateGap = personId2gap.get(candidateId);
				final double a = this.populationDistance.getACoefficient(candidateId, candidateId);
				final double b = personId2bParam.get(candidateId);

				final double deltaG;
				final double deltaD2;
				if (replannerIds.contains(candidateId)) {
					deltaG = -candidateGap;
					deltaD2 = -b + a;
				} else /* candidate is NOT a replanner */ {
					deltaG = +candidateGap;
					deltaD2 = +b + a;
				}

				// attention, now we maximize

				final double oldQ = this._Q(_G, _D2, this.effectiveEta(_Gall) * _Gall, _D2max);
				final double newQ = this._Q(_G + deltaG, _D2 + deltaD2, this.effectiveEta(_Gall) * _Gall, _D2max);

				if (newQ > oldQ) { // TODO robustify
					_G = Math.max(0.0, _G + deltaG);
					_D2 = Math.max(0.0, _D2 + deltaD2);

					final double deltaSign;
					if (replannerIds.contains(candidateId)) {
						replannerIds.remove(candidateId);
						deltaSign = -1.0;
					} else /* candidate is NOT a replanner */ {
						replannerIds.add(candidateId);
						deltaSign = +1.0;
					}

					threads = new ArrayList<>();
					try {
						for (int i = 0; i < threadCnt; i++) {
							final JobProcessor jobProcessor = jobProcessors.get(i);
							jobProcessor.prepareRun(deltaSign, candidateId);
							final Thread thread = new Thread(jobProcessor);
							threads.add(thread);
							thread.start();
						}
						for (Thread thread : threads) {
							thread.join();
						}
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}

					switched = true;

					if (this.greedoConfig.getCheckDistance()) {
						final double _Gchecked = personId2gap.entrySet().stream()
								.filter(e -> replannerIds.contains(e.getKey())).mapToDouble(e -> e.getValue()).sum();
						final double _D2checkedB = 0.5 * personId2bParam.entrySet().stream()
								.filter(e -> replannerIds.contains(e.getKey())).mapToDouble(e -> e.getValue()).sum();
						final boolean gErr = Math.abs(_Gchecked - _G) > 1e-4;
						final boolean d2ErrB = Math.abs(_D2checkedB - _D2) > 1e-4;
						if (gErr || d2ErrB) {
							String msg = "";
							if (gErr) {
								msg += "\nrecursive _G = " + _G + ", but checked _G = " + _Gchecked;
							}
							if (d2ErrB) {
								msg += "\nrecursive _D2 = " + _D2 + ", but checked _D2(B) = " + _D2checkedB;
							}
							throw new RuntimeException(msg);
						}
					}
				}
			}
		}

		if (this.logReplanningProcess) {
			Hacks.append2file(logFile, "homogeneity = " + (_Gall / Math.sqrt(_D2all)) / (_G / Math.sqrt(_D2)) + "\n");
			Hacks.append2file(logFile, "\n");
		}

		return replannerIds;
	}
}
