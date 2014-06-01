package playground.vbmh.vmEV;

import java.util.HashMap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.matsim.api.core.v01.Id;

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


	public boolean hasEV(Id personID){
		return this.ownerMap.containsKey(personID.toString());
	}
	
	
	public EV getEV(Id personID){
		EV ev = this.ownerMap.get(personID.toString()); 
		
		if (ev == null){ System.out.println("Something went wrong");}
		
		return ev;
	}
	
	public void setAllStateOfChargePercentage(double percentage){
		for(EV ev : ownerMap.values()){
			ev.setStateOfChargePercentage(percentage);
		}
	}
	
	

}
