package playground.anhorni.counts;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;

public class CountsCompareAnalysis {
	
	private final static Logger log = Logger.getLogger(CountsCompareAnalysis.class);
	String countsCompareFile0 = "input/trb/0.countscompare.txt";
	String countsCompareFile1 = "input/trb/1.countscompare.txt";
	String countsCompareFile2 = "input/trb/2.countscompare.txt";
	String countsCompareFile31 = "input/trb/32.countscompare.txt";
	String countsCompareFile32 = "input/trb/31.countscompare.txt";
	
	private List<String> filterIds = new Vector<String>();
	private TreeMap<String, CountedLink> links = new TreeMap<String, CountedLink>();
	
	
	public static void main(final String[] args) {

		Gbl.startMeasurement();
		CountsCompareAnalysis analysis = new CountsCompareAnalysis();
		analysis.run();
		Gbl.printElapsedTime();
	}
	
	
	public void run() {
		this.readFilter();
		
		this.read(countsCompareFile0);
		this.analyze("Config 0");
		this.links.clear();
		
		this.read(countsCompareFile1);
		this.analyze("Config 1");
		this.links.clear();
		
		this.read(countsCompareFile2);
		this.analyze("Config 2");
		this.links.clear();
		
		this.read(countsCompareFile31);
		this.analyze("Config 3.1");
		this.links.clear();
		
		this.read(countsCompareFile32);
		this.analyze("Config 3.2");
		this.links.clear();
		
	}
		
	private void analyze(String runId) {
		DecimalFormat formatter = new DecimalFormat("0.00");
		double cntSum = 0.0;
		double simSum = 0.0;
		
		Iterator<CountedLink> links_it = links.values().iterator();
		while (links_it.hasNext()) {
			CountedLink link = links_it.next();
			
			for (int i = 0; i < 24; i++) {
				
			//	if (i >= 10 && i <= 19 && !(i >=12 && i <14)) {
					cntSum += link.getCounts()[i];
					simSum += link.getSims()[i];
			//	}
			}
		}
		log.info(runId + ": " + formatter.format(100*(simSum -cntSum) / cntSum));
	}
	
	
	public void readFilter() {
		
		try {
			FileReader fileReader = new FileReader("input/trb/Zh_COUNTS.txt");
			BufferedReader bufferedReader = new BufferedReader(fileReader);	
			
			String curr_line; // Skip header
			while ((curr_line = bufferedReader.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);				
				String linkId = entries[0].trim();
				filterIds.add(linkId);
			}	
			bufferedReader.close();
			fileReader.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}
	
	private void read(String countsCompareFile) {
				
		try {
			FileReader fileReader = new FileReader(countsCompareFile);
			BufferedReader bufferedReader = new BufferedReader(fileReader);	
			
			String curr_line = bufferedReader.readLine(); // Skip header
			while ((curr_line = bufferedReader.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);
				
				String linkId = entries[0].trim();
				int hour = Integer.parseInt(entries[1].trim()) - 1;
				double matsimVol = Double.parseDouble(entries[2].trim());
				double countVol = Double.parseDouble(entries[3].trim());
				
				//if (filterIds.contains(linkId)) {
					if (!links.containsKey(linkId)) {
						CountedLink link = new CountedLink();
						link.setId(linkId);
						links.put(linkId, link);
					}
					CountedLink link = links.get(linkId);
					link.setCount(hour, countVol);
					link.setSim(hour, matsimVol);
				//}
			}	
			bufferedReader.close();
			fileReader.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}
}
