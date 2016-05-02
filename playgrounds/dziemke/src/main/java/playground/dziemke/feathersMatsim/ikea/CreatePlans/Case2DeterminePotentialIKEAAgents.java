package playground.dziemke.feathersMatsim.ikea.CreatePlans;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Coord;

//	Criteria:
//	- dailyShopping
//	- opening hours (1000-1955)
//	- car
//	- radius: 35km

// deterrence function:
//		p(x)=exp(-0.05x)

public class Case2DeterminePotentialIKEAAgents {

	private static String dataFile ="C:/Users/jeffw_000/Desktop/Dropbox/Uni/Master/Masterarbeit/MT/workspace new/ikeaStudy/input/prdToAsciiRectifiedTAZ.csv";
	private static String outputFile3="C:/Users/jeffw_000/Desktop/Dropbox/Uni/Master/Masterarbeit/MT/workspace new/ikeaStudy/output/Case2/prdToAsciiOwnDemand.csv";
	private static String outputFile1="C:/Users/jeffw_000/Desktop/Dropbox/Uni/Master/Masterarbeit/MT/workspace new/ikeaStudy/output/Case2/ikeaActivities.csv";
	private static String outputFile2="C:/Users/jeffw_000/Desktop/Dropbox/Uni/Master/Masterarbeit/MT/workspace new/ikeaStudy/output/Case2/diariesOfIkeaVisitors.csv";
	private static String outputFile4="C:/Users/jeffw_000/Desktop/Dropbox/Uni/Master/Masterarbeit/MT/workspace new/ikeaStudy/output/Case2/analyseDistances.csv";

	private static	ConvertTazToCoord coordTazManager = new ConvertTazToCoord();
	static HashMap<Integer,List<String>> potentialIkeaAgents=new HashMap<Integer, List<String>>();
	static int[] TripDistribution;

	private static int numberOfIkeaAgents=2250;
	private static int radius=35000;
	private static HashMap<String,Coord> agentsHomeCoords = new HashMap<String,Coord>();
	private static HashMap<String,Double> agentsPreviousActivityDistance = new HashMap<String,Double>();


	// IntArray with distribution of distances 'Agents home - IKEA' of Agents fulfilling criteria (see above)
	private static int[] distributionOfPotentialIkeaVisitors= new int[(radius/1000)+1];
	// IntArray with distribution of distances 'Agents home - IKEA' of Agents fulfilling criteria & distribution formula
	private static int[] agentsPerZone= new int[(radius/1000)+1];
	// IntArray with distribution of distances 'Agents home - IKEA' of chosen Agents
	private static int[] chosenAgents= new int[(radius/1000)+1];
	//IKEA Coordinates
	private static Double xIKEA = 662741.5;
	private static Double yIKEA = 5643343.5366;
	private static Coord CoordIKEA=new Coord(xIKEA,yIKEA);

	static int index_agentID = 7;
	static int index_activityId = 12;
	static int index_activityType = 14;
	static int index_homeLocation=1;
	static int index_activityLocation=17;
	static int index_beginningTime = 15;
	int index_activityDuration = 16;
	int index_journeyDuration = 18;
	static int index_mode=19;
	static int index_journeyDistance=20;
	static int countAgents=0;

