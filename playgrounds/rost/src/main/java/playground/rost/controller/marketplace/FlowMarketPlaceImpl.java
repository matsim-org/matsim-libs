package playground.rost.controller.marketplace;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import playground.rost.eaflow.ea_flow.Flow;
import playground.rost.eaflow.ea_flow.GlobalFlowCalculationSettings;

public class FlowMarketPlaceImpl implements MarketPlace<Flow>{

	protected static Map<String, Flow> flowMap = new HashMap<String, Flow>();
	
	protected static String generateId(String prefix)
	{
		Date date = new Date(System.currentTimeMillis());
		return prefix + "_" + date.toGMTString();
	}

	public String addElement(Flow element) {
		String id = "Flow" + GlobalFlowCalculationSettings.edgeTypeToString(element.getEdgeType());
		id = generateId(id);
		if(flowMap.containsKey(id))
			throw new RuntimeException("bad id");
		flowMap.put(id, element);
		return id;
	}

	public boolean removeElement(String id) {
		if(flowMap.containsKey(id))
		{
			flowMap.remove(id);
			return true;
		}
		return false;
	}

	public Flow getElement(String id) {
		return flowMap.get(id);
	}

	public Collection<String> getIds() {
		return flowMap.keySet();
	}
	
}
