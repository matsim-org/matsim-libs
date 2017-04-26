/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.michalm.jtrrouter;

import java.util.*;

import org.apache.commons.configuration.*;
import org.matsim.contrib.util.random.*;

/**
 * @author michalm
 */
public abstract class JTRRouter {
	private static final int MAX_NODE_ID = 100;

	protected final Flow[] flows;
	private final Turn[][] turns;// [prev_node_id][current_node_id] -> Turn
	private int genStartTime;
	private int genStopTime;
	private double flowFactor;
	private double genPeriod;

	private final UniformRandom uniform;

	// temp var for route constraction
	private List<Integer> seq = new ArrayList<>();
	private Flow inFlow;

	public JTRRouter() {
		flows = new Flow[MAX_NODE_ID];
		turns = new Turn[MAX_NODE_ID][];

		for (int i = 0; i < MAX_NODE_ID; i++) {
			turns[i] = new Turn[MAX_NODE_ID];
		}

		uniform = RandomUtils.getGlobalUniform();
	}

	public void generate(String dir, String flowsFile, String turnsFile) {
		readConfigs(dir, flowsFile, turnsFile);
		// long t1 = System.currentTimeMillis();
		generateRoutes();
		// long t2 = System.currentTimeMillis();
		generatePlans();
		// long t3 = System.currentTimeMillis();
		writePlans(dir);
		// System.out.println("routes:" + (t2-t1));
		// System.out.println("plans:" + (t3-t2));
	}

	protected void readConfigs(String dir, String flowsFile, String turnsFile) {

		// process flows.xml
		//
		// <flows startTime="0" stopTime="3600">
		// ....
		// ....
		// </flows>

		try {
			HierarchicalConfiguration flowCfg = new XMLConfiguration(dir + "\\" + flowsFile);

			genStartTime = flowCfg.getInt("[@startTime]");
			genStopTime = flowCfg.getInt("[@stopTime]");
			genPeriod = genStopTime - genStartTime;

			flowFactor = flowCfg.getDouble("[@flowFactor]");

			int count = flowCfg.getMaxIndex("flow") + 1;
			for (int i = 0; i < count; i++) {
				initFlow((HierarchicalConfiguration)flowCfg.subset("flow(" + i + ')'));
			}

			// process turns.xml
			HierarchicalConfiguration nodeCfg = new XMLConfiguration(dir + "\\" + turnsFile);

			count = nodeCfg.getMaxIndex("turn") + 1;
			for (int i = 0; i < count; i++) {
				initTurn((HierarchicalConfiguration)nodeCfg.subset("turn(" + i + ')'));
			}
		} catch (ConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

	protected abstract void initFlow(HierarchicalConfiguration flowCfg);

	// <turn id="1" prev="11" type="cross">
	// ....<next node="2" probability="0.671"/>
	// ....<next node="3" probability="0.329"/>
	// </turn>
	protected void initTurn(HierarchicalConfiguration nodeCfg) {
		int id = nodeCfg.getInt("[@id]");
		int prev = nodeCfg.getInt("[@prev]");
		int length = nodeCfg.getMaxIndex("next") + 1;

		int[] nodes = new int[length];
		double[] probs = new double[length];

		for (int i = 0; i < length; i++) {
			Configuration nextCfg = nodeCfg.subset("next(" + i + ')');
			nodes[i] = nextCfg.getInt("[@node]");
			probs[i] = nextCfg.getDouble("[@probability]");
		}

		turns[prev][id] = new Turn(id, prev, nodes, probs);
	}

	protected void generateRoutes() {
		for (int i = 0; i < flows.length; i++) {
			inFlow = flows[i];

			if (inFlow == null || !inFlow.isInFlow) {
				continue;
			}

			addTurn(inFlow.node, inFlow.next, 1);

			System.out.println(i + " " + inFlow.routes.size());

			// for (int j = 0; j < inFlow.routes.size(); j++) {
			// System.out.println(inFlow.routes.get(j).toString());
			// }
		}

	}

	private void addTurn(int prev, int node, double p) {
		Turn turn = turns[prev][node];

		if (turn == null) {
			inFlow.routes.add(new Route(inFlow, flows[node], seq, p));
			return;
		} else if (flows[node] != null) {// just check correctness
			throw new RuntimeException();// only: flow XOR turn
		}

		if (turn.visited) {
			return;
		}

		seq.add(node);
		turn.visited = true;

		for (int i = 0; i < turn.next.length; i++) {
			addTurn(node, turn.next[i], p * turn.probs[i]);
		}

		turn.visited = false;
		if (seq.remove(seq.size() - 1) != node) {// just check correctness
			throw new RuntimeException("Error");
		}
	}

	protected void generatePlans() {
		int id = 1;

		for (int i = 0; i < flows.length; i++) {
			Flow flow = flows[i];

			if (flow == null) {
				continue;
			}

			List<Route> routes = flow.routes;
			double[] cumProb = new double[routes.size()];
			double totalProb = 0;

			for (int r = 0; r < cumProb.length; r++) {
				totalProb += routes.get(r).prob;
				cumProb[r] = totalProb;
			}

			for (int subFlow = 0; subFlow < flow.counts.length; subFlow++) {
				int count = (int)Math.round(flowFactor * flow.counts[subFlow]);

				double interval = genPeriod / count;

				for (int v = 0; v < count; v++) {
					int startTime = genStartTime + (int)(v * interval);

					// select route
					Route route = null;
					double random = uniform.nextDouble(0, totalProb);

					for (int r = 0; r < cumProb.length; r++) {
						if (cumProb[r] > random) {
							route = routes.get(r);
							break;
						}
					}

					addPlan(id, startTime, route, subFlow);

					id++;
				}
			}
		}
	}

	protected abstract void addPlan(int id, int startTime, Route route, int subFlow);

	protected abstract void writePlans(String dir);
}
