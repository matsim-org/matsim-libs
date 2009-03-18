package playground.ciarif.retailers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.matsim.basic.v01.IdImpl;
import org.matsim.controler.Controler;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Link;

public class LinksRetailerReader {
	public final static String CONFIG_LINKS = "links";
	public final static String CONFIG_GROUP = "Retailers";
	private String linkIdFile;// = null;
	private ArrayList<LinkRetailersImpl> links;
	private Controler controler;
	
	public LinksRetailerReader (Controler controler){
		
	}
	
	public void ReadLinks() {
		
		this.linkIdFile = Gbl.getConfig().findParam(CONFIG_GROUP,CONFIG_LINKS);
		if (this.linkIdFile != null) { 
			
			try {
				System.out.println("link file " + this.linkIdFile);
				FileReader fr = new FileReader(this.linkIdFile);
				BufferedReader br = new BufferedReader(fr);
				// Skip header
				String curr_line = br.readLine();
				while ((curr_line = br.readLine()) != null) {
					String[] entries = curr_line.split("\t", -1);
					// header: l_id  max_fac
					// index:   0       1 
					Id lId = new IdImpl (entries[0]);
					LinkRetailersImpl l = (LinkRetailersImpl)controler.getNetwork().getLink(lId);
					l.setMaxFacOnLink(Integer.parseInt(entries[1]));
					links.add(l);
				}
			} 
			catch (IOException e) {
				Gbl.errorMsg(e);
			}
		}
		else {//Francesco: if no file stating which links are allowed is defined, any link is allowed.
		}
		
	}
	
}
