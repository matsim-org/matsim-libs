/* *********************************************************************** *
 * project: org.matsim.*
 * CA.java
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
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimNetwork;
import org.matsim.core.utils.misc.Time;

public class CA implements MobsimEngine {

//	classdef CA < handle
//    %Cellular automaton updating infrastructure and population
//    
//    properties(Access = private)
//        infrastructure;
//        population; 
//        
//        timeStep; 
//        startTime;
//        endTime;        
//        currentTime = 0;
//        
//        scenarioPlotter;
//        showSimulation;
//        plotTimeStep = 600; %1800;
//        recordSimulation = false;
//        frameCounter = 0;
//        recordFrames;
//        outFolder = '';
//        
//        caServer = CAServer(); % handles the CA data structures
//        % TODO: we should have a similar class that adapts the
//        % infrastrutcure itself. If agents, links, nodes and parking lots
//        % do that all together a mess will probably be the result
//        % ------------------------------- 
//        
//        waitingForSimulationQueue = Agent.empty(0,0);
//        removeAgentsFromWaitingForSimulationQueue = [];
//        
//        scenarioFigure;
//    end

	private static final Logger log = Logger.getLogger(CA.class);
	
	private final CANetwork network;
	private final CAServer caServer;
	private final double spatialResolution;
	private final double timeStep;
	private final double startTime;
	private final double endTime;
	private final Random random;
	
	private InternalInterface internalInterface;
	private double currentTime = Time.UNDEFINED_TIME;
	private Map<Id, CAAgent> agents;
	private PriorityQueue<CAAgent> waitingForSimulationQueue;
	private List<CAAgent> removeAgentsFromWaitingForSimulationQueue = new ArrayList<CAAgent>();
	
//    methods 
//        function this = CA(infrastructure, spatialResolution, timeStep, startTime, endTime, showSimulation)
//            this.infrastructure = infrastructure;
//            this.scenarioPlotter = ScenarioPlotter(spatialResolution);
//            this.timeStep = timeStep;
//            this.startTime = startTime;
//            this.endTime = endTime;
//            this.showSimulation = showSimulation;
//        end
	public CA(Network network, double spatialResolution, double timeStep, double startTime, double endTime) {
		this.spatialResolution = spatialResolution;
		this.timeStep = timeStep;
		this.startTime = startTime;
		this.endTime = endTime;
		
		this.random = MatsimRandom.getRandom();
		this.caServer = new CAServer();
		
		CANetworkFactory networkFactory = new CANetworkFactory(this.spatialResolution);
		this.network = new CANetwork(network, networkFactory);
		this.network.initialize(this);
	}
	
//        function init(this, population) 
//            this.population = population;
//            this.fillWatingForSimQueue();
//            this.caServer.init(this.infrastructure);
//            this.currentTime = this.startTime;
//        end
	public void init(Map<Id, CAAgent> population) {
		this.agents = population;
		this.fillWatingForSimQueue();
		this.caServer.init(this.network);
		this.currentTime = this.startTime;
	}
	
	public NetsimNetwork getNetsimNetwork() {
		return this.network;
	}
	
//        function simulate(this)
//            disp('running simulation loop ...');
//            
//            scrsz = get(0,'ScreenSize');
//            this.scenarioFigure = figure('Position',[100.0 100.0 scrsz(4)*0.9 scrsz(4)*0.7]);  % [left, bottom, width, height]
//            
//            while (this.currentTime <= this.endTime)
//                this.update();
//                this.plot(this.currentTime);
//                this.currentTime = this.currentTime + this.timeStep;
//                
//                if (mod(this.currentTime, 60) == 0)
//                    cl = fix(clock);
//                    str = sprintf('current time %i %s %d %s %d %s %d', this.currentTime / 60.0, ' min after midnight', cl(4), ':', cl(5), ':', cl(6));
//                    disp(str);
//                end                
//            end
//        end
	public void simulate() {
		log.info("running simulation loop ...");
		
		while (this.currentTime <= this.endTime) {
			this.update();
			this.plot(this.currentTime);
			this.currentTime += this.timeStep;
			
			if ((this.currentTime % 3600) == 0) {
//				cl = fix(clock);
				log.info("current time " + Time.writeTime(this.currentTime));
			}
		}
	}
	
//        function update(this)
//            this.updateAgents();
//            this.moveAgentsToSimForTimeStep();
//            this.updateNodes();
//            this.updateParkings();
//            this.updateLinks();
//            this.updateRemainingWaitingQueues();
//        end
	public void update() {
		this.updateAgents();
		this.moveAgentsToSimForTimeStep();
		this.updateNodes();
		this.updateParkings();
		this.updateLinks();
		this.updateRemainingWaitingQueues();
	}
	
//        function plot(this, time)
//            if(this.showSimulation && mod(time, this.plotTimeStep) == 0)
//                hold on;
//                this.scenarioPlotter.plot(this.infrastructure.getLinks(), this.infrastructure.getNodes(), this.population , time);
//                saveas(gcf, sprintf('%s/network_%i',this.outFolder, time), 'fig');
//                if(this.recordSimulation)
//                    this.frameCounter = this.frameCounter + 1;
//                    this.recordFrames(:,this.frameCounter) = getframe(this.scenarioFigure);  %, [250,250,300,300]
//                end 
//                pause(0.0000001); % remove asap -> slows down sim
//            end
//        end
	public void plot(double time) {
		
	}
	
//%         function added = addFrame(this, frame, time)
//%             if size(frame.cdata,1) == 0
//%                 added = false
//%             else
//%                
//%             end
//%         end
//        
//        function startRecording(this)
//            this.recordFrames = moviein(0);
//            this.recordSimulation = true;
//        end
//        
//        function stopAndSaveRecording(this, videoName)
//            this.recordSimulation = false;
//            disp('Storing frames to video. This may take a while.');
//            try
//                movie2avi(this.recordFrames, videoName,'fps',60);
//            catch e
//                disp('Error while saving video')
//                disp(e)
//            end
//        end
//        
//        function setOutfolder(this, outfolder)
//            this.outFolder = outfolder;
//        end
//
//    end
//    
//    methods (Access = private)
//           
//      function fillWatingForSimQueue(this)
//        numberOfAgents = length(this.population);
//        cnt = 0;
//        waitingForSimulationQueueTmp = Agent.empty(0,0);
//        for i=1:numberOfAgents
//            agent = this.population(i);
//            departureTime = agent.getTripStartTime();
//            if (departureTime >= this.startTime && departureTime < this.endTime)
//                cnt = cnt + 1;
//                waitingForSimulationQueueTmp(end + 1) = agent;
//            end
//        end 
//        str = sprintf('Added %i %s', cnt, ' agents to waitingForSimulationQueue');
//        disp(str);
//        this.waitingForSimulationQueue = sort(waitingForSimulationQueueTmp); % sort array ascending for faster access        
//      end
	public void fillWatingForSimQueue() {
		
		int count = 0;
		this.waitingForSimulationQueue = new PriorityQueue<CAAgent>(500, new DepartureTimeComparator());
		
		for (CAAgent agent : this.agents.values()) {
			double departureTime = agent.getMobsimAgent().getActivityEndTime();
			if (departureTime >= this.startTime && departureTime < this.endTime) {
				count++;
				this.waitingForSimulationQueue.add(agent);	
			}
		}
		log.info("Added " + count + " agents to waitingForSimulationQueue");
	}
	
//      function moveAgentsToSimForTimeStep(this)
//          if (~isempty(this.waitingForSimulationQueue)) 
//              agent = this.waitingForSimulationQueue(1);
//              departureTime =  agent.getTripStartTime(); % get departure time of first agent in queue
//              cnt = 1;
//              while ((departureTime <= this.currentTime) && (departureTime >= this.startTime) && (departureTime < this.endTime) && (cnt <= length(this.waitingForSimulationQueue)))                    
//                  node = this.infrastructure.getNodeById(agent.getOriginNode());
//                  this.caServer.addAgentToWaitungQueue(node.getId(), agent); % put all agent in waitng queue. updateRemainingWaitingQueues is called later                
//                  this.removeAgentsFromWaitingForSimulationQueue(end + 1) = cnt; % remove agent from waitingForSimQueue in finish
//                  
//                  if (cnt <  length(this.waitingForSimulationQueue)) % get departure time of next agent
//                     agent = this.waitingForSimulationQueue(cnt + 1);
//                     departureTime = agent.getTripStartTime(); 
//                  end
//                  cnt = cnt + 1;
//              end
//          end
//          this.finishWaitingForSimulationQueue();
//      end
	public void moveAgentsToSimForTimeStep() {
		if (this.waitingForSimulationQueue != null) {
			while (true) {
				CAAgent agent = this.waitingForSimulationQueue.peek();
				if (agent == null) break;
				
				double departureTime = agent.getMobsimAgent().getActivityEndTime();
				if (departureTime <= this.currentTime && departureTime < this.endTime) {
					Id currentLinkId = agent.getMobsimAgent().getCurrentLinkId();
					CALink link = (CALink) this.network.getNetsimLink(currentLinkId);

					// change agent's state from activity to leg
					agent.getMobsimAgent().endActivityAndComputeNextState(this.currentTime);
//					this.simEngine.internalInterface.arrangeNextAgentState(agent);
					
					this.caServer.addAgentToWaitingQueue(link.getToNode().getId(), agent);
					
					
					// remove agent from priority queue
					this.waitingForSimulationQueue.poll();
				} else break;
			}
		}
		this.finishWaitingForSimulationQueue();
	}
	
//      function finishWaitingForSimulationQueue(this)
//          this.waitingForSimulationQueue(this.removeAgentsFromWaitingForSimulationQueue) = [];
//          this.removeAgentsFromWaitingForSimulationQueue = [];
//      end
	public void finishWaitingForSimulationQueue() {
		for (CAAgent agent : this.removeAgentsFromWaitingForSimulationQueue) {
			this.waitingForSimulationQueue.remove(agent);
		}
		this.removeAgentsFromWaitingForSimulationQueue.clear();
	}
	
//      function updateNodes(this)
//          randomIndices = randperm(this.caServer.getOccupiedNodesSize());
//          for i=1:length(randomIndices)
//              node = this.caServer.getOccupiedNode(randomIndices(i));
//              this.updateNode(node);
//          end
//          this.caServer.finishNodes();
//      end 
	public void updateNodes() {
		// TODO: this is not random anymore
		Iterator<CANode> iter = this.caServer.getOccupiedNodes().iterator();
		while (iter.hasNext()) {
			CANode node = iter.next();
			boolean removeNode = this.updateNode(node);
			if (removeNode) iter.remove();
		}
//		for (CANode node : this.caServer.getOccupiedNodes()) {
//			this.updateNode(node);
//		}
		this.caServer.finishNodes();
	}
	
//      function updateNode(this, node)
//          agent = node.getAgent();
//          link = agent.getNextLink(node); 
//          if (isempty(link)) % agent has arrived at final destination -> remove from simulation. 
//              % TODO: We have a problem here iff agent is at destination but
//              % does not start searching even now . Although behaviorally
//              % implausible this is neverthless a possible scenario!
//              % temporary solution: forced search start at destination!
//              node.setAgent([]);
//              this.caServer.leaveNode(node);
//          else
//              if (link.enter(agent, this.currentTime)) % check if this link can be entered (1st cell must be free)
//                  node.setAgent([]);              
//                  this.caServer.enterLink(link, agent);
//              end 
//          end
//      end
	// returns true of the node can be de-activated
	public boolean updateNode(CANode node) {
		CAAgent agent = node.getAgent();
		Id linkId = agent.chooseNextLinkId();
		CALink link = (CALink) this.network.getNetsimLink(linkId);
		// if the agent's next link is null, we assume that its leg end on the current node
		if (link == null) {
			node.setAgent(null);
			agent.getMobsimAgent().endLegAndComputeNextState(this.currentTime);
//			this.caServer.leaveNode(node);
			return true;
		} else {
			if (link.enter(agent, this.currentTime)) {
				node.setAgent(null);
				this.caServer.enterLink(link, node, agent);
				return true;
			}
		}
		return false;
	}
	
//      function updateLinks(this)  
//          randomIndices = randperm(this.caServer.getLinkQueuesSize()); % randomize key set
//          for i=1:length(randomIndices)    
//              key = this.caServer.getLinkQueuesKey(randomIndices(i));
//              this.updateLink(key);   
//          end  
//          this.caServer.finishLinks(this.infrastructure);
//          this.caServer.finishWaitingQueues();
//      end 
	public void updateLinks() {
		// TODO: this is not random anymore
		for (Id linkId : this.caServer.getActiveLinkIds()) {
			this.updateLink(linkId);
		}
		this.caServer.finishLinks(this.network);
		this.caServer.finishWaitingQueues();
	}
	
//      function updateLink(this, key)  
//          this.moveAgentToIntersection(key);
//          this.caServer.finishLinkQueue(key);
//          
//          this.moveAgentsOnLink(key); 
//          this.caServer.finishLinkQueue(key);
//      end
	public void updateLink(Id linkId) {
		CALink link = (CALink) this.network.getNetsimLink(linkId);
		
		this.moveAgentToIntersection(link);
		this.caServer.finishLinkQueue(linkId);
		
		this.moveAgentsOnLink(link);
		this.caServer.finishLinkQueue(linkId);
	}
	
//      % TODO: nicer if linkagentqueues are not leaving the caServer.
//      function moveAgentsOnLink(this, key)
//          link = this.infrastructure.getLinkById(key);
//          linkagentqueue = this.caServer.getLinkAgentQueue(key);         
//          linkagentqueue.prepareForIteration();
//          
//          while (linkagentqueue.hasMoreElements())
//                agent = linkagentqueue.getNextElement();
//                link.update(agent, this.timeStep, this.currentTime); 
//                parkingLot = agent.getParkingLot();
//                if (~isempty(parkingLot) && ~agent.leaveParkingLot(this.currentTime)) % agent has parked
//                    this.caServer.parkAgent(key, parkingLot);
//                end
//          end %while
//      end
	public void moveAgentsOnLink(CALink link) {
		/*
		 * TODO: 
		 * - Is there a way to iterate only over occupied cells?
		 * - Think about performance (iterate over array is faster than iterating over a list)
		 * - What about almost empty links?
		 * - What about overtaking (if using a list)
		 */
		CACell[] cells = link.getCells();
		for (int i = cells.length - 1; i >= 0; i--) {
			CACell cell = cells[i];
			CAAgent agent = cell.getAgent();
			if (agent != null) {
				link.update(agent, this.timeStep, this.currentTime);
				
				CAParkingLot parkingLot = agent.getParkingLot();

				// agent has parked
				if (parkingLot != null && !agent.leaveParkingLot(this.currentTime)) {
					this.caServer.parkAgent(agent, parkingLot);
				}
			}
		}	
