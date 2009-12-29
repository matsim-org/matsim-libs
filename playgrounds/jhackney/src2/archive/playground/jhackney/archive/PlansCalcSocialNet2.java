/* *********************************************************************** *
 * project: org.matsim.*
 * PlansCalcSocialNet2.java
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

package playground.jhackney.deprecated;

import org.matsim.gbl.Gbl;
import org.matsim.population.Person;
import org.matsim.population.Population;
import org.matsim.population.algorithms.PersonAlgorithmI;
import org.matsim.population.algorithms.PlansAlgorithm;

import playground.jhackney.io.PajekWriter1;
import playground.jhackney.socialnet.SocialNetwork;

public class PlansCalcSocialNet2 extends PlansAlgorithm implements PersonAlgorithmI {

    SocialNetwork snet;

    Interactor plansInteractor1;// maybe Array(List) of Interactors

    Interactor plansInteractor2;

    final int max_sn_iter;

    String facType_x;

    String facType_s;

    String interactor1FacTypes[];
    String interactor2FacTypes[];

    public PlansCalcSocialNet2() {
	// TODO Auto-generated constructor stub
	super();
	max_sn_iter = Integer.parseInt(Gbl.getConfig().socnetmodule().getNumIterations());
	String interactor2FacTypesString = Gbl.getConfig().socnetmodule().getFacWt();
	String interactor1FacTypesString = Gbl.getConfig().socnetmodule().getXchange();
	interactor1FacTypes = getFacTypes(interactor1FacTypesString);
	interactor2FacTypes = getFacTypes(interactor2FacTypesString);
    }


    /**
         * @author J.Hackney This Plans algorithm generates a social network
         *         between Persons. The initial network can result from
         *         iterative link addition/subtraction (JinGirNew,
         *         WattsSmallWorld, Scale-Free, ...)or from a one-iteration
         *         algorithm (e.g. Erd�s/Renyi). Two different iteration loops
         *         are generally used; one establishing an initial base network,
         *         and another which operates on this base network, iterating
         *         over the particular set of Plans. The initial social network
         *         can use the initial plans of the Persons if there is to be
         *         spatial dependence, but the initial network could also be
         *         entirely non-spatial. Spatial dependence, or activity-travel
         *         dependence, is built into the social network with the
         *         Interactors (non-spatial, spatial). These let the Persons
         *         exchange information and get to know space and each other.
         *         Non-spatial interactors use an initial non-null social
         *         network and allow alters to exchange information about
         *         facilities, activities, persons. Only the latter might have
         *         an immediate effect on social networks. Otherwise, exchanges
         *         resulting from this interactor must be incorporated into a
         *         new day plan in order to have an influence on social
         *         networks, via spatial interactions. Spatial interactors do
         *         not need an initial non-null social network. They represent
         *         the results of face-to-face contacts, during which
         *         (time-space window) relationships can be established,
         *         re-affirmed, and/or information exchanged. The iterations
         *         allow various social networks to be simulated which require
         *         iterative construction steps. (TODO: interactor which
         *         evaluates the utility of exchanged information, either as
         *         giver or as receiver. To change plans according to their
         *         friends' plans. With ("replanning") and without ("initial
         *         demand") re-running the assignment microsimulation).
         */
    @Override
    public void run(Population plans) {

	PajekWriter1 pjw = new PajekWriter1("C:/Documents and Settings/jhackney/My Documents/sandbox00/vsp-cvs/devel/matsim/matsimJ/output");
	buildSocialNetwork(plans);
	createPlansInteractor();

	// Run the initializing algorithm for the prescribed number of
        // iterations
	// The number of iterations is defined within the algorithm, for each
        // algorithm
	for (int i = 0; i < snet.setupIter; i++) {
	    snet.generateLinks(i);
	}
	pjw.write(snet.getLinks(), plans, -1, max_sn_iter);
	// 
	// -----------Iterate here (replace with proper rePlanning
        // module--------
	//
	for (int iteration = 0; iteration < max_sn_iter; iteration++) {

	    // Non-face-to-face exchange of information (internet, telecom, non-observed)
	    // Note it might be cool to have a vector of doubles that sum to 1.0, each
	    // the probability of exchanging info about a kind of facility.

	    for (int ii = 0; ii < interactor1FacTypes.length; ii++) {
		facType_x = interactor1FacTypes[ii];
		if (!facType_x.equals("none")) {
		    plansInteractor1.interact(facType_x, iteration);

		    // Exchange of knowledge about people
		    if (facType_x.equals("person")) {
			plansInteractor1.interact(plans, iteration);
		    }
		}
	    }
	    for (int jj = 0; jj < interactor2FacTypes.length; jj++) {
		facType_s = interactor2FacTypes[jj];
		if (!facType_s.equals("none") && !facType_s.equals("person")) {
		    // Time-window chance meetings with F2F reinforcement
		    plansInteractor2.interact(plans, facType_s, iteration);
		}
	    }

	    // Remove links with desired removal algorithm
	    snet.removeLinks();
	    pjw.write(snet.getLinks(), plans, iteration, max_sn_iter);
	}
	//
	// -----------Replace above with proper rePlanning module--------
	//

    }

    public void run(Person person) {
	// TODO Auto-generated method stub
	// Implementation of PersonAlgorithm

    }

    void buildSocialNetwork(Population plans) {

	System.out.print("building Social Network...");

	SocialNetworkGenerator sNGenerator = new SocialNetworkGenerator();
	snet = sNGenerator.generateSocialNetwork(plans);

	System.out.println("...done");
	// System.out.println("Number of social
	// connections:\t"+snet.getLinks().size());

    }

    void createPlansInteractor() {
	// String type = (String) params.get("INTERACTOR");
	System.out.println("creating Social Interactors...");
	final String type1;
	final String type2;
	type1 = Gbl.getConfig().socnetmodule().getSocNetInteractor1();
	type2 = Gbl.getConfig().socnetmodule().getSocNetInteractor2();
	plansInteractor1 = InteractorFactory.createNonSpatialInteractor(type1, snet);
	plansInteractor2 = InteractorFactory.createSpatialInteractor(type2, snet);

	System.out.println("...done");
    }

    private String[] getFacTypes(String longString) {
	// TODO Auto-generated method stub
	String patternStr = ",";
	String[] s = null;
	if (longString == "all-p") {
	    s[0] = "home";
	    s[1] = "work";
	    s[3] = "education";
	    s[4] = "leisure";
	    s[5] = "shop";
	} else if (longString == "all+p") {
	    s[0] = "home";
	    s[1] = "work";
	    s[3] = "education";
	    s[4] = "leisure";
	    s[5] = "shop";
	    s[6] = "person";
	} else {
	    s = longString.split(patternStr);
	}
	for(int i=0;i<s.length;i++){
	    //if(s[i]!="home"&&s[i]!="work"&&s[i]!="education"&&s[i]!="leisure"&&s[i]!="shop"&&s[i]!="person"&&s[i]!="none"){
	    if(!s[i].equals("home")&&!s[i].equals("work")&&!s[i].equals("education")&&!s[i].equals("leisure")&&!s[i].equals("shop")&&!s[i].equals("person")&&!s[i].equals("none")){
		System.out.println(this.getClass()+":"+s[i]);
		Gbl.errorMsg("Error on type of info. Check config file. Use commas with no spaces");
	    }
	}
	return s;
    }
}
