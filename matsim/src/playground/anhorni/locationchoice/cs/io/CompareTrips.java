package playground.anhorni.locationchoice.cs.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import org.matsim.gbl.Gbl;
import org.matsim.utils.io.IOUtils;
import playground.anhorni.locationchoice.cs.helper.ChoiceSet;


public class CompareTrips {
	
	private String outdir;
	private String mode;
	
	public CompareTrips(String outdir, String mode) {
		this.outdir = outdir;
		this.mode = mode;		
	}

	public void compare(String file, List<ChoiceSet> choiceSets) {
		
		List<String> choiceSetIdList = new Vector<String>();	
		Iterator<ChoiceSet> choiceSet_it = choiceSets.iterator();
		while (choiceSet_it.hasNext()) {
			choiceSetIdList.add(choiceSet_it.next().getId().toString());
		}		
		String outfile = outdir + this.mode +"_CompareTrips.txt";	
		try {
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String curr_line = bufferedReader.readLine(); // Skip header
					
			try {		
				final String header="Id\tin choice set\twmittel\tausmittel";						
				final BufferedWriter out = IOUtils.getBufferedWriter(outfile);
				out.write(header);
				out.newLine();								
				while ((curr_line = bufferedReader.readLine()) != null) {								
					String[] entries = curr_line.split("\t", -1);					
					String tripId = entries[0].trim();					
					if (choiceSetIdList.contains(tripId)) {
						choiceSetIdList.remove(tripId);
					}
					else {
						out.write(tripId + "\t" + "0" + entries[23].trim() +"\t" + entries[8].trim());
						out.newLine();
					}
				}
				out.flush();
				
				choiceSet_it = choiceSets.iterator();
				while (choiceSet_it.hasNext()) {
					ChoiceSet choiceSet = choiceSet_it.next();
					out.write(choiceSet.getId().toString() + "\t" + "1" + this.mode +"\t" + this.mode);
					out.newLine();
				}	
				out.flush();
				
			} catch (final IOException e) {
				Gbl.errorMsg(e);
			}
		} catch (IOException e) {
				Gbl.errorMsg(e);
		}
	}	
}
