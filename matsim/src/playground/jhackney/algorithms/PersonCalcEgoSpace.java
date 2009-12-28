package playground.jhackney.algorithms;

/* *********************************************************************** *
 * project: org.matsim.*
 * PersonCalcEgoSpace.java
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

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.knowledges.KnowledgeImpl;
import org.matsim.knowledges.Knowledges;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

import playground.jhackney.activitySpaces.ActivitySpace;
import playground.jhackney.activitySpaces.ActivitySpaces;
import playground.jhackney.optimization.BeanObjective;
import playground.jhackney.optimization.CassiniObjective;
import playground.jhackney.optimization.EllipseObjective;
import playground.jhackney.optimization.Objective;
import playground.jhackney.optimization.ParamPoint;
import playground.jhackney.optimization.SimplexOptimization;
import playground.jhackney.optimization.SuperEllipseObjective;
import playground.jhackney.socialnetworks.socialnet.EgoNet;

public class PersonCalcEgoSpace extends AbstractPersonAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member constants
	//////////////////////////////////////////////////////////////////////

	// ellipse, bean, cassini, superellipse
//	private static final String activity_shape = EllipseObjective.OBJECTIVE_NAME;
	private String activity_shape = null;
	private Knowledges knowledges;
//	private static final String [] activity_shapes = {CassiniObjective.OBJECTIVE_NAME,BeanObjective.OBJECTIVE_NAME,SuperEllipseObjective.OBJECTIVE_NAME,EllipseObjective.OBJECTIVE_NAME};
	private static final String [] activity_shapes = {EllipseObjective.OBJECTIVE_NAME};
	private static final double theta_stepsize = Math.PI / 8.0;

//	private static final int shape = 1;
	// 0.0 < coverage <= 1.0
	private static final double coverage = 0.95;

	private final static Logger log = Logger.getLogger(PersonCalcEgoSpace.class);

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////

//	public PersonCalcActivitySpace() {
//	super();
//	}

	public PersonCalcEgoSpace(Knowledges knowledges) {
		super();
		this.knowledges = knowledges;
	}

	//////////////////////////////////////////////////////////////////////
	// run method
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person person) {

		// Check if knowledge exists

		final KnowledgeImpl know = this.knowledges.getKnowledgesByPersonId().get(person.getId());

		if (know == null) {
			throw new RuntimeException("Knowledge is not defined!");
		}

		final EgoNet egoNet = (EgoNet)person.getCustomAttributes().get(EgoNet.NAME);
		if(egoNet == null){
			throw new RuntimeException("EgoNet is not defined!");
		}
		
		// make an arraylist called activities of all the home activities of the ego and his alters
		ArrayList<ActivityOptionImpl> activities=new ArrayList<ActivityOptionImpl>();
		// use morning home, the first act in each selected plan
		Iterator<Person> e_it=egoNet.getAlters().iterator();
		while(e_it.hasNext()){
			activities.add( ((ActivityImpl)(e_it.next().getSelectedPlan().getPlanElements().get(0))).getFacility().getActivityOptions().get("home"));
		}
		
		Iterator<ActivityOptionImpl> a_it = null;
		// get all home activities of the alters and the ego
		a_it = activities.iterator();
		
		
		// Creating coordinate list
		ArrayList<Coord> coords = new ArrayList<Coord>();
		while (a_it.hasNext()) { coords.add(a_it.next().getFacility().getCoord()); }

		System.out.println("----------------------------------------------------------------------");
		System.out.println("Person id           = " + person.getId());
		System.out.println("Number of locations = " + coords.size());

		if (coords.size() < 1) {
			log.info("There is less than 1 location for Person id = " + person.getId() + ". Therefore, no shape will be calculated.");
		}
		else {
			// Calculating the center and the distance of the given coorinates

			double cenX;
			double cenY; // CENTER 
			int k;

			double min_x = Double.POSITIVE_INFINITY;
			double min_y = Double.POSITIVE_INFINITY;
			double max_x = Double.NEGATIVE_INFINITY;
			double max_y = Double.NEGATIVE_INFINITY;

			for( k=0;k<coords.size();k++)
			{
				double curr_x = coords.get(k).getX();
				double curr_y = coords.get(k).getY();
				if (curr_x < min_x) { min_x = curr_x; }
				if (curr_y < min_y) { min_y = curr_y; }
				if (curr_x > max_x) { max_x = curr_x; }
				if (curr_y > max_y) { max_y = curr_y; }
			}
			cenX = (min_x + max_x)/2.0;
			cenY = (min_y + max_y)/2.0;

			double distance = Math.sqrt(((max_x-min_x)/2.0)*((max_x-min_x)/2.0) + ((max_y-min_y)/2.0)*((max_y-min_y)/2.0));

			System.out.println("  cenX     = " + cenX);
			System.out.println("  cenY     = " + cenY);
			System.out.println("  distance = " + distance);

			// initialize the objective function

			Objective objFunc = null;
			double theta_start = Double.MIN_VALUE;
			double theta_end = Double.MAX_VALUE;

			for (int s=0; s<activity_shapes.length; s++) {
				activity_shape = activity_shapes[s];

				System.out.println("    activity shape = " + activity_shape);

				if (activity_shape.equals("ellipse")) {
					theta_start = Math.PI / 4.0;
					theta_end = -1.0 * Math.PI / 4.0;
					objFunc = new EllipseObjective(coords, coverage, theta_start);			

					ParamPoint p0 = objFunc.getNewParamPoint();
					p0.setValue(EllipseObjective.X_idx,cenX);
					p0.setValue(EllipseObjective.Y_idx,cenY);
					p0.setValue(EllipseObjective.RATIO_idx,1.0);
					objFunc.setInitParamPoint(p0,0);

					ParamPoint p1 = objFunc.getNewParamPoint();
					p1.setValue(EllipseObjective.X_idx,cenX);
					p1.setValue(EllipseObjective.Y_idx,cenY+distance);
					p1.setValue(EllipseObjective.RATIO_idx,1.0);
					objFunc.setInitParamPoint(p1,1);

					ParamPoint p2 = objFunc.getNewParamPoint();
					p2.setValue(EllipseObjective.X_idx,cenX+distance);
					p2.setValue(EllipseObjective.Y_idx,cenY);
					p2.setValue(EllipseObjective.RATIO_idx,1.0);
					objFunc.setInitParamPoint(p2,2);

					ParamPoint p3 = objFunc.getNewParamPoint();
					p3.setValue(EllipseObjective.X_idx,cenX);
					p3.setValue(EllipseObjective.Y_idx,cenY);
					p3.setValue(EllipseObjective.RATIO_idx,0.5);
					objFunc.setInitParamPoint(p3,3);

				}
				else if (activity_shape.equals("cassini")) {
					theta_start = Math.PI/2.0;
					theta_end = -Math.PI/2.0;
					objFunc = new CassiniObjective(coords, coverage, theta_start);			

					ParamPoint p0 = objFunc.getNewParamPoint();
					p0.setValue(CassiniObjective.X_idx,cenX);
					p0.setValue(CassiniObjective.Y_idx,cenY);
					p0.setValue(CassiniObjective.RATIO_idx,1.1);
					objFunc.setInitParamPoint(p0,0);

					ParamPoint p1 = objFunc.getNewParamPoint();
					p1.setValue(CassiniObjective.X_idx,cenX);
					p1.setValue(CassiniObjective.Y_idx,cenY+distance);
					p1.setValue(CassiniObjective.RATIO_idx,1.1);
					objFunc.setInitParamPoint(p1,1);

					ParamPoint p2 = objFunc.getNewParamPoint();
					p2.setValue(CassiniObjective.X_idx,cenX+distance);
					p2.setValue(CassiniObjective.Y_idx,cenY);
					p2.setValue(CassiniObjective.RATIO_idx,1.1);
					objFunc.setInitParamPoint(p2,2);

					ParamPoint p3 = objFunc.getNewParamPoint();
					p3.setValue(CassiniObjective.X_idx,cenX);
					p3.setValue(CassiniObjective.Y_idx,cenY);
					p3.setValue(CassiniObjective.RATIO_idx,5.0);
					objFunc.setInitParamPoint(p3,3);

				}
				else if (activity_shape.equals("superellipse")) {
					theta_start = Math.PI / 4.0;
					theta_end = -1.0 * Math.PI / 4.0;
					objFunc = new SuperEllipseObjective(coords, coverage, theta_start);			

					ParamPoint p0 = objFunc.getNewParamPoint();
					p0.setValue(SuperEllipseObjective.X_idx, cenX);
					p0.setValue(SuperEllipseObjective.Y_idx, cenY);
					p0.setValue(SuperEllipseObjective.RATIO_idx, 1.1);
					p0.setValue(SuperEllipseObjective.R_idx, 0.1);
					objFunc.setInitParamPoint(p0, 0);

					ParamPoint p1 = objFunc.getNewParamPoint();
					p1.setValue(SuperEllipseObjective.X_idx, cenX);
					p1.setValue(SuperEllipseObjective.Y_idx, cenY+distance);
					p1.setValue(SuperEllipseObjective.RATIO_idx, 1.1);
					p1.setValue(SuperEllipseObjective.R_idx, 0.9);
					objFunc.setInitParamPoint(p1, 1);

					ParamPoint p2 = objFunc.getNewParamPoint();
					p2.setValue(SuperEllipseObjective.X_idx, cenX+distance);
					p2.setValue(SuperEllipseObjective.Y_idx, cenY);
					p2.setValue(SuperEllipseObjective.RATIO_idx, 1.1);
					p2.setValue(SuperEllipseObjective.R_idx, 0.1);
					objFunc.setInitParamPoint(p2, 2);

					ParamPoint p3 = objFunc.getNewParamPoint();
					p3.setValue(SuperEllipseObjective.X_idx, cenX);
					p3.setValue(SuperEllipseObjective.Y_idx, cenY);
					p3.setValue(SuperEllipseObjective.RATIO_idx, 1.1);
					p3.setValue(SuperEllipseObjective.R_idx, 0.9);
					objFunc.setInitParamPoint(p3,3);

					ParamPoint p4 = objFunc.getNewParamPoint();
					p4.setValue(SuperEllipseObjective.X_idx, cenX);
					p4.setValue(SuperEllipseObjective.Y_idx, cenY);
					p4.setValue(SuperEllipseObjective.RATIO_idx, 2.0);
					p4.setValue(SuperEllipseObjective.R_idx, 0.4);
					objFunc.setInitParamPoint(p4, 4);

				}

				else if (activity_shape.equals("bean")) {
					theta_start = Math.PI/2.0;
					theta_end = -Math.PI/2.0;
					objFunc = new BeanObjective(coords, coverage, theta_start);			

					ParamPoint p0 = objFunc.getNewParamPoint();
					p0.setValue(BeanObjective.X_idx, cenX);
					p0.setValue(BeanObjective.Y_idx, cenY);
					p0.setValue(BeanObjective.RATIO_idx, 1.0);
					objFunc.setInitParamPoint(p0, 0);

					ParamPoint p1 = objFunc.getNewParamPoint();
					p1.setValue(BeanObjective.X_idx, cenX);
					p1.setValue(BeanObjective.Y_idx, cenY + distance);
					p1.setValue(BeanObjective.RATIO_idx, 1.0);
					objFunc.setInitParamPoint(p1, 1);

					ParamPoint p2 = objFunc.getNewParamPoint();
					p2.setValue(BeanObjective.X_idx, cenX + distance);
					p2.setValue(BeanObjective.Y_idx, cenY);
					p2.setValue(BeanObjective.RATIO_idx, 1.0);
					objFunc.setInitParamPoint(p2, 2);

					ParamPoint p3 = objFunc.getNewParamPoint();
					p3.setValue(BeanObjective.X_idx, cenX);
					p3.setValue(BeanObjective.Y_idx, cenY);
					p3.setValue(BeanObjective.RATIO_idx, 0.5);
					objFunc.setInitParamPoint(p3, 3);

				}	
				else {
					throw new RuntimeException("Activity space type unknown!");
				}


				// Calculate the best response for each given angle
				// and keep the overall best

				ParamPoint best_param_point = null;
				double best_theta = Double.MAX_VALUE;
				double curr_theta = theta_start;
				while(curr_theta > theta_end) {

					System.out.println("      current theta = " + curr_theta);

					if (objFunc instanceof EllipseObjective) {
						((EllipseObjective)objFunc).setTheta(curr_theta);
					}
					else if (objFunc instanceof CassiniObjective) {
						((CassiniObjective)objFunc).setTheta(curr_theta);
					}
					else if (objFunc instanceof SuperEllipseObjective) {
						((SuperEllipseObjective)objFunc).setTheta(curr_theta);
					}
					else if (objFunc instanceof BeanObjective) {
						((BeanObjective)objFunc).setTheta(curr_theta);
					}
					else {
						throw new RuntimeException("Something is wrong!");
					}
					ParamPoint curr_best = SimplexOptimization.getBestParams(objFunc);
					if (best_param_point == null) {
						best_param_point = curr_best;
						best_theta = curr_theta;
					}

					if (objFunc.getResponse(curr_best) < objFunc.getResponse(best_param_point)) {
						best_param_point = curr_best;
						best_theta = curr_theta;
					}

					curr_theta -= theta_stepsize;

				}

				if (objFunc instanceof EllipseObjective) {
					((EllipseObjective)objFunc).setTheta(best_theta);
				}
				else if (objFunc instanceof CassiniObjective) {
					((CassiniObjective)objFunc).setTheta(best_theta);
				}
				else if (objFunc instanceof SuperEllipseObjective) {
					((SuperEllipseObjective)objFunc).setTheta(best_theta);
				}
				else if (objFunc instanceof BeanObjective) {
					((BeanObjective)objFunc).setTheta(best_theta);
				}
				else {
					throw new RuntimeException("Something is wrong!");
				}

				// Write the result on the STDOUT

				System.out.println("    best response = " + objFunc.getResponse(best_param_point));
				System.out.println("    best theta = " + objFunc.getParamMap(best_param_point).get("theta"));

				// Add the results to the data Structure

				ActivitySpace act_space = ActivitySpaces.createActivitySpace(activity_shape,"home", person);	// all, home, work, education, shop, leisure
				act_space.addParams(objFunc.getParamMap(best_param_point));

				Gbl.printElapsedTime();
			}
		}
		Gbl.printElapsedTime();
		System.out.println("----------------------------------------------------------------------");
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	// test run for checking ellipse act spaces
	// TODO: when the others (i.e. cassini) are also done,
	// generalize it, define params, and make the code nicer. balmermi
//	private void runEllipse(Person person, ArrayList<Coord> coords) {
//	final Knowledge know = person.getKnowledge();
//	if (know == null) {
//	Gbl.errorMsg(this.getClass(),"run(Person person)","Knowledge is not defined!");
//	}

//	String actType = "all";

//	// create a list of coordinates the person knows about
//	TreeMap<String, ActivityFacilities> af = know.getActivityFacilities();
//	ArrayList<Coord> coords = new ArrayList<Coord>();
//	Iterator<ActivityFacilities> afIter = af.values().iterator();
//	while (afIter.hasNext()) {
//	ActivityFacilities af2 = afIter.next();
//	String at = af2.getActType();
//	if (actType.equals(at) || actType.equals("all")) {
//	// only add those activities which match the desired activity type
//	TreeMap<Integer, Integer> freqs = af2.getFrequencies();
//	Iterator<Facility> fIter = af2.getFacilities().values().iterator();
//	while (fIter.hasNext()) {
//	Facility f = fIter.next();
//	Integer id = new Integer(f.getId());
//	int freq = freqs.get(id);
//	if (freq < 1) {
//	freq = 1;
//	}
//	for (int i = 0; i < freq; i++) {
//	coords.add(f.getLocation().getCenter());
//	}
//	}
//	}
//	}

//	try {
//	FileWriter fw = new FileWriter(scen + "_ellipse_pid" + person.getId() + ".txt");
//	BufferedWriter out = new BufferedWriter(fw);
//	Iterator a_it = coords.iterator();
//	while (a_it.hasNext()) {
//	Coord c = (Coord)a_it.next();
//	out.write(c.getX() + "\t" + c.getY() + "\n");
//	}
//	out.flush();
//	out.close();
//	fw.close();
//	}
//	catch (IOException e) {
//	e.printStackTrace();
//	System.exit( -1);
//	}

	// instanciate the correct objective function
//	String objectiveName = EllipseObjective.OBJECTIVE_NAME;
//	double cover = 0.95; // 0.0 <= cover <= 1.0
//	double theta = Math.PI/4.0;

//	double best_area = Double.MAX_VALUE;
//	double best_a = Double.MAX_VALUE;
//	double best_b = Double.MAX_VALUE;
//	double best_theta = Double.MAX_VALUE;
//	double best_x = Double.MAX_VALUE;
//	double best_y = Double.MAX_VALUE;

//	double thetaStep = Math.PI/8.0;
//	double cenX;
//	double cenY; // CENTER 
//	int k;

//	double min_x = Double.MAX_VALUE;
//	double min_y = Double.MAX_VALUE;
//	double max_x = Double.MIN_VALUE;
//	double max_y = Double.MIN_VALUE;

//	for( k=0;k<coords.size();k++)
//	{
//	double curr_x = coords.get(k).getX();
//	double curr_y = coords.get(k).getY();
//	if (curr_x < min_x) { min_x = curr_x; }
//	if (curr_y < min_y) { min_y = curr_y; }
//	if (curr_x > max_x) { max_x = curr_x; }
//	if (curr_y > max_y) { max_y = curr_y; }
//	}
//	cenX = (min_x + max_x)/2.0;
//	cenY = (min_y + max_y)/2.0;

//	double distance = Math.sqrt(((max_x-min_x)/2.0)*((max_x-min_x)/2.0) + ((max_y-min_y)/2.0)*((max_y-min_y)/2.0));

//	System.out.println("cenX          = " + cenX);
//	System.out.println("cenY          = " + cenY);
//	System.out.println("distance      = " + distance);
//	System.out.println("Nof locations = " +coords.size());

//	while(theta > -1*Math.PI/4.0)
//	{
//	Objective objFunc = new EllipseObjective(coords, cover, theta);

//	ParamPoint p0 = objFunc.getNewParamPoint();
//	p0.setValue(((EllipseObjective)objFunc).X_idx,cenX);
//	p0.setValue(((EllipseObjective)objFunc).Y_idx,cenY);
//	p0.setValue(((EllipseObjective)objFunc).RATIO_idx,1.0);
//	objFunc.setInitParamPoint(p0,0);

//	ParamPoint p1 = objFunc.getNewParamPoint();
//	p1.setValue(((EllipseObjective)objFunc).X_idx,cenX);
//	p1.setValue(((EllipseObjective)objFunc).Y_idx,cenY+distance);
//	p1.setValue(((EllipseObjective)objFunc).RATIO_idx,1.0);
//	objFunc.setInitParamPoint(p1,1);

//	ParamPoint p2 = objFunc.getNewParamPoint();
//	p2.setValue(((EllipseObjective)objFunc).X_idx,cenX+distance);
//	p2.setValue(((EllipseObjective)objFunc).Y_idx,cenY);
//	p2.setValue(((EllipseObjective)objFunc).RATIO_idx,1.0);
//	objFunc.setInitParamPoint(p2,2);

//	ParamPoint p3 = objFunc.getNewParamPoint();
//	p3.setValue(((EllipseObjective)objFunc).X_idx,cenX);
//	p3.setValue(((EllipseObjective)objFunc).Y_idx,cenY);
//	p3.setValue(((EllipseObjective)objFunc).RATIO_idx,0.5);
//	objFunc.setInitParamPoint(p3,3);

//	// find the activity space
////	ParamPoint best = SimplexOptimization.getBestParams(objFunc);

//	//System.out.println("Area: " + objFunc.getResponse(best) + "; theta = " + theta);

//	if (objFunc.getResponse(best) < best_area) {
//	best_area = objFunc.getResponse(best);
//	best_x = objFunc.getParamMap(best).get(((EllipseObjective)objFunc).X_name);
//	best_y = objFunc.getParamMap(best).get(((EllipseObjective)objFunc).Y_name);
//	best_theta = objFunc.getParamMap(best).get(((EllipseObjective)objFunc).THETA_name);
//	best_a = objFunc.getParamMap(best).get(((EllipseObjective)objFunc).A_name);
//	best_b = objFunc.getParamMap(best).get(((EllipseObjective)objFunc).B_name);
//	}
//	theta-=(thetaStep);
//	} //end of theta while loop

//	System.out.println("best_area  = " + best_area);
//	System.out.println("best theta = " + best_theta);
//	System.out.println();

//	try {
//	FileWriter fw = new FileWriter(scen + "_ellipse_pid" + person.getId() + ".gpl");
//	BufferedWriter out = new BufferedWriter(fw);
//	out.write("load \"act-space-style.gpl\"\n\n");
//	out.write("set output \"" + scen + "_ellipse_pid" + person.getId() + ".eps\"\n\n");
//	out.write("pid = " + person.getId() + "\n");
//	out.write("x0 = " + best_x + "\n");
//	out.write("y0 = " + best_y + "\n");
//	out.write("phi = " + best_theta + "\n");
//	out.write("a = " + best_a + "\n");
//	out.write("b = " + best_b + "\n\n");
//	out.write("plot [t=0.0000:2.0*pi] \"" + scen + "_ellipse_pid" + person.getId() +
//	".txt\" title \"locations pid=" + person.getId() + "\", \\\n");
//	out.write("a*((cos(t)))*cos(phi) - b*((sin(t)))*sin(phi) + x0, \\\n");
//	out.write("a*((cos(t)))*sin(phi) + b*((sin(t)))*cos(phi) + y0 title \"" + scen + "_ellipse pid=" + person.getId() + "\"\n");
//	out.flush();
//	out.close();
//	fw.close();
//	}
//	catch (IOException e) {
//	e.printStackTrace();
//	System.exit( -1);
//	}

//	ActivitySpace act_space = know.createActivitySpace("ellipse",actType);
//	TreeMap<String,Double> params = new TreeMap<String,Double>();
//	params.put("x",best_x);
//	params.put("y",best_y);
//	params.put("theta",best_theta);
//	params.put("a",best_a);
//	params.put("b",best_b);
//	params.put("cover",cover);
//	act_space.addParams(params);
//	}

//	// TODO: checking the cassini. balmermi
//	private void runCassini(Person person) {
//	final Knowledge know = person.getKnowledge();
//	if (know == null) {
//	Gbl.errorMsg(this.getClass(),"run(Person person)","Knowledge is not defined!");
//	}

//	String actType = "all";

//	// create a list of coordinates the person knows about
//	TreeMap<String, ActivityFacilities> af = know.getActivityFacilities();
//	ArrayList<Coord> coords = new ArrayList<Coord>();
//	Iterator<ActivityFacilities> afIter = af.values().iterator();
//	while (afIter.hasNext()) {
//	ActivityFacilities af2 = afIter.next();
//	String at = af2.getActType();
//	if (actType.equals(at) || actType.equals("all")) {
//	// only add those activities which match the desired activity type
//	TreeMap<Integer, Integer> freqs = af2.getFrequencies();
//	Iterator<Facility> fIter = af2.getFacilities().values().iterator();
//	while (fIter.hasNext()) {
//	Facility f = fIter.next();
//	Integer id = new Integer(f.getId());
//	int freq = freqs.get(id);
//	if (freq < 1) {
//	freq = 1;
//	}
//	for (int i = 0; i < freq; i++) {
//	coords.add(f.getLocation().getCenter());
//	}
//	}
//	}
//	}

//	try {
//	FileWriter fw = new FileWriter(scen + "_cassini_pid" + person.getId() + ".txt");
//	BufferedWriter out = new BufferedWriter(fw);
//	Iterator a_it = coords.iterator();
//	while (a_it.hasNext()) {
//	Coord c = (Coord)a_it.next();
//	out.write(c.getX() + "\t" + c.getY() + "\n");
//	}
//	out.flush();
//	out.close();
//	fw.close();
//	}
//	catch (IOException e) {
//	e.printStackTrace();
//	System.exit( -1);
//	}

//	// instanciate the correct objective function
//	String objectiveName = CassiniObjective.OBJECTIVE_NAME;
//	double cover = 0.95; // 0.0 <= cover <= 1.0
//	double theta = Math.PI/2.0;

//	double best_area = Double.MAX_VALUE;
//	double best_a = Double.MAX_VALUE;
//	double best_b = Double.MAX_VALUE;
//	double best_theta = Double.MAX_VALUE;
//	double best_x = Double.MIN_VALUE;
//	double best_y = Double.MAX_VALUE;
//	ArrayList tempalist = new ArrayList(); 

//	double thetaStep = Math.PI/8.0;
//	double cenX;
//	double cenY; // CENTER 
//	int k;

//	double min_x = Double.MAX_VALUE;
//	double min_y = Double.MAX_VALUE;
//	double max_x = Double.MIN_VALUE;
//	double max_y = Double.MIN_VALUE;

//	for( k=0;k<coords.size();k++)
//	{
//	double curr_x = coords.get(k).getX();
//	double curr_y = coords.get(k).getY();
//	if (curr_x < min_x) { min_x = curr_x; }
//	if (curr_y < min_y) { min_y = curr_y; }
//	if (curr_x > max_x) { max_x = curr_x; }
//	if (curr_y > max_y) { max_y = curr_y; }
//	}
//	cenX = (min_x + max_x)/2.0;
//	cenY = (min_y + max_y)/2.0;

//	double distance = Math.sqrt(((max_x-min_x)/2.0)*((max_x-min_x)/2.0) + ((max_y-min_y)/2.0)*((max_y-min_y)/2.0));

//	System.out.println("cenX  = " + cenX);
//	System.out.println("cenY  = " + cenY);

//	// DO THE SAME AS ABOVE

//	while(theta >= -1*Math.PI/2.0)
//	{
//	Objective objFunc = new CassiniObjective(coords, cover, theta);

//	// calc dist center_of_mass <--> farest location

//	ParamPoint p0 = objFunc.getNewParamPoint();
//	p0.setValue(((CassiniObjective)objFunc).X_idx,cenX);
//	p0.setValue(((CassiniObjective)objFunc).Y_idx,cenY);
//	p0.setValue(((CassiniObjective)objFunc).RATIO_idx,1.0);
//	objFunc.setInitParamPoint(p0,0);

//	ParamPoint p1 = objFunc.getNewParamPoint();
//	p1.setValue(((CassiniObjective)objFunc).X_idx,cenX);
//	p1.setValue(((CassiniObjective)objFunc).Y_idx,cenY+distance);
//	p1.setValue(((CassiniObjective)objFunc).RATIO_idx,1.1);
//	objFunc.setInitParamPoint(p1,1);

//	ParamPoint p2 = objFunc.getNewParamPoint();
//	p2.setValue(((CassiniObjective)objFunc).X_idx,cenX+distance);
//	p2.setValue(((CassiniObjective)objFunc).Y_idx,cenY);
//	p2.setValue(((CassiniObjective)objFunc).RATIO_idx,1.1);
//	objFunc.setInitParamPoint(p2,2);

//	ParamPoint p3 = objFunc.getNewParamPoint();
//	p3.setValue(((CassiniObjective)objFunc).X_idx,cenX);
//	p3.setValue(((CassiniObjective)objFunc).Y_idx,cenY);
//	p3.setValue(((CassiniObjective)objFunc).RATIO_idx,105.0);
//	objFunc.setInitParamPoint(p3,3);

//	// find the activity space
//	ParamPoint best = SimplexOptimization.getBestParams(objFunc);

////	System.out.println("Area: " + objFunc.getResponse(best) + "; theta = " + theta);

//	if (objFunc.getResponse(best) < best_area) {
//	best_area = objFunc.getResponse(best);
//	best_x = objFunc.getParamMap(best).get(((CassiniObjective)objFunc).X_name);
//	best_y = objFunc.getParamMap(best).get(((CassiniObjective)objFunc).Y_name);
//	best_theta = objFunc.getParamMap(best).get(((CassiniObjective)objFunc).THETA_name);
//	best_a = objFunc.getParamMap(best).get(((CassiniObjective)objFunc).A_name);
//	best_b = objFunc.getParamMap(best).get(((CassiniObjective)objFunc).B_name);
//	}
//	theta-=(thetaStep);
//	} //end of theta while loop

//	System.out.println("best_area  = " + best_area);
//	System.out.println("best theta = " + best_theta);
//	System.out.println();

//	try {
//	FileWriter fw = new FileWriter(scen + "_cassini_pid" + person.getId() + ".gpl");
//	BufferedWriter out = new BufferedWriter(fw);
//	out.write("load \"act-space-style.gpl\"\n\n");
//	out.write("set output \"" + scen +"_cassini_pid" + person.getId() + ".eps\"\n\n");
//	out.write("pid = " + person.getId() + "\n");
//	out.write("x0 = " + best_x + "\n");
//	out.write("y0 = " + best_y + "\n");
//	out.write("phi = " + best_theta + "\n");
//	out.write("a = " + best_a + "\n");
//	out.write("b = " + best_b + "\n\n");
//	out.write("plot [t=0.0000:2.0*pi] \"" + scen + "_cassini_pid" + person.getId() +
//	".txt\" title \"locations pid=" + person.getId() + "\", \\\n");
//	out.write("a*((cos(2*t)+((b/a)**4-(sin(2*t)/a)**2)**0.5)**0.5)*cos(phi+t)+ x0, \\\n");
//	out.write("a*((cos(2*t)+((b/a)**4-(sin(2*t)/a)**2)**0.5)**0.5)*sin(phi+t)+ y0 title \""+ scen + "_cassini pid=" + person.getId() + "\"\n");
//	out.flush();
//	out.close();
//	fw.close();
//	}
//	catch (IOException e) {
//	e.printStackTrace();
//	System.exit( -1);
//	}

//	ActivitySpace act_space = know.createActivitySpace("cassini",actType);
//	TreeMap<String,Double> params = new TreeMap<String,Double>();
//	params.put("x",best_x);
//	params.put("y",best_y);
//	params.put("theta",best_theta);
//	params.put("a",best_a);
//	params.put("b",best_b);
//	params.put("cover",cover);
//	act_space.addParams(params);
//	}


//	private void runSuperEllipse1(Person person) {
//	final Knowledge know = person.getKnowledge();
//	if (know == null) {
//	Gbl.errorMsg(this.getClass(),"run(Person person)","Knowledge is not defined!");
//	}

//	String actType = "all";

//	// create a list of coordinates the person knows about
//	TreeMap<String, ActivityFacilities> af = know.getActivityFacilities();
//	ArrayList<Coord> coords = new ArrayList<Coord>();
//	Iterator<ActivityFacilities> afIter = af.values().iterator();
//	while (afIter.hasNext()) {
//	ActivityFacilities af2 = afIter.next();
//	String at = af2.getActType();
//	if (actType.equals(at) || actType.equals("all")) {
//	// only add those activities which match the desired activity type
//	TreeMap<Integer, Integer> freqs = af2.getFrequencies();
//	Iterator<Facility> fIter = af2.getFacilities().values().iterator();
//	while (fIter.hasNext()) {
//	Facility f = fIter.next();
//	Integer id = new Integer(f.getId());
//	int freq = freqs.get(id);
//	if (freq < 1) {
//	freq = 1;
//	}
//	for (int i = 0; i < freq; i++) {
//	coords.add(f.getLocation().getCenter());
//	}
//	}
//	}
//	}

//	try {
//	FileWriter fw = new FileWriter(scen + "_super_ellipse_1_pid" + person.getId() + ".txt");
//	BufferedWriter out = new BufferedWriter(fw);
//	Iterator a_it = coords.iterator();
//	while (a_it.hasNext()) {
//	Coord c = (Coord)a_it.next();
//	out.write(c.getX() + "\t" + c.getY() + "\n");
//	}
//	out.flush();
//	out.close();
//	fw.close();
//	}
//	catch (IOException e) {
//	e.printStackTrace();
//	System.exit( -1);
//	}

//	// instanciate the correct objective function
//	String objectiveName = SuperEllipseObjective.OBJECTIVE_NAME;
//	double cover = 0.95; // 0.0 <= cover <= 1.0
//	double theta = Math.PI/4.0;

//	double best_area = Double.MAX_VALUE;
//	double best_a = Double.MAX_VALUE;
//	double best_b = Double.MAX_VALUE;
//	double best_theta = Double.MAX_VALUE;
//	double best_r = Double.MAX_VALUE;
//	double best_x = Double.MIN_VALUE;
//	double best_y = Double.MAX_VALUE;

//	double thetaStep = Math.PI/8.0;
//	double cenX;
//	double cenY; // CENTER 
//	int k;

//	double min_x = Double.MAX_VALUE;
//	double min_y = Double.MAX_VALUE;
//	double max_x = Double.MIN_VALUE;
//	double max_y = Double.MIN_VALUE;

//	for( k=0;k<coords.size();k++)
//	{
//	double curr_x = coords.get(k).getX();
//	double curr_y = coords.get(k).getY();
//	if (curr_x < min_x) { min_x = curr_x; }
//	if (curr_y < min_y) { min_y = curr_y; }
//	if (curr_x > max_x) { max_x = curr_x; }
//	if (curr_y > max_y) { max_y = curr_y; }
//	}
//	cenX = (min_x + max_x)/2.0;
//	cenY = (min_y + max_y)/2.0;

//	double distance = Math.sqrt(((max_x-min_x)/2.0)*((max_x-min_x)/2.0) + ((max_y-min_y)/2.0)*((max_y-min_y)/2.0));

//	System.out.println("cenX  = " + cenX);
//	System.out.println("cenY  = " + cenY);

//	while(theta >=-Math.PI/4.0)
//	{
//	Objective objFunc = new SuperEllipseObjective(coords, cover, theta);

//	ParamPoint p0 = objFunc.getNewParamPoint();
//	p0.setValue(((SuperEllipseObjective)objFunc).X_idx,cenX);
//	p0.setValue(((SuperEllipseObjective)objFunc).Y_idx,cenY);
//	p0.setValue(((SuperEllipseObjective)objFunc).RATIO_idx,1.1);
//	p0.setValue(((SuperEllipseObjective)objFunc).R_idx,0.1);
//	objFunc.setInitParamPoint(p0,0);

//	ParamPoint p1 = objFunc.getNewParamPoint();
//	p1.setValue(((SuperEllipseObjective)objFunc).X_idx,cenX);
//	p1.setValue(((SuperEllipseObjective)objFunc).Y_idx,cenY+distance);
//	p1.setValue(((SuperEllipseObjective)objFunc).RATIO_idx,1.1);
//	p1.setValue(((SuperEllipseObjective)objFunc).R_idx,0.9);
//	objFunc.setInitParamPoint(p1,1);

//	ParamPoint p2 = objFunc.getNewParamPoint();
//	p2.setValue(((SuperEllipseObjective)objFunc).X_idx,cenX+distance);
//	p2.setValue(((SuperEllipseObjective)objFunc).Y_idx,cenY);
//	p2.setValue(((SuperEllipseObjective)objFunc).RATIO_idx,1.1);
//	p2.setValue(((SuperEllipseObjective)objFunc).R_idx,0.1);
//	objFunc.setInitParamPoint(p2,2);

//	ParamPoint p3 = objFunc.getNewParamPoint();
//	p3.setValue(((SuperEllipseObjective)objFunc).X_idx,cenX);
//	p3.setValue(((SuperEllipseObjective)objFunc).Y_idx,cenY);
//	p3.setValue(((SuperEllipseObjective)objFunc).RATIO_idx,1.1);
//	p3.setValue(((SuperEllipseObjective)objFunc).R_idx,0.9);
//	objFunc.setInitParamPoint(p3,3);

//	ParamPoint p4 = objFunc.getNewParamPoint();
//	p4.setValue(((SuperEllipseObjective)objFunc).X_idx,cenX);
//	p4.setValue(((SuperEllipseObjective)objFunc).Y_idx,cenY);
//	p4.setValue(((SuperEllipseObjective)objFunc).RATIO_idx,2.0);
//	p4.setValue(((SuperEllipseObjective)objFunc).R_idx,0.4);
//	objFunc.setInitParamPoint(p4,4);

//	// find the activity space
//	ParamPoint best = SimplexOptimization.getBestParams(objFunc);

//	//System.out.println("Area: " + objFunc.getResponse(best) + "; theta = " + theta);

//	if (objFunc.getResponse(best) < best_area) {
//	best_area = objFunc.getResponse(best);
//	best_x = objFunc.getParamMap(best).get(((SuperEllipseObjective)objFunc).X_name);
//	best_y = objFunc.getParamMap(best).get(((SuperEllipseObjective)objFunc).Y_name);
//	best_theta = objFunc.getParamMap(best).get(((SuperEllipseObjective)objFunc).THETA_name);
//	best_r = objFunc.getParamMap(best).get(((SuperEllipseObjective)objFunc).R_name);
//	best_a = objFunc.getParamMap(best).get(((SuperEllipseObjective)objFunc).A_name);
//	best_b = objFunc.getParamMap(best).get(((SuperEllipseObjective)objFunc).B_name);
//	}
//	theta-=(thetaStep);
//	} //end of theta while loop

//	System.out.println("best_area  = " + best_area);
//	System.out.println("best theta = " + best_theta);
//	System.out.println("best r = " + best_r);
//	System.out.println();

//	try {
//	FileWriter fw = new FileWriter(scen + "_super_ellipse_1_pid" + person.getId() + ".gpl");
//	BufferedWriter out = new BufferedWriter(fw);
//	out.write("load \"act-space-style.gpl\"\n\n");
//	out.write("set output \"" + scen + "_super_ellipse_1_pid" + person.getId() + ".eps\"\n\n");
//	out.write("pid = " + person.getId() + "\n");
//	out.write("x0 = " + best_x + "\n");
//	out.write("y0 = " + best_y + "\n");
//	out.write("phi = " + best_theta + "\n");
//	out.write("a = " + best_a + "\n");
//	out.write("b = " + best_b + "\n");
//	out.write("r = " + best_r + "\n\n");

//	out.write("plot [t=0.0000:2.0*pi] \"" + scen + "_super_ellipse_1_pids" + person.getId() +
//	".txt\" title \"locations pid=" + person.getId() + "\", \\\n");
//	out.write("a*((cos(t))**(2.00/r))*cos(phi)-b*((sin(t))**(2.00/r))*sin(phi)+ x0, \\\n");
//	out.write("a*((cos(t))**(2.00/r))*sin(phi)+b*((sin(t))**(2.00/r))*cos(phi)+ y0 title \"" + scen + "super_ellipse_1 pid=" + person.getId() + "\"\n");
//	out.flush();
//	out.close();
//	fw.close();
//	}
//	catch (IOException e) {
//	e.printStackTrace();
//	System.exit( -1);
//	}
//	ActivitySpace act_space = know.createActivitySpace("super_ellipse",actType);
//	TreeMap<String,Double> params = new TreeMap<String,Double>();
//	params.put("x",best_x);
//	params.put("y",best_y);
//	params.put("theta",best_theta);
//	params.put("a",best_a);
//	params.put("b",best_b);
//	params.put("cover",cover);
//	act_space.addParams(params);
//	}



//	private void runBean(Person person) {
//	final Knowledge know = person.getKnowledge();
//	if (know == null) {
//	Gbl.errorMsg(this.getClass(),"run(Person person)","Knowledge is not defined!");
//	}

//	String actType = "all";

//	// create a list of coordinates the person knows about
//	TreeMap<String, ActivityFacilities> af = know.getActivityFacilities();
//	ArrayList<Coord> coords = new ArrayList<Coord>();
//	Iterator<ActivityFacilities> afIter = af.values().iterator();
//	while (afIter.hasNext()) {
//	ActivityFacilities af2 = afIter.next();
//	String at = af2.getActType();
//	if (actType.equals(at) || actType.equals("all")) {
//	// only add those activities which match the desired activity type
//	TreeMap<Integer, Integer> freqs = af2.getFrequencies();
//	Iterator<Facility> fIter = af2.getFacilities().values().iterator();
//	while (fIter.hasNext()) {
//	Facility f = fIter.next();
//	Integer id = new Integer(f.getId());
//	int freq = freqs.get(id);
//	if (freq < 1) {
//	freq = 1;
//	}
//	for (int i = 0; i < freq; i++) {
//	coords.add(f.getLocation().getCenter());
//	}
//	}
//	}
//	}

//	try {
//	FileWriter fw = new FileWriter(scen + "_bean_pid" + person.getId() + ".txt");
//	BufferedWriter out = new BufferedWriter(fw);
//	Iterator a_it = coords.iterator();
//	while (a_it.hasNext()) {
//	Coord c = (Coord)a_it.next();
//	out.write(c.getX() + "\t" + c.getY() + "\n");
//	}
//	out.flush();
//	out.close();
//	fw.close();
//	}
//	catch (IOException e) {
//	e.printStackTrace();
//	System.exit( -1);
//	}

//	// instanciate the correct objective function
//	String objectiveName = BeanObjective.OBJECTIVE_NAME;
//	double cover = 0.95; // 0.0 <= cover <= 1.0
//	double theta = Math.PI;

//	double best_area = Double.MAX_VALUE;
//	double best_a = Double.MAX_VALUE;
//	double best_b = Double.MAX_VALUE;
//	double best_theta = Double.MAX_VALUE;
//	double best_x = Double.MIN_VALUE;
//	double best_y = Double.MAX_VALUE;

//	double thetaStep = Math.PI/8.0;
//	double cenX;
//	double cenY; // CENTER 
//	int k;

//	double min_x = Double.MAX_VALUE;
//	double min_y = Double.MAX_VALUE;
//	double max_x = Double.MIN_VALUE;
//	double max_y = Double.MIN_VALUE;

//	for( k=0;k<coords.size();k++)
//	{
//	double curr_x = coords.get(k).getX();
//	double curr_y = coords.get(k).getY();
//	if (curr_x < min_x) { min_x = curr_x; }
//	if (curr_y < min_y) { min_y = curr_y; }
//	if (curr_x > max_x) { max_x = curr_x; }
//	if (curr_y > max_y) { max_y = curr_y; }
//	}
//	cenX = (min_x + max_x)/2.0;
//	cenY = (min_y + max_y)/2.0;

//	double distance = Math.sqrt(((max_x-min_x)/2.0)*((max_x-min_x)/2.0) + ((max_y-min_y)/2.0)*((max_y-min_y)/2.0));

//	System.out.println("cenX          = " + cenX);
//	System.out.println("cenY          = " + cenY);
//	System.out.println("distance      = " + distance);
//	System.out.println("Nof locations = " +coords.size());

//	while(theta >=-Math.PI)
//	{
//	Objective objFunc = new BeanObjective(coords, cover, theta);

//	ParamPoint p0 = objFunc.getNewParamPoint();
//	p0.setValue(((BeanObjective)objFunc).X_idx,cenX);
//	p0.setValue(((BeanObjective)objFunc).Y_idx,cenY);
//	p0.setValue(((BeanObjective)objFunc).RATIO_idx,1.0);
//	objFunc.setInitParamPoint(p0,0);

//	ParamPoint p1 = objFunc.getNewParamPoint();
//	p1.setValue(((BeanObjective)objFunc).X_idx,cenX);
//	p1.setValue(((BeanObjective)objFunc).Y_idx,cenY+distance);
//	p1.setValue(((BeanObjective)objFunc).RATIO_idx,1.0);
//	objFunc.setInitParamPoint(p1,1);

//	ParamPoint p2 = objFunc.getNewParamPoint();
//	p2.setValue(((BeanObjective)objFunc).X_idx,cenX+distance);
//	p2.setValue(((BeanObjective)objFunc).Y_idx,cenY);
//	p2.setValue(((BeanObjective)objFunc).RATIO_idx,1.0);
//	objFunc.setInitParamPoint(p2,2);

//	ParamPoint p3 = objFunc.getNewParamPoint();
//	p3.setValue(((BeanObjective)objFunc).X_idx,cenX);
//	p3.setValue(((BeanObjective)objFunc).Y_idx,cenY);
//	p3.setValue(((BeanObjective)objFunc).RATIO_idx,0.5);
//	objFunc.setInitParamPoint(p3,3);

//	// find the activity space
//	ParamPoint best = SimplexOptimization.getBestParams(objFunc);

//	//System.out.println("Area: " + objFunc.getResponse(best) + "; theta = " + theta);

//	if (objFunc.getResponse(best) < best_area) {
//	best_area = objFunc.getResponse(best);
//	best_x = objFunc.getParamMap(best).get(((BeanObjective)objFunc).X_name);
//	best_y = objFunc.getParamMap(best).get(((BeanObjective)objFunc).Y_name);
//	best_theta = objFunc.getParamMap(best).get(((BeanObjective)objFunc).THETA_name);
//	best_a = objFunc.getParamMap(best).get(((BeanObjective)objFunc).A_name);
//	best_b = objFunc.getParamMap(best).get(((BeanObjective)objFunc).B_name);
//	}
//	theta-=(thetaStep);
//	} //end of theta while loop

//	System.out.println("best_area  = " + best_area);
//	System.out.println("best theta = " + best_theta);
//	System.out.println();

//	try {
//	FileWriter fw = new FileWriter(scen + "_bean_pid" + person.getId() + ".gpl");
//	BufferedWriter out = new BufferedWriter(fw);
//	out.write("load \"act-space-style.gpl\"\n\n");
//	out.write("set output \"" + scen + "_bean_pid" + person.getId() + ".eps\"\n\n");
//	out.write("pid = " + person.getId() + "\n");
//	out.write("x0 = " + best_x + "\n");
//	out.write("y0 = " + best_y + "\n");
//	out.write("phi = " + best_theta + "\n");
//	out.write("a = " + best_a + "\n");
//	out.write("b = " + best_b + "\n\n");
////	out.write("plot [t=0.0000:2.0*pi] \"" + scen + "_bean_pid" + person.getId() +
////	".txt\" title \"locations pid=" + person.getId() + "\", \\\n");
////	out.write("a*(-sin(t)*sin(t)*cos(phi))+b*((sin(t)*(((cos(t))**2+(cos(t)*(1+3*sin(t)*sin(t))**0.5)/2)**0.5))*sin(phi))+x0, \\\n");
////	out.write("a*(sin(t)*sin(t)*sin(phi))+b*((sin(t)*(((cos(t))**2+(cos(t)*(1+3*sin(t)*sin(t))**0.5)/2)**0.5))*cos(phi))+y0/((2)**0.5) title \"" + scen + "_bean curve pid=" + person.getId() + "\"\n");
//	out.flush();
//	out.close();
//	fw.close();
//	}
//	catch (IOException e) {
//	e.printStackTrace();
//	System.exit( -1);
//	}
//	ActivitySpace act_space = know.createActivitySpace("bean_curve",actType);
//	TreeMap<String,Double> params = new TreeMap<String,Double>();
//	params.put("x",best_x);
//	params.put("y",best_y);
//	params.put("theta",best_theta);
//	params.put("a",best_a);
//	params.put("b",best_b);
//	params.put("cover",cover);
//	act_space.addParams(params);
//	}
}
