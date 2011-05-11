package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.Controler;

import playground.wrashid.PSF.data.HubLinkMapping;

public class StellasHubMapping extends MappingClass{
	
	private double minX=Integer.MAX_VALUE;
	private double maxX=Integer.MIN_VALUE;
	private double minY=Integer.MAX_VALUE;
	private double  maxY=Integer.MIN_VALUE;
	
	final private int divHubsX=2;
	final private int divHubsY=2;
	
	public StellasHubMapping(){
	
	}
	
	/**
	 * fill hubLinkMapping 
	 * assign hubIds to the different Links--> hubLinkMapping.addMapping(link, hub)
	 * 
	 * finds minX-maxX, minY-maxY and splits the area into rectangular fields
	 * with xDiv divisions in x direction and yDiv divisions in y direction
	 * 
	 * it then assigns hubs to each link accordingly
	 * 
	 * @param deterministicHubLoadDistribution
	 * @param hubLinkMapping
	 * @param controler
	 */
	@Override
	public HubLinkMapping mapHubs(Controler controler){
		HubLinkMapping hubLinkMapping=new HubLinkMapping(4);
		
		findMinMaxXYNetwork(controler);
		
		for (Link link:controler.getNetwork().getLinks().values()){
			double xDiv= (maxX-minX)/divHubsX;
			double yDiv= (maxY-minY)/divHubsY;
			
			double coordX=link.getCoord().getX();
			coordX=coordX-minX; // relative position
			double coordY=link.getCoord().getY();
			coordY=coordY-minY; // relative position
			
			int hubNoX=(int)Math.ceil(coordX/xDiv);
			int hubNoY=(int)Math.ceil(coordY/yDiv);
			
			int hubNumber= hubNoX + (hubNoY-1)*hubNoX;
			/*
			 * example 	divX=2
			 * 			divY=2
			 * minX -------------------------->  maxX
			 *minY				]
			 *]		1			]		2
			 *]					]
			 *]--------------------------------
			 *]					]
			 *]		3			]		4
			 *]					]
			 *maxY
			 */
			
			
			hubLinkMapping.addMapping(link.getId().toString(), hubNumber);
						
		}
		return hubLinkMapping;
	}	
	
	
	
	public void findMinMaxXYNetwork(Controler controler){
		
		for (Link link:controler.getNetwork().getLinks().values()){
			minX= Math.min(minX, link.getCoord().getX()); 
			maxX= Math.max(maxX,link.getCoord().getX());
			
			minY= Math.min(minY, link.getCoord().getY()); 
			maxY= Math.max(maxY,link.getCoord().getY());
		}
	}
	
	
	
	
}
