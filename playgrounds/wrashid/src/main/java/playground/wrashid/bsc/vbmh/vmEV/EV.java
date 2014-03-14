package playground.wrashid.bsc.vbmh.vmEV;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
public class EV {
	
	private String id;
	private String ownerPersonId;
	public double stateOfCharge; //Absolut in KWh 
	public double batteryCapacity;
	
	@XmlAttribute
	public String getId() {
		return id;
	}
	
	
	public void setId(String id) {
		this.id = id;
	}

	@XmlElement (name="OwnerId")
	public String getOwnerPersonId() {
		return ownerPersonId;
	}


	public void setOwnerPersonId(String ownerPersonId) {
		this.ownerPersonId = ownerPersonId;
	}
	

}