//		Id linkId = link.getId();
//		IQueue queue = this.caServer.getLinkAgentQueue(linkId);
//		queue.prepareForIteration();
//				
//		while (!queue.isEmpty()) {
//			CAAgent agent = queue.getFirstElement();
//			link.update(agent, this.timeStep, this.currentTime);
//			CAParkingLot parkingLot = agent.getParkingLot();
//
//			// agent has parked
//			if (parkingLot != null && !agent.leaveParkingLot(this.currentTime)) {
//				this.caServer.parkAgent(agent, parkingLot);
//			}
//		}
	}
	
//      function moveAgentToIntersection(this, key)
//          link = this.infrastructure.getLinkById(key); 
//          toNode = link.getToNode();
//                    
//          takeFromLink = (rand(1,1) > 0.5); % randomly draw from links and waiting queue. If no agent is moved by one strategy, try the other.
//          
//          if (takeFromLink)
//              if (link.leave())  % test if last agent wants to and actually can enter intersection.
//                 this.caServer.moveAgentFromLinkToNode(key, toNode);
//              else % no agent was ready to leave the link
//                 this.moveAgentInWaitingQueueToNode(toNode);              
//              end
//          else 
//            if (~this.moveAgentInWaitingQueueToNode(toNode))
//                % waiting queue was empty
//                if (link.leave()) 
//                    this.caServer.moveAgentFromLinkToNode(key, toNode);
//                end %if
//            end % if
//          end  %else
//      end % function
	public void moveAgentToIntersection(CALink link) {
		CANode toNode = link.getToNode();
		
		// randomly draw from links and waiting queue. If no agent is moved by one strategy, try the other.
		boolean takeFromLink = this.random.nextDouble() > 0.5;
		
		if (takeFromLink) {
			// Test if last agent wants to and actually can enter intersection.
			if (link.leave()) {
				 this.caServer.moveAgentFromLinkToNode(link.getId(), toNode);
			} else { // no agent was ready to leave the link
				 this.moveAgentInWaitingQueueToNode(toNode);
			}
		} else {
			if (!this.moveAgentInWaitingQueueToNode(toNode)) {
				 // waiting queue was empty
				if (link.leave()) {
					this.caServer.moveAgentFromLinkToNode(link.getId(), toNode);
				}
			}
		}
	}
	
