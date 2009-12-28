package playground.anhorni.locationchoice.analysis.mc.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;

import playground.anhorni.locationchoice.analysis.mc.filters.DateFilter;

public class ZPReader {
	
	DateFilter filter = new DateFilter();
	List<Id> mzFilteredTargetPersons = new Vector<Id>();
	
	public List<Id> read(String file) {
		
		try {
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String curr_line = bufferedReader.readLine(); // Skip header
						
			while ((curr_line = bufferedReader.readLine()) != null) {
								
				String[] entries = curr_line.split("\t", -1);							
				String HHNR = entries[0].trim();
				String ZIELPNR = entries[1].trim();
				if (ZIELPNR.length() == 1) ZIELPNR = "0" + ZIELPNR; 
				Id personId = new IdImpl(HHNR+ZIELPNR);
				
				String[] date = entries[2].trim().split("/", -1);
				String day = date[1].trim();
				String month = date[0].trim();
				String year = "2005";
				
				if (this.filter.passedFilter(year, month, day)) {
					this.mzFilteredTargetPersons.add(personId);
				}
			}
				
		} catch (IOException e) {
				Gbl.errorMsg(e);
		}
		return this.mzFilteredTargetPersons;
	}
}
