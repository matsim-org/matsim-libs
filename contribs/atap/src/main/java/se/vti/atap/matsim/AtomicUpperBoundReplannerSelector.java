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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import se.vti.utils.misc.fileio.Hacks;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class AtomicUpperBoundReplannerSelector extends AbstractReplannerSelector {

	// -------------------- CONSTANTS --------------------

	private final double eps = 1e-8;

	private final boolean checkDistance = true;

	private final boolean logReplanningProcess = true;

	private final ATAPConfigGroup.DistanceTransformation distanceTransformation;

	// -------------------- MEMBERS --------------------

	private PopulationDistance populationDistance = null;

	// -------------------- CONSTRUCTION --------------------

	AtomicUpperBoundReplannerSelector(final Function<Integer, Double> iterationToEta,
			final ATAPConfigGroup.DistanceTransformation distanceTransformation) {
		super(iterationToEta);
		this.distanceTransformation = distanceTransformation;
	}

	// -------------------- INTERNALS --------------------

	private double _Q(final double _G, final double _Dsum, final double gamma, final double _DsumMax) {
		final double transformedD = this.distanceTransformation.transform(_Dsum, _DsumMax);
		return (_G - gamma) / Math.max(this.eps, transformedD);
	}

	private Map<Id<Person>, Double> createdPersonId2D2WithoutSelf(Map<Id<Person>, Double> personId2bParam,
			Set<Id<Person>> replannerIds) {
		double _D2 = 0.5 * replannerIds.stream().mapToDouble(r -> personId2bParam.get(r)).sum();
		Map<Id<Person>, Double> personId2D2withoutSelf = new LinkedHashMap<>(personId2bParam.size());
		for (Map.Entry<Id<Person>, Double> p2bEntry : personId2bParam.entrySet()) {
			Id<Person> personId = p2bEntry.getKey();
			if (replannerIds.contains(personId)) {
				personId2D2withoutSelf.put(personId,
						_D2 + this.populationDistance.getACoefficient(personId, personId) - p2bEntry.getValue());
			} else {
				personId2D2withoutSelf.put(personId, _D2);
			}
		}
		return personId2D2withoutSelf;
	}

	private double _Dsum(Map<Id<Person>, Double> personId2D2withoutSelf) {
		return personId2D2withoutSelf.values().stream().mapToDouble(d2 -> Math.sqrt(Math.max(this.eps, d2))).sum();
	}

	private double computeDSumChangeWhenAddingToReplanners(Id<Person> switcherId,
			Map<Id<Person>, Double> personId2D2withoutSelf, Map<Id<Person>, Double> personId2bCoeff,
			Set<Id<Person>> replannerIds) {
		assert (!replannerIds.contains(switcherId));
		double result = 0.0;
		for (Id<Person> personId : personId2D2withoutSelf.keySet()) {
			if (!personId.equals(switcherId)) {
				final double d2WithoutSelf = personId2D2withoutSelf.get(personId);
				result += Math.sqrt(Math.max(this.eps,
						d2WithoutSelf + personId2bCoeff.get(switcherId)
								+ this.populationDistance.getACoefficient(switcherId, switcherId)
								- (replannerIds.contains(personId)
										? this.populationDistance.getACoefficient(switcherId, personId)
												+ this.populationDistance.getACoefficient(personId, switcherId)
										: 0.0)));
				result -= Math.sqrt(Math.max(this.eps, d2WithoutSelf));
			}
		}
		return result;
	}

	private double computeDSumChangeWhenRemovingFromReplanners(Id<Person> switcherId,
			Map<Id<Person>, Double> personId2D2withoutSelf, Map<Id<Person>, Double> personId2bCoeff,
			Set<Id<Person>> replannerIds) {
		assert (replannerIds.contains(switcherId));
		double result = 0.0;
		for (Id<Person> personId : personId2D2withoutSelf.keySet()) {
			if (!personId.equals(switcherId)) {
				final double d2WithoutSelf = personId2D2withoutSelf.get(personId);
				result += Math.sqrt(Math.max(this.eps,
						d2WithoutSelf - personId2bCoeff.get(switcherId)
								+ this.populationDistance.getACoefficient(switcherId, switcherId)
								+ (replannerIds.contains(personId)
										? this.populationDistance.getACoefficient(switcherId, personId)
												+ this.populationDistance.getACoefficient(personId, switcherId)
										: 0.0)));
				result -= Math.sqrt(Math.max(this.eps, d2WithoutSelf));
			}
		}
		return result;
	}

	// --------------- OVERRIDING OF AbstractReplannerSelector ---------------

	@Override
	void setDistanceToReplannedPopulation(final PopulationDistance populationDistance) {
		this.populationDistance = populationDistance;
	}

	@Override
	Set<Id<Person>> selectReplannersHook(Map<Id<Person>, Double> personId2gap) {

		/*
		 * (1) Initialize.
		 */

		// Start with a maximum amount of replanning gap.
		final Set<Id<Person>> replannerIds = personId2gap.entrySet().stream().filter(e -> e.getValue() > 0.0)
				.map(e -> e.getKey()).collect(Collectors.toSet());
		if (replannerIds.size() == 0) {
			return Collections.emptySet();
		}

		final Map<Id<Person>, Double> personId2bParam = new LinkedHashMap<>(personId2gap.size());
		for (Id<Person> personId : personId2gap.keySet()) {
			double b = 0.0;
			for (Id<Person> replannerId : replannerIds) {
				b += this.populationDistance.getACoefficient(replannerId, personId)
						+ this.populationDistance.getACoefficient(personId, replannerId);
			}
			personId2bParam.put(personId, b);
		}

		Map<Id<Person>, Double> personId2D2withoutSelf = this.createdPersonId2D2WithoutSelf(personId2bParam,
				replannerIds);

		final String logFile = "exact-replanning.log";
		if (this.logReplanningProcess) {
			Hacks.append2file(logFile, "strictly positive gaps: "
					+ ((double) personId2gap.size()) / ((double) personId2gap.size()) + "\n");
			Hacks.append2file(logFile, "G(lambda)\tDSum(lambda)\tQ(lambda)\n");
		}

		final double _Gall = personId2gap.entrySet().stream().mapToDouble(e -> e.getValue()).sum();

		double _G = replannerIds.stream().mapToDouble(r -> personId2gap.get(r)).sum();
		double _Dsum = personId2D2withoutSelf.values().stream().mapToDouble(d2 -> Math.sqrt(Math.max(this.eps, d2)))
				.sum();
		final double _DsumMax = Math.max(0.0, _Dsum);

		/*
		 * (2) Repeatedly switch (non)replanners.
		 */

		final List<Id<Person>> allPersonIds = new LinkedList<>(personId2gap.keySet());
		boolean switched = true;

		while (switched) {

			if (this.logReplanningProcess) {
				Hacks.append2file(logFile, _G + "\t" + Math.sqrt(_Dsum) + "\t"
						+ this._Q(_G, _Dsum, this.getTargetReplanningRate() * _Gall, _DsumMax) + "\n");
			}

			switched = false;
			Collections.shuffle(allPersonIds);

			for (Id<Person> candidateId : allPersonIds) {

				final double candidateGap = personId2gap.get(candidateId);

				final double deltaG;
				final double deltaDsum;
				if (replannerIds.contains(candidateId)) {
					deltaG = -candidateGap;
					deltaDsum = this.computeDSumChangeWhenRemovingFromReplanners(candidateId, personId2D2withoutSelf,
							personId2bParam, replannerIds);
				} else /* candidate is NOT a replanner */ {
					deltaG = +candidateGap;
					deltaDsum = this.computeDSumChangeWhenAddingToReplanners(candidateId, personId2D2withoutSelf,
							personId2bParam, replannerIds);
				}

				// attention, now we maximize

				final double oldQ = this._Q(_G, _Dsum, this.getTargetReplanningRate() * _Gall, _DsumMax);
				final double newQ = this._Q(_G + deltaG, _Dsum + deltaDsum, this.getTargetReplanningRate() * _Gall, _DsumMax);

				if (newQ > oldQ) { // TODO robustify
					_G = Math.max(0.0, _G + deltaG);
					_Dsum = Math.max(0.0, _Dsum + deltaDsum);

					final double deltaSign;
					if (replannerIds.contains(candidateId)) {
						replannerIds.remove(candidateId);
						deltaSign = -1.0;
					} else /* candidate is NOT a replanner */ {
						replannerIds.add(candidateId);
						deltaSign = +1.0;
					}
					for (Id<Person> personId : personId2gap.keySet()) {
						final double deltaB = deltaSign
								* (this.populationDistance.getACoefficient(candidateId, personId)
										+ this.populationDistance.getACoefficient(personId, candidateId));
						personId2bParam.compute(personId, (id, b2) -> b2 + deltaB);
					}
					personId2D2withoutSelf = this.createdPersonId2D2WithoutSelf(personId2bParam, replannerIds);
					switched = true;

					if (this.checkDistance) {
						final double _Gchecked = personId2gap.entrySet().stream()
								.filter(e -> replannerIds.contains(e.getKey())).mapToDouble(e -> e.getValue()).sum();
						final double _DsumChecked = this._Dsum(personId2D2withoutSelf);
						final boolean gErr = Math.abs(_Gchecked - _G) > 1e-4;
						final boolean d2Err = Math.abs(_DsumChecked - _Dsum) > 1e-4;
						if (gErr || d2Err) {
							String msg = "";
							if (gErr) {
								msg += "\nrecursive _G = " + _G + ", but checked _G = " + _Gchecked;
							}
							if (d2Err) {
								msg += "\nrecursive _Daum = " + _Dsum + ", but checked _Dsum = " + _DsumChecked;
							}
							throw new RuntimeException(msg);
						}
					}
				}
			}
		}

		return replannerIds;
	}
}
