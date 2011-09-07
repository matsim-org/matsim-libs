/*******************************************************************************
 * Copyright (C) 2011 Stefan Schroeder.
 * eMail: stefan.schroeder@kit.edu
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package vrp.algorithms.ruinAndRecreate.basics;

import vrp.algorithms.ruinAndRecreate.api.TourActivityStatusUpdater;
import vrp.algorithms.ruinAndRecreate.api.TourAgent;
import vrp.algorithms.ruinAndRecreate.api.TourAgentFactory;
import vrp.api.VRP;
import vrp.basics.Tour;
import vrp.basics.Vehicle;

/**
 * 
 * @author stefan schroeder
 *
 */

public class RRTourAgentWithTimeWindowFactory implements TourAgentFactory{

	private VRP vrp;
	
	public RRTourAgentWithTimeWindowFactory(VRP vrp) {
		super();
		this.vrp = vrp;
	}

	@Override
	public TourAgent createTourAgent(Tour tour, Vehicle vehicle) {
		TourActivityStatusUpdater updater = new TourActivityStatusUpdaterWithTWImpl(vrp.getCosts());
		BestTourBuilder tourBuilder = new BestTourBuilder();
		tourBuilder.setConstraints(vrp.getConstraints());
		tourBuilder.setCosts(vrp.getCosts());
		tourBuilder.setTourActivityStatusUpdater(updater);
		tourBuilder.setVehicle(vehicle);
		RRTourAgent tourAgent = new RRTourAgent(vrp.getCosts(), tour, vehicle, updater);
		tourAgent.setConstraint(vrp.getConstraints());
		tourAgent.setTourBuilder(tourBuilder);
		return tourAgent;
	}

}
