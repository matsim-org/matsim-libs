package playground.wrashid.sschieffer.SetUp.NetworkTopology;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.Controler;

import playground.wrashid.PSF.data.HubLinkMapping;


/**
 * you can specify number of hubs in X and Y direction and then the network 
 * automatically divides the links into sections 
 * 
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
			 
 * @author Stella
 *
 */
public class StellasHubMapping extends MappingClass{
	
	private double minX=Integer.MAX_VALUE;
	private double maxX=Integer.MIN_VALUE;
	private double minY=Integer.MAX_VALUE;
	private double  maxY=Integer.MIN_VALUE;
	
	private int divHubsX;
	private int divHubsY;
	
	public StellasHubMapping(int divHubsX, int divHubsY){
		this.divHubsX=divHubsX;
		this.divHubsY=divHubsY;
		
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
			
			int hubNoX=Math.max(1,(int)Math.ceil(coordX/xDiv));
			int hubNoY=Math.max(1,(int)Math.ceil(coordY/yDiv));
			
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
			//System.out.println("assigned link "+link.getId().toString()+ "to hub "+ hubNumber);			
		}
		return hubLinkMapping;
	}	
	
	
	
	public void findMinMaxXYNetwork(Controler controler){
		System.out.println("find Max and Min XY in Network in Stellas Hub Mapping");
		for (Link link:controler.getNetwork().getLinks().values()){
			
			double startNodeX=link.getFromNode().getCoord().getX();
			double startNodeY=link.getFromNode().getCoord().getY();
			double endNodeX=link.getToNode().getCoord().getX();
			double endNodeY=link.getToNode().getCoord().getY();
			
			//System.out.println("From ("+startNodeX+", "+startNodeY+") to ("+ endNodeX+", "+endNodeY+")");
			
			minX= Math.min(minX, startNodeX); 
			minX= Math.min(minX, endNodeX);
			
			maxX= Math.max(maxX, startNodeX); 
			maxX= Math.max(maxX, endNodeX);
			
			minY= Math.min(minY, startNodeY); 
			minY= Math.min(minY, startNodeY);
			
			maxY= Math.max(maxY,startNodeY);
			maxY= Math.max(maxY,endNodeY);
		}
		//System.out.println("final X: "+ minX + ", " + maxX);
		//System.out.println("final Y: "+ minY + ", " + maxY);
	}
	
	
	
	
}
