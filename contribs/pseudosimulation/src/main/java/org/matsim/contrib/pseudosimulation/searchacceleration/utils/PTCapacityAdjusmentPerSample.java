/*
 * Copyright 2018 Mohammad Saleem
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: salee@kth.se
 *
 */ 
package org.matsim.contrib.pseudosimulation.searchacceleration.utils;

import java.util.Iterator;

import org.matsim.api.core.v01.Scenario;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;
/**
 * A class to scale capacity of vehicles as per sample size.
 * 
 * @author Mohammad Saleem
 *
 */
public class PTCapacityAdjusmentPerSample {
	public void adjustStoarageAndFlowCapacity(Scenario scenario, double samplesize){

		// Changing vehicle and road capacity according to sample size
		Vehicles vehicles = scenario.getTransitVehicles();
		Iterator<VehicleType> vehtypes = vehicles.getVehicleTypes().values().iterator();
		while(vehtypes.hasNext()){//Set flow and storage capacities according to sample size
			VehicleType vt = (VehicleType)vehtypes.next();
			VehicleCapacity cap = vt.getCapacity();

			cap.setSeats((int)Math.ceil(cap.getSeats()*samplesize));
			cap.setStandingRoom((int)Math.ceil(cap.getStandingRoom()*samplesize));
//			cap.setSeats(100 * 1000);
//			cap.setStandingRoom(100 * 1000);
			
			vt.setCapacity(cap);
//			vt.setPcuEquivalents(vt.getPcuEquivalents()*samplesize); // FIXME !!!
			vt.setPcuEquivalents(vt.getPcuEquivalents() / samplesize);

			vt.setAccessTime(vt.getAccessTime() / samplesize);
			vt.setEgressTime(vt.getEgressTime() / samplesize);
//			vt.setAccessTime(0.0);
//			vt.setEgressTime(0.0);
		}
	}
}
