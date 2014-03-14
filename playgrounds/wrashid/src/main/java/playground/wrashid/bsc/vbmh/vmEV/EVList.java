package playground.wrashid.bsc.vbmh.vmEV;

import java.util.HashMap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class EVList {
	private HashMap <String, EV> ownerMap = new HashMap<String, EV>();
	private String testvar = "test";
	
	public EVList(){
		
	}
	
	public void addEV(EV ev){
		ownerMap.put(ev.getOwnerPersonId(), ev);	
	}

	//@XmlElement(name = "EV")
	public HashMap<String, EV> getOwnerMap() {
		return ownerMap;
	}

	public void setOwnerMap(HashMap<String, EV> ownerMap) {
		this.ownerMap = ownerMap;
	}

	@XmlElement(name = "test")
	public String getTestvar() {
		return testvar;
	}
	
	

}
