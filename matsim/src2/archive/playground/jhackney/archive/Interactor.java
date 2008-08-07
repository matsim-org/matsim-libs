/* *********************************************************************** *
 * project: org.matsim.*
 * Interactor.java
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

import org.matsim.population.Population;

import playground.jhackney.socialnet.SocialNetwork;

/**
 * @author F.Marchal, J.Hackney This interface specifies interaction methods for
 *         spatial and non-spatial (ICT or non-observed) interactions between
 *         the agents in the network. Note that some
 *         of the interact methods change the social network.
 */

public abstract class Interactor {

    public SocialNetwork net;

    public String type;

    public Interactor(SocialNetwork net) {
	this.net = net;
    }

    public void setType(String type) {
	this.type = type;
    }

    public String getType() {
	return this.type;
    }

    /**
     * Non-spatial information exchange about a facility.
     *@author jhackney  
     * @param facType
     *                is the type of facility about which information is
     *                exchanged.
     * @param iteration is the iteration of the social network
     */
    public abstract void interact(String facType, int iteration);
/**
 * Non-spatial introduction of friends of friends
 * @author jhackney
 * @param plans the list of people and their factivities
 * @param iteration the social network iteration
 */
    public abstract void interact(Population plans, int iteration);
/**
 * This is the old spatial interact which is called for each facility type in which
 * the agents are to have a chance to meet. Thus the weighting of the facility,
 * i.e. the fraction of the meetings between agents which take place in this facility,
 * must be determined before calling this interact method, and the appropriate
 * facility type sent as an argument.
 * @author jhackney
 * @param plans
 * @param facType
 * @param iteration
 */
    public abstract void interact(Population plans, String facType, int iteration);
/**
 * The current spatial interactor which uses a likelihood for interactions in
 * each facility type
 * @param plans list of people and their factivities
 * @param facTypes String array of facility types (all of them)
 * @param facWeights can be either normalized to 1 or not. Be careful which you are using
 * because this code doesn't care
 * @param iteration social network iteration
 */
    public abstract void interact(Population plans, String[] facTypes, double[] facWeights, int iteration);

}
