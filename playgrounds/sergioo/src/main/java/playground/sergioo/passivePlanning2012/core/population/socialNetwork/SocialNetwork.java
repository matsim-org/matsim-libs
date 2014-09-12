package playground.sergioo.passivePlanning2012.core.population.socialNetwork;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.api.core.v01.Id;

public class SocialNetwork {
	
	private Map<Id, Map<Id, Set<String>>> network = new HashMap<Id, Map<Id, Set<String>>>();
	private String description;
	
	public Map<Id, Map<Id, Set<String>>> getNetwork() {
		return network;
	}
	public void relate(Id egoId, Id alterId) {
		Map<Id, Set<String>> relations = network.get(egoId);
		if(relations == null) {
			relations = new HashMap<Id, Set<String>>();
			network.put(egoId, relations);
		}
		Set<String> alter = relations.get(alterId);
		if(alter == null) {
			alter = new HashSet<String>();
			relations.put(alterId, alter);
		}
	}
	public void relate(Id egoId, Id alterId, String type) {
		Map<Id, Set<String>> relations = network.get(egoId);
		if(relations == null) {
			relations = new HashMap<Id, Set<String>>();
			network.put(egoId, relations);
		}
		Set<String> alter = relations.get(alterId);
		if(alter == null) {
			alter = new HashSet<String>();
			relations.put(alterId, alter);
		}
		alter.add(type);
	}
	public void relate(Id egoId, Id alterId, Set<String> types) {
		Map<Id, Set<String>> relations = network.get(egoId);
		if(relations == null) {
			relations = new HashMap<Id, Set<String>>();
			network.put(egoId, relations);
		}
		Set<String> alter = relations.get(alterId);
		if(alter == null) {
			alter = new HashSet<String>();
			relations.put(alterId, alter);
		}
		alter.addAll(types);
	}
	public boolean areRelated(Id egoId, Id alterId) {
		if(network.get(egoId)!=null && network.get(egoId).get(alterId)!=null)
			return true;
		return false;
	}
	public Set<String> getRelationTypes(Id egoId, Id alterId) {
		if(areRelated(egoId, alterId))
			return network.get(egoId).get(alterId);
		return null;
	}
	public Map<Id, Set<String>> getAlters(Id egoId) {
		return network.get(egoId);
	}
	public Set<Id> getAlterIds(Id egoId) {
		Map<Id, Set<String>> map = network.get(egoId);
		return map==null?new HashSet<Id>():map.keySet();
	}
	public Map<Id, Set<String>> getEgos(Id alterId) {
		Map<Id, Set<String>> relationsMap = new HashMap<Id, Set<String>>();
		for(Entry<Id, Map<Id, Set<String>>> relations:network.entrySet()) {
			Set<String> savedRelation = relations.getValue().get(alterId);
			if(savedRelation != null) {
				Set<String> relation = new HashSet<String>(); 
				relationsMap.put(relations.getKey(), relation);
				relation.addAll(savedRelation);
			}
		}
		return relationsMap;
	}
	public Set<Id> getEgoIds(Id alterId) {
		Set<Id> egoIds = new HashSet<Id>();
		for(Entry<Id, Map<Id, Set<String>>> relations:network.entrySet())
			if(relations.getValue().containsKey(alterId))
				egoIds.add(relations.getKey());
		return getEgoIds(alterId);
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getDescription() {
		return description;
	}

}
