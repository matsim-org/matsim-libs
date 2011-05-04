package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.Controler;

import playground.wrashid.PSF.data.HubLinkMapping;

public class StellasHubMapping {

	private HubLinkMapping hubLinkMapping;
	private Controler controler;
	
	public StellasHubMapping(Controler controler){
		this.controler=controler;
		hubLinkMapping=new HubLinkMapping(4);
	}
	
	/**
	 * fill hubLinkMapping 
	 * assign hubIds to the different Links--> hubLinkMapping.addMapping(link, hub)
	 * according to scenario relevant hublocations
	 * 
	 * @param deterministicHubLoadDistribution
	 * @param hubLinkMapping
	 * @param controler
	 */
	public HubLinkMapping mapHubs(){
		
		
		double maxX=5000;
		double minX=-20000;
		double diff= maxX-minX;
		
		for (Link link:controler.getNetwork().getLinks().values()){
			// x values of equil from -20000 up to 5000
			if (link.getCoord().getX()<(minX+diff)/4){
				
				hubLinkMapping.addMapping(link.getId().toString(), 1);
			}else{
				if (link.getCoord().getX()<(minX+diff)*2/4){
					hubLinkMapping.addMapping(link.getId().toString(), 2);
				}else{
					if (link.getCoord().getX()<(minX+diff)*3/4){
						hubLinkMapping.addMapping(link.getId().toString(), 3);
					}else{
						hubLinkMapping.addMapping(link.getId().toString(), 4);
					}
				}
			}
			
		}
		return hubLinkMapping;
	}	
	
	
}
