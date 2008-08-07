/* *********************************************************************** *
 * project: org.matsim.*
 * PersonSubTourAnalysis.java
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

package playground.balmermi.algos;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.TreeMap;

import org.matsim.gbl.Gbl;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.utils.geometry.Coord;

public class PersonSubTourAnalysis extends AbstractPersonAlgorithm implements PlanAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final int WALK = 0;
	private static final int BIKE = 1;
	private static final int CAR = 2;
	private static final int PT = 3;
	private static final int UNDEF = 4;

	// #trips of SubTour versus # per mode
	private final TreeMap<Integer,Integer[]> trips_mode_cnt = new TreeMap<Integer,Integer[]>();

	// distance of SubTour versus # per mode
	private final TreeMap<Integer,Integer[]> dist_mode_cnt = new TreeMap<Integer,Integer[]>();

	// #trips of SubTour versus #SubTours
	private final TreeMap<Integer,Integer> trips_subtour_cnt = new TreeMap<Integer,Integer>();

	// distance of SubTour versus sum of distance per mode
	private final TreeMap<Integer,Double[]> dist_mode_sumdist = new TreeMap<Integer,Double[]>();

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonSubTourAnalysis() {
		super();
		System.out.println("    init " + this.getClass().getName() + " module...");
		System.out.println("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// private method
	//////////////////////////////////////////////////////////////////////

	private final void handleSubTour(final Plan plan, final int start, final int i, final int j, final int end) {
//		System.out.println("[" + start + "," + i + "]&[" + j + "," + end + "]");

		// calc number of trips
		int trip_cnt = ((i-start)/2) + ((end-j)/2);

		// get the subtour mode
		int idx = -1;
		String mode = ((Leg)plan.getActsLegs().get(start+1)).getMode();
		if (mode.equals("walk")) { idx = WALK; }
		else if (mode.equals("bike")) { idx = BIKE; }
		else if (mode.equals("car")) { idx = CAR; }
		else if (mode.equals("pt")) { idx = PT; }
		else if (mode.equals("undef")) { idx = UNDEF; }
		else { Gbl.errorMsg("pid=" + plan.getPerson().getId() + ": mode=" + mode + " unknown!"); }

		// calculate the SubTour distance (in 1 km time bins)
		double d = 0.0;
		Coord prev = ((Act)plan.getActsLegs().get(start)).getCoord();
		for (int k=start+2; k<=i; k=k+2) {
			Coord curr = ((Act)plan.getActsLegs().get(k)).getCoord();
			d = d + curr.calcDistance(prev);
			prev = curr;
		}
		prev = ((Act)plan.getActsLegs().get(j)).getCoord();
		for (int k=j+2; k<=end; k=k+2) {
			Coord curr = ((Act)plan.getActsLegs().get(k)).getCoord();
			d = d + curr.calcDistance(prev);
			prev = curr;
		}
		d = d/1000.0;
		Integer dist = (int)d; // [m]==>[km] (floor: 1.7km ==> 1km)

		// #trips of SubTour vs # per mode
		Integer[] mode_cnt = this.trips_mode_cnt.remove(trip_cnt);
		if (mode_cnt == null) { mode_cnt = new Integer[5]; for (int m=0; m<5; m++) { mode_cnt[m] = 0; } }
		mode_cnt[idx]++;
		this.trips_mode_cnt.put(trip_cnt,mode_cnt);

		// dist of SubTour vs # per mode
		mode_cnt = this.dist_mode_cnt.remove(dist);
		if (mode_cnt == null) { mode_cnt = new Integer[5]; for (int m=0; m<5; m++) { mode_cnt[m] = 0; } }
		mode_cnt[idx]++;
		this.dist_mode_cnt.put(dist,mode_cnt);

		// #trips of SubTour vs #SubTours
		Integer st_cnt = this.trips_subtour_cnt.remove(trip_cnt);
		if (st_cnt == null) { st_cnt = new Integer(0); }
		st_cnt++;
		this.trips_subtour_cnt.put(trip_cnt,st_cnt);

		// dist of SubTour vs sumdist per mode
		Double[] mode_dist = this.dist_mode_sumdist.remove(dist);
		if (mode_dist == null) { mode_dist = new Double[5]; for (int m=0; m<5; m++) { mode_dist[m] = 0.0; } }
		mode_dist[idx] += d;
		this.dist_mode_sumdist.put(dist,mode_dist);
	}

	private final void extractSubTours(Plan plan, int start, int end) {
		boolean is_leaf = true;
		for (int i=start+2; i<end-1; i=i+2) {
			Act acti = (Act)plan.getActsLegs().get(i);
			for (int j=end-2; j>i; j=j-2) {
				Act actj = (Act)plan.getActsLegs().get(j);
				if ((acti.getCoord().getX() == actj.getCoord().getX()) &&
				    (acti.getCoord().getY() == actj.getCoord().getY())) {
					// subtour found: start..i & j..end
					is_leaf = false;
					this.handleSubTour(plan,start,i,j,end);

					// next recursive step
					int ii = i;
					Act actii = acti;
					for (int jj=i+2; jj<=j; jj=jj+2) {
						Act actjj = (Act)plan.getActsLegs().get(jj);
						if ((actii.getCoord().getX() == actjj.getCoord().getX()) &&
						    (actii.getCoord().getY() == actjj.getCoord().getY())) {
							this.extractSubTours(plan,ii,jj);
							ii = jj;
							actii = (Act)plan.getActsLegs().get(ii);
						}
					}
					return;
				}
			}
		}
		if (is_leaf) {
			// leaf-sub-tour: start..end
			this.handleSubTour(plan,start,end,end,end);
			// TODO balmermi: check if this is all right
		}
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person person) {
		Plan plan = person.getSelectedPlan();
		if (plan == null) { Gbl.errorMsg("Person id=" + person.getId() + "does not have a selected plan."); }
		this.run(plan);
	}

	public void run(Plan plan) {
//		System.out.println("----------------------------------------");
//		System.out.println("pid=" + plan.getPerson().getId() + ":");
//		for (int i=0; i<plan.getActsLegs().size(); i=i+2) {
//			System.out.println("  " + i + ": " + ((Act)plan.getActsLegs().get(i)).getCoord());
//		}
		this.extractSubTours(plan,0,plan.getActsLegs().size()-1);
//		System.out.println("----------------------------------------");
	}

	//////////////////////////////////////////////////////////////////////
	// write methods
	//////////////////////////////////////////////////////////////////////

	// #trips of SubTour versus # per mode
	public final void writeSubtourTripCntVsModeCnt(String outfile) {
		try {
			FileWriter fw = new FileWriter(outfile);
			BufferedWriter out = new BufferedWriter(fw);
			out.write("TripsPerSubtour\tWalkCnt\tBikeCnt\tCarCnt\tPtCnt\tUndefCnt\n");
			Iterator<Integer> trip_it = this.trips_mode_cnt.keySet().iterator();
			while (trip_it.hasNext()) {
				Integer trip = trip_it.next();
				Integer[] mode_cnt = this.trips_mode_cnt.get(trip);
				out.write(trip.toString());
				for (int i=0; i<mode_cnt.length; i++) { out.write("\t" + mode_cnt[i]); }
				out.write("\n");
			}
			out.flush();
			out.close();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	// distance of SubTour versus # per mode
	public final void writeSubtourDistVsModeCnt(String outfile) {
		try {
			FileWriter fw = new FileWriter(outfile);
			BufferedWriter out = new BufferedWriter(fw);
			out.write("SubtourDist\tWalkCnt\tBikeCnt\tCarCnt\tPtCnt\tUndefCnt\n");
			Iterator<Integer> dist_it = this.dist_mode_cnt.keySet().iterator();
			while (dist_it.hasNext()) {
				Integer dist = dist_it.next();
				Integer[] mode_cnt = this.dist_mode_cnt.get(dist);
				out.write(dist.toString());
				for (int i=0; i<mode_cnt.length; i++) { out.write("\t" + mode_cnt[i]); }
				out.write("\n");
			}
			out.flush();
			out.close();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	// #trips of SubTour versus #SubTours
	public final void writeSubtourTripCntVsSubtourCnt(String outfile) {
		try {
			FileWriter fw = new FileWriter(outfile);
			BufferedWriter out = new BufferedWriter(fw);
			out.write("TripsPerSubtour\tSubTourCnt\n");
			Iterator<Integer> trip_it = this.trips_subtour_cnt.keySet().iterator();
			while (trip_it.hasNext()) {
				Integer trip = trip_it.next();
				Integer st_cnt = this.trips_subtour_cnt.get(trip);
				out.write(trip.toString() + "\t" + st_cnt + "\n");
			}
			out.flush();
			out.close();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	// distance of SubTour versus sum of distance per mode
	public final void writeSubtourDistVsModeDistSum(String outfile) {
		try {
			FileWriter fw = new FileWriter(outfile);
			BufferedWriter out = new BufferedWriter(fw);
			out.write("SubtourDist\tWalkDistSum\tBikeDistSum\tCarDistSum\tPtDistSum\tUndefDistSum\n");
			Iterator<Integer> dist_it = this.dist_mode_sumdist.keySet().iterator();
			while (dist_it.hasNext()) {
				Integer dist = dist_it.next();
				Double[] mode_distsum = this.dist_mode_sumdist.get(dist);
				out.write(dist.toString());
				for (int i=0; i<mode_distsum.length; i++) { out.write("\t" + mode_distsum[i]); }
				out.write("\n");
			}
			out.flush();
			out.close();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
