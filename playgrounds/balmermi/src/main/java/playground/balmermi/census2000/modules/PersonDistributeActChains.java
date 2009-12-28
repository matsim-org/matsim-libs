/* *********************************************************************** *
 * project: org.matsim.*
 * PersonDistributeActChains.java
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

package playground.balmermi.census2000.modules;

import java.util.ArrayList;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.balmermi.census2000.data.ActChains;

public class PersonDistributeActChains extends AbstractPersonAlgorithm implements PlanAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final String UNDEF = "undef";
	private static final String L = "l";
	private static final String S = "s";
	private static final String E = "e";
	private static final String W = "w";
	private static final String H = "h";

	private static final int rows = 8;
	private static final int cols = 16;
	private static final double[][] DISTR = new double[rows][cols];
	private final ActChains actchains;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonDistributeActChains(ActChains actchains) {
		System.out.println("    init " + this.getClass().getName() + " module...");
		this.actchains = actchains;
		DISTR[0][0]=0;
		DISTR[0][1]=1;
		DISTR[0][2]=0;
		DISTR[0][3]=0;
		DISTR[0][4]=0;
		DISTR[0][5]=0;
		DISTR[0][6]=0;
		DISTR[0][7]=0;
		DISTR[0][8]=0;
		DISTR[0][9]=0;
		DISTR[0][10]=0;
		DISTR[0][11]=0;
		DISTR[0][12]=0;
		DISTR[0][13]=0;
		DISTR[0][14]=0;
		DISTR[0][15]=0;
		DISTR[1][0]=0;
		DISTR[1][1]=0;
		DISTR[1][2]=0;
		DISTR[1][3]=0;
		DISTR[1][4]=0;
		DISTR[1][5]=0;
		DISTR[1][6]=0;
		DISTR[1][7]=0;
		DISTR[1][8]=0;
		DISTR[1][9]=0;
		DISTR[1][10]=0;
		DISTR[1][11]=0;
		DISTR[1][12]=0;
		DISTR[1][13]=0;
		DISTR[1][14]=0;
		DISTR[1][15]=0;
		DISTR[2][0]=0;
		DISTR[2][1]=0.263301659647184;
		DISTR[2][2]=0;
		DISTR[2][3]=0;
		DISTR[2][4]=0.354412573711777;
		DISTR[2][5]=0.382285766641039;
		DISTR[2][6]=0;
		DISTR[2][7]=0;
		DISTR[2][8]=0;
		DISTR[2][9]=0;
		DISTR[2][10]=0;
		DISTR[2][11]=0;
		DISTR[2][12]=0;
		DISTR[2][13]=0;
		DISTR[2][14]=0;
		DISTR[2][15]=0;
		DISTR[3][0]=0;
		DISTR[3][1]=0;
		DISTR[3][2]=0;
		DISTR[3][3]=0;
		DISTR[3][4]=0;
		DISTR[3][5]=0;
		DISTR[3][6]=0;
		DISTR[3][7]=0;
		DISTR[3][8]=0;
		DISTR[3][9]=0;
		DISTR[3][10]=0;
		DISTR[3][11]=0;
		DISTR[3][12]=0;
		DISTR[3][13]=0;
		DISTR[3][14]=0;
		DISTR[3][15]=0;
		DISTR[4][0]=0;
		DISTR[4][1]=0.128517328440751;
		DISTR[4][2]=0.201803536958672;
		DISTR[4][3]=0.231152127047925;
		DISTR[4][4]=0.172988492363784;
		DISTR[4][5]=0.186593375429019;
		DISTR[4][6]=0.0374002599612285;
		DISTR[4][7]=0.0415448797986206;
		DISTR[4][8]=0;
		DISTR[4][9]=0;
		DISTR[4][10]=0;
		DISTR[4][11]=0;
		DISTR[4][12]=0;
		DISTR[4][13]=0;
		DISTR[4][14]=0;
		DISTR[4][15]=0;
		DISTR[5][0]=0;
		DISTR[5][1]=0.00397661542945824;
		DISTR[5][2]=0.00624425568540405;
		DISTR[5][3]=0.00715236712529891;
		DISTR[5][4]=0.005352653344095;
		DISTR[5][5]=0.00577361905019534;
		DISTR[5][6]=0.00115724822972909;
		DISTR[5][7]=0.00128549209687584;
		DISTR[5][8]=0.331463280461527;
		DISTR[5][9]=0.325481534423424;
		DISTR[5][10]=0.145727403480194;
		DISTR[5][11]=0.135609910181325;
		DISTR[5][12]=0.00844079883682432;
		DISTR[5][13]=0.0112483993028043;
		DISTR[5][14]=0.00467933410996658;
		DISTR[5][15]=0.00640708824287731;
		DISTR[6][0]=0;
		DISTR[6][1]=0.228893161682755;
		DISTR[6][2]=0.359418065825483;
		DISTR[6][3]=0.411688772491762;
		DISTR[6][4]=0;
		DISTR[6][5]=0;
		DISTR[6][6]=0;
		DISTR[6][7]=0;
		DISTR[6][8]=0;
		DISTR[6][9]=0;
		DISTR[6][10]=0;
		DISTR[6][11]=0;
		DISTR[6][12]=0;
		DISTR[6][13]=0;
		DISTR[6][14]=0;
		DISTR[6][15]=0;
		DISTR[7][0]=0;
		DISTR[7][1]=0.00416113964056888;
		DISTR[7][2]=0.00653400368210165;
		DISTR[7][3]=0.00748425360634825;
		DISTR[7][4]=0;
		DISTR[7][5]=0;
		DISTR[7][6]=0;
		DISTR[7][7]=0;
		DISTR[7][8]=0.346843948123333;
		DISTR[7][9]=0.340584635147133;
		DISTR[7][10]=0.152489494167964;
		DISTR[7][11]=0.141902525632551;
		DISTR[7][12]=0;
		DISTR[7][13]=0;
		DISTR[7][14]=0;
		DISTR[7][15]=0;
		MatsimRandom.getRandom().nextDouble();
		System.out.println("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final void setChain(Person p, int bitcode) {
		ArrayList<ArrayList<Integer>> chains = this.actchains.getChains(bitcode);
		int index = MatsimRandom.getRandom().nextInt(chains.size());
		ArrayList<Integer> chain = chains.get(index);
		PlanImpl plan =  ((PersonImpl) p).createAndAddPlan(true);
		int time_sum = 0;
		for (int i=0; i<chain.size(); i=i+2) {
			int val = chain.get(i);
			String type = null;
			if (val == 16) { type = H; }
			else if (val == 8) { type = W; }
			else if (val == 4) { type = E; }
			else if (val == 2) { type = S; }
			else if (val == 1) { type = L; }
			else { Gbl.errorMsg("THIS SHOULD NEVER HAPPEN!"); }

			boolean primary = this.isPrimary(bitcode,val);

			if (i == chain.size()-1) {
				int start_time = time_sum;
				try {
					ActivityImpl a = plan.createAndAddActivity(type, new CoordImpl(0.0,0.0));
					a.setStartTime(start_time);
				}
				catch (Exception e) { Gbl.errorMsg(e); }
			}
			else {
				int start_time = time_sum;
				int dur = chain.get(i+1);
				time_sum += dur;
				int end_time = time_sum;
				try {
					ActivityImpl a = plan.createAndAddActivity(type, new CoordImpl(0.0,0.0));
					a.setStartTime(start_time);
					a.setEndTime(end_time);
					a.setDuration(dur);
					LegImpl l = plan.createAndAddLeg(TransportMode.undefined);
					l.setArrivalTime(end_time);
					l.setTravelTime(0);
					l.setDepartureTime(end_time);
				}
				catch (Exception e) { Gbl.errorMsg(e); }
			}
		}
	}

	private final boolean isPrimary(int bitcode, int type) {
		if (type == 8) { return true; } // work act
		if ((type == 4) && (bitcode < 8)) { return true; } // educ act
		return false;
	}
	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person person) {
		int age = ((PersonImpl) person).getAge();
		boolean employed = ((PersonImpl) person).isEmployed();
		int row = -1;
		if (age < 6) { row = 0; }
		else if (age < 8) { row = 2; }
		else if (age < 66) { row = 4; }
		else { row = 6; }
		if (employed) { row++; }
		double r = MatsimRandom.getRandom().nextDouble();
		double dist_sum = 0.0;
		for (int j=0; j<cols; j++) {
			dist_sum += DISTR[row][j];
			if (r < dist_sum) {
				this.setChain(person,j);
				break;
			}
		}
	}

	public void run(Plan plan) {
	}
}
