/* *********************************************************************** *
 * project: org.matsim.*
 * CAAgent.java
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

import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimNetwork;
import org.matsim.core.utils.geometry.CoordUtils;

public class CAAgent implements DriverAgent {

	private MobsimAgent mobsimAgent;
	private CAParkingLot parkingLot;
	private CACell cell;
//	private CANode destinationNode;
//	private Id destinationNodeId;
	private CARoute routeTo;
	private CARoute routeAway;
	private ParkingDecision parkingDecisionType;
	private Id linkMemoryLastVisitedNodeId;
	private NetsimNetwork network;
	private RouteChoice routeChooser;
	private Random random;
	
	private boolean transit;
	
	private double currentSpeed = 10.0;

	private double actDur;
	private double parkTime;
	private double tripStartTime;
	private double startTimeSearchingForParking;
	private double startTimeForParkingSearch;
	
	private int shortTermMemorySize = 5;
	private Deque<Id> parkingIdsMemory = new LinkedList<Id>();
	private Deque<ParkingMemory> shortTermParkingMemory = new LinkedList<ParkingMemory>();
	
	public CAAgent(MobsimAgent agent, NetsimNetwork network) {
		this.mobsimAgent = agent;
		this.network = network;
		
		this.random = MatsimRandom.getLocalInstance();
		this.routeChooser = new RandomRouteChoice();
	}
	
	public MobsimAgent getMobsimAgent() {
		return this.mobsimAgent;
	}
	
//	  properties
//      plotHandle;
//  end
//  
//  properties (Access = private)
//      id;
//      originNode;
//      destinationNode;
//      tripStartTime;
//      actDur; 
//      parkingDecisionType;
//      routeChooser;
//      routeTo;
//      routeAway;
//      transit;
//      
//      shortTermMemorySize = 10;
//      shortTermParkingMemory; %x, y, occupied, size
//      parkingIdsMemory = {};
//      
//      linkMemoryLastVisitedNode = ''; % last Node
//              
//      adaptedRoute;
//      startTimeSearchingForParking = -1.0;  
//      currentSpeed = 10.0; % m/s
//      
//      cell;
//      
//      % for analysis
//      parkTime = -99.0;
//      parkingLot;
//      
//      infrastructure;
//      
//      hasPrivateParking;
//  end
//  
//  methods
//      function this = Agent(id, tripStartTime, parkingDecisionType, routeTo, routeAway, actDur, transit, infrastructure, hasPrivateParking)
//          this.id = id;
//          this.tripStartTime = tripStartTime;
//          this.actDur = actDur;
//          this.transit = transit;
//          this.routeTo = routeTo;
//          this.routeAway = routeAway;
//          
//          if (this.transit)
//              this.originNode = this.routeAway.getOriginNodeId();
//              this.destinationNode = this.routeAway.getDestinationNodeId();               
//          else 
//              this.originNode = this.routeTo.getOriginNodeId();
//              this.destinationNode = this.routeTo.getDestinationNodeId();
//          end
//          this.parkingDecisionType = parkingDecisionType;
//          %this.routeChooser = RandomRouteChoice();
//          this.routeChooser = WeightedRandomRouteChoice();
//          this.shortTermParkingMemory = repmat([-999999; -999999; 1; 1], 1, this.shortTermMemorySize);
//          this.infrastructure = infrastructure;
//          this.hasPrivateParking = hasPrivateParking;
//      end
//              
//      function [originNode] = getOriginNode(this)
//      % corresponds with start node of route
//          originNode = this.originNode;
//      end
//
//      function [link] = getNextLink(this, currentNode)
//          % either give next link on the route to destination or give next link for parking search
//          if (this.startTimeSearchingForParking < 0 && (~this.transit)) % agent has not yet started parking search -> follow routeTo
//               link = this.routeTo.getNextLink(currentNode, this.infrastructure); 
//          else
//              if (this.parkTime > 0 || this.transit) % agent has left parking lot and is now on the way back home -> follow routeAway
//                 link = this.routeAway.getNextLink(currentNode, this.infrastructure); 
//              else % agent searches a parking lot
//                  destination = this.infrastructure.getNodeById(this.destinationNode).getPosition();
//                  linkAlternatives = currentNode.getFromLinks();
//                  % remove link agent just came from with 90% probability
//                  if(rand() < 0.9 && numel(linkAlternatives) > 1 && ~isempty(this.linkMemoryLastVisitedNode))
//                     keep = ones(size(linkAlternatives)) == 1;
//                     for l = 1 : numel(linkAlternatives)
//                         link = linkAlternatives(l);
//                         if strcmp(link.getToNode.getId, this.linkMemoryLastVisitedNode)
//                             keep(l) = 0;
//                         end
//                     end
//                     if(sum(keep) > 0)
//                          linkAlternatives = linkAlternatives(keep);
//                     end
//                  end
//                  link = this.routeChooser.chooseLink(currentNode.getPosition(), destination, this.shortTermParkingMemory, linkAlternatives);
//              end
//          end
//          % add link to memory
//          this.addLinkToMemory(link);
//          %DEBUG
//          %link
//      end

	@Override
	public Id chooseNextLinkId() {
		
		Id currentLinkId = this.getMobsimAgent().getCurrentLinkId();
		Id currentNodeId = this.network.getNetsimLink(currentLinkId).getLink().getToNode().getId();
		
		//either give next link on the route to destination or give next link for parking search
		CALink link = null;
		
		// agent has not yet started parking search -> follow routeTo
		if (this.startTimeSearchingForParking < 0 && !this.transit) {
			link = this.routeTo.getNextLink(currentNodeId, this.network);
		} else {
			// agent has left parking lot and is now on the way back home -> follow routeAway
			if (this.parkTime > 0 || this.transit) {
				link = this.routeAway.getNextLink(currentNodeId, this.network); 
			} else { // agent searches a parking lot
				CANode destination = (CANode) this.network.getNetsimNode(this.getDestinationNodeId());
				CANode currentNode = (CANode) this.network.getNetsimNode(currentNodeId);
				
				// added to stop agent's route at its destination
				if (destination == currentNode) return null;
				
				List<CALink> linkAlternatives = new ArrayList<CALink>();
				for (CALink l : currentNode.getFromLinks()) linkAlternatives.add(l);

				// remove link agent just came from with 90% probability
				if (this.random.nextDouble() < 0.90 && linkAlternatives.size() > 1 && this.linkMemoryLastVisitedNodeId != null) {
					// TODO ...
//                  keep = ones(size(linkAlternatives)) == 1;
//                  for l = 1 : numel(linkAlternatives)
//                      link = linkAlternatives(l);
//                      if strcmp(link.getToNode.getId, this.linkMemoryLastVisitedNode)
//                          keep(l) = 0;
//                      end
//                  end
//                  if(sum(keep) > 0)
//                       linkAlternatives = linkAlternatives(keep);
//                  end
				}
				link = this.routeChooser.chooseLink(currentNode.getCoord(), destination, this.shortTermParkingMemory, linkAlternatives);				
			}
		}
		
		return link.getId();
	}
	
//      function [isSearching] = isSearching(this, currentTime)
//          isSearching = (this.startTimeSearchingForParking > 0 && currentTime >= this.startTimeSearchingForParking && this.parkTime < 0);
//          
//          if (~isSearching)
//              isSearching = this.tryStartSearching(currentTime); 
//          end         
//      end 
	public boolean isSearching(double currentTime) {
		boolean isSearching = this.startTimeSearchingForParking > 0 && currentTime >= this.startTimeSearchingForParking && this.parkTime < 0;
		
		if (!isSearching) {
			isSearching = this.tryStartSearching(currentTime);
		}
		
		return isSearching;
	}
	
//      function setCell(this, cell)
//          this.cell = cell;
//      end
	public void setCell(CACell cell) {
		this.cell = cell;
	}
	
//      function [cell] = getCell(this)
//          cell = this.cell;
//      end
	public CACell getCell() {
		return this.cell;
	}
	
//      function resetCell(this)
//          this.cell = [];
//      end
	public void resetCell() {
		this.cell = null;
	}
	
//      function [agentId] = getId(this)
//          agentId = this.id;
//      end
	
	public Id getId() {
		return this.mobsimAgent.getId();
	}
	
//      function [currentSpeed] = getCurrentSpeed(this)
//          currentSpeed = this.currentSpeed;
//      end
	public double getCurrentSpeed() {
		return this.currentSpeed;
	}
//      function setCurrentSpeed(this, speed)
//          this.currentSpeed = speed;
//      end
	public void setCurrentSpeed(double speed) {
		this.currentSpeed = speed;
	}
	
//      function [startSearchTime] = getStartSearchTime(this)
//          startSearchTime = this.startTimeSearchingForParking;
//      end
	public double getStartSearchTime() {
		return this.startTimeSearchingForParking;
	}
	
//      function [parkTime] = getParkTime(this)
//          parkTime = this.parkTime;
//      end
	public double getParkTime() {
		return this.parkTime;
	}
	
//      function [parkDur] = getParkDuration(this)
//          parkDur = this.actDur;
//      end
	public double getParkDuration() {
		return this.actDur;
	}
	
//      function [searchDur] = getSearchDuration(this)
//          searchDur = -99;
//          if(this.parkTime > 0 && this.startTimeSearchingForParking > 0)
//              searchDur = this.parkTime - this.startTimeSearchingForParking;
//          end
//      end
	public double getSearchDuration() {
		double searchDur = -99;
		if (this.parkTime > 0 && this.startTimeForParkingSearch > 0) {
			searchDur = this.parkTime - this.startTimeForParkingSearch;
		}
		return searchDur;
	}
	
//      function [parkingLot] = getParkingLot(this)
//          parkingLot = this.parkingLot;
//      end
	public CAParkingLot getParkingLot() {
		return this.parkingLot;
	}
	
//      function [node] = getDestinationNode(this)
//          node = this.destinationNode;
//      end
	public Id getDestinationNodeId() {
		return this.network.getNetsimLink(this.mobsimAgent.getDestinationLinkId()).getLink().getToNode().getId();
//		return this.destinationNodeId;
	}
		
//      function [nodeId] = getLeaveParkingNodeId(this)
//          nodeId = '';
//          if(~isempty(this.routeAway))
//              nodeId = this.routeAway.getOriginNodeId();
//          end
//          if(isempty(nodeId))
//              nodeId = this.destinationNode;
//          end
//      end
	public Id getLeaveParkingNodeId() {
		if (this.routeAway != null) {
			return this.routeAway.getOriginNodeId();
		} else return this.getDestinationNodeId();
	}
	
//      function [isTransit] = isTransit(this)
//          isTransit = this.transit;
//      end
	public boolean isTransit() {
		return this.transit;
	}
//      function [leaveParkingLot] = leaveParkingLot(this, currentTime)
//          % make this dependent on arrival time + activity duration
//          leaveParkingLot = false;
//          if (currentTime >= (this.parkTime + this.actDur))
//              leaveParkingLot = true;
//          end
//      end
	public boolean leaveParkingLot(double currentTime) {
		// make this dependent on arrival time + activity duration
		boolean leaveParkingLot = false;
		if (currentTime >= this.parkTime + this.actDur) {
			leaveParkingLot = true;
		}
		return leaveParkingLot;
	}
	
//      function [distanceToDestination] = getDistance2Destination(this, parkingLot)
//      % returns crow-fly distance to destination from a given parking lot
//          destination = this.getDestination(this.infrastructure);
//          distanceToDestination = SUtils().length(destination, parkingLot);
//      end
	public double getDistance2Destination(CAParkingLot parkingLot) {
		// returns crow-fly distance to destination from a given parking lot
		CANode destination = this.getDestination(this.network);
//		distanceToDestination = SUtils().length(destination, parkingLot);		
		double distanceToDestination = CoordUtils.calcDistance(destination.getCoord(), parkingLot.getCoord());
		return distanceToDestination;
	}
	
//      function [tripStartTime] = getTripStartTime(this)
//          tripStartTime = this.tripStartTime;
//      end
	public double getTripStartTime() {
		return this.tripStartTime;
	}
//      function [parkHere] = isParkingLotChosen(this, currentTime, parkingLot)
//          % check if free ...
//          parkHere = false;
//
//          if(this.isSearching(currentTime))
//              try
//                  distanceToDestination = this.getDistance2Destination(parkingLot);
//              catch e
//                  sprintf('%s %s', 'Agent: ', this.id)
//                  sprintf('%s %s', 'Dest Node: ', this.destinationNode)
//                  sprintf('%s %s', 'Parking Lot: ', parkingLot.getId())
//                  throw(e);
//              end
//              p = this.parkingDecisionType.parkProbability(currentTime - this.startTimeSearchingForParking, distanceToDestination);
//              parkHere = (rand(1) < p);
//          end
//          if (parkHere)
//              this.parkTime = currentTime;
//              this.parkingLot = parkingLot;
//              this.cell = [];
//          end
//      end   
	public boolean isParkingLotChosen(double currentTime, CAParkingLot parkingLot) {
		
		boolean parkHere = false;
		
		if (this.isSearching(currentTime)) {
			double distanceToDestination = this.getDistance2Destination(parkingLot);

			double p = this.parkingDecisionType.parkProbability(currentTime - this.startTimeSearchingForParking, distanceToDestination);
			parkHere = random.nextDouble() < p;
		}
		if (parkHere) {
			this.parkTime = currentTime;
			this.parkingLot = parkingLot;
			this.cell = null;
		}
		
		return parkHere;
	}
	
//      % called in NLink.parkAgent
//      function addParkingLotToMemory(this, parkingLot)
//          parkingMemory = [parkingLot.getPosition(); parkingLot.getNrOccupiedSpaces(); parkingLot.getSize()];   
//          removedIdx = this.addIdToParkingMemory(parkingLot.getId());
//          
//          if(isempty(removedIdx))
//              this.shortTermParkingMemory = [parkingMemory this.shortTermParkingMemory(:,1:this.shortTermMemorySize-1)];
//          else
//              this.shortTermParkingMemory = [parkingMemory this.shortTermParkingMemory(:,1:removedIdx-1) this.shortTermParkingMemory(:,removedIdx+1:this.shortTermMemorySize)];
//          end
//      end
	public void addParkingLotToMemory(CAParkingLot parkingLot) {
		
		ParkingMemory parkingMemory = new ParkingMemory();
		parkingMemory.parkingLotId = parkingLot.getId();
		parkingMemory.nrOccupiedSpaces = parkingLot.getNrOccupiedSpaces();
		parkingMemory.size = parkingLot.getSize();
		boolean isNewInMemory = this.addIdToParkingMemory(parkingLot.getId());
      
		if (isNewInMemory) {
			this.shortTermParkingMemory.addFirst(parkingMemory);
		} else {
			Iterator<ParkingMemory> iter = this.shortTermParkingMemory.iterator();
			while (iter.hasNext()) {
				ParkingMemory oldMemory = iter.next();
				if (oldMemory.parkingLotId.equals(parkingLot.getId())) {
					iter.remove();
					break;
				}
			}
			this.shortTermParkingMemory.addFirst(parkingMemory);
		}
	}
	
//      %currently Size one
//      function addLinkToMemory(this, link)
//          if ~isempty(link) && ~isempty(link.getFromNode)
//              this.linkMemoryLastVisitedNode = link.getFromNode.getId;
//          else
//              this.linkMemoryLastVisitedNode = '';
//          end
//      end
	public void addLinkToMemory(CALink link) {
		if (link != null && link.getLink().getFromNode() != null) {
			 this.linkMemoryLastVisitedNodeId = link.getLink().getFromNode().getId();
		} else this.linkMemoryLastVisitedNodeId = null;
	}
	
//      function [this,idx] = sort(this, varargin)
//          [~,idx] = sort([this.tripStartTime], varargin{:});  
//          this = this(idx);
//      end   
//     
//      function [hasPrivateParkingSpace] = hasPrivateParkingSpace(this)
//          hasPrivateParkingSpace = this.hasPrivateParking;
//      end
//  end
//
//  methods (Access = private)
//      
//      function removedIdx = addIdToParkingMemory(this, parkingLotId)
//          size = length(this.parkingIdsMemory);
//          removedIdx = find(strcmp(this.parkingIdsMemory, parkingLotId));
//          
//          % move memories one back
//          if(isempty(removedIdx))
//              if(size >= this.shortTermMemorySize)
//                  startIndex = size - 1; %overwrite last item, move all others one back
//              else
//                  startIndex = size; %move all existin items one back
//              end
//          else
//              startIndex = removedIdx - 1; %move parkingLotId to the front, move all before that one back
//          end
//
//          for ind = startIndex : -1 : 1
//              this.parkingIdsMemory(ind+1) = this.parkingIdsMemory(ind);
//          end
//
//          % add new memory
//          this.parkingIdsMemory(1) = {parkingLotId};  
//      end
	// returns true if the parking lot was not already present in the agent's memory
	public boolean addIdToParkingMemory(Id parkingLotId) {
		
		if (!this.parkingIdsMemory.contains(parkingLotId)) {
			int size = this.parkingIdsMemory.size();
			if (size >= this.shortTermMemorySize) {
				this.parkingIdsMemory.removeLast();
			}
			this.parkingIdsMemory.addFirst(parkingLotId);
			return true;
		} else {
			// move entry to the deque's head
			this.parkingIdsMemory.remove(parkingLotId);
			this.parkingIdsMemory.addFirst(parkingLotId);
			return false;
		}
	}
	
//      function dest = getDestination(this, infrastructure)
//          dest = infrastructure.getNodeById(this.destinationNode);
//      end
	public CANode getDestination(NetsimNetwork infrastructure) {
		// cast NetsimNode to CANode
		return (CANode) this.network.getNetsimNode(this.getDestinationNodeId());
	}
	
//      function isSearching = tryStartSearching(this, currentTime)   
//         isSearching = false;
//         if (this.transit) % agent never wants to park 
//             return;
//         end 
//         
//         if (isempty(this.cell)) % agent is parking or in a waiting queue
//             return;
//         end
//                    
//         distanceToDestination = SUtils().length(this.getDestination(this.infrastructure), this.cell);
//         if(this.parkTime < 0) % agent has not yet parked 
//             if (this.parkingDecisionType.isSearchStarting(distanceToDestination)) % agent wants to start searching now
//               this.startTimeSearchingForParking = currentTime;
//               % here speed during parking search could be adapted
//               % newSpeed = this.getCurrentSpeed() * 0.8;
//               % this.setCurrentSpeed(newSpeed); % adapt speed for parking search
//               isSearching = true;
//             end
//         end
//      end
	public boolean tryStartSearching(double currentTime) {
		boolean isSearching = false;
		
		// agent never wants to park
		if (this.transit) {
			return isSearching;
		}
		
		// agent is parking or in a waiting queue
		if (this.cell == null) {
			return isSearching;
		}
		
		double distanceToDestination = CoordUtils.calcDistance(this.getDestination(this.network).getCoord(), this.cell.getCoord());
		// agent has not yet parked
		if (this.parkTime < 0) {
			//agent wants to start searching now
			if (this.parkingDecisionType.isSearchStarting(distanceToDestination)) {
				this.startTimeSearchingForParking = currentTime;
				// here speed during parking search could be adapted
//				newSpeed = this.getCurrentSpeed() * 0.8;
//				this.setCurrentSpeed(newSpeed);
				isSearching = true;
			}
		}
		
		return isSearching;
	}
	
	public static class ParkingMemory {
		Id parkingLotId;
		Object position;
		int nrOccupiedSpaces;
		int size;
	}

	@Override
	public Id getCurrentLinkId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Id getDestinationLinkId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setVehicle(MobsimVehicle veh) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public MobsimVehicle getVehicle() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Id getPlannedVehicleId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void notifyMoveOverNode(Id newLinkId) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public boolean isWantingToArriveOnCurrentLink() {
		// The following is the old condition: Being at the end of the plan means you arrive anyways, no matter if you are on the right or wrong link.
		// kai, nov'14
		if ( this.chooseNextLinkId()==null ) {
			return true ;
		} else {
			return false ;
		}
	}

}
