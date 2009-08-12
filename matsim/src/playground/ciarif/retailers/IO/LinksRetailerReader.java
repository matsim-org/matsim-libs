package playground.ciarif.retailers.IO;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacility;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.LinkImpl;
import playground.ciarif.retailers.data.LinkRetailersImpl;
import playground.ciarif.retailers.data.Retailer;
import playground.ciarif.retailers.data.Retailers;

public class LinksRetailerReader {
	
	public final static String CONFIG_LINKS = "links";
	public final static String CONFIG_LINKS_PAR = "freeLinksParameter";
	public final static String CONFIG_GROUP = "Retailers";
	private String linkIdFile;// = null;
	private Controler controler;
	protected ArrayList<LinkRetailersImpl> allLinks = new ArrayList<LinkRetailersImpl>();
	protected ArrayList<LinkRetailersImpl> freeLinks = new ArrayList<LinkRetailersImpl>();
	private ArrayList<LinkRetailersImpl> currentLinks =  new ArrayList<LinkRetailersImpl>();
	private Retailers retailers;
	private final static Logger log = Logger.getLogger(LinksRetailerReader.class);
	
	// Constructors
	public LinksRetailerReader (Controler controler){
		this.controler = controler;
	}
	
	
	public LinksRetailerReader(Controler controler, Retailers retailers) {
		this.controler = controler;
		this.retailers = retailers;
	}
	
	// Public methods
	public void init() {
		this.linkIdFile = Gbl.getConfig().findParam(CONFIG_GROUP,CONFIG_LINKS);
		this.detectRetailersActualLinks();
		if (this.linkIdFile != null) {this.readFreeLinks();}
		else {this.createFreeLinks();}	
		this.allLinks.addAll(currentLinks);
	}
	
	
	private void createFreeLinks() {
		String freeLinksParameter = Gbl.getConfig().findParam(CONFIG_GROUP,CONFIG_LINKS_PAR);
		Integer newLinksMax = (Math.round(this.currentLinks.size()*Integer.parseInt(freeLinksParameter)));
		
		while (this.freeLinks.size()<(newLinksMax)) {
			int rd = MatsimRandom.getRandom().nextInt(controler.getNetwork().getLinks().values().size());
			LinkRetailersImpl link = new LinkRetailersImpl((LinkImpl)controler.getNetwork().getLinks().values().toArray()[rd],controler.getNetwork());
			if (currentLinks.contains(link.getId())) {}
			else {	
				this.freeLinks.add(link);
				this.allLinks.add(link);
			}		
		}
	}

	public void readFreeLinks() {
		
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
					LinkRetailersImpl l = new LinkRetailersImpl(controler.getNetwork().getLink(lId),controler.getNetwork());
					// ciarif: if facilities are already on this link the number of already 
					// existing facilities is compared with the max from the file. The larger is taken.
					//TODO verify if it is still actual
					if (l.getUpMapping().size()>(Integer.parseInt(entries[1]))) {
						
						l.setMaxFacOnLink(l.getUpMapping().size());
					}
					else {
						l.setMaxFacOnLink(Integer.parseInt(entries[1]));
					}
					
					this.freeLinks.add(l);
					this.allLinks.add(l);
				}
			} 
			catch (IOException e) {
				Gbl.errorMsg(e);
			}
		}
	
	public void detectRetailersActualLinks(){
		ArrayList<LinkRetailersImpl> links =  new ArrayList<LinkRetailersImpl>();
		for (Retailer r:retailers.getRetailers().values()) {
			for (ActivityFacility af: r.getFacilities().values()){
				LinkRetailersImpl link = new LinkRetailersImpl((LinkImpl)af.getLink(), controler.getNetwork());
				links.add(link);
			}
		}
		this.currentLinks=links;
	}
		
	public ArrayList<LinkRetailersImpl> getFreeLinks() {
		return this.freeLinks;
	}


	public void updateFreeLinks() {
		
		ArrayList<LinkRetailersImpl> links =  new ArrayList<LinkRetailersImpl>();
		ArrayList<LinkRetailersImpl> linksToRemove =  new ArrayList<LinkRetailersImpl>();
		this.detectRetailersActualLinks();
		
		for (LinkRetailersImpl link:allLinks) {
			links.add(link);
		}
		for (LinkRetailersImpl link:currentLinks) {
			Id id = link.getId();
			for (LinkRetailersImpl link2:links){
				Id id2 = link2.getId();
				if (id==id2){
					linksToRemove.add(link2);
				}
			}
		}
		links.removeAll(linksToRemove);
		this.freeLinks=links;
	}
}
