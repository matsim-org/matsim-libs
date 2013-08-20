/* *********************************************************************** *
 * project: org.matsim.*
 * CACell.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.mobsim.ca;

import org.matsim.api.core.v01.Coord;

/**
 * TODO: 
 * Allow multiple parking lots per cell. This will allow to
 * assign them based on their location to their nearest cell.
 * 
 * @author cdobler
 *
 */
public class CACell {

	private CAAgent agentInCell;

	private final int id;
	private final Coord coord;
	private final double length;
	
	private CAParkingLot parkingLot;
	
	public CACell(int id, Coord coord, double length) {
		this.id = id;
		this.coord = coord;
		this.length = length;
		
		this.agentInCell = null;
	}
	
	public int getId() {
		return this.id;
	}
	
	public Coord getCoord() {
		return this.coord;
	}
	
//        function [m] = setAgent(this, agentInCell) 
//            this.agentInCell = agentInCell;
//            this.hasAgent = true;
//            m = true;
//        end
	public boolean setAgent(CAAgent agentInCell) {
		this.agentInCell = agentInCell;
		return true;
	}
	
//        function reset(this) 
//            this.agentInCell = [];
//            this.hasAgent = false;
//        end
	public void reset() {
		this.agentInCell = null;
	}
	
//        function [length] = getLength(this)
//            length = this.length;
//        end
	public double getLength() {
		return this.length;
	}
        
//        function [agent] = getAgent(this)
//            agent = this.agentInCell;
//        end
	public CAAgent getAgent() {
		return this.agentInCell;
	}
	
	public boolean hasAgent() {
		return this.agentInCell != null;
	}
	
//	% Basic unit of CA. Consecutive cells constitute a link. Parking lots are attached to cells
//    properties 
//        hasAgent = false;
//    end
//        
//    properties (Access = private)
//        agentInCell;
//        parkingLot;
//        length;
//    end
//    
//    methods
//        function this = LCell(id, position_x, position_y, length)
//            this = this@SpatialElement(id, position_x, position_y);
//            if ischar(length)
//                length = str2double(position_y);
//            end
//            this.length = length;
//        end
//                
//        function [m] = setParkingLot(this, parkingLot)
//            this.parkingLot = parkingLot;
//            m = true;
//        end
	public boolean setParkingLot(CAParkingLot parkingLot) {
		this.parkingLot = parkingLot;
		return true;
	}
//        function [parkingLot] = getParkingLot(this)
//            parkingLot = this.parkingLot;
//        end
//        
	public CAParkingLot getParkingLot() {
		return this.parkingLot;
	}
}
