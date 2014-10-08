package playground.toronto.analysis.handlers;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.misc.Time;

public class LinkHistogramHandler implements LinkEnterEventHandler,
		LinkLeaveEventHandler {

	public static void main(String[] args) throws IOException{
		
		//Declare arguments so that they exist outside of the try...catch statement
		String eventsFile;
		String reportFile;
		double startTime;
		double endTime;
		HashSet<NetworkRoute> routeSet = new HashSet<NetworkRoute>();
		
		//Try parsing the arguments
		try{
			eventsFile = args[0];
			reportFile  = args[1];
			startTime = Time.parseTime(args[2]);
			endTime= Time.parseTime(args[3]);
			
			//The remaining arguments are assumed to be a comma-separated list of linkIds.
			for (int i = 4; i < args.length; i++){
				String[] idStr = args[i].split(",");
				
				Id<Link> startId = Id.create(idStr[0], Link.class);
				Id<Link> endId = Id.create(idStr[idStr.length - 1], Link.class);
				Id<Link>[] inIds = new Id[idStr.length - 2];
				for (int j = 0; j < inIds.length; j++){
					inIds[j] = Id.create(idStr[j + 2], Link.class);
				}
				
				LinkNetworkRouteImpl nr = new LinkNetworkRouteImpl(startId, inIds, endId);
				routeSet.add(nr);
			}
		
		//If any error occurs, kill the process while printing an error message.
		}catch (Exception e){
			String helpmsg = "ERROR:" + e.getMessage() +"\n\n" +
					"USAGE: [0] = eventsFile - The event file to be analyzed.\n" +
					"[1] = reportFile - A text file to write the report to.\n" +
					"[2] = startTime (HH:MM:SS) - The start time of the analysis period.\n" +
					"[3] = endTime (HH:MM:SS) - The inclusive end time of the analysis period\n" +
					"[4]...[5]...[6].... = Multiple comma-separated lists of continuous link Ids.";
			System.out.println(helpmsg);
			
			return;
		}
		
		//Create the handler.
		LinkHistogramHandler lhh = new LinkHistogramHandler(startTime, endTime, routeSet);
		
		//Set up the EventsManager and add the handler.
		EventsManager em = EventsUtils.createEventsManager();
		em.addHandler(lhh);
		
		//Create and run an EventsReader
		new MatsimEventsReader(em).readFile(eventsFile);
		
		//Now the LinkHistogramHandler is ready to return results.
		BufferedWriter writer = new BufferedWriter(new FileWriter(reportFile));
		
		//Write to the file here:
		writer.write("Link Histogram Results:"); //Writes something to the file
		writer.newLine(); //Starts a new line.
		
		writer.close();
	}
	
	private Set<NetworkRoute> routes;
	public TreeMap<NetworkRoute, List<Double>> entering; //Changed to public so that the results are accessible.
	public TreeMap<NetworkRoute,  List<Double>> exiting; //Changed to public so that the results are accessible.
	private final double startTime;
	private final double endTime;
	
	private Map<Id<Link>, NetworkRoute> enterLinks;
	private Map<Id<Link>, NetworkRoute> exitLinks;
	
	public LinkHistogramHandler (double startTime, double endTime, Set<NetworkRoute> routes){
		this.startTime =startTime;
		this.endTime =endTime;
		this.routes =routes;
		
		this.enterLinks = new HashMap<>();
		this.exitLinks = new HashMap<>();
		
		this.entering = new TreeMap<NetworkRoute, List<Double>>();
		this.exiting = new TreeMap<NetworkRoute, List<Double>>();
		
		for (NetworkRoute r : this.routes){
			this.enterLinks.put(r.getStartLinkId(), r);
			this.exitLinks.put(r.getEndLinkId(), r);
			
			this.entering.put(r, new ArrayList<Double>());
			this.exiting.put(r, new ArrayList<Double>());
		}
	}
	
	@Override
	public void reset(int iteration) {
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (event.getTime() >= this.startTime && event.getTime() <= this.endTime){
			if (this.exitLinks.containsKey(event.getLinkId())){
				this.exiting.get(this.exitLinks.get(event.getLinkId())).add(event.getTime());
			}
		}
		

	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (event.getTime() >= this.startTime && event.getTime() <= this.endTime){
			if (this.enterLinks.containsKey(event.getLinkId())){
				this.entering.get(this.enterLinks.get(event.getLinkId())).add(event.getTime());
			}
		}
	}
	

}
