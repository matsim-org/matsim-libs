/* *********************************************************************** *
 * project: org.matsim.*
 * SocialNetworkStatistics.java
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

package org.matsim.socialnetworks.statistics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.BasicPlanImpl.ActIterator;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Act;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.socialnetworks.algorithms.PersonCalculateActivitySpaces;
import org.matsim.socialnetworks.algorithms.PlanEuclideanLength;
import org.matsim.socialnetworks.socialnet.SocialNetEdge;
import org.matsim.socialnetworks.socialnet.SocialNetwork;
import org.matsim.utils.geometry.shared.Coord;

import cern.colt.list.DoubleArrayList;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.DirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.DirectedSparseGraph;
import edu.uci.ics.jung.graph.impl.DirectedSparseVertex;
import edu.uci.ics.jung.graph.impl.UndirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.impl.UndirectedSparseVertex;
import edu.uci.ics.jung.statistics.DegreeDistributions;
import edu.uci.ics.jung.statistics.GraphStatistics;
import edu.uci.ics.jung.statistics.Histogram;
import edu.uci.ics.jung.statistics.StatisticalMoments;
import edu.uci.ics.jung.utils.UserData;

/**
 * Interface between MatSim and JUNG library to use the JUNG network statistics
 * package on the MatSim social network output. IMPORTANT: you must have the
 * JUNG jar (at least 1.7 and preferably the newest) as well as Apache Commons
 * Collections jar (at least 3.1) and Cern Colt jar externally linked.
 *
 * @author jhackney
 *
 */
public class SocialNetworkStatistics {

	private String statsoutdir;

	private BufferedWriter aout = null;

	// static String aoutfile = statsoutdir+"agent.txt";
	private BufferedWriter eout = null;

	// static String eoutfile = statsoutdir+"edge.txt";
	private BufferedWriter gout = null;

	// static String goutfile = statsoutdir+"graph.txt";
	Graph g;

	Map clusterMap;

	// This is a map of MatSim Person ID to the JUNG vertex object
	// (The JUNG Vertex UserDatum container is used
	// to get the Person ID given a Vertex object
	TreeMap<Id, Vertex> verticesPersons = new TreeMap<Id, Vertex>();

	public SocialNetworkStatistics(String dir) {

		// statsoutdir = Gbl.getConfig().socnetmodule().getOutDir()+"stats/";
		statsoutdir = dir + "stats/";
	}

	public void openFiles() {

		File snDir = new File(statsoutdir);
		if (!snDir.mkdir() && !snDir.exists()) {
			Gbl.errorMsg("Cannot make directory " + statsoutdir);
		}
		String aoutfile = statsoutdir + "agent.txt";
		String eoutfile = statsoutdir + "edge.txt";
		String goutfile = statsoutdir + "graph.txt";

		try {
			eout = new BufferedWriter(new FileWriter(eoutfile));
			eout.write("iter tlast tfirst dist egoid alterid purpose timesmet\n");
			aout = new BufferedWriter(new FileWriter(aoutfile));
			// aout.write("tstep egoid egozone egodeg egoasd egoclust egoaccess
			// lastactivity rseed var\n");
			aout.write("iter id homeid deg asd1 asd2 asd3 clust plantype numknown\n");
			gout = new BufferedWriter(new FileWriter(goutfile));
			gout.write("iter deg clust clustratio asd1 asd2 asd3 dyad_dist link_age meet_freq\n");
		} catch (IOException ex) {
		}
	}

	public void openFiles(String outputPath) {

		// String directory = statsoutdir;
		File snDir = new File(outputPath);

		String aoutfile = outputPath + "/agent.txt";
		String eoutfile = outputPath + "/edge.txt";
		String goutfile = outputPath + "/graph.txt";

		if (!snDir.mkdir() && !snDir.exists()) {
			Gbl.errorMsg("Cannot make directory " + statsoutdir);
		}

		try {
			eout = new BufferedWriter(new FileWriter(eoutfile));
			eout.write("iter tlast tfirst dist egoid alterid purpose timesmet\n");
			aout = new BufferedWriter(new FileWriter(aoutfile));
			// aout.write("tstep egoid egozone egodeg egoasd egoclust egoaccess
			// lastactivity rseed var\n");
			aout.write("iter id homeid deg asd1 asd2 asd3 clust plantype numknown\n");
			gout = new BufferedWriter(new FileWriter(goutfile));
			gout.write("iter deg clust clustratio asd1 asd2 asd3 dyad_dist link_age meet_freq\n");
		} catch (IOException ex) {
		}
	}

