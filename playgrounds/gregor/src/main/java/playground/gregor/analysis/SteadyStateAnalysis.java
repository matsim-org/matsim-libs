package playground.gregor.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;


import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderTXTv1;

public class SteadyStateAnalysis implements LinkLeaveEventHandler{
	
	
	
	
	private String ef;
	private Map<Id, StringBuffer> hashs;
	public SteadyStateAnalysis(String ef) {
		this.ef = ef;
	}

	private void run(Map<Id, StringBuffer> currentRouteHashs) {
		this.hashs = currentRouteHashs;
		EventsManager ev = new EventsManagerImpl();
		ev.addHandler(this);
		
		new EventsReaderTXTv1(ev).readFile(ef);
//		try {
//			new EventsReaderXMLv1(ev).parse(ef);
//		} catch (SAXException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (ParserConfigurationException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
	}
	
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		StringBuffer bf = this.hashs.get(event.getPersonId());
		if (bf == null) {
			bf = new StringBuffer();
			this.hashs.put(event.getPersonId(), bf);
		}
		bf.append(event.getLinkId().toString());
		
	}

	
	public static void main (String [] args) throws IOException {
		
		
		String baseDir = "/Users/laemmel/Documents/workspace/matsim/test/output/org/matsim/evacuation/run/MarginalRiskCostControllerTest/testMarginalRiskCostController/";
		if (args.length == 1) {
			baseDir = args[0];
		}
		int start = 0;
		int end = 1000;
		
		String out = baseDir + "/steadyStateConvergence.txt";
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(out)));
		
		
		
		String ef = baseDir + "/ITERS/it." + start + "/" + start + ".events.txt.gz";
		
		Map<Id,StringBuffer> currentRouteHashs = new HashMap<Id, StringBuffer>();
		new SteadyStateAnalysis(ef).run(currentRouteHashs);
		
		for (int i = start+1; i <= end; i++) {
			
			Map<Id,StringBuffer>  oldRouteHashs = currentRouteHashs; 
			
			currentRouteHashs = new HashMap<Id, StringBuffer>();
			
			
			ef = baseDir + "/ITERS/it." + i + "/" + i + ".events.txt.gz";
			new SteadyStateAnalysis(ef).run(currentRouteHashs);
			
			double perc = compare(oldRouteHashs,currentRouteHashs);
		
			bw.append(i + "\t" + perc + "\n");
			
		}
		
		bw.close();
		
		
		
	}



	private static double compare(Map<Id, StringBuffer> old,
			Map<Id, StringBuffer> current) {
		int equal = 0;
		for (Entry<Id, StringBuffer> oe  : old.entrySet()) {
			StringBuffer cbf = current.get(oe.getKey());
			if (cbf.toString().equals(oe.getValue().toString())) {
				equal++;
			}
		}
		int differ = (old.size()-equal);
		double perc = differ/(double)old.size();
		System.err.println("total:" + old.size() + " equal:" + equal + " differ:" + differ + " perc differ:" + perc );
		
		return perc;
		
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}



}
