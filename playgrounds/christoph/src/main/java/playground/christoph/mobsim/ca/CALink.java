/* *********************************************************************** *
 * project: org.matsim.*
 * CALink.java
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

import java.util.Collection;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimLink;
import org.matsim.vis.snapshotwriters.VisData;

/**
 * 
 * TODO:
 * - check array index conversion (java: 0...n; matlab 1...n+1?)
 * 
 * @author cdobler
 */
public class CALink implements NetsimLink {

	final private static Logger log = Logger.getLogger(CALink.class);
	
	private final Random random;
	private final Link link;
	private final CANode toNode;
	
	private final double spatialResolution;
	private final double minSpeed;
	private final double maxSpeed;
	
	private int nAgents;
	private CACell[] cells;
	private CACell firstCell;	// cache first cell
	private CACell lastCell;	// cache last cell
	
//	private Map<Id, CAAgent> caAgents;

//  function this = NLink(id, fromNode, toNode, spatialResolution)
//  this.id = id;
//  this.fromNode = fromNode;
//  this.toNode = toNode;
//  this.spatialResolution = spatialResolution;
//  this.minSpeed = this.spatialResolution + 2.0;
//  this.countStation = CountStation();
//end
	public CALink(Link link, CANode toNode, Random random, double spatialResolution) {
		this.link = link;
		this.toNode = toNode;
		this.random = random;
		this.spatialResolution = spatialResolution;
		
		this.minSpeed = this.spatialResolution + 2.0;	//???
		this.maxSpeed = link.getFreespeed();
		this.nAgents = 0;
		
//		this.caAgents = new HashMap<Id, CAAgent>();
		
		this.createCells();
	}

	public CANode getToNode() {
		return this.toNode;
	}
	
