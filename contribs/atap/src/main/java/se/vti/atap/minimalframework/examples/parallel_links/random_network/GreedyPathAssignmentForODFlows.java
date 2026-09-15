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
package se.vti.atap.minimalframework.examples.parallel_links.random_network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import se.vti.atap.minimalframework.PlanInnovation;
import se.vti.atap.minimalframework.examples.parallel_links.AgentImpl;
import se.vti.atap.minimalframework.examples.parallel_links.Network;
import se.vti.atap.minimalframework.examples.parallel_links.NetworkConditionsImpl;
import se.vti.atap.minimalframework.examples.parallel_links.PathFlows;

/**
 * 
 * @author GunnarF
 * 
 */
public class GreedyPathAssignmentForODFlows implements PlanInnovation<AgentImpl, NetworkConditionsImpl> {

	private class SingleODBeckmanApproximation {

		private final AgentImpl odPair;

		private List<Integer> _H;

		private double[] s;
		private double[] c;
		private double[] g;
		private double[] v;

		public SingleODBeckmanApproximation(AgentImpl odPair) {
			this.odPair = odPair;
		}

		public double[] computeApproximatelyEquilibratedPathFlows_veh(NetworkConditionsImpl networkConditions) {

			if (this._H == null) {
				this._H = new ArrayList<>(this.odPair.getNumberOfPaths());
				for (int path = 0; path < this.odPair.getNumberOfPaths(); path++) {
					if (this.odPair.getCurrentPlan().pathFlows_veh[path] > 0) {
						this._H.add(path);
					}
				}
				this.s = new double[this.odPair.getNumberOfPaths()];
				this.c = new double[this.odPair.getNumberOfPaths()];
				this.g = new double[this.odPair.getNumberOfPaths()];
				this.v = new double[this.odPair.getNumberOfPaths()];
			}

			int bestPath = this.odPair.computeBestPath(networkConditions);
			if (!this._H.contains(bestPath)) {
				this._H.add(bestPath);
			}

			for (int h : this._H) {
				int ij = this.odPair.availableLinks[h];
				this.g[h] = this.odPair.getCurrentPlan().pathFlows_veh[h];
				this.v[h] = networkConditions.linkTravelTimes_s[ij];
				this.s[h] = networkConditions.dLinkTravelTimes_dLinkFlows_s_veh[ij];
				this.c[h] = this.v[h] - this.s[h] * this.g[h];
			}

			Collections.sort(this._H, new Comparator<>() {
				@Override
				public int compare(Integer h1, Integer h2) {
					return Double.compare(c[h1], c[h2]);
				}
			});

			double d = this.odPair.size_veh;

			List<Integer> _Hhat = new ArrayList<>(this._H.size());
			Iterator<Integer> hIterator = this._H.iterator();

			double _B = 0;
			double _C = 0;
			double w;

			int h = hIterator.next();
			do {
				_B += 1.0 / (this.s[h] * d);
				_C += this.c[h] / (this.s[h] * d);
				w = (1.0 + _C) / _B;

				_Hhat.add(h);

				if (hIterator.hasNext()) {
					h = hIterator.next();
				} else {
					h = -1;
				}
			} while ((h >= 0) && (this.c[h] < w));

			this._H.clear();
			this._H.addAll(_Hhat);

			double[] f = new double[this.odPair.getNumberOfPaths()];
			for (int h2 : _Hhat) {
				f[h2] = (w - this.c[h2]) / this.s[h2];
			}
			return f;
		}
	}

	private final NetworkConditionsImpl initialNetworkConditions;

	private Random rnd = null;

	private final Map<AgentImpl, SingleODBeckmanApproximation> od2beckman = new LinkedHashMap<>();

	public GreedyPathAssignmentForODFlows(Network network) {
		this.initialNetworkConditions = NetworkConditionsImpl.createEmptyNetworkConditions(network);
	}

	public GreedyPathAssignmentForODFlows setRandomizing(Random rnd) {
		this.rnd = rnd;
		return this;
	}

	private double[] computeRandomPathFlows_veh(AgentImpl odPair) {
		double[] pathFlows_veh = new double[odPair.getNumberOfPaths()];
		
		do {
			for (int path = 0; path < odPair.getNumberOfPaths(); path++) {
				pathFlows_veh[path] = this.rnd.nextDouble();
			}
		} while (Arrays.stream(pathFlows_veh).sum() < 1e-8);
		
		double scale = odPair.size_veh / Arrays.stream(pathFlows_veh).sum();
		for (int path = 0; path < odPair.getNumberOfPaths(); path++) {
			pathFlows_veh[path] *= scale;
		}
		return pathFlows_veh;
	}

	@Override
	public void assignInitialPlan(AgentImpl odPair) {
		if (this.rnd != null) {
			double[] pathFlows_veh = this.computeRandomPathFlows_veh(odPair);
			odPair.setCurrentPlan(new PathFlows(pathFlows_veh));
		} else {
			double[] pathFlows_veh = new double[odPair.getNumberOfPaths()];
			pathFlows_veh[odPair.computeBestPath(this.initialNetworkConditions)] = odPair.size_veh;
			odPair.setCurrentPlan(new PathFlows(pathFlows_veh));
		}
		this.od2beckman.put(odPair, new SingleODBeckmanApproximation(odPair));
	}

	@Override
	public void assignCandidatePlan(AgentImpl odPair, NetworkConditionsImpl networkConditions) {
		if (this.rnd != null) {
			double[] pathFlows_veh = this.computeRandomPathFlows_veh(odPair);
			odPair.setCandidatePlan(new PathFlows(pathFlows_veh));
		} else {
			double[] newPathFlows_veh = this.od2beckman.get(odPair)
					.computeApproximatelyEquilibratedPathFlows_veh(networkConditions);
			odPair.setCandidatePlan(new PathFlows(newPathFlows_veh));	
		}		
	}
}