	public void calculate(int iteration, SocialNetwork snet, Plans plans) {
		// First instantiate the JUNG-compatible graph structure
		if (snet.UNDIRECTED) {
			this.g = new UndirectedSparseGraph();
		}
		else {
			this.g = new DirectedSparseGraph();
			// fillDirectedGraph(g, snet, plans);
		}
		// Then fill the graph using the MatSim ego nets
		fillGraph(this.g, snet, plans);

		System.out
				.println("   MatSim social network converted into a JUNG graph for analysis");
		System.out
				.println("     >> See Palla et al for k-clustering calculations or check JUNG");
		// Now you can run whatever statistics you want on g, its vertices, or
		// its edges

		// Prepare aggregate graph stats
		this.clusterMap = GraphStatistics.clusteringCoefficients(this.g);
		double deg = getGraphAvgDeg(this.g);
		System.out.println("   avg degree " + deg);
		if (deg != 0) {
			makeDegreeHistogram(iteration);
		}
		// Graph statistics
		// Calcualted and output in Person and Edge statistics
		// Persons statistics
		runPersonStatistics(iteration, plans);
		// Edge statistics
		runEdgeStatistics(iteration, plans);
	}

	private void runEdgeStatistics(int iter, Plans plans) {
		StatisticalMoments smDD = new StatisticalMoments();
		StatisticalMoments smDur = new StatisticalMoments();
		StatisticalMoments smNum = new StatisticalMoments();
		// iter tlast tfirst dist egoid alterid purpose rseed var
		Set edges = this.g.getEdges();
		double dyadDist = 0.;
		Iterator eIter = edges.iterator();
		while (eIter.hasNext()) {
			Edge myEdge = (Edge) eIter.next();
			// Distance separating home addresses of two acquaintances
			dyadDist = getDyadDistance(myEdge, plans);
			// Average distance separating acquaintances
			smDD.accumulate(dyadDist);
			// Need persons to get Id's
			Vector<Person> dyad = getEdgePersons(myEdge, plans);
			Person pFrom = dyad.elementAt(0);
			Person pTo = dyad.elementAt(1);
			// Average duration of a social link
			double x1 = (iter - Integer.parseInt(myEdge.getUserDatum("timeLastUsed")
					.toString()));
			double x2 = (Integer.parseInt(myEdge.getUserDatum("visitNum").toString()));
			smDur.accumulate(x1);
			smNum.accumulate(x2);
			try {
				eout.write(iter + " " + myEdge.getUserDatum("timeLastUsed") + " "
						+ myEdge.getUserDatum("timeMet") + " " + dyadDist + " "
						+ pFrom.getId() + " " + pTo.getId() + " "
						+ myEdge.getUserDatum("type") + " "
						+ myEdge.getUserDatum("visitNum"));
				eout.newLine();
			} catch (IOException ex) {
			}
		}
		try {
			gout.write(" " + smDD.average() + " " + smDur.average() + " "
					+ smNum.average() / (iter));
			gout.newLine();
		} catch (IOException ex) {
		}
	}

	private Vector<Person> getEdgePersons(Edge e, Plans plans) {
		Vector<Person> persons = new Vector<Person>(2);
		Vertex v1 = (Vertex) e.getEndpoints().getFirst();
		Person p1 = plans.getPerson(v1.getUserDatum("personId").toString());
		Vertex v2 = (Vertex) e.getEndpoints().getSecond();
		Person p2 = plans.getPerson(v2.getUserDatum("personId").toString());
		persons.add(p1);
		persons.add(p2);
		return persons;
	}

