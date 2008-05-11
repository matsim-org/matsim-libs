/* *********************************************************************** *
 * project: org.matsim.*
 * PersonSetActChains.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.plans.algorithms;

import java.text.DecimalFormat;

import org.apache.log4j.Logger;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;

public class PersonSetActChains extends PersonAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// menber variables
	//////////////////////////////////////////////////////////////////////

	private final String [] work_chains = {"hwh","hwlwh","hwwh","hwswh","hwlh"
																				,"hwsh","hlwh","hswh","hweh"};
	private final double [] work_probs  = {0.2641,0.0306,0.0175,0.0174,0.0098
																				,0.0078,0.0071,0.0046,0.0007};

	private final String [] educ_chains = {"heh","helh","heeh","hleh","hesh"};
	private final double [] educ_probs  = {0.1217,0.0040,0.0020,0.0010,0.0005};

	private final String [] shop_chains = {"hsh","hslh","hlslh","hssh","hlsh"};
	private final double [] shop_probs  = {0.1663,0.0157,0.0108,0.0087,0.0080};

	private final String [] leis_chains = {"hlh","hllh"};
	private final double [] leis_probs  = {0.2774,0.0243};

	private final String [] age0_5_groups = {"leis"};
	private final double [] age0_5_probs  = new double[1];

	private final String [] age6_17_groups = {"educ","shop","leis"};
	private final double [] age6_17_probs  = new double[3];

	private final String [] age18_26_groups = {"work","educ","shop","leis"};
	private final double [] age18_26_probs  = new double[4];

	private final String [] age27_64_groups = {"work","shop","leis"};
	private final double [] age27_64_probs  = new double[3];

	private final String [] age65_99_groups = {"shop","leis"};
	private final double [] age65_99_probs  = new double[2];

	private final int [] chain_cnt = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
	
	private final static Logger log = Logger.getLogger(PersonSetActChains.class);

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonSetActChains() {
		super();
		this.initProbs();
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final void initProbs() {

		// calc the sum of probs of each group
		double work_sum_prob = 0.0;
		for (int i=0; i<this.work_probs.length; i++) {
			work_sum_prob += this.work_probs[i];
		}

		double educ_sum_prob = 0.0;
		for (int i=0; i<this.educ_probs.length; i++) {
			educ_sum_prob += this.educ_probs[i];
		}

		double shop_sum_prob = 0.0;
		for (int i=0; i<this.shop_probs.length; i++) {
			shop_sum_prob += this.shop_probs[i];
		}

		double leis_sum_prob = 0.0;
		for (int i=0; i<this.leis_probs.length; i++) {
			leis_sum_prob += this.leis_probs[i];
		}

		// calc the sum of groups for each age
		double age0_5_sum_probs = leis_sum_prob;
		double age6_17_sum_probs = educ_sum_prob + shop_sum_prob + leis_sum_prob;
		double age18_26_sum_probs = work_sum_prob + educ_sum_prob + shop_sum_prob + leis_sum_prob;
		double age27_64_sum_probs = work_sum_prob + shop_sum_prob + leis_sum_prob;
		double age65_99_sum_probs = shop_sum_prob + leis_sum_prob;

		// calc the transition prob of the groups of each age
		this.age0_5_probs[0] = leis_sum_prob / age0_5_sum_probs;

		this.age6_17_probs[0] = educ_sum_prob / age6_17_sum_probs;
		this.age6_17_probs[1] = shop_sum_prob / age6_17_sum_probs;
		this.age6_17_probs[2] = leis_sum_prob / age6_17_sum_probs;

		this.age18_26_probs[0] = work_sum_prob / age18_26_sum_probs;
		this.age18_26_probs[1] = educ_sum_prob / age18_26_sum_probs;
		this.age18_26_probs[2] = shop_sum_prob / age18_26_sum_probs;
		this.age18_26_probs[3] = leis_sum_prob / age18_26_sum_probs;

		this.age27_64_probs[0] = work_sum_prob / age27_64_sum_probs;
		this.age27_64_probs[1] = shop_sum_prob / age27_64_sum_probs;
		this.age27_64_probs[2] = leis_sum_prob / age27_64_sum_probs;

		this.age65_99_probs[0] = shop_sum_prob / age65_99_sum_probs;
		this.age65_99_probs[1] = leis_sum_prob / age65_99_sum_probs;

		// normalize the probs of each pattern such that the sum of those
		// is 1 for each group
		for (int i=0; i<this.work_probs.length; i++) {
			this.work_probs[i] = this.work_probs[i] / work_sum_prob;
		}

		for (int i=0; i<this.educ_probs.length; i++) {
			this.educ_probs[i] = this.educ_probs[i] / educ_sum_prob;
		}

		for (int i=0; i<this.shop_probs.length; i++) {
			this.shop_probs[i] = this.shop_probs[i] / shop_sum_prob;
		}

		for (int i=0; i<this.leis_probs.length; i++) {
			this.leis_probs[i] = this.leis_probs[i] / leis_sum_prob;
		}
	}

	private final String getChain(final String [] chains, final double [] probs) {

		double [] probsum = new double[probs.length];
		for (int i=0; i<probsum.length; i++) {
			probsum[i] = 0.0;
			for (int j=0; j<=i; j++) {
				probsum[i] += probs[j];
			}
		}

		double rd = Gbl.random.nextDouble();
		int index;
		for (index=0; index<probsum.length; index++) {
			if (probsum[index] > rd) {
				break;
			}
		}
		if (index >= probsum.length) {
			index = probsum.length - 1;
			log.warn("[index was running over the end of the array! Doublecheck what was going on]");
		}

		return chains[index];
	}

	private final String getGroup(final String [] groups, final double [] probs) {
		return this.getChain(groups,probs);
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(final Person person) {

		String group = null;
		if ((person.getAge() >= 0) && (person.getAge() < 6)) {
			group = this.getGroup(this.age0_5_groups,this.age0_5_probs);
		}
		else if ((person.getAge() >= 6) && (person.getAge() < 18)) {
			group = this.getGroup(this.age6_17_groups,this.age6_17_probs);
		}
		else if ((person.getAge() >= 18) && (person.getAge() < 27)) {
			group = this.getGroup(this.age18_26_groups,this.age18_26_probs);
		}
		else if ((person.getAge() >= 27) && (person.getAge() < 65)) {
			group = this.getGroup(this.age27_64_groups,this.age27_64_probs);
		}
		else if ((person.getAge() >= 65) && (person.getAge() < 100)) {
			group = this.getGroup(this.age65_99_groups,this.age65_99_probs);
		}
		else {
			throw new RuntimeException("The age of person id=" + person.getId() + " is out of range [0,99]");
		}

		String chain = null;
		if (group.equals("work")) {
			chain = this.getChain(this.work_chains,this.work_probs);
		}
		else if (group.equals("educ")) {
			chain = this.getChain(this.educ_chains,this.educ_probs);
		}
		else if (group.equals("shop")) {
			chain = this.getChain(this.shop_chains,this.shop_probs);
		}
		else if (group.equals("leis")) {
			chain = this.getChain(this.leis_chains,this.leis_probs);
		}
		else {
			throw new RuntimeException("For some reason the group is wrong (group=" + group + ")");
		}

		person.getPlans().clear();
		Plan p = person.createPlan(null, "yes");

		String [] acttypes = chain.split("");
		// note: by splitting, the string[0] is always = ""!
		int leg_cnt = 0;
		for (int j=1; j<acttypes.length; j++) {
			try {
				if (j != 1) {
					p.createLeg(Integer.toString(leg_cnt),"car",null,null,null);
					leg_cnt++;
				}
				p.createAct(acttypes[j],"-1","-1",null,"00:00:00", "00:00:00", "00:00:00", null);
			}
			catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}

		int cnt = 0;
		for (int i=0; i<this.work_chains.length; i++) {
			if (chain.equals(this.work_chains[i])) {
				this.chain_cnt[cnt]++;
			}
			cnt++;
		}
		for (int i=0; i<this.educ_chains.length; i++) {
			if (chain.equals(this.educ_chains[i])) {
				this.chain_cnt[cnt]++;
			}
			cnt++;
		}
		for (int i=0; i<this.shop_chains.length; i++) {
			if (chain.equals(this.shop_chains[i])) {
				this.chain_cnt[cnt]++;
			}
			cnt++;
		}
		for (int i=0; i<this.leis_chains.length; i++) {
			if (chain.equals(this.leis_chains[i])) {
				this.chain_cnt[cnt]++;
			}
			cnt++;
		}
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	public final void print() {
		DecimalFormat formatter = new DecimalFormat("0.0000");

		double sum = 0.0;
		for (int i=0; i<this.chain_cnt.length; i++) {
			sum += this.chain_cnt[i];
		}
		System.out.println("----------------------------------------");
		System.out.println(this.getClass().getName() + ":");
		int cnt = 0;
		for (int i=0; i<this.work_chains.length; i++) {
			System.out.println(this.work_chains[i] + ":\t" + this.chain_cnt[cnt] + "\t--> " + formatter.format(this.chain_cnt[cnt]/sum));
			cnt++;
		}
		for (int i=0; i<this.educ_chains.length; i++) {
			System.out.println(this.educ_chains[i] + ":\t" + this.chain_cnt[cnt] + "\t--> " + formatter.format(this.chain_cnt[cnt]/sum));
			cnt++;
		}
		for (int i=0; i<this.shop_chains.length; i++) {
			System.out.println(this.shop_chains[i] + ":\t" + this.chain_cnt[cnt] + "\t--> " + formatter.format(this.chain_cnt[cnt]/sum));
			cnt++;
		}
		for (int i=0; i<this.leis_chains.length; i++) {
			System.out.println(this.leis_chains[i] + ":\t" + this.chain_cnt[cnt] + "\t--> " + formatter.format(this.chain_cnt[cnt]/sum));
			cnt++;
		}
		System.out.println("----------------------------------------");
	}
}
