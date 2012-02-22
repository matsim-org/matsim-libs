package playground.anhorni.surprice;

import java.util.TreeMap;

import org.matsim.api.core.v01.Id;

public class AgentMemories {
	
	private TreeMap<Id, AgentMemory> memories = new TreeMap<Id, AgentMemory>();
	
	public AgentMemory getMemory(Id agentId) {
		return this.memories.get(agentId);
	}
	
	public void addMemory(Id agentId, AgentMemory memory) {
		this.memories.put(agentId, memory);
	}
}
