package playground.acmarmol.matsim2030.network;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;


public class NetworkPostProcessor {

	private Network network;
	private final static Logger log = Logger.getLogger(NetworkPostProcessor.class);
	private LinkedList<Id<Link>> unresolved_capacity_links;
	private LinkedList<Id<Link>> unresolved_freespeed_links;
	private LinkedList<Id<Link>> unresolved_length_links;
	
	public NetworkPostProcessor(Network network){
		this.network = network;
		this.unresolved_capacity_links = new LinkedList<>();
		this.unresolved_freespeed_links = new LinkedList<>();
		this.unresolved_length_links = new LinkedList<>();
	}
	
	
	public void process(){
		log.info("Starting processing...");	
		System.out.println("Initial network status");
		printNetworkStatus();
		removeDuplicatedNodes();
			
		System.out.println("Network status after eliminating duplicated nodes");
		printNetworkStatus();
		
		
		replaceWithReturnLinkAttributes();		
		calculateMissingLengths(unresolved_length_links);
		
		System.out.println("Final network status");
		printNetworkStatus();
		log.info("...done");
	}


	private void removeDuplicatedNodes() {
		
		System.out.println("Updating network to avoid duplicated nodes...");
		
		TreeMap<Id, Id> duplicatedNodes = findDuplicatedNodes();
		System.out.println("		"+duplicatedNodes.size()+" duplicated nodes found...");
		
		
		
		//change links and nodes information 
		ArrayList<Id<Link>> linksToRemove = new ArrayList<>();
		int links_edited = 0;
		
		
		System.out.println("		changing links information...");
			
			for(Link link:network.getLinks().values()){
				
				boolean edited = false;
				
				Node fromNode = ((LinkImpl)link).getFromNode();
				Node toNode = ((LinkImpl)link).getToNode();
				
				
				if(duplicatedNodes.containsKey(fromNode.getId())){
					link.setFromNode(network.getNodes().get(duplicatedNodes.get(fromNode.getId())));
					edited = true;
				}
				
				if(duplicatedNodes.containsKey(toNode.getId())){
					link.setToNode(network.getNodes().get(duplicatedNodes.get(toNode.getId())));
					edited = true;
				}
				
				if(link.getFromNode().equals(link.getToNode())){
					linksToRemove.add(link.getId());
				}else if(edited)
					links_edited++;
				
			}
		
		
		System.out.println("			" +linksToRemove.size() + " links removed... ");
		for(Id<Link> linkToRemoveID:linksToRemove){
			network.removeLink(linkToRemoveID);
			this.unresolved_capacity_links.remove(linkToRemoveID);
			this.unresolved_freespeed_links.remove(linkToRemoveID);
			this.unresolved_length_links.remove(linkToRemoveID);
		}
		System.out.println("			"+ links_edited + " links edited... ");
		
		
		System.out.println("		removing duplicated nodes...");
		for(Entry<Id, Id> entry  : duplicatedNodes.entrySet()){
			
			network.getNodes().remove(entry.getValue());
		}
		System.out.println("		" + duplicatedNodes.size() + " nodes removed...");
		
	}


	private TreeMap<Id, Id> findDuplicatedNodes() {
		
		TreeMap<Id, Id> duplicatedNodes = new TreeMap<Id, Id>();
		System.out.println(network.getNodes().size());

		for(Node node : network.getNodes().values()){
			
			
			if(duplicatedNodes.containsKey(node.getId()))
				continue;
			
			double x = node.getCoord().getX();
			double y = node.getCoord().getY();
			Id id = node.getId();
					
			for(Node node2 : network.getNodes().values()){
				
				if(node2.getId().equals(id))
					continue;
				
				if(node2.getCoord().getX()==x && node2.getCoord().getY()==y){
					
					//arbitrarily, smallest id prevails.
					//key = node that is going to be removed.
					//value = node that prevails.
					
					if(id.compareTo(node2.getId())<0){
						duplicatedNodes.put(node2.getId(),id );
					}else{
						duplicatedNodes.put(id, node2.getId());
					}
						
				}
					

				
			}
		
		}
		
		return duplicatedNodes;
		
	}


	private void calculateMissingLengths(LinkedList<Id<Link>> link_ids) {
		
		System.out.println("calculating missing lenghts with euclidian distance...");
		for(Id<Link> link_id : link_ids){
			
			Link link = network.getLinks().get(link_id);
			Node fromNode = link.getFromNode();
			Node toNode = link.getToNode();
			
			double length = Math.sqrt(Math.pow((fromNode.getCoord().getX()-toNode.getCoord().getX()),2)+Math.pow(fromNode.getCoord().getY()-toNode.getCoord().getY(),2));
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
		this.unresolved_capacity_links = new LinkedList<>();
		this.unresolved_freespeed_links = new LinkedList<>();
		this.unresolved_length_links = new LinkedList<>();
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
			
			Id<Link> id = Id.create(link.getId().toString()+"R", Link.class);
			
			if(network.getLinks().containsKey(id)){
				
				returnLink= network.getLinks().get(id);
			}
		}else{
			
			Id id = Id.create(link.getId().toString().substring(0,link.getId().toString().indexOf("R")), Link.class);
			
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
					}else{throw new RuntimeException("Error: no attribute: "+ attribute + " on class link");
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