	public CACell[] getCells() {
		return this.cells;
	}
	
//    function [cells] = createCells(this) 
//            linkLength = SUtils().length(this.fromNode, this.toNode);
//            numberOfCells = ceil(linkLength / this.spatialResolution);
//            this.cells = LCell.empty(numberOfCells, 0); 
//            
//            dxy = (this.toNode.getPosition() - this.fromNode.getPosition())./numberOfCells;
//            
//            dxy_norm = dxy/norm(dxy);
//            p = [- dxy_norm(2); dxy_norm(1)];
//            
//            cellLength = linkLength / numberOfCells;
//            xIndex = 1;
//            yIndex = 2;
//            for i = 1:numberOfCells   
//                position = this.fromNode.getPosition() + (i-0.5)*dxy - p; % add half of the cell length. position is mid point of cell!
//                this.cells(i) = LCell(i, position(xIndex), position(yIndex), cellLength);
//            end            
//            cells = this.cells;
//        end
//        
//        function ctStation = getCountStation(this)
//            ctStation = this.countStation;
//        end
//   end
	private void createCells() {
		
		double linkLength = this.link.getLength();
		int numberOfCells = (int) Math.ceil(linkLength / this.spatialResolution);
		double  cellLength = linkLength / numberOfCells;
		
		Coord fromCoord = this.link.getFromNode().getCoord();
		Coord toCoord = this.link.getToNode().getCoord();
		double dx = (toCoord.getX() - fromCoord.getX()) / numberOfCells;
		double dy = (toCoord.getY() - fromCoord.getY()) / numberOfCells;
		
		// p... shift lanes??
		
		this.cells = new CACell[numberOfCells];
		for (int i = 0; i < numberOfCells; i++) {
			// add half of the cell length. position is mid point of cell!
			double x = fromCoord.getX() + (i - 0.5) * dx;
			double y = fromCoord.getY() + (i - 0.5) * dy;

			this.cells[i] = new CACell(i, new Coord(x, y), cellLength);
		}
		this.firstCell = this.cells[0];
		this.lastCell = this.cells[numberOfCells - 1];
	}
	                      
//        function [successful] = leave(this)
//          successful = false;
//          lastCell = this.getLastCell();
//          if (lastCell.hasAgent() && ~(this.toNode.hasAgent()))
//             % update infrastructure
//             agent = lastCell.getAgent();
//             this.toNode.setAgent(agent);             
//             agent.resetCell(); 
//             lastCell.reset(); 
//             this.nAgents = this.nAgents - 1;
//             successful = true;
//          end
//        end
	public boolean leave() {
		boolean successful = false;
		if (this.lastCell.hasAgent() && !this.toNode.hasAgent()) {
			// update infrastructure
			CAAgent agent = lastCell.getAgent();
			this.toNode.setAgent(agent);
//			agent.resetCell();
			lastCell.reset();
			this.nAgents--;
			log.info("Agent " + agent.getId() + " leaves link " + this.getId());
			successful = true;
		}
		
		return successful;
	}
             
//        function update(this, agent, timeStep, currentTime)    
//            currentCell = agent.getCell(); % so there is an agent in that cell
//            
//            % find cell which is reached with v * Delta(t) or head of gap            
//            nextCell = this.getNextCell(agent, timeStep, currentTime); % find cell of agent
//            currentCell.reset();
//            if (isempty(agent.getParkingLot()) || agent.leaveParkingLot(currentTime)) % agent is on the road
//                nextCell.setAgent(agent);
//            end
//            agent.setCell(nextCell); 
//        end
	public void update(CAAgent agent, double timeStep, double currentTime) {
		
		CACell currentCell = agent.getCell();
		
		// find cell which is reached with v * Delta(t) or head of gap            
		CACell nextCell = this.getNextCell(agent, timeStep, currentTime); // find cell of agent
		currentCell.reset();
		nextCell.setAgent(agent);
		agent.setCell(nextCell);
//		agent.setCell(nextCell);
//      if (isempty(agent.getParkingLot()) || agent.leaveParkingLot(currentTime)) % agent is on the road
//          nextCell.setAgent(agent);
//      end
//      agent.setCell(nextCell); 
	}
	
//        function [successful] = enter(this, agent, currentTime)
//            successful = false;       
//            firstCell = this.cells(1);            
//            if (~firstCell.hasAgent()) % first cell has no agent    
//                this.cells(1).setAgent(agent); % put agent into first cell
//                this.nAgents = this.nAgents + 1;
//                agent.setCell(this.cells(1));
//                this.countStation.increaseCount(currentTime);
//                successful = true;
//            end
//        end      
	public boolean enter(CAAgent agent, double currentTime) {
		boolean successful = false;
		
		if (!this.firstCell.hasAgent()) {
			this.firstCell.setAgent(agent);
			this.nAgents++;
			agent.setCell(this.firstCell);
			log.info("Agent " + agent.getId() + " enters link " + this.getId());
			((MobsimDriverAgent) agent.getMobsimAgent()).notifyMoveOverNode(this.getId());
			successful = true;
		}
		
		return successful;
	}
	
	
//        function [cells] = getCells(this) 
//            cells = this.cells;
//        end

//         function [fromNode] = getFromNode(this) 
//            fromNode = this.fromNode;
//         end
//        
//         function [toNode] = getToNode(this) 
//            toNode = this.toNode;
//         end
//         
//         function [id] = getId(this)
//             id = this.id;
//         end;
//         
//         function [nAgents] = getNumberOfAgentsOnLinkOrInParkings(this)
//             nAgents = this.nAgents;
//         end
	public int getNumberOfAgentsOnLink() {
		return this.nAgents;
	}
	
//         function [lastCell] = getLastCell(this)
//             lastCell = this.cells(length(this.cells));
//         end
	public CACell getLastCell() {
		return this.lastCell;
	}
	
//         function [cell] = getCell(this, id)
//             if (id > length(this.cells))
//                 id = length(this.cells);
//             end
//             cell = this.cells(id);
//         end
	private CACell getCell(int index) {
		if (index >= this.cells.length) return this.lastCell;
		else return this.cells[index];
	}
	
//    methods (Access = private)   
//        function [cell] = getNextCell(this, agent, timeStep, currentTime)
//            currentIndex = agent.getCell().getId();
//            nextIndex = min(currentIndex + floor(agent.getCurrentSpeed() * timeStep / this.spatialResolution), length(this.cells)); % min(id, id(link length))
//            nextCellIndex = this.moveOrParkInGap(currentIndex + 1, nextIndex, agent, currentTime, timeStep); % start at next cell not current cell
//            cell = this.getCell(nextCellIndex);
//        end
	private CACell getNextCell(CAAgent caAgent, double timeStep, double currentTime) {
		
		int currentIndex = caAgent.getCell().getId();

		// min(id, id(link length))
		int nextIndex = Math.min(currentIndex + (int) Math.floor(caAgent.getCurrentSpeed() * timeStep / this.spatialResolution), this.cells.length);

		// start at next cell not current cell
		int nextCellIndex = this.moveOrParkInGap(currentIndex + 1, nextIndex, caAgent, currentTime, timeStep);
		CACell cell = this.getCell(nextCellIndex);
		return cell;
	}
	
//        function [index] = moveOrParkInGap(this, startIndex, endIndex, agent, currentTime, timeStep)
//            index = endIndex;                      
//            for i = startIndex:endIndex 
//                cell = this.getCell(i);
//                if (~cell.hasAgent()) % cell is reachable
//                    index = i;
//                    if (this.isParking(agent, cell, currentTime))
//                       break;
//                    end
//                    if (i + 1 < length(this.cells) && ~this.getCell(i + 1).hasAgent()) % next cell is also free
//                        newSpeed = max(this.minSpeed, min(agent.getCurrentSpeed() + this.spatialResolution / timeStep + rand(), this.maxSpeed - rand()));
//                        agent.setCurrentSpeed(newSpeed); % step 1 and 2 of NaSch 
//                    end
//                else
//                    index = i - 1; % go to last free cell of gap
//                    newSpeed = max(this.minSpeed, min((index - startIndex + 1) / timeStep *  this.spatialResolution - rand(), this.maxSpeed - rand())); % + 1 as we start at agent's current position + 1! -> (startIndex - 1 - startIndex + 1) if agent should not move
//                    agent.setCurrentSpeed(newSpeed); % step 2 and 3 of NaSch
//                    break;
//                end
//            end
//        end

