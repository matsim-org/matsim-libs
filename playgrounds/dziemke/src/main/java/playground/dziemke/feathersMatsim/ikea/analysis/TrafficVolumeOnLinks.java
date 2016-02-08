package playground.dziemke.feathersMatsim.ikea.analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class TrafficVolumeOnLinks {

	static String outputDirectory="C:/Users/jeffw_000/Desktop/Dropbox/Uni/Master/Masterarbeit/MT/workspace new/ikeaStudy/output/";
	static String[] cases={"Case0","Case1","Case1b","Case2","Case2b"};
	static String[] hours={"0-2","1-2","2-3","3-4","4-5","5-6","6-7","7-8","8-9","9-10","10-11","11-12","12-13","13-14","14-15","15-16","16-17","17-18","18-19","19-20","20-21","21-22","22-23","23-24"};
	static int[] index_Volumes={8,11,14,17,20,23,26,29,32,35,38,41,44,47,50,53,56,59,62,65,68,71,74,77};
	static int index_total_average=80;
	static int[] index_TravleTime={83,86,89,92,95,98,101,104,107,110,113,116,119,122,125,128,131,134,137,140,143,146,149,152};
	static List<String> linksNoIkea;
	static List<String> linksIkea;
	
	
	static int index_linkID=0;
	
	
	
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

		linksNoIkea.add("1-2");
		linksIkea.add("test");

		for(int j=0;j<linksNoIkea.size();j++){
			String name=linksNoIkea.get(j);
			
		}
		
		for(int i=0;i<5;i++){
			String output=outputDirectory+cases[i]+"/ITERS/it.20/run0.20.linkstats.txt.gz";
			BufferedReader br=new BufferedReader(new FileReader(output));
			String line=br.readLine();
			while((line=br.readLine())!=null){
							String parts[]=line.split("\\t");
							if(linksIkea.contains(parts[index_linkID])||linksNoIkea.contains(parts[index_linkID])){
								
							}

							BufferedWriter bw=new BufferedWriter(new FileWriter(outputDirectory+cases[i]+"TrafficVolume_link"+parts[index_linkID]+".csv"));
							bw.write("time;volume_avg;traveltime_avg;total volume");
							bw.newLine();				
			}
		}
		
	}

}
