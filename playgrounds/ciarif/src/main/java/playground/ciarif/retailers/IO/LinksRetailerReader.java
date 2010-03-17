package playground.ciarif.retailers.IO;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;

import playground.ciarif.retailers.data.LinkRetailersImpl;
import playground.ciarif.retailers.data.Retailer;
import playground.ciarif.retailers.data.Retailers;

public class LinksRetailerReader {
	
	public final static String CONFIG_LINKS = "links";
	public final static String CONFIG_LINKS_PAR = "freeLinksParameter";
	public final static String CONFIG_GROUP = "Retailers";
	private String linkIdFile;// = null;
	private Controler controler;
	protected TreeMap<Id,LinkRetailersImpl> allLinks = new TreeMap<Id,LinkRetailersImpl>();
	protected TreeMap<Id,LinkRetailersImpl> freeLinks = new TreeMap<Id,LinkRetailersImpl>();
	private TreeMap<Id,LinkRetailersImpl> currentLinks =  new TreeMap<Id,LinkRetailersImpl>();
	private Retailers retailers;
	private final static Logger log = Logger.getLogger(LinksRetailerReader.class);
	
	//Constructors
	
	public LinksRetailerReader(Controler controler, Retailers retailers) {
		this.controler = controler;
		this.retailers = retailers;
	}
	
	// Public methods
	public void init() {
		this.linkIdFile = Gbl.getConfig().findParam(CONFIG_GROUP,CONFIG_LINKS);
		this.detectRetailersActualLinks();
		if (this.linkIdFile != null) {
			this.readFreeLinks();
		}
		else {
			this.createFreeLinks();
		}	
		
		this.allLinks.putAll(currentLinks);
	}
	
	
	public void updateFreeLinks() {
		
		TreeMap<Id,LinkRetailersImpl> links =  new TreeMap<Id,LinkRetailersImpl>();
		TreeMap<Id,LinkRetailersImpl> linksToRemove =  new TreeMap<Id,LinkRetailersImpl>();
		this.detectRetailersActualLinks();
		
		for (LinkRetailersImpl link:allLinks.values()) {
			links.put(link.getId(),link);
		}
		for (LinkRetailersImpl link:currentLinks.values()) {
			Id id = link.getId();
			for (LinkRetailersImpl link2:links.values()){
				Id id2 = link2.getId();
				if (id==id2){
					linksToRemove.put(link2.getId(), link2);
				}
			}
		}
		for (LinkRetailersImpl l:linksToRemove.values()){
			links.remove(l.getId());
		}
		this.freeLinks=links;
	}
	
	//private methods
	
	private void readFreeLinks() {
		
		try {
			
			FileReader fr = new FileReader(this.linkIdFile);
			BufferedReader br = new BufferedReader(fr);
			// Skip header
			String curr_line = br.readLine();
			
			while ((curr_line = br.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);
				// header: l_id  max_fac
				// index:   0       1 
				Id lId = new IdImpl (entries[0]);
				LinkRetailersImpl l = new LinkRetailersImpl(controler.getNetwork().getLinks().get(lId),(NetworkLayer) controler.getNetwork());
				// ciarif: if facilities are already on this link the number of already 
				// existing facilities is compared with the max from the file. The larger is taken.
				//TODO verify if it is still actual
				if (l.getUpMapping().size()>(Integer.parseInt(entries[1]))) {
					
					l.setMaxFacOnLink(l.getUpMapping().size());
				}
				else {
					l.setMaxFacOnLink(Integer.parseInt(entries[1]));
				}
				
				this.freeLinks.put(l.getId(),l);
				this.allLinks.put(l.getId(),l);
			}
		} 
		catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}
	
	private void detectRetailersActualLinks(){
		TreeMap<Id,LinkRetailersImpl> links =  new TreeMap<Id,LinkRetailersImpl>();
		for (Retailer r:retailers.getRetailers().values()) {
			for (ActivityFacility af: r.getFacilities().values()){
				Link fLink = this.controler.getNetwork().getLinks().get(af.getLinkId());
				LinkRetailersImpl link = new LinkRetailersImpl(fLink, (NetworkLayer) controler.getNetwork());
				links.put(link.getId(),link);
				log.info(("The facility " + af.getId()+ " is currently on the link: " + link.getId()));
			}
		}
		this.currentLinks=links;
		for (LinkRetailersImpl l:currentLinks.values()) {
			log.info(("Current Links list contains link: " + l.getId()));
		}
	}
		
	private void createFreeLinks() {
		String freeLinksParameterString = Gbl.getConfig().findParam(CONFIG_GROUP,CONFIG_LINKS_PAR);
		Double freeLinksParameterInt=Double.parseDouble(freeLinksParameterString);
		Integer newLinksMax = (int)(Math.round(this.currentLinks.size()*freeLinksParameterInt));
		
		while (this.freeLinks.size()<(newLinksMax)) {
			int rd = MatsimRandom.getRandom().nextInt(controler.getNetwork().getLinks().values().size());
			LinkRetailersImpl link = new LinkRetailersImpl((LinkImpl)controler.getNetwork().getLinks().values().toArray()[rd],(NetworkLayer) controler.getNetwork());
			if (currentLinks.containsKey(link.getId())) {}
			else {	
				if ((freeLinks.containsKey(link.getId())))	{log.info("The link " + link.getId() + " is already in the list");}
				else {
					this.freeLinks.put(link.getId(), link);
					log.info("the link " + link.getId() + " has been added to the free links" );
					log.info("free links are" + freeLinks.keySet());
					this.allLinks.put(link.getId(),link);
				}	
			}		
		}
	}
	
	// get methods
	public TreeMap<Id,LinkRetailersImpl> getFreeLinks() {
		return this.freeLinks;
	}
}
