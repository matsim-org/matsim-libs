/* *********************************************************************** *
 * project: michalm
 * TaxiModeDepartureHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.taxicab;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.ptproject.qsim.interfaces.DepartureHandler;
import org.matsim.ptproject.qsim.interfaces.Netsim;

/**
 * @author nagel
 *
 */
public class TaxiModeDepartureHandler implements DepartureHandler {
	
	private Netsim mobsim ;

	TaxiModeDepartureHandler( Netsim mobsim ) {
		this.mobsim = mobsim ;
	}

	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id linkId) {

		if ( agent.getMode().equals("taxi") ) {
			Logger.getLogger(this.getClass()).warn("performing a taxi mode departure" ) ;

			mobsim.getEventsManager().processEvent( 
					new PassengerTaxiRequestEvent( now, agent.getId(), linkId )) ;
			
			this.mobsim.registerAdditionalAgentOnLink(agent) ; 

			
			Logger.getLogger(this.getClass()).warn("done with taxi mode departure" ) ;
			return true ;
		} else {
			return false ;
		}
	}

}
