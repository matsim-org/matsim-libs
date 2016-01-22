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

public class MergeFeathersPopulationsWithAdditionalConditions {

	static String inputFile1="./input/prdToAsciiRectifiedTAZ.csv";
	static String inputFile2="./input/prdToAscii_IKEA.csv";
	static String outputFile="./output/mergedPrdToAscii.csv";
	static String outputFile2="./output/substitutedPlans.csv";

	static int index_travelMode=19;
	static int index_homeZone=1;
	static int index_agentID=7;
	static int index_activityZone=17;
	static int index_activityType=14;

	//socio-economic parameters:
	static int index_gender=10;
	static int index_age=8;
	static int index_income=3;
	static int index_work=9;
	static int index_children=5;
	static int index_HHcomposition=2;
	static int index_HHage=8;
	static int index_NbrCars=6;
	static int index_Driver=11;
	static int index_startTime=15;


	static int IKEAcounter=0;
	static int IKEAvisitorID=0;

	static int[] congruentParameters=new int[10];

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
						&&IKEAcounter<2250){
					IKEAvisitorID=Integer.parseInt(parts2[index_agentID]);
					IKEAcounter++;
					System.out.println(IKEAcounter+" agents substituted");
					// look for agent in order to substitute him/her
					List<int[]> AgentList=SortedAgents.get(homeZoneIKEAvisitor);
					int chosenAgent=0;
					Boolean match=false;
					while(!match){
						for(int matchingParameters=10;matchingParameters>0;matchingParameters=matchingParameters-1){
							switch(matchingParameters){
							case 10: 
								for(int index=0;index<AgentList.size();index++){
									int[] bufferedAgent=AgentList.get(index);
									if(bufferedAgent[2]==(Integer.parseInt(parts2[index_gender]))&&
											bufferedAgent[3]==Integer.parseInt(parts2[index_age])&&
											bufferedAgent[4]==Integer.parseInt(parts2[index_income])&&
											bufferedAgent[5]==Integer.parseInt(parts2[index_work])&&
											bufferedAgent[6]==Integer.parseInt(parts2[index_children])&&
											bufferedAgent[7]==Integer.parseInt(parts2[index_HHcomposition])&&
											bufferedAgent[8]==Integer.parseInt(parts2[index_HHage])&&
											bufferedAgent[9]==Integer.parseInt(parts2[index_NbrCars])&&
											bufferedAgent[10]==Integer.parseInt(parts2[index_Driver])
											){
										chosenAgent=index;
										congruentParameters[9]++;

										System.out.println(congruentParameters[0]+"\t"+congruentParameters[1]+"\t"+congruentParameters[2]+"\t"+congruentParameters[3]+"\t"+congruentParameters[4]+"\t"+congruentParameters[5]+"\t"+congruentParameters[6]+"\t"+congruentParameters[7]+"\t"+congruentParameters[8]+"\t"+congruentParameters[9]);

										match=true;
										break;
									}
								}
								break;
							case 9: 
								for(int index=0;index<AgentList.size();index++){
									int[] bufferedAgent=AgentList.get(index);
									if(bufferedAgent[2]==(Integer.parseInt(parts2[index_gender]))&&
											bufferedAgent[3]==Integer.parseInt(parts2[index_age])&&
											bufferedAgent[4]==Integer.parseInt(parts2[index_income])&&
											bufferedAgent[5]==Integer.parseInt(parts2[index_work])&&
											bufferedAgent[6]==Integer.parseInt(parts2[index_children])&&
											bufferedAgent[7]==Integer.parseInt(parts2[index_HHcomposition])&&
											bufferedAgent[8]==Integer.parseInt(parts2[index_HHage])&&
											bufferedAgent[9]==Integer.parseInt(parts2[index_NbrCars])
											){
										chosenAgent=index;
										congruentParameters[8]++;
										System.out.println(congruentParameters[0]+"\t"+congruentParameters[1]+"\t"+congruentParameters[2]+"\t"+congruentParameters[3]+"\t"+congruentParameters[4]+"\t"+congruentParameters[5]+"\t"+congruentParameters[6]+"\t"+congruentParameters[7]+"\t"+congruentParameters[8]+"\t"+congruentParameters[9]);
										match=true;
										break;
									}
								}
								break;
							case 8: 
								for(int index=0;index<AgentList.size();index++){
									int[] bufferedAgent=AgentList.get(index);
									if(bufferedAgent[2]==(Integer.parseInt(parts2[index_gender]))&&
											bufferedAgent[3]==Integer.parseInt(parts2[index_age])&&
											bufferedAgent[4]==Integer.parseInt(parts2[index_income])&&
											bufferedAgent[5]==Integer.parseInt(parts2[index_work])&&
											bufferedAgent[6]==Integer.parseInt(parts2[index_children])&&
											bufferedAgent[7]==Integer.parseInt(parts2[index_HHcomposition])&&
											bufferedAgent[8]==Integer.parseInt(parts2[index_HHage])
											){
										chosenAgent=index;
										congruentParameters[7]++;
										System.out.println(congruentParameters[0]+"\t"+congruentParameters[1]+"\t"+congruentParameters[2]+"\t"+congruentParameters[3]+"\t"+congruentParameters[4]+"\t"+congruentParameters[5]+"\t"+congruentParameters[6]+"\t"+congruentParameters[7]+"\t"+congruentParameters[8]+"\t"+congruentParameters[9]);
										match=true;
										break;
									}
								}
								break;
							case 7: 
								for(int index=0;index<AgentList.size();index++){
									int[] bufferedAgent=AgentList.get(index);
									if(bufferedAgent[2]==(Integer.parseInt(parts2[index_gender]))&&
											bufferedAgent[3]==Integer.parseInt(parts2[index_age])&&
											bufferedAgent[4]==Integer.parseInt(parts2[index_income])&&
											bufferedAgent[5]==Integer.parseInt(parts2[index_work])&&
											bufferedAgent[6]==Integer.parseInt(parts2[index_children])&&
											bufferedAgent[7]==Integer.parseInt(parts2[index_HHcomposition])
											){
										chosenAgent=index;
										congruentParameters[6]++;
										System.out.println(congruentParameters[0]+"\t"+congruentParameters[1]+"\t"+congruentParameters[2]+"\t"+congruentParameters[3]+"\t"+congruentParameters[4]+"\t"+congruentParameters[5]+"\t"+congruentParameters[6]+"\t"+congruentParameters[7]+"\t"+congruentParameters[8]+"\t"+congruentParameters[9]);
										match=true;
										break;
									}
								}
								break;

							case 6: 
								for(int index=0;index<AgentList.size();index++){
									int[] bufferedAgent=AgentList.get(index);
									if(bufferedAgent[2]==(Integer.parseInt(parts2[index_gender]))&&
											bufferedAgent[3]==Integer.parseInt(parts2[index_age])&&
											bufferedAgent[4]==Integer.parseInt(parts2[index_income])&&
											bufferedAgent[5]==Integer.parseInt(parts2[index_work])&&
											bufferedAgent[6]==Integer.parseInt(parts2[index_children])
											){
										chosenAgent=index;
										congruentParameters[5]++;
										System.out.println(congruentParameters[0]+"\t"+congruentParameters[1]+"\t"+congruentParameters[2]+"\t"+congruentParameters[3]+"\t"+congruentParameters[4]+"\t"+congruentParameters[5]+"\t"+congruentParameters[6]+"\t"+congruentParameters[7]+"\t"+congruentParameters[8]+"\t"+congruentParameters[9]);
										match=true;
										break;
									}
								}	
								break;

							case 5: 
								for(int index=0;index<AgentList.size();index++){
									int[] bufferedAgent=AgentList.get(index);
									if(bufferedAgent[2]==(Integer.parseInt(parts2[index_gender]))&&
											bufferedAgent[3]==Integer.parseInt(parts2[index_age])&&
											bufferedAgent[4]==Integer.parseInt(parts2[index_income])&&
											bufferedAgent[5]==Integer.parseInt(parts2[index_work])
											){
										chosenAgent=index;
										congruentParameters[4]++;
										System.out.println(congruentParameters[0]+"\t"+congruentParameters[1]+"\t"+congruentParameters[2]+"\t"+congruentParameters[3]+"\t"+congruentParameters[4]+"\t"+congruentParameters[5]+"\t"+congruentParameters[6]+"\t"+congruentParameters[7]+"\t"+congruentParameters[8]+"\t"+congruentParameters[9]);
										match=true;
										break;
									}
								}	
								break;

							case 4: 
								for(int index=0;index<AgentList.size();index++){
									int[] bufferedAgent=AgentList.get(index);
									if(bufferedAgent[2]==(Integer.parseInt(parts2[index_gender]))&&
											bufferedAgent[3]==Integer.parseInt(parts2[index_age])&&
											bufferedAgent[4]==Integer.parseInt(parts2[index_income])
											){
										chosenAgent=index;
										congruentParameters[3]++;
										System.out.println(congruentParameters[0]+"\t"+congruentParameters[1]+"\t"+congruentParameters[2]+"\t"+congruentParameters[3]+"\t"+congruentParameters[4]+"\t"+congruentParameters[5]+"\t"+congruentParameters[6]+"\t"+congruentParameters[7]+"\t"+congruentParameters[8]+"\t"+congruentParameters[9]);
										match=true;
										break;
									}
								}	
								break;

							case 3: 
								for(int index=0;index<AgentList.size();index++){
									int[] bufferedAgent=AgentList.get(index);
									if(bufferedAgent[2]==(Integer.parseInt(parts2[index_gender]))&&
											bufferedAgent[3]==Integer.parseInt(parts2[index_age])
											){
										chosenAgent=index;
										congruentParameters[2]++;
										System.out.println(congruentParameters[0]+"\t"+congruentParameters[1]+"\t"+congruentParameters[2]+"\t"+congruentParameters[3]+"\t"+congruentParameters[4]+"\t"+congruentParameters[5]+"\t"+congruentParameters[6]+"\t"+congruentParameters[7]+"\t"+congruentParameters[8]+"\t"+congruentParameters[9]);
										match=true;
										break;
									}
								}
								break;

							case 2: 
								for(int index=0;index<AgentList.size();index++){
									int[] bufferedAgent=AgentList.get(index);
									if(bufferedAgent[2]==(Integer.parseInt(parts2[index_gender]))
											){
										chosenAgent=index;
										congruentParameters[1]++;
										System.out.println(congruentParameters[0]+"\t"+congruentParameters[1]+"\t"+congruentParameters[2]+"\t"+congruentParameters[3]+"\t"+congruentParameters[4]+"\t"+congruentParameters[5]+"\t"+congruentParameters[6]+"\t"+congruentParameters[7]+"\t"+congruentParameters[8]+"\t"+congruentParameters[9]);
										match=true;
										break;
									}
								}	
								break;

							case 1:
								chosenAgent=0;
								congruentParameters[0]++;
								System.out.println(congruentParameters[0]+"\t"+congruentParameters[1]+"\t"+congruentParameters[2]+"\t"+congruentParameters[3]+"\t"+congruentParameters[4]+"\t"+congruentParameters[5]+"\t"+congruentParameters[6]+"\t"+congruentParameters[7]+"\t"+congruentParameters[8]+"\t"+congruentParameters[9]);
								match=true;
								break;							
							}	
							if(match==true){break;}
						}
					}

					int[] SubstitutedAgent=AgentList.get(chosenAgent);
					AgentList.remove(chosenAgent);
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
					int[] agent={Integer.parseInt(parts[index_agentID]), 
							lineCounter,
							Integer.parseInt(parts[index_gender]),
							Integer.parseInt(parts[index_age]),
							Integer.parseInt(parts[index_income]),
							Integer.parseInt(parts[index_work]),
							Integer.parseInt(parts[index_children]),
							Integer.parseInt(parts[index_HHcomposition]),
							Integer.parseInt(parts[index_HHage]),
							Integer.parseInt(parts[index_NbrCars]),
							Integer.parseInt(parts[index_Driver])												
					};

					bufferedListOfAgents.add(agent);
					sortedAgents.put(key,bufferedListOfAgents);
					previousAgent=Integer.parseInt(parts[index_agentID]);}
			}
			else{
				int[] agent={Integer.parseInt(parts[index_agentID]), 
						lineCounter,
						Integer.parseInt(parts[index_gender]),
						Integer.parseInt(parts[index_age]),
						Integer.parseInt(parts[index_income]),
						Integer.parseInt(parts[index_work]),
						Integer.parseInt(parts[index_children]),
						Integer.parseInt(parts[index_HHcomposition]),
						Integer.parseInt(parts[index_HHage]),
						Integer.parseInt(parts[index_NbrCars]),
						Integer.parseInt(parts[index_Driver])								
				};
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
