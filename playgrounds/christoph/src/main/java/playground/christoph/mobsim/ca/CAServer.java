/* *********************************************************************** *
 * project: org.matsim.*
 * CAServer.java
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimNode;

public class CAServer {
//properties (Access = private)
//    % occupied data structures: pointers to the infrastructure for efficient handling -----
//    % do not iterate over link cells but over agents for efficiency reasons
//    occupiedLinkCells_Queue =  containers.Map();  %id = link id     
//    occupiedNodes = {}; % test if faster than NNode.empty(0,0); % iterate over occupied nodes only
//    occupiedParkingLots = ParkingLot.empty(0,0); % iterate over occupied parking lots only       
//    waitingForNodes_Queue = containers.Map(); % id = node id
//    
//    removeNodeIndices = [];
//    removeAgentsFromLinkQueue = [];
//    removeKeys = {};
//end
	private final List<CANode> occupiedNodes = new LinkedList<CANode>();
	private final Set<Id> activeLinks = new LinkedHashSet<Id>();
	private final Map<Id, IQueue> waitingForNodes_Queue = new HashMap<Id, IQueue>();
	
	private final Set<Id> removeKeys = new HashSet<Id>();
	private final List<CAParkingLot> occupiedParkingLots = new ArrayList<CAParkingLot>();	// TODO: change type to Set
	private final List<CANode> nodesToRemove = new ArrayList<CANode>();
	private final List<CAAgent> removeAgentsFromLinkQueue = new ArrayList<CAAgent>();
	
//methods
//  function this = CAServer()
//  end
//    
//  function init(this, infrastructure)      
//     nodes = infrastructure.getNodes();
//     keys = nodes.keys();
//     for i = 1:numel(keys) % could also use nodes.Count here
//        key = keys{i};
//        node = nodes(key);
//        if (node.hasAgent())
//            this.addOccupiedNode(node); %this.occupiedNodes{end + 1} = node;
//        end
//     end
//  end % function
	public void init(CANetwork network) {
		
		for (NetsimNode node : network.getNetsimNodes().values()) {
			CANode caNode = (CANode) node;
			if (caNode.hasAgent()) {
				this.addOccupiedNode(caNode);
			}
		}
	}
	
//  function enterLink(this, link, agent)
//      if (~isKey(this.occupiedLinkCells_Queue, link.getId())) 
//         this.occupiedLinkCells_Queue(link.getId()) = IQueue(Agent.empty(0,0)); 
//      end
//      linkagentqueue = this.occupiedLinkCells_Queue(link.getId());
//      linkagentqueue.addElement(agent);
//      this.leaveNode(link.getFromNode());
//  end
	public void enterLink(CALink link, CANode fromNode, CAAgent agent) {
		// ensure that link is active
		this.activeLinks.add(link.getId());
//		this.leaveNode(fromNode);
	}
	
//  function leaveNode(this, node)
//      removed = false;
//      for idx = 1 : length(this.occupiedNodes)
//          if this.occupiedNodes{idx} == node
//              removed = true;
//            this.removeNodeIndices = [idx this.removeNodeIndices];
//          end  
//      end
//      %DEBUG
//      if ~removed
//          error = 'leaveNode, node not found';
//          disp(error);
//      end
//  end
	public void leaveNode(CANode node) {
		
		boolean removed = this.occupiedNodes.remove(node);
		if (!removed) System.out.println("leaveNode, node not found");
	}
	
//  function finishNodes(this)
//      this.occupiedNodes(this.removeNodeIndices) = []; % TODO: is this correct?
//      this.removeNodeIndices = [];
//  end
	public void finishNodes() {
		this.occupiedNodes.removeAll(this.nodesToRemove);
		this.nodesToRemove.clear();
	}
	
//  function [nodes] = getOccupiedNodes(this) 
//      nodes = this.occupiedNodes;
//  end 
	public List<CANode> getOccupiedNodes() {
		return this.occupiedNodes;
	}
	
//  function [node] = getOccupiedNode(this, index)
//      node = this.occupiedNodes{index};
//  end
	public CANode getOccupiedNode(int index) {
		return this.occupiedNodes.get(index);
	}
	
//  function [key] = getLinkQueuesKey(this, index)
//      keys = this.occupiedLinkCells_Queue.keys();
//      key = keys{index};
//  end
	public Set<Id> getActiveLinkIds() {
		return this.activeLinks;
	}
	
//  function [s] = getLinkQueuesSize(this)
//      s = this.occupiedLinkCells_Queue.Count;
//  end
	public int getLinkQueuesSize() {
		return this.activeLinks.size();
	}
	
//  function [s] = getOccupiedNodesSize(this)
//        s = length(this.occupiedNodes);
//  end
	public int getOccupiedNodesSize() {
		return this.occupiedNodes.size();
	}
	
//  function [queue] = getLinkAgentQueue(this, key)
//      queue = this.occupiedLinkCells_Queue(key);
//  end
//	public IQueue getLinkAgentQueue(Id linkId) {
//		return this.occupiedLinkCells_Queue.get(linkId);
//	}
	
//  function [queue] = getNodeAgentQueue(this, key)
//      queue = this.waitingForNodes_Queue(key);
//  end
	public IQueue getNodeAgentQueue(Id nodeId) {
		return this.waitingForNodes_Queue.get(nodeId);
	}
	
//  function [l] = getNodeWaitingQueueLength(this)
//      l = this.waitingForNodes_Queue.Count;
//  end
	public int getNodeWaitingQueueLength() {
		return this.waitingForNodes_Queue.size();
	}
	
//  function [key] = getWaitingQueuesKey(this, index)
//      keys = this.waitingForNodes_Queue.keys();
//      key = keys{index};
//  end
	public Set<Id> getWaitingQueuesKey() {
		return this.waitingForNodes_Queue.keySet();
	}
	
//  function finishLinks(this, infrastructure)
//      removeKeyIndices = [];
//      keys = this.occupiedLinkCells_Queue.keys();
//      for i = 1:length(keys)
//          link = infrastructure.getLinkById(keys{i});
//          if (link.getNumberOfAgentsOnLinkOrInParkings() == 0)
//                removeKeyIndices = [i removeKeyIndices];
//          end
//      end
//      for i = 1:length(removeKeyIndices)
//        remove(this.occupiedLinkCells_Queue, keys{removeKeyIndices(i)});
//      end
//      removeKeyIndices = [];
//  end
	// de-active links without agents present 
	public void finishLinks(CANetwork network) {
		Iterator<Id> iter = this.activeLinks.iterator();
		while (iter.hasNext()) {
			Id linkId = iter.next();
			CALink link = (CALink) network.getNetsimLink(linkId);
			if (link.getNumberOfAgentsOnLink() == 0) iter.remove();
		}
	}
	
//  function updateParkings(this, currentTime, infrastructure)
//      % check if an agent wants to leave parking.
//      % TODO: this should be made faster somehow. Not yet an idea how.
//      % Maybe agents should notify observer, when they are ready to
//      % leave. -> pushing instead of polling. -> event-based instead of
//      % time-teps based
//       removedAgents = Agent.empty(0,0);
//       emptyParkingLotsIndices = [];
//       for i=1:length(this.occupiedParkingLots)
//           parkingLot = this.occupiedParkingLots(i);
//           removedAgents = [parkingLot.handleAllAgents(currentTime) removedAgents]; % TODO: not nice to have this call here, as it changes the infrastructure!
//           if (parkingLot.isEmpty()) 
//               index = find(this.occupiedParkingLots == parkingLot);
//               emptyParkingLotsIndices = [i emptyParkingLotsIndices];                                    
//           end
//       end
//       this.occupiedParkingLots(emptyParkingLotsIndices) = [];
//        
//       for i = 1:length(removedAgents) % add agents that left parking lot to the waiting queue of their destination node
//           agent = removedAgents(i);
//           this.addAgentToWaitungQueue(agent.getLeaveParkingNodeId(), agent);
//       end
//  end   
	public void updateParkings(double currentTime, CANetwork network) {
		
		List<CAAgent> removedAgents = new ArrayList<CAAgent>();
		Iterator<CAParkingLot> iter = this.occupiedParkingLots.iterator();
		while (iter.hasNext()) {
			CAParkingLot parkingLot = iter.next();
			List<CAAgent> handledAgents = parkingLot.handleAllAgents(currentTime);
			removedAgents.addAll(handledAgents);

			if (parkingLot.isEmpty()) iter.remove();
		}
		
		for (CAAgent agent : removedAgents) this.addAgentToWaitingQueue(agent.getLeaveParkingNodeId(), agent);
	}
	
//  function parkAgent(this, key, parkingLot)
//      if (isempty(find(this.occupiedParkingLots == parkingLot))) % parking lot is not yet in ca datastructure 
//        this.occupiedParkingLots(end + 1) = parkingLot;                         
//      end 
//      linkagentqueue = this.getLinkAgentQueue(key);          
//      this.removeAgentsFromLinkQueue = [linkagentqueue.getCurrentIndex() + 1 this.removeAgentsFromLinkQueue];  
//  end      
	public void parkAgent(CAAgent agent, CAParkingLot parkingLot) {
		if (!this.occupiedParkingLots.contains(parkingLot)) {
			this.occupiedParkingLots.add(parkingLot);
		}
		this.removeAgentsFromLinkQueue.add(agent);
	}
	
//  function moveAgentFromLinkToNode(this, key, toNode)
//    linkagentqueue = this.getLinkAgentQueue(key);
//    this.removeAgentsFromLinkQueue = [linkagentqueue.getLastIndex() this.removeAgentsFromLinkQueue];
//    this.addOccupiedNode(toNode); %this.occupiedNodes{end + 1} = toNode; % node cannot be occupied        
//  end
	public void moveAgentFromLinkToNode(Id linkId, CANode toNode) {
//		IQueue queue = this.getLinkAgentQueue(linkId);
//		this.removeAgentsFromLinkQueue.add(queue.getLastIndex());
		this.addOccupiedNode(toNode);
	}
	
//  function finishLinkQueue(this, key)
//     linkagentqueue = this.getLinkAgentQueue(key);
//     linkagentqueue.removeElements(this.removeAgentsFromLinkQueue);
//     this.removeAgentsFromLinkQueue = [];
//  end
	public void finishLinkQueue(Id linkId) {
		// nothing to do here anymore ??
//		IQueue queue = this.getLinkAgentQueue(linkId);
//		for (CAAgent agent : this.removeAgentsFromLinkQueue) queue.removeElement(agent);
//		this.removeAgentsFromLinkQueue.clear();
	}
	
//  function [agent] = moveAgentFromWaitingQueueToNode(this, node)
//      agent = [];
//       if (isKey(this.waitingForNodes_Queue, node.getId()))
//           queue = this.waitingForNodes_Queue(node.getId());
//           agent = queue.getLastElement();
//           this.addOccupiedNode(node); %this.occupiedNodes{end + 1} = node; % node was empty before
//           queue.removeLastElement();
//           if (~queue.hasElements())  % if waiting queue is now empty remove the queue from the waiting queues in finishWaitingQueues()
//               this.removeKeys{end + 1} = node.getId();
//           end
//       end
//  end 
	public CAAgent moveAgentFromWaitingQueueToNode(CANode node) {
		
		CAAgent agent = null;
		
		IQueue queue = this.waitingForNodes_Queue.get(node.getId());
		if (queue != null) {
			agent = queue.removeFirstElement();
			this.addOccupiedNode(node);
			
			// if waiting queue is now empty remove the queue from the waiting queues in finishWaitingQueues()
			if (!queue.hasElements()) {
				this.removeKeys.add(node.getId());
			}
		}
		
		
		return agent;
	}
	
//  function finishWaitingQueues(this)
//      if (~isempty(this.removeKeys))
//        remove(this.waitingForNodes_Queue, this.removeKeys); 
//        this.removeKeys = {};
//      end
//  end
	public void finishWaitingQueues() {
		for (Id id : this.removeKeys) {
			this.waitingForNodes_Queue.remove(id);
		}
		this.removeKeys.clear();
	}
	
//  function addAgentToWaitungQueue(this, nodeId, agent)       
//      if (~isKey(this.waitingForNodes_Queue, nodeId))
//          this.waitingForNodes_Queue(nodeId) = IQueue(Agent.empty(0,0));
//      end
//      queue = this.waitingForNodes_Queue(nodeId);
//      queue.addElement(agent);         
//  end
	public void addAgentToWaitingQueue(Id nodeId, CAAgent agent) {
		IQueue queue = this.waitingForNodes_Queue.get(nodeId);
		if (queue == null) {
			queue = new IQueue(new LinkedList<CAAgent>());
			this.waitingForNodes_Queue.put(nodeId, queue);
		}
		queue.addElement(agent);
	}

//methods (Access = private)      
//    function addOccupiedNode(this, node)
//        %disp('DEBUG add node')
//        this.occupiedNodes{end+1} = node;
//    end
	private void addOccupiedNode(CANode node) {
		this.occupiedNodes.add(node);
	}
	
}
