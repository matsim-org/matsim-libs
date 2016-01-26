package pedCA.output;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import matsimConnector.events.CAAgentChangeLinkEvent;
import matsimConnector.events.CAAgentConstructEvent;
import matsimConnector.events.CAAgentEnterEnvironmentEvent;
import matsimConnector.events.CAAgentExitEvent;
import matsimConnector.events.CAAgentLeaveEnvironmentEvent;
import matsimConnector.events.CAAgentMoveEvent;
import matsimConnector.events.CAAgentMoveToOrigin;
import matsimConnector.events.CAEngineStepPerformedEvent;
import matsimConnector.events.CAEventHandler;
import matsimConnector.utility.Constants;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

public class LinksAnalyzer implements CAEventHandler{

	private HashMap <String,Integer> countLinksUsage;
	private File csvFile;
	private int countAgentsInsideEnvironment = 0;
	private double simulationTime = 0;
	
	public LinksAnalyzer(Network net) {
		countLinksUsage = new HashMap<String, Integer>();
		initLinkMap(net);
		try{
			File saveDir = new File(Constants.OUTPUT_PATH);
			if(!saveDir.exists()){
			    saveDir.mkdirs(); 
			}
			csvFile = new File(Constants.OUTPUT_PATH+"/countLinksUsage.csv");
			FileWriter csvWriter;
			csvFile.createNewFile();
			csvWriter = new FileWriter(csvFile);
			csvWriter.write("it,");
			for (String linkId : countLinksUsage.keySet()){
				System.out.println(linkId);
				csvWriter.write(linkId+",");
			}
			csvWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	private void initLinkMap(Network net) {
		for (Id<Link> linkId : net.getLinks().keySet()){
			if(linkId.toString().startsWith("HybridNode"))
				countLinksUsage.put(linkId.toString(), 0);
		}
	}


	@Override
	public void reset(int iteration) {
		if (iteration == 0)
			return;
		FileWriter csvWriter;
		try {
			csvWriter = new FileWriter(csvFile,true);
			csvWriter.write("\n"+iteration+",");
			for (String linkId : countLinksUsage.keySet()){
				csvWriter.write(countLinksUsage.get(linkId)+",");
			}
			csvWriter.write("\n"+iteration+",");
			for (String linkId : countLinksUsage.keySet()){
				csvWriter.write(((double)countLinksUsage.get(linkId)/simulationTime)+",");
				countLinksUsage.put(linkId, 0);
			}
			
			csvWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (iteration == Constants.SIMULATION_ITERATIONS-1)
			try {
				File f = new File(Constants.OUTPUT_PATH+"/simEnded");
				f.createNewFile();			
				FileWriter fw = new FileWriter(f);
				fw.write(""+countLinksUsage.size());
				fw.close();	
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}		
	}
	
	@Override
	public void handleEvent(CAAgentConstructEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(CAAgentMoveEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(CAAgentExitEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(CAAgentMoveToOrigin event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(CAAgentLeaveEnvironmentEvent event) {
		if (--this.countAgentsInsideEnvironment == 0)
			this.simulationTime = event.getTime();
	}

	@Override
	public void handleEvent(CAAgentEnterEnvironmentEvent event) {
		this.countLinksUsage.put(event.getCALinkId(),countLinksUsage.get(event.getCALinkId())+1);
		this.countAgentsInsideEnvironment++;
	}

	@Override
	public void handleEvent(CAAgentChangeLinkEvent event) {
		this.countLinksUsage.put(event.getToLinkId(),countLinksUsage.get(event.getToLinkId())+1);
	
	}


	@Override
	public void handleEvent(CAEngineStepPerformedEvent event) {
		// TODO Auto-generated method stub
		
	}

}
