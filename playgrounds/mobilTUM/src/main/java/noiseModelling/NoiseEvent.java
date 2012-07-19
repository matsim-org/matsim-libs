package noiseModelling;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;



public interface NoiseEvent extends Event {
	
	public final static String EVENT_TYPE = "NoiseEvent";
	public final static String ATTRIBUTE_LINK_ID = "linkId";
	public Id getLinkId();
	//public Map<Double , Double> getL_mE();
	

}