	private double getDyadDistance(Edge myEdge, Plans plans) {
		double dist = 0.;
		Vertex vFrom = (Vertex) myEdge.getEndpoints().getFirst();
		Person pFrom = plans.getPerson(vFrom.getUserDatum("personId").toString());
		Coord fromCoord = (Coord) ((Act) pFrom.getSelectedPlan().getActsLegs().get(
				0)).getCoord();
		Vertex vTo = (Vertex) myEdge.getEndpoints().getSecond();
		Person pTo = plans.getPerson(vTo.getUserDatum("personId").toString());
		Coord toCoord = (Coord) ((Act) pTo.getSelectedPlan().getActsLegs().get(0))
				.getCoord();
		dist = fromCoord.calcDistance(toCoord);
		return dist;
	}

	private void makeDegreeHistogram(int iteration) {
		DoubleArrayList degvals = DegreeDistributions.getDegreeValues(this.g
				.getVertices());
		int maxval = max(degvals);
		Histogram h = DegreeDistributions.getIndegreeHistogram(
				this.g.getVertices(), 0, maxval, maxval + 1);
		String name = statsoutdir + "deg_histogram" + iteration + ".txt";
		DegreeDistributions.saveDistribution(h, name);
	}

	private int max(DoubleArrayList degvals) {
		degvals.quickSort();
		System.out.println("   max degree " + degvals.get(degvals.size() - 1));
		return (int) degvals.get(degvals.size() - 1);
	}

	private double getGraphAvgDeg(Graph g) {
		return 2. * g.numEdges() / g.numVertices();
	}

