/* *********************************************************************** *
 * project: org.matsim.*
 * SOTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.gregor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.core.gbl.MatsimRandom;

public class SOTest {


	private static final boolean MSA_INTERNAL = true;
	private static final boolean MSA_EXTERNAL = true;

	private static final double BETA = 3;//*60.;
	private static final int MAX_PLANS = 6;
	private static Map<String,double[]> ttLookUp = new HashMap<String, double[]>();
	private static Map<String,double[]> xLookUp = new HashMap<String, double[]>();
	static {

		//travel time lookup table
		ttLookUp.put("000",new double []{5,7,9});
		ttLookUp.put("001",new double []{5,7,8});
		ttLookUp.put("010",new double []{5,8,6});
		ttLookUp.put("011",new double []{5,8,8});
		ttLookUp.put("100",new double []{8,5,7});
		ttLookUp.put("101",new double []{8,5,8});
		ttLookUp.put("110",new double []{8,8,5});
		ttLookUp.put("111",new double []{5,7,9});


		//		//Laemmel external costs
		//		xLookUp.put("000",new double []{6,3,0});
		//		xLookUp.put("001",new double []{2,0,0});
		//		xLookUp.put("010",new double []{1,0,0});
		//		xLookUp.put("011",new double []{0,0,0});
		//		xLookUp.put("100",new double []{0,2,0});
		//		xLookUp.put("101",new double []{0,0,0});
		//		xLookUp.put("110",new double []{0,0,0});
		//		xLookUp.put("111",new double []{0,0,0});

		//Kaddoura external costs
		xLookUp.put("000",new double []{3,3,0});
		xLookUp.put("001",new double []{2,0,0});
		xLookUp.put("010",new double []{1,0,0});
		xLookUp.put("011",new double []{0,0,0});
		xLookUp.put("100",new double []{0,2,0});
		xLookUp.put("101",new double []{0,0,0});
		xLookUp.put("110",new double []{0,0,0});
		xLookUp.put("111",new double []{0,0,0});
	}

	private final BufferedWriter bf;


	public SOTest(BufferedWriter bf) {
		this.bf = bf;
	}

	public static void main(String [] args) throws Exception {
		BufferedWriter bf = new BufferedWriter(new FileWriter(new File("/Users/laemmel/devel/sone/so")));
		for (int round = 0; round < 100; round++) {
			new SOTest(bf).run();
		}
		bf.close();

	}

	private int it;

	private void run() {
		MatsimRandom.reset(System.currentTimeMillis());
		List<Agent> agents = new ArrayList<Agent>();
		for (int i = 0; i< 3; i++) {
			Agent a0 = new Agent();
			a0.selected = 0;
			Plan p1 = new Plan();
			p1.strategy = MatsimRandom.getRandom().nextInt(2);
			a0.plans.add(p1);
			agents.add(a0);
		}
		for (this.it = 0; this.it < 100000; this.it++) {
			scoreAgents(agents);
			//			report(agents);
			replan(agents);
		}

	}

	private void replan(List<Agent> agents) {
		int [] strategies = new int[3];
		for (int i = 0; i< 3; i++) {
			strategies[i] = getStrategy(agents.get(i));
		}

		for (int i = 0; i < 3; i++) {
			Agent a = agents.get(i);
			if (this.it < 80000 && MatsimRandom.getRandom().nextDouble() < 0.1) { //10% re-route
				explorate(a,i,strategies);
			} else {
				select(a);
			}
		}


	}



	private int getStrategy(Agent agent) {
		Plan p = agent.plans.get(agent.selected);
		return p.strategy;
	}

	private void explorate(Agent a,int idx,int [] strategies) {
		Plan p = retrieveWorst(a);

		StringBuffer s0 = new StringBuffer();
		for (int i = 0; i < idx; i++) {
			s0.append(strategies[i]);
		}
		s0.append(0);
		for (int i = idx+1; i < 3; i++) {
			s0.append(strategies[i]);
		}
		StringBuffer s1 = new StringBuffer();
		for (int i = 0; i < idx; i++) {
			s1.append(strategies[i]);
		}
		s1.append(1);
		for (int i = idx+1; i < 3; i++) {
			s1.append(strategies[i]);
		}


		//assumed travel costs for either strategy
		double sc0 = -a.t0- a.x; 
		double sc1 = -a.t1;
		if (sc0 > sc1) {
			p.strategy = 0;
		} else {
			p.strategy = 1;
		}
	}

	private Plan retrieveWorst(Agent a) {
		if (a.plans.size() < MAX_PLANS) {
			Plan p = new Plan();
			a.selected = a.plans.size();
			a.plans.add(p);
			return p;
		}
		Plan w = null;
		double s = 0;
		int idx = 0;

		for (int i = 0; i < MAX_PLANS; i++) {
			Plan p = a.plans.get(i);
			if (p.score < s) {
				s = p.score;
				w = p;
				idx = i;
			}
		}
		a.selected = idx;
		return w;
	}

	private void report(List<Agent> agents) {

		StringBuffer key = new StringBuffer();
		StringBuffer sc = new StringBuffer();
		for (Agent a : agents) {
			Plan p = a.plans.get(a.selected);
			sc.append(p.score + " ");
			key.append(p.strategy);
		}
		System.out.println(key.toString() + " " + sc.toString());


	}

	private void select(Agent a) {
		Plan selected = a.plans.get(a.selected);
		int rIdx = MatsimRandom.getRandom().nextInt(a.plans.size());
		Plan random = a.plans.get(rIdx);
		double diff = random.score - selected.score;
		double tmp = 0.5 * BETA * diff;
		double weight = Math.exp(tmp);
		double proba = 0.01*weight;
		if (MatsimRandom.getRandom().nextDouble() < proba ) { // as of now, 0.01 is hardcoded (proba to change when both
			a.selected = rIdx;
		}
	}



	private void scoreAgents(List<Agent> agents) {
		StringBuffer key = new StringBuffer();
		for (Agent a : agents) {
			Plan p = a.plans.get(a.selected);
			key.append(p.strategy);
		}
		double[] tt = ttLookUp.get(key.toString());
		double[] x = xLookUp.get(key.toString());
		double travelTime = 0;
		for (int i = 0; i < 3; i++) {
			Agent a = agents.get(i);
			Plan p = a.plans.get(a.selected);
			if (p.strategy == 0) {

				if (MSA_EXTERNAL) {
					//external w/ MSA
					a.x = a.x * this.it/(this.it+1.) + x[i]/(this.it+1.);
				} else {
					//external w/o MSA
					a.x = x[i];
				}

				if (MSA_INTERNAL) {
					//internal w/ MSA
					a.t0 = a.t0 * this.it/(this.it+1.) + tt[i]/(this.it+1.);
				} else {
					//internal w/o MSA
					a.t0 = tt[i];
				}

				p.score = -x[i]-tt[i];
				travelTime+=tt[i];
			} else {
				if (MSA_EXTERNAL) {
					//external w/ MSA
					a.x = a.x * this.it/(this.it+1.);
				} else {
					//external w/o MSA
					a.x = 0;
				}

				if (MSA_INTERNAL) {
					//internal w/ MSA
					a.t0 = a.t0 * this.it/(this.it+1.) + 5./(this.it+1.);
				} else {
					//internal w/ MSA
					a.t0 = 5;
				}

				p.score = -a.t1;
				travelTime+=8;
			}
		}
		//		System.out.println(this.it + " " + (travelTime/3.+4));
		if (this.it < 99000) {
			return;
		}
		try {
			double rnd = (MatsimRandom.getRandom().nextDouble()-0.5)/4;
			this.bf.append(this.it + " " + (travelTime/3.+4+rnd) + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private  class Plan {
		double score;
		int strategy;
		@Override
		public String toString() {
			return this.strategy + " " + this.score + " ";
		}
	}

	private  class Agent{
		int selected;
		double x = 0;
		double t0 = 5;
		double t1 = 8;
		List<Plan> plans = new ArrayList<Plan>();

		@Override
		public String toString() {
			return this.plans.get(this.selected).toString();
		}
	}
}
