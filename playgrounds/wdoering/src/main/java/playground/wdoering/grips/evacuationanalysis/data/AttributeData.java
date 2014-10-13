package playground.wdoering.grips.evacuationanalysis.data;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;

public class AttributeData<T> {

	private HashMap<Id, T> attributeData;
	
	public AttributeData() {
		this.attributeData = new HashMap<Id, T>();
	}
	
	public HashMap<Id, T> getAttributeData() {
		return attributeData;
	}
	
	public void setAttributeData(HashMap<Id, T> attributeData) {
		this.attributeData = attributeData;
	}

	public void setAttribute(Id id, T data) {
		attributeData.put(id, data);
	}
	
	public T getAttribute(Id id)
	{
		return attributeData.get(id);
	}
	
	
}
