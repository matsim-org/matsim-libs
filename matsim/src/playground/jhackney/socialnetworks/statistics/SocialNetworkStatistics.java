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

package playground.jhackney.socialnetworks.statistics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.knowledges.Knowledges;
import org.matsim.world.Location;

import playground.jhackney.socialnetworks.algorithms.PersonCalculateActivitySpaces;
import playground.jhackney.socialnetworks.algorithms.PlanEuclideanLength;
import playground.jhackney.socialnetworks.socialnet.SocialNetEdge;
import playground.jhackney.socialnetworks.socialnet.SocialNetwork;
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
 * Could be tailored to the network object in MATSim instead of causing overhead by using JUNG
 * 
 * Should be static -jh 9/2008
 *  
 * @author jhackney
 *
 */
public class SocialNetworkStatistics {

	private final int writeInterval=50;
	private final Logger log = Logger.getLogger(SocialNetworkStatistics.class);
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
		statsoutdir = dir + "/stats/";
	}

	public void openFiles() {

		File snDir = new File(statsoutdir);
		if (!snDir.mkdir() && !snDir.exists()) {
			log.error("Cannot make directory" + statsoutdir);
		}
		String aoutfile = statsoutdir + "agent.txt";
		String eoutfile = statsoutdir + "edge.txt";
		String goutfile = statsoutdir + "graph.txt";

		try {
			eout = new BufferedWriter(new FileWriter(eoutfile));
			eout.write("iter tlast tfirst dist egoid alterid purpose timesmet\r\n");
			aout = new BufferedWriter(new FileWriter(aoutfile));
			// aout.write("tstep egoid egozone egodeg egoasd egoclust egoaccess
			// lastactivity rseed var\r\n");
//			aout.write("iter id homeid deg asd1 asd2 asd3 clust plantype placesknown a b x y theta pop\r\n");
			aout.write("iter id homeid deg asd1 asd2 asd3 clust plantype placesknown pop\r\n");
			gout = new BufferedWriter(new FileWriter(goutfile));
			gout.write("iter deg clust clustratio asd1 asd2 asd3 dyad_dist link_age meet_freq\r\n");
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
			log.error("Cannot make directory " + statsoutdir);
		}

		try {
			eout = new BufferedWriter(new FileWriter(eoutfile));
			eout.write("iter tlast tfirst dist egoid alterid purpose timesmet\r\n");
			aout = new BufferedWriter(new FileWriter(aoutfile));
			// aout.write("tstep egoid egozone egodeg egoasd egoclust egoaccess
			// lastactivity rseed var\r\n");
//			aout.write("iter id homeid deg asd1 asd2 asd3 clust plantype placesknown a b x y theta pop\r\n");
			aout.write("iter id homeid deg asd1 asd2 asd3 clust plantype placesknown pop\r\n");
			gout = new BufferedWriter(new FileWriter(goutfile));
			gout.write("iter deg clust clustratio asd1 asd2 asd3 dyad_dist link_age meet_freq\r\n");
		} catch (IOException ex) {
		}
	}

	public void calculate(int iteration, SocialNetwork snet, Population plans, Knowledges knowledges) {
		// First instantiate the JUNG-compatible graph structure
		if (snet.isUNDIRECTED()) {
			this.g = new UndirectedSparseGraph();
		}
		else {
			this.g = new DirectedSparseGraph();
			// fillDirectedGraph(g, snet, plans);
		}
		// Then fill the graph using the MatSim ego nets
		log.info("Filling graph object for JUNG, g is instantiated but not filled");
		Gbl.printMemoryUsage();
		fillGraph(this.g, snet, plans);

		log.info("   MatSim social network converted into a JUNG graph for analysis");
		log.info("     >> See Palla et al for k-clustering calculations or check JUNG");
		Gbl.printMemoryUsage();
		// Now you can run whatever statistics you want on g, its vertices, or
		// its edges

		// Prepare aggregate graph stats
		this.clusterMap = GraphStatistics.clusteringCoefficients(this.g);
		double deg = getGraphAvgDeg(this.g);
		log.info("   avg degree " + deg);
		if (deg != 0) {
			makeDegreeHistogram(iteration);
		}
		// Graph statistics
		// Calcualted and output in Person and Edge statistics
		// Persons statistics

		runPersonStatistics(iteration, plans, snet, knowledges);
		// Edge statistics
//		runEdgeStatistics(iteration, plans);
		runEdgeStatistics2(iteration,snet);

	}
	private void runEdgeStatistics2(int iter, SocialNetwork snet) {
		StatisticalMoments smDD = new StatisticalMoments();
		StatisticalMoments smDur = new StatisticalMoments();
		StatisticalMoments smNum = new StatisticalMoments();
		// iter tlast tfirst dist egoid alterid purpose rseed var
		Set<SocialNetEdge> edges = snet.getLinks();
		double dyadDist = 0.;
		Iterator<SocialNetEdge> eIter = edges.iterator();
		while (eIter.hasNext()) {
			SocialNetEdge myEdge = eIter.next();
			// Distance separating home addresses of two acquaintances
			dyadDist = getDyadDistance(myEdge);
			// Average distance separating acquaintances
			smDD.accumulate(dyadDist);
			// Need persons to get Id's
			Person pFrom = myEdge.getPersonFrom();
			Person pTo = myEdge.getPersonTo();
			// Average duration of a social link
			double x1 = myEdge.getTimeLastUsed();
			double x2 = myEdge.getTimesMet();
			smDur.accumulate(x1);
			smNum.accumulate(x2);
			if(iter%writeInterval==0){
				try {
					eout.write(iter + " " + myEdge.getTimeLastUsed() + " "
							+ myEdge.getTimeMade() + " " + dyadDist + " "
							+ pFrom.getId() + " " + pTo.getId() + " "
							+ myEdge.getType() + " "
							+ myEdge.getTimesMet());
					eout.write("\r\n");
//					eout.newLine();
					eout.flush();
				} catch (IOException ex) {
				}
			}
		}
		try {
			gout.write(" " + smDD.average() + " " + smDur.average() + " "
					+ smNum.average() / (iter));
			gout.write("\r\n");
//			gout.newLine();
			gout.flush();
		} catch (IOException ex) {
		}
	}
	
	private void runEdgeStatistics(int iter, PopulationImpl plans) {
		StatisticalMoments smDD = new StatisticalMoments();
		StatisticalMoments smDur = new StatisticalMoments();
		StatisticalMoments smNum = new StatisticalMoments();
		// iter tlast tfirst dist egoid alterid purpose rseed var
		Set<?> edges = this.g.getEdges();
		double dyadDist = 0.;
		Iterator<?> eIter = edges.iterator();
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
			if(iter%writeInterval==0){
				try {
					eout.write(iter + " " + myEdge.getUserDatum("timeLastUsed") + " "
							+ myEdge.getUserDatum("timeMet") + " " + dyadDist + " "
							+ pFrom.getId() + " " + pTo.getId() + " "
							+ myEdge.getUserDatum("type") + " "
							+ myEdge.getUserDatum("visitNum"));
					eout.write("\r\n");
//					eout.newLine();
					eout.flush();
				} catch (IOException ex) {
				}
			}
		}
		try {
			gout.write(" " + smDD.average() + " " + smDur.average() + " "
					+ smNum.average() / (iter));
			gout.write("\r\n");
//			gout.newLine();
			gout.flush();
		} catch (IOException ex) {
		}
	}

	private Vector<Person> getEdgePersons(Edge e, PopulationImpl plans) {
		Vector<Person> persons = new Vector<Person>(2);
		Vertex v1 = (Vertex) e.getEndpoints().getFirst();
		Person p1 = plans.getPersons().get((Id) v1.getUserDatum("personId"));
		Vertex v2 = (Vertex) e.getEndpoints().getSecond();
		Person p2 = plans.getPersons().get((Id) v2.getUserDatum("personId"));
		persons.add(p1);
		persons.add(p2);
		return persons;
	}

	private double getDyadDistance(SocialNetEdge myEdge) {
		double dist = 0.;
		Person pFrom = myEdge.getPersonFrom();
		Coord fromCoord = ((ActivityImpl) pFrom.getSelectedPlan().getPlanElements().get(
				0)).getCoord();
		Person pTo = myEdge.getPersonTo();
		Coord toCoord = ((ActivityImpl) pTo.getSelectedPlan().getPlanElements().get(0))
		.getCoord();
		dist = CoordUtils.calcDistance(fromCoord, toCoord);
		return dist;
	}
	private double getDyadDistance(Edge myEdge, PopulationImpl plans) {
		double dist = 0.;
		Vertex vFrom = (Vertex) myEdge.getEndpoints().getFirst();
		Person pFrom = plans.getPersons().get((Id) vFrom.getUserDatum("personId"));
		Coord fromCoord = ((ActivityImpl) pFrom.getSelectedPlan().getPlanElements().get(
				0)).getCoord();
		Vertex vTo = (Vertex) myEdge.getEndpoints().getSecond();
		Person pTo = plans.getPersons().get((Id) vTo.getUserDatum("personId"));
		Coord toCoord = ((ActivityImpl) pTo.getSelectedPlan().getPlanElements().get(0))
		.getCoord();
		dist = CoordUtils.calcDistance(fromCoord, toCoord);
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
		log.info("   max degree " + degvals.get(degvals.size() - 1));
		return (int) degvals.get(degvals.size() - 1);
	}

	private double getGraphAvgDeg(Graph g) {
		return 2. * g.numEdges() / g.numVertices();
	}

	private void runPersonStatistics(int iter, Population plans, SocialNetwork snet, Knowledges knowledges) {

		double clusteringRatio = 0.;
		double clusterCoef = 0.;
		PersonCalculateActivitySpaces pcasd1 = new PersonCalculateActivitySpaces();
		StatisticalMoments smAD = new StatisticalMoments();
		StatisticalMoments smCC = new StatisticalMoments();
		StatisticalMoments smASD1 = new StatisticalMoments();
		StatisticalMoments smASD2 = new StatisticalMoments();
		StatisticalMoments smASD3 = new StatisticalMoments();
		PlanEuclideanLength len = new PlanEuclideanLength();
		GeoStatistics gstat = new GeoStatistics(plans, snet);
		Graph g = gstat.makeJungGraph();

		Set<Vertex> vertices = this.g.getVertices();
		Iterator<Vertex> ivert = vertices.iterator();
		StringBuilder planTypeString;
		while (ivert.hasNext()) {

			Vertex myVert = ivert.next();
			Person myPerson = plans.getPersons().get((Id) myVert.getUserDatum("personId"));
			int id = Integer.parseInt(myVert.getUserDatum("personId").toString());
			// Agent's Home Location ID
			ActivityImpl myAct = (ActivityImpl) myPerson.getSelectedPlan().getPlanElements().get(0);
			String homeId = myAct.getLinkId().toString();
			// Agent's approx activity space diameter (radius to all alters)
			double aSd1 = pcasd1.getPersonASD1(plans, myPerson);
			// Agent's approx activity space diameter (radius to all activities)
			double aSd2 = pcasd1.getPersonASD2(myPerson.getSelectedPlan());
			// Agent's approx activity space 2, length of plan
			//TODO access the TravelDistanceStats object and get the plan's length including routes
			double aSd3 = len.getPlanLength(myPerson.getSelectedPlan());

			//calculate the ego space and record xcen,ycen,a,b,theta
//			double esa=0;
//			double esb=0;
//			double esx=0;
//			double esy=0;
//			double est=0;
//			new PersonCalcEgoSpace().run(myPerson);
//			ActivitySpace space = myPerson.getKnowledge().getActivitySpaces().get(0);
//			if(space instanceof ActivitySpaceEllipse){
//			esa=space.getParams().get("a");
//			esb=space.getParams().get("b");
//			esx=space.getParams().get("x");
//			esy=space.getParams().get("y");
//			est=space.getParams().get("theta");
//			}
//			myPerson.getKnowledge().clearActivitySpaces();

			//Geographical aggregation
			ActivityFacilityImpl myHome=(ActivityFacilityImpl) ((ActivityImpl)(myPerson.getSelectedPlan().getPlanElements().get(0))).getFacility();
			Location myLoc=myHome.getUpMapping().get(myHome.getUpMapping().firstKey());
			Vertex myVertex=gstat.getLocVertex().get(myLoc);
			double pop=(Integer) myVertex.getUserDatum("population");

			// Agent's Plan Type
			Plan thisPlan = myPerson.getSelectedPlan();
			PlanImpl.Type planType = ((PlanImpl) thisPlan).getType();
			if ((planType == null) || (planType == PlanImpl.Type.UNDEFINED)) {
				planTypeString = new StringBuilder();
				for (PlanElement pe : thisPlan.getPlanElements()) {
					if (pe instanceof ActivityImpl) {
						ActivityImpl nextAct = (ActivityImpl) pe;
						planTypeString.append(nextAct.getType().charAt(0));
					}
				}
			}
			else {
				planTypeString = new StringBuilder(planType.toString());
			}
			// Agent's degree
			int deg = myVert.degree();
			// Agent's clustering coeff
			// Note that the JUNG algorithm counts a node with degree = 1 as having
			// clustering = 1 and other implementations do not. For comparison with other
			// measures, e.g. in Pajek, replace this 1 with 0
			clusterCoef = Double.parseDouble(this.clusterMap.get(myVert).toString());
			if ((deg < 2) && (clusterCoef == 1)) {
				clusterCoef = 0.;
			}
			// Iterator viter = myPerson.getKnowledge().map.getAllPlaces().iterator();
			// while(viter.hasNext()){
			// CoolPlace place = (CoolPlace) viter.next();
			// log.info(iter+"\t"+id+"\t"+place.activity.getType()+"\t"+place.facility.getId());
			// }
			if(iter%writeInterval==0){
				try {
//					aout.write(iter + " " + id + " " + homeId + " " + deg + " " + aSd1
//					+ " " + aSd2 + " " + aSd3 + " " + clusterCoef + " " + planTypeString.toString()
//					+ " " + myPerson.getKnowledge().getActivities().size() + " "+
//					esa+" "+esb+" "+esx+" "+esy+" "+est+" "+pop);
					aout.write(iter + " " + id + " " + homeId + " " + deg + " " + aSd1
							+ " " + aSd2 + " " + aSd3 + " " + clusterCoef + " " + planTypeString.toString()
							+ " " + knowledges.getKnowledgesByPersonId().get(myPerson.getId()).getActivities().size() + " "+pop);
					aout.newLine();
					aout.flush();
				} catch (IOException e) {

					e.printStackTrace();
				}
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
	void fillGraph(Graph g, SocialNetwork snet, Population plans) {
		Vertex v;
		Edge e;
		for (Person p : plans.getPersons().values()) {
			if (snet.isUNDIRECTED()) {
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
			if (snet.isUNDIRECTED()) {
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
			// log.info(myLink.getTimeMade()-myLink.getTimeLastUsed());
			// Add the link to the graph
			g.addEdge(e);
//			System.out.println("0" + " " + e.getUserDatum("timeLastUsed") + " "
//					+ e.getUserDatum("timeMet") + " " + "0." + " "
//					+ myLink.getPersonFrom().getId() + " " + myLink.getPersonTo().getId() + " "
//					+ e.getUserDatum("type") + " "
//					+ e.getUserDatum("visitNum"));
//			log.info("Adding edge to JUNG graph object, g is being filled");
//			Gbl.printMemoryUsage();
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
			log.info("Batch: closed output files");
			gout.close();
		} catch (IOException ex2) {
		}
		log.info("Social network output files closed.");
		
	}

}
