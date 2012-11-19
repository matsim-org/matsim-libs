package playground.wdoering.grips.evacuationanalysis.data;

import java.awt.Color;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

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

	public void setAttribute(IdImpl id, T data) {
		attributeData.put(id, data);
	}
	
	public T getAttribute(IdImpl id)
	{
		return attributeData.get(id);
	}
	
	
}