//      function [moved] = moveAgentInWaitingQueueToNode(this, toNode)
//          moved = false;             
//          if (isempty(toNode.getAgent())) % test if node is free, else do not move agent to node
//            agent = this.caServer.moveAgentFromWaitingQueueToNode(toNode);
//            if (~isempty(agent))
//               toNode.setAgent(agent);
//               moved = true;
//            end
//          end         
//      end
	@Deprecated	// we want agents to return to the link where they left it
	public boolean moveAgentInWaitingQueueToNode(CANode toNode) {

		// test if node is free, else do not move agent to node
		if (toNode.getAgent() == null) {
			CAAgent agent = this.caServer.moveAgentFromWaitingQueueToNode(toNode);
			if (agent != null) {
				toNode.setAgent(agent);
				return true;
			}
		}
		return false;
	}
	
//      function updateParkings(this)
//         this.caServer.updateParkings(this.currentTime, this.infrastructure); 
//      end  
	public void updateParkings() {
		this.caServer.updateParkings(this.currentTime, this.network);
	}
//      function updateAgents(this)
//          for agent = this.population
//              agent.isSearching(this.currentTime);
//          end
//      end
	public void updateAgents() {
		for (CAAgent agent : this.agents.values()) {
			agent.isSearching(this.currentTime);
		}
	}
	