	private int moveOrParkInGap(int startIndex, int endIndex, CAAgent caAgent, double currentTime, double timeStep) {
		int index = endIndex;
		
		for (int i = startIndex; i <= endIndex; i++) {
			// TODO: check - can we use the same random value twice?
			double rand = random.nextDouble();
			
			CACell cell = this.getCell(i);
			// cell is reachable
			if (!cell.hasAgent()) {
				index = i;
				if (this.isParking(caAgent, cell, currentTime)) {
					break;
				}
				// next cell is also free
				if (i + 1 < this.cells.length && !this.getCell(i + 1).hasAgent()) {
					double newSpeed = Math.max(this.minSpeed, Math.min(caAgent.getCurrentSpeed() + this.spatialResolution / timeStep + rand, this.maxSpeed - rand));
					//step 1 and 2 of NaSch 
					caAgent.setCurrentSpeed(newSpeed); 
				}
			} else {
				// go to last free cell of gap
				index = i - 1;
				
				// + 1 as we start at agent's current position + 1! -> (startIndex - 1 - startIndex + 1) if agent should not move
				double newSpeed = Math.max(this.minSpeed, Math.min((index - startIndex + 1) / timeStep *  this.spatialResolution - rand, this.maxSpeed - rand)); 

				// step 2 and 3 of NaSch
				caAgent.setCurrentSpeed(newSpeed);
				
				break;
			}
		}
		return index;
	}
	
//        function [successful] = isParking(this, agent, cell, currentTime)
//            successful = false;
//            
//            if (~isempty(agent.getParkingLot())) % agent has already parked
//                return;
//            end
//                                   
//            if (~isempty(cell.getParkingLot()) && cell.getParkingLot().isFree() && this.isAgentParkingHere(agent, cell.getParkingLot(), currentTime))
//                cell.reset();
//                parkingLot = cell.getParkingLot();
//                parkingLot.addAgent(agent); 
//                successful = true;
//                % str = sprintf('%s %i', parkingLot.getId(), parkingLot.getSize());
//                % disp(str);
//            end
//        end
	private boolean isParking(CAAgent caAgent, CACell cell, double currentTime) {
		boolean successful = false;
		
		// agent has already parked
		if (caAgent.getParkingLot() != null) {
			return successful;
		}
		
		if (cell.getParkingLot() != null && cell.getParkingLot().isFree() && 
				this.isAgentParkingHere(caAgent, cell.getParkingLot(), currentTime)) {
			cell.reset();
			CAParkingLot parkingLot = cell.getParkingLot();
			parkingLot.addAgent(caAgent);
			successful = true;
		}
		
		return successful;
	}
	
//        function [park] = isAgentParkingHere(this, agent, parkingLot, currentTime)
//            agent.addParkingLotToMemory(parkingLot); % TODO: check if parking link is free and add to good or bad list
//            park = agent.isParkingLotChosen(currentTime, parkingLot); % here parking lot is assigned to agent with prob.
//        end
//    end
	private boolean isAgentParkingHere(CAAgent caAgent, CAParkingLot parkingLot, double currentTime) {
		// TODO: check if parking link is free and add to good or bad list
		caAgent.addParkingLotToMemory(parkingLot);
		// here parking lot is assigned to agent with prob.
		boolean park = caAgent.isParkingLotChosen(currentTime, parkingLot);
		return park;
	}
	
	@Override
	public Map<String, Object> getCustomAttributes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public VisData getVisData() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Link getLink() {
		return this.link;
	}

	@Override
	public void recalcTimeVariantAttributes(double time) {
		// TODO Auto-generated method stub

	}

	@Override
	public Collection<MobsimVehicle> getAllVehicles() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<MobsimVehicle> getAllNonParkedVehicles() {
		// TODO Auto-generated method stub
		return null;
	}

	public Id getId() {
		return this.link.getId();
	}
}