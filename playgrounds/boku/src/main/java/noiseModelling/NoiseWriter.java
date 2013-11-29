package noiseModelling;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;

public class NoiseWriter {
	private static final Logger logger = Logger.getLogger(NoiseWriter.class);
	private List<NoiseEventImpl> eventsList ;
	private Map<Id,Map<Double,Double>> calLmeCarHdvHour ;
	private Map<Id,Double> linkId2Lden ;
	private static String runDirectory = "../../detEval/kuhmo/output/output_baseCase_ctd/";
	String outputfile =runDirectory+ "noiseEvents/noiseEvents.xml";
	
	//Das Schreiben der Events habe ich auskommentiert, weil es nicht funktioniert, siehe NoiseTool
	
	public NoiseWriter (Map<Id,Map<Double,Double>> calLmeCarHdvHour , Map<Id,Double> linkId2Lden ){
		this.calLmeCarHdvHour = calLmeCarHdvHour;
		this.linkId2Lden = linkId2Lden;
		eventsList = new ArrayList<NoiseEventImpl> ();
		createEvents ();
	}
	
	private void createEvents (){ //1.change = private
		for (Entry<Id,Map<Double,Double>> entry : calLmeCarHdvHour.entrySet()){
			Id linkId = entry.getKey();
			Double l_DEN = linkId2Lden.get(linkId);
			Map<Double,Double> l_mE = entry.getValue(); 
			double time = 0.0 ; 
			NoiseEventImpl event = new NoiseEventImpl (time,linkId,l_DEN); 
			eventsList.add(event);
		}		
	}
	
	public void writeEvents (){
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		EventWriterXML eWriter = new EventWriterXML(outputfile);
		eventsManager.addHandler(eWriter);
		for(NoiseEventImpl event : eventsList){
			eventsManager.processEvent(event);
		}	
		eWriter.closeFile();
		logger.info("Finished writing output to " + outputfile);
		
	}
	
	//write LinkId;hour;car;HDV;freespeed from handler.getlinkId2hour2vehicles() to ../mobilTUM/OutputTests/InfosProStunde.txt
	public void writeVehiclesFreespeedProStunde(Map <Id,double [][]> infos)throws IOException{
		File target = new File(runDirectory+ "InfosProStunde.txt");
		FileOutputStream fos = new FileOutputStream(target);
		OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
		BufferedWriter bw = new BufferedWriter(osw);
		String str1 = "LinkId;hour;car;HDV;freespeed";
		bw.write(str1);
		bw.newLine();
		for (Entry<Id,double[][]> entry : infos.entrySet()){
			String linkId = entry.getKey()+"";									
			for (int i =0;i<24; ++i){
				String str0 = "";
				double car = entry.getValue()[i][0];
				double HDV = entry.getValue()[i][1];
				double freespeed = entry.getValue()[i][2];
				int timeclass = i+1;
				//str0 = linkId+";"+timeclass+";"+heavy+";"+total ; 
				str0 = linkId+";"+timeclass+";"+car+";"+HDV+";"+freespeed ;
				bw.write(str0);
				bw.newLine();
			}
			
		}
		bw.close();
		osw.close();
		fos.close();
		System.out.println("----------------------------vehicles per hour calculated--------------------------------");
	}

	//write LinkId;hour;freespeed;totalvehicles;HDV from handler.getlinkId2hourd2vehicles() to ../mobilTUM/OutputTests/InfosProStundeDouble.txt
	public void writeVehiclesFreespeedProStundeDouble(Map<Id, Map<Double, double[]>> infos)throws IOException{
		File target = new File(runDirectory+ "InfosProStundeDouble.txt");
		FileOutputStream fos = new FileOutputStream(target);
		OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
		BufferedWriter bw = new BufferedWriter(osw);
		String str1 = "LinkId;hour;freespeed;totalvehicles;HDV;";
		bw.write(str1);
		bw.newLine();
		for (Entry<Id, Map<Double, double[]>> entry : infos.entrySet()){
			String linkId = entry.getKey()+"";		
			Map<Double, double[]> hour2Infos = entry.getValue(); //hour,freespeed,total,HDV
			for (Entry <Double , double[]> element : hour2Infos.entrySet()){
				double hour = element.getKey();
				//double [] hourInfos = element.getValue();
				double freespeed = element.getValue()[0];
				double total = element.getValue()[1];
				double HDV = element.getValue()[2];
				String str0 = "";
				str0 = linkId+";"+hour+";"+total+";"+HDV+";"+freespeed ;
				bw.write(str0);
				bw.newLine();
			}
		}
		bw.close();
		osw.close();
		fos.close();
		System.out.println("----------------------------vehicles per hour calculated--------------------------------");
	}
	
	
}