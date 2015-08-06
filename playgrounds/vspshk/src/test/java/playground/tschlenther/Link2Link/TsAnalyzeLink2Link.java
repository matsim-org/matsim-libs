package playground.tschlenther.Link2Link;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;

public class TsAnalyzeLink2Link implements LinkEnterEventHandler{

	private Map<Integer,int[]> routeUsersPerIter;
	
	private int iteration;
	
	public TsAnalyzeLink2Link() {
		super();
		this.routeUsersPerIter = new HashMap<Integer,int[]>();
		this.reset(0);
	}
	
	@Override
	public void reset(int iteration) {
		this.iteration = iteration;
		this.routeUsersPerIter.put(iteration,new int[2]);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if(event.getLinkId().equals(Id.createLinkId("Link3"))){
			this.routeUsersPerIter.get(this.iteration)[0] += 1;
		}
		else if(event.getLinkId().equals(Id.createLinkId("Link5"))){
			this.routeUsersPerIter.get(this.iteration)[1] += 1;
		}
	}
	
	public void writeResults(){
		for(int i : this.routeUsersPerIter.keySet()){
			System.out.println("\n ITERATION "+ i + ":\n");

			System.out.println("Fahrer auf oberer Route: " + this.routeUsersPerIter.get(i)[0]);
			System.out.println("Fahrer auf unterer Route: " + this.routeUsersPerIter.get(i)[1]);
		}
	}

	
	
}
