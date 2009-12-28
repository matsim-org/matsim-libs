/* *********************************************************************** *
 * project: org.matsim.*
 * PersonWriteActivitySpaceTable.java
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

package playground.jhackney.activitySpaces;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PersonImpl;
import org.matsim.knowledges.KnowledgeImpl;
import org.matsim.knowledges.Knowledges;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

import playground.jhackney.optimization.BeanObjective;
import playground.jhackney.optimization.CassiniObjective;
import playground.jhackney.optimization.EllipseObjective;
import playground.jhackney.optimization.SuperEllipseObjective;


public class PersonWriteActivitySpaceTable extends AbstractPersonAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member constants
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private FileWriter fw = null;
	private BufferedWriter out = null;
	private Knowledges knowledges;

	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////

	public PersonWriteActivitySpaceTable(Knowledges kn) {
		super();
		this.knowledges = kn;
		try {
			fw = new FileWriter("output/person-act-space-table.txt");
			out = new BufferedWriter(fw);
			out.write("pid\tage\tsex\tcar_avail\tlicence\tct_type\tn_loc\tshape\tarea\tcover\tx\ty\ttheta\ta\tb\tr\n");
			out.flush();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}

	//////////////////////////////////////////////////////////////////////
	// final method
	//////////////////////////////////////////////////////////////////////

	public final void close() {
		try {
			out.flush();
			out.close();
			fw.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	// routine for calculating the area of a cassini
	private double rSquare(double aa, double bb, double ang) {
		return aa*aa*(Math.cos(2*ang)+Math.sqrt(Math.pow((bb/aa),4)-Math.pow(Math.sin(2*ang),2)));
	}

	//	routine for calculating the value of the gamma function
	//  which is a prerequesite for area of a superellipse
	private double dgamma(double x) {
		int k;
		int n = x<1.5 ? -((int)(2.5-x)) : (int)(x-1.5);
		double w = x-(n+2);
		double y = (((((((((((((-1.99542863674e-7 * w + 1.337767384067e-6) * w - 2.591225267689e-6)
				* w - 1.7545539395205e-5)
				* w + 1.45596568617526e-4)
				* w - 3.60837876648255e-4)
				* w - 8.04329819255744e-4)
				* w + 0.008023273027855346)
				* w - 0.017645244547851414)
				* w - 0.024552490005641278)
				* w + 0.19109110138763841)
				* w - 0.233093736421782878)
				* w - 0.422784335098466784)
				* w + 0.99999999999999999);
		if (n > 0) {
			w = x - 1;
			for (k = 2; k <= n; k++) { w *= x - k; }
		} else {
			w = 1;
			for (k = 0; k > n; k--) { y *= x - k; }
		}
		return w / y;
	}

	// ////////////////////////////////////////////////////////////////////
	// run method
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person person) {
		final KnowledgeImpl know = this.knowledges.getKnowledgesByPersonId().get(person.getId());
		if (know == null) {
			Gbl.errorMsg("Knowledge is not defined!");
		}

		// 0=all; 1=home; 2=work; 3=educ; 4=shop; 5=leis
//		int [] nof_locs = {-1,-1,-1,-1,-1,-1};
		// 0=cassini; 1=ellipse; 2=bean; 3=superellipse
		double [] area_all = {Double.NaN,Double.NaN,Double.NaN,Double.NaN};
		double [] area_home = {Double.NaN,Double.NaN,Double.NaN,Double.NaN};
		double [] area_shop = {Double.NaN,Double.NaN,Double.NaN,Double.NaN};
		double [] area_work = {Double.NaN,Double.NaN,Double.NaN,Double.NaN};
		double [] area_leis = {Double.NaN,Double.NaN,Double.NaN,Double.NaN};
		double [] area_educ = {Double.NaN,Double.NaN,Double.NaN,Double.NaN};

		// calc the number of visits per activity type
		TreeMap<String, Integer> loc_cnts = new TreeMap<String, Integer>();
		Iterator<String> at_it = know.getActivityTypes().iterator();
		while (at_it.hasNext()) {
			String act_type = at_it.next();
			int nof_loc = know.getActivities(act_type).size();
			loc_cnts.put(act_type,nof_loc);
		}
		// add also the act_type "all" with the sum of all visits
		loc_cnts.put("all",know.getActivities().size());

		try {
			Id pid = person.getId();
			List<ActivitySpace> act_spaces = ActivitySpaces.getActivitySpaces(person);
			for (int i=0; i<act_spaces.size(); i++) {
				ActivitySpace as = act_spaces.get(i);
				String act_type = as.getActType();
				String as_type = null;
				if (as instanceof ActivitySpaceEllipse) {
					ActivitySpaceEllipse ase = (ActivitySpaceEllipse)as;
					as_type = EllipseObjective.OBJECTIVE_NAME;
					double x = ase.getParam("x").doubleValue();
					double y = ase.getParam("y").doubleValue();
					double theta = ase.getParam("theta").doubleValue();
					double a = ase.getParam("a").doubleValue();
					double b = ase.getParam("b").doubleValue();
					double cover = ase.getParam("cover").doubleValue();

					double area = Math.PI*a*b;

					out.write(pid + "\t" + ((PersonImpl) person).getAge() + "\t" + 
										((PersonImpl) person).getSex() + "\t" + 
										((PersonImpl) person).getCarAvail() + "\t"+ 
										((PersonImpl) person).getLicense() + "\t"+ act_type + "\t" +
					          loc_cnts.get(act_type) + "\t" + as_type + "\t" +
					          area + "\t" + cover + "\t" + x + "\t" + y + "\t" +
					          theta + "\t" + a + "\t" + b + "\n");

					if (act_type.equals("all")) { area_all[1] = area; }
					else if (act_type.equals("home")) { area_home[1] = area; }
					else if (act_type.equals("work")) { area_work[1] = area; }
					else if (act_type.equals("education")) { area_educ[1] = area; }
					else if (act_type.equals("shop")) { area_shop[1] = area; }
					else if (act_type.equals("leisure")) { area_leis[1] = area; }
					else { Gbl.errorMsg("SOMETHING IS WRONG!"); }
				}
				else if (as instanceof ActivitySpaceCassini) {
					ActivitySpaceCassini asc = (ActivitySpaceCassini)as;
					as_type = CassiniObjective.OBJECTIVE_NAME;
					double x = asc.getParam("x").doubleValue();
					double y = asc.getParam("y").doubleValue();
					double theta = asc.getParam("theta").doubleValue();
					double a = asc.getParam("a").doubleValue();
					double b = asc.getParam("b").doubleValue();
					double cover = asc.getParam("cover").doubleValue();

					double area = 0.0;
					// area Integral
					double stepSize = (Math.PI/2.0)/25.0; // Denominator decides accuracy level
					double angle = -1.0*Math.PI/4.0;
					while(angle < Math.PI/4.0){
						area +=((rSquare(a,b,angle)+rSquare(a,b,angle+stepSize))*stepSize)/2.0;
						angle += stepSize;
					}

					out.write(pid + "\t" + ((PersonImpl) person).getAge() + "\t" + ((PersonImpl) person).getSex() + "\t" + 
										((PersonImpl) person).getCarAvail() + "\t"+ ((PersonImpl) person).getLicense() + "\t"+ act_type + "\t" +
					          loc_cnts.get(act_type) + "\t" + as_type + "\t" +
					          area + "\t" + cover + "\t" + x + "\t" + y + "\t" +
					          theta + "\t" + a + "\t" + b + "\n");

					if (act_type.equals("all")) { area_all[0] = area; }
					else if (act_type.equals("home")) { area_home[0] = area; }
					else if (act_type.equals("work")) { area_work[0] = area; }
					else if (act_type.equals("education")) { area_educ[0] = area; }
					else if (act_type.equals("shop")) { area_shop[0] = area; }
					else if (act_type.equals("leisure")) { area_leis[0] = area; }
					else { Gbl.errorMsg("SOMETHING IS WRONG!"); }
				}
				else if (as instanceof ActivitySpaceSuperEllipse) {
					ActivitySpaceSuperEllipse ass = (ActivitySpaceSuperEllipse)as;
					as_type = SuperEllipseObjective.OBJECTIVE_NAME;
					double x = ass.getParam("x").doubleValue();
					double y = ass.getParam("y").doubleValue();
					double theta = ass.getParam("theta").doubleValue();
					double a = ass.getParam("a").doubleValue();
					double b = ass.getParam("b").doubleValue();
					double cover = ass.getParam("cover").doubleValue();
					double r = ass.getParam("r").doubleValue();

					double area = a*b*Math.sqrt(Math.PI)*(Math.pow(4.0,1.0-1.0/r)*dgamma(1.0+1.0/r)/dgamma(0.5+1/r));

					out.write(pid + "\t" + ((PersonImpl) person).getAge() + "\t" + ((PersonImpl) person).getSex() + "\t" + 
										((PersonImpl) person).getCarAvail() + "\t"+ ((PersonImpl) person).getLicense() + "\t"+ act_type + "\t" +
					          loc_cnts.get(act_type) + "\t" + as_type + "\t" +
					          area + "\t" + cover + "\t" + x + "\t" + y + "\t" +
					          theta + "\t" + a + "\t" + b + "\t" + r + "\n");

					if (act_type.equals("all")) { area_all[3] = area; }
					else if (act_type.equals("home")) { area_home[3] = area; }
					else if (act_type.equals("work")) { area_work[3] = area; }
					else if (act_type.equals("education")) { area_educ[3] = area; }
					else if (act_type.equals("shop")) { area_shop[3] = area; }
					else if (act_type.equals("leisure")) { area_leis[3] = area; }
					else { Gbl.errorMsg("SOMETHING IS WRONG!"); }
				}
				else if (as instanceof ActivitySpaceBean) {
					ActivitySpaceBean asb = (ActivitySpaceBean)as;
					as_type = BeanObjective.OBJECTIVE_NAME;
					double x = asb.getParam("x").doubleValue();
					double y = asb.getParam("y").doubleValue();
					double theta = asb.getParam("theta").doubleValue();
					double a = asb.getParam("a").doubleValue();
					double b = asb.getParam("b").doubleValue();
					double cover = asb.getParam("cover").doubleValue();

					// approximation
					// TODO: is that really correct? if yes, then the bean is something very special!
					double area = 1.058049*a*b;

					out.write(pid + "\t" + ((PersonImpl) person).getAge() + "\t" + ((PersonImpl) person).getSex() + "\t" + 
										((PersonImpl) person).getCarAvail() + "\t"+ ((PersonImpl) person).getLicense() + "\t"+ act_type + "\t" +
					          loc_cnts.get(act_type) + "\t" + as_type + "\t" +
					          area + "\t" + cover + "\t" + x + "\t" + y + "\t" +
					          theta + "\t" + a + "\t" + b + "\n");

					if (act_type.equals("all")) { area_all[2] = area; }
					else if (act_type.equals("home")) { area_home[2] = area; }
					else if (act_type.equals("work")) { area_work[2] = area; }
					else if (act_type.equals("education")) { area_educ[2] = area; }
					else if (act_type.equals("shop")) { area_shop[2] = area; }
					else if (act_type.equals("leisure")) { area_leis[2] = area; }
					else { Gbl.errorMsg("SOMETHING IS WRONG!"); }
				}
				else {
					Gbl.errorMsg("Something is completely wrong!");
				}
			}
			out.flush();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}
}
