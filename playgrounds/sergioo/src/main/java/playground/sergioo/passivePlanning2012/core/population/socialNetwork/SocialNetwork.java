package playground.sergioo.passivePlanning2012.core.population.socialNetwork;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class SocialNetwork {
	
	private Map<Id<Person>, Map<Id<Person>, Set<String>>> network = new HashMap<Id<Person>, Map<Id<Person>, Set<String>>>();
	private String description;
	
	public Map<Id<Person>, Map<Id<Person>, Set<String>>> getNetwork() {
		return network;
	}
	public void relate(Id<Person> egoId, Id<Person> alterId) {
		Map<Id<Person>, Set<String>> relations = network.get(egoId);
		if(relations == null) {
			relations = new HashMap<Id<Person>, Set<String>>();
			network.put(egoId, relations);
		}
		Set<String> alter = relations.get(alterId);
		if(alter == null) {
			alter = new HashSet<String>();
			relations.put(alterId, alter);
		}
	}
	public void relate(Id<Person> egoId, Id<Person> alterId, String type) {
		Map<Id<Person>, Set<String>> relations = network.get(egoId);
		if(relations == null) {
			relations = new HashMap<Id<Person>, Set<String>>();
			network.put(egoId, relations);
		}
		Set<String> alter = relations.get(alterId);
		if(alter == null) {
			alter = new HashSet<String>();
			relations.put(alterId, alter);
		}
		alter.add(type);
	}
	public void relate(Id<Person> egoId, Id<Person> alterId, Set<String> types) {
		Map<Id<Person>, Set<String>> relations = network.get(egoId);
		if(relations == null) {
			relations = new HashMap<Id<Person>, Set<String>>();
			network.put(egoId, relations);
		}
		Set<String> alter = relations.get(alterId);
		if(alter == null) {
			alter = new HashSet<String>();
			relations.put(alterId, alter);
		}
		alter.addAll(types);
	}
	public boolean areRelated(Id<Person> egoId, Id<Person> alterId) {
		if(network.get(egoId)!=null && network.get(egoId).get(alterId)!=null)
			return true;
		return false;
	}
	public Set<String> getRelationTypes(Id<Person> egoId, Id<Person> alterId) {
		if(areRelated(egoId, alterId))
			return network.get(egoId).get(alterId);
		return null;
	}
	public Map<Id<Person>, Set<String>> getAlters(Id<Person> egoId) {
		return network.get(egoId);
	}
	public Set<Id<Person>> getAlterIds(Id<Person> egoId) {
		Map<Id<Person>, Set<String>> map = network.get(egoId);
		return map==null?new HashSet<Id<Person>>():map.keySet();
	}
	public Map<Id<Person>, Set<String>> getEgos(Id<Person> alterId) {
		Map<Id<Person>, Set<String>> relationsMap = new HashMap<Id<Person>, Set<String>>();
		for(Entry<Id<Person>, Map<Id<Person>, Set<String>>> relations:network.entrySet()) {
			Set<String> savedRelation = relations.getValue().get(alterId);
			if(savedRelation != null) {
				Set<String> relation = new HashSet<String>(); 
				relationsMap.put(relations.getKey(), relation);
				relation.addAll(savedRelation);
			}
		}
		return relationsMap;
	}
	public Set<Id<Person>> getEgoIds(Id<Person> alterId) {
		Set<Id<Person>> egoIds = new HashSet<Id<Person>>();
		for(Entry<Id<Person>, Map<Id<Person>, Set<String>>> relations:network.entrySet())
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
