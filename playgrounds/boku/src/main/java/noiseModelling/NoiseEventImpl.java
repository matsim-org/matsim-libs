package noiseModelling;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;

public class NoiseEventImpl extends Event {
	

	public final static String EVENT_TYPE = "NoiseEvent";
	public final static String ATTRIBUTE_LINK_ID = "linkId";
	private final Id linkId;
	//private final Map<Double , Double> L_mE;
	private final Double L_DEN ;

	public NoiseEventImpl(double time, Id linkId, Double L_DEN) {
		super(time);
		this.linkId = linkId ;
		//this.L_mE = L_mE ;		
		this.L_DEN = L_DEN ;
	}

	public Id getLinkId() {
		return linkId ;
	}

	public Double getL_DEN (){
		return L_DEN;
	}

	@Override
	public String getEventType() {
		// TODO Auto-generated method stub
		return EVENT_TYPE;
	}

	public Map<String, String> getAttributes(){
		Map<String, String> attributes = super.getAttributes();
		attributes.put(ATTRIBUTE_LINK_ID , this.linkId.toString());
		/*for(Entry<String, Double> entry : L_mE.entrySet()){
			String timestamp = entry.getKey();
			Double value = entry.getValue();
			attributes.put("lmE_"+timestamp, value.toString());
		}*/
		attributes.put("l_DEN", L_DEN.toString());
		return attributes;
	}

}