package playground.ciarif.retailers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.network.BasicLink;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.LinkImpl;

public class LinksRetailerReader {
	public final static String CONFIG_LINKS = "links";
	public final static String CONFIG_GROUP = "Retailers";
	private String linkIdFile;// = null;
	protected ArrayList<LinkRetailersImpl> links = new ArrayList<LinkRetailersImpl>();
	private Controler controler;
	private ArrayList<Id> actualLinks;
	private final static Logger log = Logger.getLogger(RetailersSequentialLocationListener.class);
	public LinksRetailerReader (Controler controler){
		this.controler = controler;
	}
	
	public LinksRetailerReader(Controler controler, ArrayList<Id> retailersLinks) {
		this.controler = controler;
		this.actualLinks = retailersLinks;
	}

	public ArrayList<LinkRetailersImpl> ReadLinks() {
		
		this.linkIdFile = Gbl.getConfig().findParam(CONFIG_GROUP,CONFIG_LINKS);
		if (this.linkIdFile != null) { 
			
			ArrayList<LinkRetailersImpl> allowed_links = new ArrayList<LinkRetailersImpl>();
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
					if (l.getUpMapping().size()>(Integer.parseInt(entries[1]))) {
						
						l.setMaxFacOnLink(l.getUpMapping().size());
					}
					else {
						l.setMaxFacOnLink(Integer.parseInt(entries[1]));
					}
					
					allowed_links.add(l);
				}
				System.out.println("Links allowed for relocation have been added");
				this.links = allowed_links;
			} 
			catch (IOException e) {
				Gbl.errorMsg(e);
			}
		}
		else {//Francesco: if no file stating which links are allowed is defined
			Integer linksMax = Integer.parseInt(String.valueOf(Math.round(actualLinks.size()*4)));
			log.info("links max= " + linksMax);
			log.info("actual Links= " + actualLinks);
			//TODO note that usually the ratio links/ actual links is so large than this number doesn't really matter, but a check in this sense would be not bad! 
			int i=0;
			while (this.links.size()<linksMax) {
				log.info("links size= " + links.size());
				if (this.links.size()<actualLinks.size()) {
					LinkRetailersImpl link = new LinkRetailersImpl (controler.getNetwork().getLink(actualLinks.get(i)), controler.getNetwork());
					this.links.add(link);
					i=i+1;
				}
				else {
					int rd = MatsimRandom.getRandom().nextInt(controler.getNetwork().getLinks().values().size());
					LinkRetailersImpl link = new LinkRetailersImpl((LinkImpl)controler.getNetwork().getLinks().values().toArray()[rd],controler.getNetwork());
					if (actualLinks.contains(link.getId())) {}
					else {	
						this.links.add(link);
					}	
				}	
				for (LinkRetailersImpl l:this.links) {
					log.info("This link is allowed for relocation: " + l.getId()); 
				}
			
				
			}
		}
		return this.links;
	}	
}
