package playground.wrashid.sschieffer.DecentralizedSmartCharger.DSC;

import org.matsim.core.controler.Controler;

import playground.wrashid.PSF.data.HubLinkMapping;

/**
 * abstract class to define Mapping of Links to Hubs for simulation scenario
 * every child should override the method :
 * public HubLinkMapping mapHubs(Controler)
 * 
 * 
 * @author Stella
 *
 */
public abstract class MappingClass {

	
	public MappingClass() {
		
	}
	
	/**
	 * method to map the linkIds to Hubs for scenario
	 * @param controler
	 * @return returns a HubLinkMapping
	 */
	public HubLinkMapping mapHubs(Controler controler){
		HubLinkMapping hubLinkMapping=new HubLinkMapping(1);
		return hubLinkMapping;
	}

}
