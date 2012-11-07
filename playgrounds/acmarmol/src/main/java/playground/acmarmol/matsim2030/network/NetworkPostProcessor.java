package playground.acmarmol.matsim2030.network;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;


public class NetworkPostProcessor {

	private Network network;
	private final static Logger log = Logger.getLogger(NetworkPostProcessor.class);
	private LinkedList<Id> unresolved_capacity_links;
	private LinkedList<Id> unresolved_freespeed_links;
	private LinkedList<Id> unresolved_length_links;
	
	public NetworkPostProcessor(Network network){
		this.network = network;
		this.unresolved_capacity_links = new LinkedList<Id>();
		this.unresolved_freespeed_links = new LinkedList<Id>();
		this.unresolved_length_links = new LinkedList<Id>();
	}
	
	
	public void Process(){
		log.info("Starting processing...");
		
		System.out.println("Initial network status");
		
		printNetworkStatus();
		replaceWithReturnLinkAttributes();
		calculateMissingLengths(unresolved_length_links);
		
		System.out.println("Final network status");
		printNetworkStatus();
		log.info("...done");
	}


	private void calculateMissingLengths(LinkedList<Id> link_ids) {
		
		for(Id link_id : link_ids){
			
			Link link = network.getLinks().get(link_id);
			Node fromNode = link.getFromNode();
			Node toNode = link.getToNode();
			
			int length = (int) Math.sqrt(Math.pow((fromNode.getCoord().getX()-toNode.getCoord().getX()),2)+Math.pow(fromNode.getCoord().getY()-toNode.getCoord().getY(),2));
			link.setLength(length);
			
			
		}
		
		
	}


//	private void replaceWithSimilarAdjacentLink() {
//		this.replaceCapacityWithSimilarAdjacentLink();
//		
//	}


	private void replaceWithReturnLinkAttributes() {
		this.replaceCapacityWithReturnLink();
		this.replaceFreespeedWithReturnLink();
		this.replaceLengthWithReturnLink();
		
	}


	private void printNetworkStatus() {
		
		printNumberOfZeroCapacityLinks();
		printNumberOfZeroFreespeedLinks();
		printNumberOfZeroLengthLinks();
		
	}


	private void printNumberOfZeroLengthLinks() {
		int counter = 0;
		for(Link link : this.network.getLinks().values()){
			
			if(link.getLength()==0.0){
				counter++;
				this.unresolved_length_links.add(link.getId());
			}
		}
		System.out.println("    " + counter + " links have length value = 0.0");
		//System.out.println(this.unresolved_length_links);
		
	}


	private void printNumberOfZeroFreespeedLinks() {
		int counter = 0;
		for(Link link : this.network.getLinks().values()){
			
			if(link.getFreespeed()==0.0){
				counter++;
				this.unresolved_freespeed_links.add(link.getId());
			}
		}
		System.out.println("    " + counter + " links have freespeed value = 0.0");
		//System.out.println(this.unresolved_freespeed_links);
	}
		
	


	private void printNumberOfZeroCapacityLinks() {
		int counter = 0;
		for(Link link : this.network.getLinks().values()){
			
			if(link.getCapacity()==0.0){
				counter++;
				this.unresolved_capacity_links.add(link.getId());
			}
		}
		System.out.println("    " + counter + " links have capacity value = 0.0");
		//System.out.println(this.unresolved_capacity_links);
		
	}
	
	public Link getReturnLink(Link link){
		
		Link returnLink = null;
		if(!link.getId().toString().contains("R")){
			
			IdImpl id = new IdImpl(link.getId().toString()+"R");
			
			if(network.getLinks().containsKey(id)){
				
				returnLink= network.getLinks().get(id);
			}
		}else{
			
			IdImpl id = new IdImpl(link.getId().toString().substring(0,link.getId().toString().indexOf("R")));
			
			returnLink = network.getLinks().get(id);
		}
		
		return returnLink;

	}
	
	
public Link findAdjacentLinks(Link link, String[] attributes){
		
		Node fromNode = link.getFromNode();
		Node toNode = link.getToNode();
		
		for(Link l: this.network.getLinks().values()){
			
			if(l.getToNode().equals(fromNode) | l.getFromNode().equals(toNode)){
				boolean match = true;
				//check if other attributes match
				for(String attribute: attributes){
					if(attribute.equals("capacity")){
						if(link.getCapacity()!=l.getCapacity()){match = false;}
					}else if(attribute.equals("freeSpeed")){
						if(link.getFreespeed()!=l.getFreespeed()){match=false;}
					}else if(attribute.equals("numOfLanes")){
						if(link.getNumberOfLanes()!=l.getNumberOfLanes()){match=false;}
					}else if(attribute.equals("allowedModes")){
						if(!link.getAllowedModes().equals(l.getAllowedModes())){match=false;}
					}else if(attribute.equals("length")){
						if(link.getLength() != l.getLength()){match=false;}
					}else{Gbl.errorMsg("Error: no attribute: "+ attribute + " on class link");
					}
								
				}		
							
				
				if(match & !this.unresolved_capacity_links.contains(l.getId())
						& !this.unresolved_freespeed_links.contains(l.getId())
						& !this.unresolved_length_links.contains(l.getId())){
					//System.out.println(l.getId().toString());
					//System.out.println(link.getId().toString());
					//System.out.println(l.getId().toString().contains(link.getId().toString()));
					return l;
				}
				
			}			
		}
		
		return null;
		
	}
	