	public static void main(String[] args) throws IOException {
		coordTazManager.convertCoordinates();

		// identifies potential agents fulfilling criteria and distributed according to formula (see description above)
		potentialIkeaAgents=potentialAgents();
		System.out.println("size: "+potentialIkeaAgents.size());
		System.out.println(potentialIkeaAgents.toString());
		TripDistribution=tripDistribution();

		// generate list with Ikea visitors
		List<String> IkeaVisitors=determineIkeaVisitors();
		System.out.println("Size of IkeaVisitors: "+IkeaVisitors.size());

		BufferedWriter bufferedwriter1=new BufferedWriter(new FileWriter(outputFile1,false));
		BufferedWriter bufferedwriter2=new BufferedWriter(new FileWriter(outputFile2,false));
		BufferedWriter bufferedwriter3=new BufferedWriter(new FileWriter(outputFile3,false));
		BufferedWriter bufferedwriter4=new BufferedWriter(new FileWriter(outputFile4,false));


		BufferedReader bufferedreader=new BufferedReader(new FileReader(dataFile));
		// read & write header
		String line=bufferedreader.readLine();
		bufferedwriter1.write(line);
		bufferedwriter1.newLine();
		bufferedwriter2.write(line);
		bufferedwriter2.newLine();
		bufferedwriter3.write(line);
		bufferedwriter3.newLine();
		Boolean consider=false;
		Boolean alreadyIKEAvisitor=false;
		String previousAgent="0";

		int counterIKEA=0;

		while((line=bufferedreader.readLine())!=null){
			String[] parts=line.split(";");

			if((countAgents<=numberOfIkeaAgents)&&consider&&parts[index_agentID].equals(previousAgent)){
				String oldAgentID=parts[index_agentID];
				String newAgentID=oldAgentID+"_IKEA";
				line=line.replace(oldAgentID, newAgentID);
				if(!alreadyIKEAvisitor
						&&Integer.parseInt(parts[index_activityType])==4
						&&Integer.parseInt(parts[index_mode])==1
						&&Integer.parseInt(parts[index_beginningTime])>=1000
						&&Integer.parseInt(parts[index_beginningTime])<=1955
						&&Integer.parseInt(parts[index_activityLocation])!=-1){
					Coord CoordHome=agentsHomeCoords.get(parts[index_agentID]);

					double distanceHomeIKEA=Math.sqrt(
							Math.pow(CoordHome.getX()-CoordIKEA.getX(),2)
							+Math.pow(CoordHome.getY()-CoordIKEA.getY(), 2)
							);
					if(distanceHomeIKEA<=radius){
						parts[index_activityLocation]="IKEA";
						parts[index_journeyDistance]=agentsPreviousActivityDistance.get(parts[index_agentID]).toString();
						line=parts[0];
				//		chosenAgents2[(int) Math.round(distanceHomeIKEA/1000)]++;
						countAgents++;
						for(int i=1;i<21;i++){
							String lineOLD=line+";";
							line=lineOLD+parts[i];
						}
						counterIKEA++;
						alreadyIKEAvisitor=true;
						oldAgentID=parts[index_agentID];
						newAgentID=oldAgentID+"_IKEA";
						line=line.replace(oldAgentID, newAgentID);
						bufferedwriter1.write(line);
						bufferedwriter1.newLine();
					}
					else{System.out.println("Distance "+distanceHomeIKEA);}
				}
				bufferedwriter2.write(line);
				bufferedwriter2.newLine();				
			}
			else {
				consider=false;
				alreadyIKEAvisitor=false;
			}
			// IKEA agent found, rename and copy first line
			if(IkeaVisitors.contains(parts[index_agentID])&&
					!parts[index_agentID].equals(previousAgent)
					&&	!consider){
				previousAgent=parts[index_agentID];
				consider=true;
				String oldAgentID=parts[index_agentID];
				String newAgentID=oldAgentID+"_IKEA";
				line=line.replace(oldAgentID, newAgentID);
				bufferedwriter2.write(line);
				bufferedwriter2.newLine();

			}

			bufferedwriter3.write(line);
			bufferedwriter3.newLine();
		}
		System.out.println(counterIKEA+" IKEA Activities created.");
		System.out.println("Statistics:");
		System.out.println(Arrays.toString(distributionOfPotentialIkeaVisitors));
		System.out.println(Arrays.toString(agentsPerZone));
//		System.out.println("chosen agents2"+Arrays.toString(chosenAgents2));
		bufferedwriter4.write(Arrays.toString(distributionOfPotentialIkeaVisitors));
		bufferedwriter4.newLine();
		bufferedwriter4.write(Arrays.toString(agentsPerZone));
		bufferedwriter4.newLine();
		bufferedwriter4.write(Arrays.toString(chosenAgents));
		bufferedwriter4.newLine();

		bufferedreader.close();

		bufferedwriter1.close();
		bufferedwriter2.close();
		bufferedwriter3.close();
		bufferedwriter4.close();
	}

	// Creates an HashSet containing all Agents fulfilling the following criteria:
	//	- dailyShopping activity
	//	- between 10:00 and 21:55
	//	- travel mode 'car'
	//	

