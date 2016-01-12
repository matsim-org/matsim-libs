package playground.dziemke.feathersMatsim.ikea.CreatePlans;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

//	Plan: Take 2,250 IKEA visitors who go by car and substitute them in original population; how? 1. Location, 2. Socioeconomic data?
//	Auf gesamtbev�lkerung ersetzen
//	Ersetzte plane dokumentieren und dann analysieren, insb. In hinblick darauf wie unterschiedliche die ersetzTEN plane von den ersetzENDEN pl�nen sind

public class MergeFeathersPopulations {

	static String inputFile1="./input/prdToAsciiRectifiedTAZ.csv";
	static String inputFile2="./input/prdToAscii_IKEA.csv";
	static String outputFile="./output/mergedPrdToAscii.csv";
	static String outputFile2="./output/substitutedPlans.csv";

	static int index_travelMode=19;
	static int index_homeZone=1;
	static int index_agentID=7;
	static int index_activityZone=17;
	static int index_activityType=14;

	static int IKEAcounter=0;
	static int IKEAvisitorID=0;

	private static List<Integer> IKEAvisitorsList;
	private static List<Integer> SubstitutedAgentsList=new ArrayList<Integer>();
	private static HashMap<Integer, List<int[]>> SortedAgents;

	public static void main(String[] args) throws IOException {

		BufferedReader bufferedReader2 = new BufferedReader(new FileReader(inputFile2));
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, true));
		BufferedWriter writer2= new BufferedWriter(new FileWriter(outputFile2, true));

		String line2 = bufferedReader2.readLine();

		writer.write(line2);
		writer.newLine();
		writer2.write(line2);
		writer2.newLine();

		System.out.println("generate list of IKEA visitors...");
		IKEAvisitorsList=generateListOfIKEAVisitors();
		System.out.println("done.");
		System.out.println("sort agents by TAZ...");
		SortedAgents=sortAgentsByHomeTAZ();
		System.out.println("done");				

		// Read IKEA file
		while((line2=bufferedReader2.readLine())!=null){
			String parts2[]=line2.split(";");
			int homeZoneIKEAvisitor=Integer.parseInt(parts2[index_homeZone]);

			if(Integer.parseInt(parts2[index_agentID])==IKEAvisitorID){
				// write diary of IKEA visitor to new population
				for (int i=0;i<=20;i++){
					if(i==index_agentID){
						parts2[i]=parts2[i]+"_IKEA";
					}
					writer.write(parts2[i]);
					writer2.write(parts2[i]);
					writer.write(";");
					writer2.write(";");
				}								
				writer.newLine();
				writer2.newLine();
			}
			else{
				// if IKEA visitor considered
				if(IKEAvisitorsList.contains(Integer.parseInt(parts2[index_agentID]))
						&&IKEAcounter<=2250){
					IKEAvisitorID=Integer.parseInt(parts2[index_agentID]);
					IKEAcounter++;
					System.out.println(IKEAcounter+" agents substituted");
					// look for agent in order to substitute him/her
					List<int[]> AgentList=SortedAgents.get(homeZoneIKEAvisitor);						
					int[] SubstitutedAgent=AgentList.get(0);
					AgentList.remove(0);
					SortedAgents.put(homeZoneIKEAvisitor, AgentList);
					SubstitutedAgentsList.add(SubstitutedAgent[0]);

					// document diary of substituted agent
					BufferedReader bufferedReaderFindAgent = new BufferedReader(new FileReader(inputFile1));
					for (int i=0;i<SubstitutedAgent[1];i++){
						bufferedReaderFindAgent.readLine();
					}
					String lineFindAgent=bufferedReaderFindAgent.readLine();
					String parts[]=lineFindAgent.split(";");
					if(Integer.parseInt(parts[index_agentID])==SubstitutedAgent[0]){
						int id=Integer.parseInt(parts[index_agentID]);
						while(id==SubstitutedAgent[0]){
							writer2.write(lineFindAgent);
							writer2.newLine();
							lineFindAgent=bufferedReaderFindAgent.readLine();
							String parts3[]=lineFindAgent.split(";");
							id=Integer.parseInt(parts3[index_agentID]);
						}
						bufferedReaderFindAgent.close();
					}
					else{
						System.out.println("Error: Agent "+SubstitutedAgent[0]+" not found in this line.");
					}
					System.out.println("Agent "+SubstitutedAgent[0]+" substituted by agent "+parts2[index_agentID]+"_IKEA.");
					// write diary of IKEA visitor to new population
					for (int i=0;i<=20;i++){
						if(i==index_agentID){
							parts2[i]=parts2[i]+"_IKEA";
						}
						writer.write(parts2[i]);
						writer2.write(parts2[i]);
						writer.write(";");
						writer2.write(";");
					}					

					writer.newLine();
					writer2.newLine();
				}
			}
		}
		bufferedReader2.close();

		BufferedReader bufferedReader1 = new BufferedReader(new FileReader(inputFile1));
		String line=bufferedReader1.readLine();
		while((line=bufferedReader1.readLine())!=null){
			String parts[]=line.split(";");
			if (!SubstitutedAgentsList.contains(Integer.parseInt(parts[index_agentID]))){
				writer.write(line);
				writer.newLine();
			}
		}

		bufferedReader1.close();
		writer.close();	
		writer2.close();
		System.out.println(IKEAcounter+" agents substituted");
		System.out.println("finito");
	}
	// generates a list of all IKEA visitors that are being considered
	// = drive to TAZ 1955 for a DAILY SHOPPING activity by CAR
	private static List<Integer> generateListOfIKEAVisitors() throws IOException{
		List<Integer> IKEAvisitors = new ArrayList<Integer>();
		BufferedReader bufferedReader3 = new BufferedReader(new FileReader(inputFile2));
		String line3 = bufferedReader3.readLine();
		int IDBuffer=0;
		while((line3=bufferedReader3.readLine())!=null){
			String parts3[]=line3.split(";");
			if(Integer.parseInt(parts3[index_travelMode])==1
					&&Integer.parseInt(parts3[index_activityZone])==1955
					&&Integer.parseInt(parts3[index_activityType])==4
					&&Integer.parseInt(parts3[index_agentID])!=IDBuffer){
				IDBuffer=Integer.parseInt(parts3[index_agentID]);
				IKEAvisitors.add(IDBuffer);
			}

		}
		bufferedReader3.close();
		System.out.println(IKEAvisitors.size()+" IKEA visitors traveling by car found");
		return IKEAvisitors;
	}


	// sorts agents from population by home zone and generates for every TAZ a list of agents
	// with the respective line number in prdToAscii.csv
	private static HashMap<Integer, List<int[]>> sortAgentsByHomeTAZ() throws IOException{
		HashMap<Integer, List<int[]>> sortedAgents =new HashMap<Integer, List<int[]>>(4000);
		BufferedReader sortReader = new BufferedReader(new FileReader(inputFile1));
		String line=sortReader.readLine();
		int lineCounter=1;
		int TAZcounter=0;
		int previousAgent=0;
		while((line=sortReader.readLine())!=null){
			String parts[]=line.split(";");
			int key=Integer.parseInt(parts[index_homeZone]);
			if(sortedAgents.containsKey(key)){
				if(Integer.parseInt(parts[index_agentID])!=previousAgent){
					List<int[]> bufferedListOfAgents=sortedAgents.get(key);
					int[] agent={Integer.parseInt(parts[index_agentID]), lineCounter};
					bufferedListOfAgents.add(agent);
					sortedAgents.put(key,bufferedListOfAgents);
					previousAgent=Integer.parseInt(parts[index_agentID]);}
			}
			else{
				int[] agent={Integer.parseInt(parts[index_agentID]), lineCounter};
				List<int[]> listOfAgents = new ArrayList<int[]>();
				listOfAgents.add(agent);
				sortedAgents.put(key, listOfAgents);
				TAZcounter++;
				System.out.println(TAZcounter);
				previousAgent=Integer.parseInt(parts[index_agentID]);

			}
			lineCounter++;
		}
		sortReader.close();
		return sortedAgents;
	}

}