	public void replaceCapacityWithReturnLink(){
	
	System.out.println("\n   Replacing zero capacities with return link capacity...");
	System.out.println("   Number of initial unresolved links: " + this.unresolved_capacity_links.size());
	int counter = 0;
	Set<Id> ids_remove = new LinkedHashSet<Id>();
	
	for(Id id : this.unresolved_capacity_links){
		
		Link link = network.getLinks().get(id);
		Link returnLink = getReturnLink(link);
			
			if(!this.unresolved_capacity_links.contains(returnLink.getId())){
				link.setCapacity(returnLink.getCapacity());
				counter++;
				ids_remove.add(id);
			}

		}
	this.unresolved_capacity_links.removeAll(ids_remove);
	
	System.out.println("   ...total links resolved: " + counter);
	//System.out.println("   ...still unresolved links " + this.unresolved_capacity_links +"\n");
	
	}
	
	
	public void replaceFreespeedWithReturnLink(){
		
	System.out.println("\n   Replacing zero freespeed with return link freespeed...");
	System.out.println("   Number of initial unresolved links: " + this.unresolved_freespeed_links.size());
	int counter = 0;
	Set<Id> ids_remove = new LinkedHashSet<Id>();
	
	for(Id id : this.unresolved_freespeed_links){
		
		Link link = network.getLinks().get(id);
		Link returnLink = getReturnLink(link);
			
			if(!this.unresolved_freespeed_links.contains(returnLink.getId())){
				link.setFreespeed(returnLink.getFreespeed());
				counter++;
				ids_remove.add(id);
			}

		}
	this.unresolved_freespeed_links.removeAll(ids_remove);
	
	System.out.println("   ...total links resolved: " + counter);
	//System.out.println("   ...still unresolved links " + this.unresolved_freespeed_links +"\n");
	
	}

	public void replaceLengthWithReturnLink(){
		
	System.out.println("\n   Replacing zero length with return link length...");
	System.out.println("   Number of initial unresolved links: " + this.unresolved_length_links.size());
	int counter = 0;
	Set<Id> ids_remove = new LinkedHashSet<Id>();
	
	for(Id id : this.unresolved_length_links){
		
		Link link = network.getLinks().get(id);
		Link returnLink = getReturnLink(link);
			
			if(!this.unresolved_length_links.contains(returnLink.getId())){
				link.setLength(returnLink.getLength());
				counter++;
				ids_remove.add(id);
			}

		}
	this.unresolved_length_links.removeAll(ids_remove);
	
	System.out.println("   ...total links resolved: " + counter);
	//System.out.println("   ...still unresolved links " + this.unresolved_length_links +"\n");
	
	}
	
//	
//	
//	public void replaceCapacityWithSimilarAdjacentLink(){
//	
//	System.out.println("\n   Replacing zero capacity with 'similar' adjacent link capacity...");
//	System.out.println("   Number of initial unresolved links: " + this.unresolved_capacity_links.size());	
//		
//	Set<Id> idsToRemove = new HashSet<Id>();
//	int counter=0;
//	for(Id id :this.unresolved_capacity_links){
//					
//		Link link = network.getLinks().get(id);
//		
//		Link similarLink = this.findAdjacentLinks(link,new String[]{"freeSpeed","allowedModes","numOfLanes"});
//		
//		if(similarLink!=null){
//		link.setCapacity(similarLink.getCapacity());
//		idsToRemove.add(id);
//		counter++;
//		}
//		
//	}
//	this.unresolved_capacity_links.removeAll(idsToRemove);
//	
//	System.out.println("   ...total links resolved: " + counter);
//	//System.out.println("   ...still unresolved links " + this.unresolved_capacity_links +"\n");
//}	
	
}
