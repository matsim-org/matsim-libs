package playground.wrashid.sschieffer.DecentralizedSmartCharger.scenarios;

import java.io.BufferedWriter;
import java.io.FileWriter;

import playground.wrashid.sschieffer.DecentralizedSmartCharger.DecentralizedSmartCharger;

public class PlanXMLGenerator {


	private final String outputPath="D:\\ETH\\MasterThesis\\Output\\";
	private int numAgents;
	private  String title;
	
	public PlanXMLGenerator (int numAgent){
		
		this.numAgents=numAgent;		
		title="plans"+Integer.toString(numAgents)+".xml";
		writeFile();
	}
	
	public void setAgents(int numAgent){

		this.numAgents=numAgent;
		title="plans"+Integer.toString(numAgents)+".xml";
		writeFile();
	}
	
	public void writeFile(){
		try{
		    // Create file 
			
		    FileWriter fstream = new FileWriter(outputPath+title);
		    BufferedWriter out = new BufferedWriter(fstream);
		    //out.write("Penetration: "+ Main.penetrationPercent+"\n");
		   
		    out.write("<?xml version='1.0' ?>");
		    out.write(" <!DOCTYPE plans SYSTEM 'http://www.matsim.org/files/dtd/plans_v4.dtd'>");
		    out.write("");
		    out.write("<plans xml:lang='de-CH'>");
		    out.write("");
		    out.write("<!-- ====================================================================== -->");			
			
		    for(int agent=1; agent<=numAgents;agent++){
		    	  out.write("");
		    	 out.write("<person id='"+agent+"'>");
		    	 out.write("<plan selected='yes'>");
		    	 out.write("<act type='h' link='1' facility='1' x='-25000.0' y='0.0' end_time='06:00:00' />");
		    	 out.write("<leg num='0' mode='car' dep_time='06:00:00'>");
		    	 out.write("<route dist='15000.0'>");
		    	 out.write("2 7 12 ");
		    	 out.write("</route>");
		    	 out.write("</leg>");
		    	 out.write("<act type='w' link='20' facility='20' x='10000.0' y='0.0' dur='08:00:00' />");
		    	 out.write("<leg num='1' mode='car' dep_time='14:00:00'>");
		    	 out.write("<route dist='55000.0'>");
		    	 out.write("13 14 15 1 ");
		    	 out.write("</route>");
		    	 out.write("</leg>");
		    	 out.write("<act type='h' link='1' facility='1' x='-25000.0' y='0.0' />");
		    	 out.write("</plan>");
		    	 out.write("</person>");
		    	  out.write("");
		    	 out.write("<!-- ====================================================================== -->");			
					
		    }
		   
		  
		    out.close();
		    }catch (Exception e){
		    	//Catch exception if any
		    }
	}

}
