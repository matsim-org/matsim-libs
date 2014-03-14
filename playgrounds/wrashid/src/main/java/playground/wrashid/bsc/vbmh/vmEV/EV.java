package playground.wrashid.bsc.vbmh.vmEV;

import javax.xml.bind.annotation.XmlAttribute;

public class EV {
	public  @XmlAttribute String id;
	public String ownerPersonId;
	public double stateOfCharge; //Absolut in KWh 
	public double batteryCapacity;
	

}
