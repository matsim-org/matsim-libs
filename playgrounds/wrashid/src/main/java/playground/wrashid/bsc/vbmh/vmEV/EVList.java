package playground.wrashid.bsc.vbmh.vmEV;

import java.util.HashMap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class EVList {
	private HashMap <String, EV> ownerMap = new HashMap<String, EV>();
	
	public void addEV(EV ev){
		ownerMap.put(ev.ownerPersonId, ev);	
	}

	@XmlElement(name = "EV")
	public HashMap<String, EV> getOwnerMap() {
		return ownerMap;
	}

	public void setOwnerMap(HashMap<String, EV> ownerMap) {
		this.ownerMap = ownerMap;
	}
	
	

}