	private void runPersonStatistics(int iter, Plans plans) {

		double clusteringRatio = 0.;
		double clusterCoef = 0.;
		PersonCalculateActivitySpaces pcasd1 = new PersonCalculateActivitySpaces();
		StatisticalMoments smAD = new StatisticalMoments();
		StatisticalMoments smCC = new StatisticalMoments();
		StatisticalMoments smASD1 = new StatisticalMoments();
		StatisticalMoments smASD2 = new StatisticalMoments();
		StatisticalMoments smASD3 = new StatisticalMoments();
		PlanEuclideanLength len = new PlanEuclideanLength();

		Set vertices = this.g.getVertices();
		Iterator ivert = vertices.iterator();
		StringBuilder planTypeString;
		while (ivert.hasNext()) {

			Vertex myVert = (Vertex) ivert.next();
			Person myPerson = plans.getPerson(myVert.getUserDatum("personId")
					.toString());
			// Agent ID
			int id = Integer.parseInt(myVert.getUserDatum("personId").toString());
			// Agent's Home Location ID
			Act myAct = (Act) myPerson.getSelectedPlan().getActsLegs().get(0);
			String homeId = myAct.getLinkId().toString();
			// Agent's approx activity space diameter (radius to all alters)
			double aSd1 = pcasd1.getPersonASD1(plans, myPerson);
			// Agent's approx activity space diamter (radius to all activities)
			double aSd2 = pcasd1.getPersonASD2(myPerson.getSelectedPlan());
			// Agent's approx activity space 2, length of plan
			double aSd3 = len.getPlanLength(myPerson.getSelectedPlan());
			// Agent's Plan Type
			Plan thisPlan = myPerson.getSelectedPlan();
			Plan.Type planType = thisPlan.getType();
			if ((planType == null) || (planType == Plan.Type.UNDEFINED)) {
				planTypeString = new StringBuilder();
				ActIterator a_it = thisPlan.getIteratorAct();
				while (a_it.hasNext()) {
					Act nextAct = (Act) a_it.next();
						planTypeString.append(nextAct.getType().charAt(0));
				}
				// 10.03.08 JH If Plan.getType() is to be called in social nets in the future, for example
//				to compare some statistics across plan types, remove this comment. However this
//				could lead to setting the type to undefined values because the type string that is
//				constructed above is not checked vs the DTD and might result in nonsense plan types
//				for other users
//				thisPlan.setType(planType);
			}
			else {
				planTypeString = new StringBuilder(planType.toString());
			}
			// Agent's degree
			int deg = myVert.degree();
			// Agent's clustering coeff
			// Note that the JUNG algorithm counts a node with degree = 1 as having
			// clustering = 1 and other algorithms do not. For comparison with other
			// measures, e.g. in Pajek, replace this 1 with 0
			clusterCoef = Double.parseDouble(this.clusterMap.get(myVert).toString());
			if ((deg < 2) && (clusterCoef == 1)) {
				clusterCoef = 0.;
			}
			// Iterator viter = myPerson.getKnowledge().map.getAllPlaces().iterator();
			// while(viter.hasNext()){
			// CoolPlace place = (CoolPlace) viter.next();
			// System.out.println(iter+"\t"+id+"\t"+place.activity.getType()+"\t"+place.facility.getId());
			// }
			try {
				aout.write(iter + " " + id + " " + homeId + " " + deg + " " + aSd1
						+ " " + aSd2 + " " + aSd3 + " " + clusterCoef + " " + planTypeString.toString()
						+ " " + myPerson.getKnowledge().map.getNumKnownFacilities());
				aout.newLine();
			} catch (IOException e) {

				e.printStackTrace();
			}

			// Node-based Graph statistics (temporary solution)
			smAD.accumulate(deg);
			smCC.accumulate(clusterCoef);
			if (aSd1 != aSd1) {
				// aSd is NaN
			}
			else {
				smASD1.accumulate(aSd1);
			}
			if (aSd2 != aSd2) {
				// aSd is NaN
			}
			else {
				smASD2.accumulate(aSd2);
			}
			if (aSd3 != aSd3) {
				// aSd is NaN
			}
			else {
				smASD3.accumulate(aSd3);
			}
		}
		// Ratio of clustering in this graph relative to an Erd�s/Renyi graph
		// See Watts 1999 book on Small Worlds

		clusteringRatio = smCC.average() / (smAD.average() / vertices.size());

		try {
			gout.write(iter + " " + smAD.average() + " " + smCC.average() + " "
					+ clusteringRatio + " " + smASD1.average() + " " + smASD2.average()
					+ " " + smASD3.average());
			// gout.newLine();//write newLine() after adding variables in Edge
			// statistics
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Glues MatSim egonets together to make a JUNG graph object for further
	 * analysis with JUNG statistical package.
	 *
	 * @author jhackney
	 * @param g
	 * @param snet
	 * @param plans
	 */
	void fillGraph(Graph g, SocialNetwork snet, Plans plans) {
		Collection<Person> personList = plans.getPersons().values();
		Vertex v;
		Edge e;
		Iterator<Person> iperson = personList.iterator();
		while (iperson.hasNext()) {
			Person p = iperson.next();
			if (snet.UNDIRECTED) {
				v = new UndirectedSparseVertex();
			}
			else {
				v = new DirectedSparseVertex();
			}
			this.verticesPersons.put(p.getId(), v);
			// Add the Person ID to the user data container for the vertex
			v.addUserDatum("personId", p.getId(), UserData.SHARED);
			// Add the vertex to the graph
			g.addVertex(v);
		}
		Iterator<SocialNetEdge> ilinks = snet.getLinks().iterator();
		while (ilinks.hasNext()) {
			SocialNetEdge myLink = ilinks.next();
			Vertex egoVertex = this.verticesPersons.get(myLink.getPersonFrom()
					.getId());
			Vertex alterVertex = this.verticesPersons.get(myLink.getPersonTo()
					.getId());
			if (snet.UNDIRECTED) {
				e = new UndirectedSparseEdge(egoVertex, alterVertex);
			}
			else {
				e = new DirectedSparseEdge(egoVertex, alterVertex);
			}
			// Add the link attributes in the UserDatum container
			e.addUserDatum("timeMet", myLink.getTimeMade(), UserData.SHARED);
			e.addUserDatum("timeLastUsed", myLink.getTimeLastUsed(), UserData.SHARED);
			e.addUserDatum("type", myLink.getType(), UserData.SHARED);
			e.addUserDatum("visitNum", myLink.getTimesMet(), UserData.SHARED);
			// System.out.println(myLink.getTimeMade()-myLink.getTimeLastUsed());
			// Add the link to the graph
			g.addEdge(e);
		}
	}

	public void closeFiles() {
		try {
			aout.close();
		} catch (IOException ex2) {
		}
		try {
			eout.close();
		} catch (IOException ex2) {
		}
		try {
			System.out.println("Batch: closed output files");
			gout.close();
		} catch (IOException ex2) {
		}
		System.out.println("Social network output files closed.");
	}

}
