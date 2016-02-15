package playground.dziemke.feathersMatsim.ikea.analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.matsim.core.utils.io.IOUtils;

public class TrafficVolumeOnLinks {

	static String outputDirectory="C:/Users/jeffw_000/Desktop/Dropbox/Uni/Master/Masterarbeit/MT/workspace new/ikeaStudy/output/";
	static String[] cases={	
			"Case0", 
			"Case0_fcf_3",
			"Case0_fcf_4",
			"Case1",
			"Case1_fcf_3",
			"Case1_fcf_4",
			//			"Case1b",
			"Case1b_fcf_3",
			//			"Case1b_fcf_4",
			"Case2",
			"Case2_fcf_3",
			"Case2_fcf_4",
			"Case2b"
			//"Case2b_fcf_3"

	};
	static String[] hours={"time","0-1","1-2","2-3","3-4","4-5","5-6","6-7","7-8","8-9","9-10","10-11","11-12","12-13","13-14","14-15","15-16","16-17","17-18","18-19","19-20","20-21","21-22","22-23","23-24","0-24"};
	static int[] index_Volumes={0,8,11,14,17,20,23,26,29,32,35,38,41,44,47,50,53,56,59,62,65,68,71,74,77,80};
	static int index_total_average=80;
	static int[] index_TravleTime={83,86,89,92,95,98,101,104,107,110,113,116,119,122,125,128,131,134,137,140,143,146,149,152};
	static List<String> linksList=new ArrayList<String>();
	static String linkName="nA";
	static List<String> outPutLines=new ArrayList<String>();

	static int index_linkID=0;

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

		linksList.add("1-2");
		linksList.add("IKEA_link_HJ");
		linksList.add("IKEA_link_JH");
		linksList.add("Link4Residents1");
		linksList.add("Link4Residents2");

		for(int j=0;j<linksList.size();j++){
			linkName=linksList.get(j);
			

			for (int m=0;m<=25;m++){
				//System.out.println(m+hours[m]);
				outPutLines.add(hours[m]);
			}
			
			System.out.println(outPutLines.get(2));
			for(int i=0;i<=10;i++){
				
				String output=outputDirectory+cases[i]+"/ITERS/it.20/run0.20.linkstats.txt.gz";
				System.out.println(linkName+" --- "+cases[i]);
				BufferedReader br= IOUtils.getBufferedReader(output);
				String line=br.readLine();
				while((line=br.readLine())!=null){
					String parts[]=line.split("\\t");
					if(parts[index_linkID].equals(linkName)){
						System.out.println("link found!");
						String temp=outPutLines.get(0);
						outPutLines.set(0, temp+";"+cases[i]);
						for(int k=1;k<=25;k++){
							String tempString=outPutLines.get(k);
							outPutLines.set(k, tempString+";"+parts[index_Volumes[k]]);
						}
					}
				}
				br.close();
			}

			BufferedWriter bw=new BufferedWriter(new FileWriter(outputDirectory+"traffic_volumes/TrafficVolume_link_"+linkName+".csv"));
			for(int k=0;k<=25;k++){
				bw.write(outPutLines.get(k));
				bw.newLine();
			}
			bw.close();
			outPutLines.clear();
			System.out.println(linkName+": Traffic volumes written to file.");
		}
	}
}
