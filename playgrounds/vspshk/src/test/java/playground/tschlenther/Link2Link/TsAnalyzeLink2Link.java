package playground.tschlenther.Link2Link;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;

public class TsAnalyzeLink2Link implements LinkEnterEventHandler{

	private Map<Integer,int[]> routeUsersPerIter;
	private Map<RunSettings, Map<Integer,int[]>> resultsOfRun;
	private int iteration;
	private RunSettings currentRunSettings;
	
	
	public TsAnalyzeLink2Link(RunSettings settings) {
		super();
		this.currentRunSettings = settings;
		this.routeUsersPerIter = new HashMap<Integer,int[]>();
		this.resultsOfRun = new HashMap<RunSettings, Map<Integer,int[]>>();
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
	
	public void saveRunResults(){
		if(!this.resultsOfRun.containsKey(currentRunSettings)){
			this.resultsOfRun.put(currentRunSettings, routeUsersPerIter);
		}
		else{
			throw new RuntimeException("won't save same run settings twice");
		}
	}
	
	public void setToNextRun(RunSettings settings){
		this.currentRunSettings = new RunSettings(settings.isUseLanes(),settings.isUseSignals(),settings.isUseLink2Link());
		this.reset(0);
	}
	
	public void writeResults(){
		for(RunSettings settings : resultsOfRun.keySet()){
			System.out.println("\n RUNSETTINGS : \t Signals: " + settings.isUseSignals() + "\t Lanes: " + settings.isUseLanes() + "\t Link2Link: " + settings.isUseLink2Link() );

			for(int i : resultsOfRun.get(settings).keySet()){
				
				
				System.out.println("\n ITERATION "+ i + ":");
				System.out.println("Fahrer auf oberer Route: " + this.routeUsersPerIter.get(i)[0]);
				System.out.println("Fahrer auf unterer Route: " + this.routeUsersPerIter.get(i)[1]);
			}
	
		}
	}

	
	
}

class RunSettings {
	
	private boolean useLanes;
	private boolean useSignals;
	private boolean useLink2Link;

	public RunSettings(){
		this.useLanes = false;
		this.useSignals = false;
		this.useLink2Link = false;
	}
	
	public RunSettings(boolean useLanes, boolean useSignals, boolean useLink2Link){
		this.useLanes = useLanes;
		this.useSignals = useSignals;
		this.useLink2Link = useLink2Link;
	}
	
	public boolean isUseLanes() {
		return useLanes;
	}

	public void setUseLanes(boolean useLanes) {
		this.useLanes = useLanes;
	}

	public boolean isUseSignals() {
		return useSignals;
	}

	public void setUseSignals(boolean useSignals) {
		this.useSignals = useSignals;
	}

	public boolean isUseLink2Link() {
		return useLink2Link;
	}

	public void setUseLink2Link(boolean useLink2Link) {
		this.useLink2Link = useLink2Link;
	}

	
}

