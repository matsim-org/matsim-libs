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
		    out.write(" <!DOCTYPE plans SYSTEM 'http://www.matsim.org/files/dtd/plans_v4.dtd'> \n");
		    out.write(" \n");
		    out.write("<plans xml:lang='de-CH'> \n");
		    out.write(" \n");
		    out.write("<!-- ====================================================================== --> \n");			
			
		    for(int agent=1; agent<=numAgents;agent++){
		    	  out.write(" \n");
		    	 out.write("<person id='"+agent+"'> \n");
		    	 out.write("<plan selected='yes'> \n");
		    	 out.write("<act type='h' link='1' facility='1' x='-25000.0' y='0.0' end_time='06:00:00' /> \n");
		    	 out.write("<leg num='0' mode='car' dep_time='06:00:00'> \n");
		    	 out.write("<route dist='15000.0'> \n");
		    	 out.write("2 7 12  \n");
		    	 out.write("</route> \n");
		    	 out.write("</leg> \n");
		    	 out.write("<act type='w' link='20' facility='20' x='10000.0' y='0.0' dur='08:00:00' /> \n");
		    	 out.write("<leg num='1' mode='car' dep_time='14:00:00'> \n");
		    	 out.write("<route dist='55000.0'> \n");
		    	 out.write("13 14 15 1  \n");
		    	 out.write("</route> \n");
		    	 out.write("</leg> \n");
		    	 out.write("<act type='h' link='1' facility='1' x='-25000.0' y='0.0' /> \n");
		    	 out.write("</plan> \n");
		    	 out.write("</person> \n");
		    	  out.write(" \n");
		    	 out.write("<!-- ====================================================================== --> \n");			
					
		    }
		    out.write(" </plans> \n");
		  
		    out.close();
		    }catch (Exception e){
		    	//Catch exception if any
		    }
	}

}
