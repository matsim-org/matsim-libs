package playground.wrashid.lib.obj.plan;

import java.util.Collection;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import playground.wrashid.lib.obj.LinkedListValueHashMap;
import playground.wrashid.lib.obj.TwoHashMapsConcatenated;

/**
 * Container for selecting a subset of people and assigning a label to them.
 * @author wrashid
 *
 */
public class PersonGroups {

	// key: groupLabel, Id: personId
	LinkedListValueHashMap<String, Id> labelPersonMapping=new LinkedListValueHashMap<String, Id>();
	// key1: groupLabel, key2: attributeName, value: whatever value you want to store
	TwoHashMapsConcatenated<String, String, Object> concatenatedHashMap=new TwoHashMapsConcatenated<String, String, Object>();
	
	public int getGroupSize(String groupLabel){
		return labelPersonMapping.get(groupLabel).size();
	}
	
	public Collection<String> getGroupLabels(){
		return labelPersonMapping.getKeySet();
	}
	
	public int getNumberOfGroups(){
		return labelPersonMapping.size();
	}
	
	public void addPersonToGroup(String groupLabel, Id personId){
		labelPersonMapping.putAndSetBackPointer(groupLabel, personId);
	}
	
	public LinkedList<Id> getPersonsInGroup(String groupLabel){
		return labelPersonMapping.get(groupLabel);
	}
	
	public void setAttributeValueToGroup(String groupLabel, String attribute, Object value){
		checkIfGroupLabelDefined(groupLabel);
		
		concatenatedHashMap.put(groupLabel, attribute, value);
	}
	
	public Object getAttributeValueForGroup(String groupLabel, String attribute){
		return concatenatedHashMap.get(groupLabel, attribute);
	}
	
	public void setAttributeValueForGroupToWhichThePersonBelongs(Id personId, String attribute, Object value){
		String groupLabel=labelPersonMapping.getKey(personId);
		
		if (groupLabel==null){
			// this means, that the person belongs to no group and therefore no attribute needs to be set.
			return;
		}
		
		setAttributeValueToGroup(groupLabel,attribute,value);
	}
	
	public Object getAttributeValueForGroupToWhichThePersonBelongs(Id personId, String attribute){
		String groupLabel=labelPersonMapping.getKey(personId);
		return concatenatedHashMap.get(groupLabel, attribute);
	}
	
	
	private void checkIfGroupLabelDefined(String groupLabel){
		if (labelPersonMapping.get(groupLabel)==null){
			throw new Error("there is no such groupLabel defined");
		}
	}
	
}