	public static HashMap<Integer,List<String>> potentialAgents() throws IOException{
		HashMap<Integer,List<String>> agents = new HashMap<Integer,List<String>>();
		BufferedReader reader=new BufferedReader(new FileReader(dataFile));
		String line=reader.readLine();
		Coord CoordPriorActivity=null;
		while((line=reader.readLine())!=null){
			String parts[]=line.split(";");

			if(Integer.parseInt(parts[index_activityType])==4
					&&Integer.parseInt(parts[index_mode])==1
					&&Integer.parseInt(parts[index_beginningTime])>=1000
					&&Integer.parseInt(parts[index_beginningTime])<=1955
					){
				Coord CoordHome=coordTazManager.randomCoordinates(Integer.parseInt(parts[index_homeLocation]));

				double distanceHomeIKEA=Math.sqrt(
						Math.pow(CoordHome.getX()-CoordIKEA.getX(),2)
						+Math.pow(CoordHome.getY()-CoordIKEA.getY(), 2)
						);
				double distancePriorActivityIKEA=Math.sqrt(
						Math.pow(CoordPriorActivity.getX()-CoordIKEA.getX(),2)
						+Math.pow(CoordPriorActivity.getY()-CoordIKEA.getY(), 2));
				if(distanceHomeIKEA<=radius&&distancePriorActivityIKEA<=radius){
					List<String> agentList=new ArrayList<String>(1000);
					if(agents.containsKey((int)Math.round(distanceHomeIKEA/1000))){
						agentList=agents.get((int)Math.round(distanceHomeIKEA/1000));
					}
					agentList.add(parts[index_agentID]);
					agentsHomeCoords.put(parts[index_agentID], CoordHome);
					agentsPreviousActivityDistance.put(parts[index_agentID], distancePriorActivityIKEA/1000);
					agents.put((int)Math.round(distanceHomeIKEA/1000),agentList);
					distributionOfPotentialIkeaVisitors[(int) Math.round(distanceHomeIKEA/1000)]++;}
			}

			if(Integer.parseInt(parts[index_activityLocation])!=-1)
			{
				CoordPriorActivity=coordTazManager.randomCoordinates(Integer.parseInt(parts[index_activityLocation]));
			}
		}
		System.out.println(Arrays.toString(distributionOfPotentialIkeaVisitors));

		reader.close();
		return agents;
	}

	public static int[] tripDistribution(){
		// deterrence function: exp(-0.05*d)

		// determine alpha:
		// 1. Sum of all O(d)*deterrence function:
		double Sum=0;
		for (int d=0;d<=(radius/1000)-1;d++){
			Sum=Sum+(distributionOfPotentialIkeaVisitors[d]*Math.exp(-0.05*d));
		}
		//2. calculate alpha
		double alpha=1/Sum;
		System.out.println("Alpha: "+alpha);

		//determine distribution:
		int checkSum=0;
		for(int i=0; i<=(radius/1000)-1;i++){
			//calculate T(i)=number of trips from zone i directed to IKEA=numberOfIkeaAgents*alpha*O(i)*deterrence function
			int T=(int)Math.round(numberOfIkeaAgents*alpha*distributionOfPotentialIkeaVisitors[i]*Math.exp(-0.05*i));
			agentsPerZone[i]=T;
			checkSum=checkSum+T;
		}
		System.out.println("Departures per zone: "+Arrays.toString(agentsPerZone));
		System.out.println(checkSum+" agents distributed");

		return agentsPerZone;
	}

	public static List<String> determineIkeaVisitors(){
		List<String> IkeaVisitors=new ArrayList<String>();
		Random random=new Random();
		for(int i=0;i<(radius/1000);i++){
			for(int j=0;j<TripDistribution[i];j++){
				List<String>PotentialAgents=potentialIkeaAgents.get(i);
				String IkeavisitorID="nA";
				int k=0;
				while(IkeaVisitors.contains(IkeavisitorID)){
					k=random.nextInt(PotentialAgents.size());
					IkeavisitorID=PotentialAgents.get(k);
				}
				IkeaVisitors.add(IkeavisitorID);
				PotentialAgents.remove(k);
				potentialIkeaAgents.put(i, PotentialAgents);
				chosenAgents[i]++;
			}
		}
		System.out.println("chosen agents: "+Arrays.toString(chosenAgents));
		return IkeaVisitors;
	}

}
