package playground.ciarif.retailers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.matsim.basic.v01.IdImpl;
import org.matsim.controler.Controler;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.basic.v01.network.BasicLink;
import org.matsim.interfaces.core.v01.Link;

public class LinksRetailerReader {
	public final static String CONFIG_LINKS = "links";
	public final static String CONFIG_GROUP = "Retailers";
	private String linkIdFile;// = null;
	protected Object[] links;
	private Controler controler;
	
	public LinksRetailerReader (Controler controler){
		this.controler = controler;
	}
	
	public Object[] ReadLinks() {
		
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
					System.out.println("Link id " + lId);
					System.out.println("Link in controler " + controler.getNetwork().getLink(lId));
					LinkRetailersImpl l = new LinkRetailersImpl(controler.getNetwork().getLink(lId),controler.getNetwork());
					// ciarif: if facilities are already on this link the number of already 
					// existing facilities is compared with the max from the file. The larger is taken.
					System.out.println("Link " + l.getId());
					System.out.println("upmapping " + l.getUpMapping());
					System.out.println("entry " + Integer.parseInt(entries[1]));
					if (l.getUpMapping().size()>(Integer.parseInt(entries[1]))) {
						
						l.setMaxFacOnLink(l.getUpMapping().size());
					}
					else {
						l.setMaxFacOnLink(Integer.parseInt(entries[1]));
					}
					
					allowed_links.add(l);
				}
				System.out.println("Links allowed for relocation have been added");
				this.links = allowed_links.toArray();
			} 
			catch (IOException e) {
				Gbl.errorMsg(e);
			}
		}
		else {//Francesco: if no file stating which links are allowed is defined, any link is allowed.
			this.links = controler.getNetwork().getLinks().values().toArray();
			//this.links = new ArrayList<Object>(controler.getNetwork().getLinks().values());
			System.out.println("All links are allowed for relocation");
		}
		return links;
	}

}