//      function updateRemainingWaitingQueues(this) % some of the node waiting queues are updated during link update, the rest (with empty links) is updated here
//          randomIndices = randperm(this.caServer.getNodeWaitingQueueLength()); % randomize key set
//          for i=1:length(randomIndices)    
//              key = this.caServer.getWaitingQueuesKey(randomIndices(i));
//              node = this.infrastructure.getNodeById(key);
//              this.moveAgentInWaitingQueueToNode(node);
//          end
//          this.caServer.finishWaitingQueues();
//      end
	
	// some of the node waiting queues are updated during link update, the rest (with empty links) is updated here
	public void updateRemainingWaitingQueues() {
		// TODO: this is not random anymore
		for (Id nodeId : this.caServer.getWaitingQueuesKey()) {
			CANode toNode = (CANode) this.network.getNetsimNode(nodeId);
			this.moveAgentInWaitingQueueToNode(toNode);
		}
		this.caServer.finishWaitingQueues();
	}
	
	private static class DepartureTimeComparator implements Comparator<CAAgent> {

		@Override
		public int compare(CAAgent arg0, CAAgent arg1) {
			int cmp = Double.compare(arg0.getMobsimAgent().getActivityEndTime(), arg1.getMobsimAgent().getActivityEndTime());
			if (cmp == 0) {
				return arg1.getId().compareTo(arg0.getId());
			}
			return cmp;
		}
		
	}

	@Override
	public void doSimStep(double time) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPrepareSim() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterSim() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}
}
