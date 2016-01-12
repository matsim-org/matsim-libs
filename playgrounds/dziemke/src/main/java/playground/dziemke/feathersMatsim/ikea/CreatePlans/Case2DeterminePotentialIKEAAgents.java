package playground.dziemke.feathersMatsim.ikea.CreatePlans;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

import org.matsim.api.core.v01.Coord;

//	Criteria:
//	- dailyShopping
//	- opening hours (1000-2155)
//	- car
//	- radius: 50km

// distribution formula:
//								
// 		1 - ( 1 - ( x/r - 1 )^2 )^0,5

public class Case2DeterminePotentialIKEAAgents {

	private static String dataFile ="./input/prdToAsciiRectifiedTAZ.csv";
	private static String outputFile3="./output/case2/prdToAsciiOwnDemand.csv";
	private static String outputFile1="./output/case2/ikeaActivities.csv";
	private static String outputFile2="./output/case2/diariesOfIkeaVisitors.csv";
	private static String outputFile4="./output/case2/analyseDistances.csv";


	private static	ConvertTazToCoord coordTazManager = new ConvertTazToCoord();

	private static double numberOfIkeaAgents=2250;
	private static int radius=50000;
	private static Random random = new Random();

	// IntArray with distribution of distances 'Agents home - IKEA' of Agents fulfilling criteria (see above)
	private static int[] statistics= new int[(radius/1000)+1];
	// IntArray with distribution of distances 'Agents home - IKEA' of Agents fulfilling criteria & distribution formula
	private static int[] statistics2= new int[(radius/1000)+1];
	// IntArray with distribution of distances 'Agents home - IKEA' of chosen Agents
	private static int[] statistics3= new int[(radius/1000)+1];


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
	int index_journeyDistance=20;

	public static void main(String[] args) throws IOException {
		coordTazManager.convertCoordinates();

		// identifies potential agents fulfilling criteria and distributed according to formula (see description above)
		HashSet<String> potentialIkeaAgents=potentialAgents();

		System.out.println("size: "+potentialIkeaAgents.size());

		double numberOfPotentialAgents=potentialIkeaAgents.size();
		double probability= numberOfIkeaAgents/numberOfPotentialAgents;

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
		int counterAgent=0;

		while((line=bufferedreader.readLine())!=null){
			String[] parts=line.split(";");

			if(consider&&parts[index_agentID].equals(previousAgent)){
				String oldAgentID=parts[index_agentID];
				String newAgentID=oldAgentID+"_IKEA";
				line=line.replace(oldAgentID, newAgentID);
				if(!alreadyIKEAvisitor
						&&Integer.parseInt(parts[index_activityType])==4
						&&Integer.parseInt(parts[index_mode])==1
						&&Integer.parseInt(parts[index_beginningTime])>=1000
						&&Integer.parseInt(parts[index_beginningTime])<=2155
						&&Integer.parseInt(parts[index_activityLocation])!=-1){
					Coord CoordHome=coordTazManager.randomCoordinates(Integer.parseInt(parts[index_homeLocation]));

					double distanceHomeIKEA=Math.sqrt(
							Math.pow(CoordHome.getX()-CoordIKEA.getX(),2)
							+Math.pow(CoordHome.getY()-CoordIKEA.getY(), 2)
							);
					if(distanceHomeIKEA<=radius+3000){
						parts[index_activityLocation]="IKEA";
						line=parts[0];
						statistics3[(int) Math.round(distanceHomeIKEA/1000)]++;
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
				}
				bufferedwriter2.write(line);
				bufferedwriter2.newLine();				
			}
			else {
				consider=false;
				alreadyIKEAvisitor=false;
			}


			if(potentialIkeaAgents.contains(parts[index_agentID])
					&&!parts[index_agentID].equals(previousAgent)
					&&!consider){
				double r=random.nextDouble();
				previousAgent=parts[index_agentID];
				if(r<probability){
					consider=true;
					counterAgent++;
					String oldAgentID=parts[index_agentID];
					String newAgentID=oldAgentID+"_IKEA";
					line=line.replace(oldAgentID, newAgentID);
					bufferedwriter2.write(line);
					bufferedwriter2.newLine();
				}
			}

			bufferedwriter3.write(line);
			bufferedwriter3.newLine();
		}
		System.out.println(counterIKEA+" IKEA Activities created.");
		System.out.println("Statistics:");
		System.out.println(Arrays.toString(statistics));
		System.out.println(Arrays.toString(statistics2));
		System.out.println(Arrays.toString(statistics3));
		bufferedwriter4.write(Arrays.toString(statistics));
		bufferedwriter4.newLine();
		bufferedwriter4.write(Arrays.toString(statistics2));
		bufferedwriter4.newLine();
		bufferedwriter4.write(Arrays.toString(statistics3));
		bufferedwriter4.newLine();

		bufferedreader.close();

		bufferedwriter1.close();
		bufferedwriter2.close();
		bufferedwriter3.close();
		bufferedwriter4.close();

	}

	public static HashSet<String> potentialAgents() throws IOException{
		HashSet<String> agents = new HashSet<String>();
		BufferedReader reader=new BufferedReader(new FileReader(dataFile));
		String line=reader.readLine();
		Coord CoordPriorActivity=null;
		while((line=reader.readLine())!=null){
			String parts[]=line.split(";");

			// does agent fulfill criteria?
			if(Integer.parseInt(parts[index_activityType])==4
					&&Integer.parseInt(parts[index_mode])==1
					&&Integer.parseInt(parts[index_beginningTime])>=1000
					&&Integer.parseInt(parts[index_beginningTime])<=2155
					&&Integer.parseInt(parts[index_activityLocation])!=-1
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
					statistics[(int) Math.round(distanceHomeIKEA/1000)]++;

					// distribution formula:
					double p_agent=
							1-(distanceHomeIKEA/radius);

					//			1-
					//		Math.pow(1-
					//			Math.pow(
					//				(distanceHomeIKEA/radius)-1,
					//			2),
					//		0.5);
					System.out.println("dist1: "+distanceHomeIKEA+" dist2: "+distancePriorActivityIKEA+" probability of being considered: "+p_agent);

					if(random.nextDouble()<=p_agent){
						agents.add(parts[index_agentID]);
						statistics2[(int) Math.round(distanceHomeIKEA/1000)]++;
					}}
			}
			if(Integer.parseInt(parts[index_activityLocation])!=-1)
			{
				CoordPriorActivity=coordTazManager.randomCoordinates(Integer.parseInt(parts[index_activityLocation]));
			}
		}


		reader.close();
		return agents;
	}

}
