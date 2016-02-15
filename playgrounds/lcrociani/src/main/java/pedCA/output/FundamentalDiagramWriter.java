package pedCA.output;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import matsimConnector.events.CAAgentChangeLinkEvent;
import matsimConnector.events.CAAgentConstructEvent;
import matsimConnector.events.CAAgentEnterEnvironmentEvent;
import matsimConnector.events.CAAgentExitEvent;
import matsimConnector.events.CAAgentLeaveEnvironmentEvent;
import matsimConnector.events.CAAgentMoveEvent;
import matsimConnector.events.CAAgentMoveToOrigin;
import matsimConnector.events.CAEngineStepPerformedEvent;
import matsimConnector.events.CAEventHandler;

public class FundamentalDiagramWriter implements CAEventHandler{
	
	private final double density;
	private File csvFile;
	private int pedestrianInside;
	private final int populationSize;
	
	public FundamentalDiagramWriter(double density, int populationSize, String outputFileName){
		this.density = density;
		this.populationSize = populationSize;
		this.pedestrianInside = 0;
		try {
			 csvFile = new File(outputFileName);
			 FileWriter csvWriter;
			 if(!csvFile.exists()){
			    csvFile.createNewFile();
			    csvWriter = new FileWriter(csvFile);
			    csvWriter.write("#Density[m^-2],TravelTime[sec]\n");
			    
			}else
				csvWriter = new FileWriter(csvFile,true);
			csvWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
	}

	@Override
	public void handleEvent(CAAgentConstructEvent event) {
	}

	@Override
	public void handleEvent(CAAgentMoveEvent event) {
	}

	@Override
	public void handleEvent(CAAgentExitEvent event) {
	}


	@Override
	public void handleEvent(CAAgentMoveToOrigin event) {
		if(pedestrianInside == populationSize)
			try {
				FileWriter csvWriter = new FileWriter(csvFile,true);
				csvWriter.write(this.density+","+event.getTravelTime()+"\n");
				csvWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(0);
			}
		else{
			event.getPedestrian().lastTimeCheckAtExit=null;
		}
	}

	@Override
	public void handleEvent(CAAgentEnterEnvironmentEvent event) {
		this.pedestrianInside+=1;
	}	

	@Override
	public void handleEvent(CAAgentLeaveEnvironmentEvent event) {
		this.pedestrianInside-=1;
		
	}


	@Override
	public void handleEvent(CAAgentChangeLinkEvent event) {		
	}


	@Override
	public void handleEvent(CAEngineStepPerformedEvent event) {
		// TODO Auto-generated method stub
		
	}

}
