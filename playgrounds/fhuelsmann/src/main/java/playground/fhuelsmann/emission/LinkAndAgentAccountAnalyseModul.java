package playground.fhuelsmann.emission;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;


public class LinkAndAgentAccountAnalyseModul implements analyseModul{
	
	public  String findHbefaFromVisumRoadType(int roadType){

		return this.roadTypes[roadType].getHBEFA_RT_NR();
	}
	private final VisumObject[] roadTypes = new VisumObject[100];
	

	
	EmissionsPerEvent emissionFactor = new EmissionsPerEvent();

	private Map<Id, double[]> emissionsLink = new TreeMap<Id,double[]>();
	private Map<Id, double[]> emissionsPerson = new TreeMap<Id,double[]>();


@Override
	public void calculateEmissionsPerLink(double travelTime, Id linkId, double averageSpeed, int roadType, int freeVelocity, double distance, HbefaObject[][] hbefaTable) {
	
	//linkage between Hbefa road types and Visum road types
	createRoadTypes("../../detailedEval/teststrecke/sim/inputEmissions/road_types.txt");
	int Hbefa_road_type = Integer.valueOf(findHbefaFromVisumRoadType(roadType));
	
	//get emissions calculated per event differentiated by fraction and average speed approach
	double [] inputForEmissions = emissionFactor.collectInputForEmission(Hbefa_road_type, averageSpeed, distance,hbefaTable);
				
		//falls kein LinkId in der Map vorhanden ist
		
		if(this.emissionsLink.get(linkId) == null) {
			this.emissionsLink.put(linkId, inputForEmissions);// in dem Fall muss nichts aufaddiert werden, weil es zum ersten mal Daten eingelesen werden
//			System.out.println(this.emissions);

		}
		else{
		
			double [] actualEmissions = new double[12]; // es werden hier die neuen  Daten (nach der Aufsummierung) gespeichert
			double [] previousEmissions = this.emissionsLink.get(linkId); // oldValue ist die bisherige Summe
		
			for(int i=0; i<12 ; i++){
			
			actualEmissions[i]= previousEmissions[i] + inputForEmissions[i];
			System.out.println("\n LinkID "+ linkId);
		}
		
		// put newValue in the Map
		
		emissionsLink.put(linkId, actualEmissions);
//		System.out.println(this.emissions);
		
		
	}
	
}

@Override
	public void calculateEmissionsPerPerson(double travelTime, Id personId, double averageSpeed, int roadType, int freeVelocity, double distance, HbefaObject[][] hbefaTable) {

	//linkage between Hbefa road types and Visum road types
	createRoadTypes("../../detailedEval/teststrecke/sim/inputEmissions/road_types.txt");
	int Hbefa_road_type = Integer.valueOf(findHbefaFromVisumRoadType(roadType));

	//get emissions calculated per event differentiated by fraction and average speed approach
	double [] inputForEmissions = emissionFactor.collectInputForEmission(Hbefa_road_type, averageSpeed, distance,hbefaTable);
			
	//falls kein LinkId in der Map vorhanden ist
	
	if(this.emissionsPerson.get(personId) == null) {
		this.emissionsPerson.put(personId, inputForEmissions);// in dem Fall muss nichts aufaddiert werden, weil es zum ersten mal Daten eingelesen werden
//		System.out.println(this.emissions);

	}
	else{
	
		double [] actualEmissions = new double[12]; // es werden hier die neuen  Daten (nach der Aufsummierung) gespeichert
		double [] previousEmissions = this.emissionsPerson.get(personId); // oldValue ist die bisherige Summe
	
		for(int i=0; i<12 ; i++){
		
		actualEmissions[i]= previousEmissions[i] + inputForEmissions[i];
		
	}
	
	// put newValue in the Map
	
		emissionsPerson.put(personId, actualEmissions);
//	System.out.println(this.emissions);
	
	
}

}

	// road_Types erstellen
	public void createRoadTypes(String filename){

		try{

			FileInputStream fstream = new FileInputStream(filename);
		    // Get the object of DataInputStream
		    DataInputStream in = new DataInputStream(fstream);
	        BufferedReader br = new BufferedReader(new InputStreamReader(in));
		    String strLine;
		    //Read File Line By Line
		    br.readLine();
			    while ((strLine = br.readLine()) != null)   {
			    	//for all lines (whole text) we split the line to an array 
			    
			       	String[] array = strLine.split(",");
			    	
			    	VisumObject obj = new VisumObject(Integer.parseInt(array[0]), array[2]);
			    	this.roadTypes[obj.getVISUM_RT_NR()] = obj;
			    }
			    //Close the input stream
			    in.close();
			    }catch (Exception e){//Catch exception if any
			      System.err.println("Error: " + e.getMessage());
			    }
	}

	
public void printTotalEmissionsPerLink(){
	
	try{ 
		String result ="";
	
		for(Entry<Id, double[]> LinkIdEntry : this.emissionsLink.entrySet())	{
				for(int i=0 ; i<12 ; i++)
				result+= LinkIdEntry.getKey()+ ";" +i+ ";" + LinkIdEntry.getValue()[i] + "\n";
				
				/*					mKrBasedOnAverageSpeed [0]
									noxEmissionsBasedOnAverageSpeed [1]
									co2repEmissionsBasedOnAverageSpeed [2]
									co2EmissionsBasedOnAverageSpeed [3]
									no2EmissionsBasedOnAverageSpeed[4]
									pmEmissionsBasedOnAverageSpeed[5]

									mKrBasedOnFractions[6]
									noxEmissionsBasedOnFractions[7]
									co2repEmissionsBasedOnFractions[8]
									co2EmissionsBasedOnFractions[9]
									no2EmissionsBasedOnFractions[10]
									pmEmissionsBasedOnFractions[11] */
				
				}

					// Create file 
					FileWriter fstream = 
							new FileWriter("../../detailedEval/teststrecke/sim/outputEmissions/emissionsPerLink.txt");			
									BufferedWriter out = new BufferedWriter(fstream);
							out.write("LinkId \t Luftschadstoff \t Emissionen \n"   
										+ result);
							//Close the output stream
							out.close();
								}catch (Exception e){//Catch exception if any
								System.err.println("Error: " + e.getMessage());
}}

public void printTotalEmissionsPerAgent(){
	System.out.println("+++++++++++++++");
	try{ 
		String result2 ="";
	
		for(Entry<Id, double[]> PersonIdEntry : this.emissionsPerson.entrySet())	{
				for(int i=0 ; i<12 ; i++)
				result2+= PersonIdEntry.getKey()+ ";" +i+ ";" + PersonIdEntry.getValue()[i] + "\n";
				
				/*					mKrBasedOnAverageSpeed [0]
									noxEmissionsBasedOnAverageSpeed [1]
									co2repEmissionsBasedOnAverageSpeed [2]
									co2EmissionsBasedOnAverageSpeed [3]
									no2EmissionsBasedOnAverageSpeed[4]
									pmEmissionsBasedOnAverageSpeed[5]

									mKrBasedOnFractions[6]
									noxEmissionsBasedOnFractions[7]
									co2repEmissionsBasedOnFractions[8]
									co2EmissionsBasedOnFractions[9]
									no2EmissionsBasedOnFractions[10]
									pmEmissionsBasedOnFractions[11] */
				
				}

					// Create file 
					FileWriter fstream = 
							new FileWriter("../../detailedEval/teststrecke/sim/outputEmissions/emissionsPerAgent.txt");			
									BufferedWriter out = new BufferedWriter(fstream);
							out.write("PersonId \t Luftschadstoff \t Emissionen \n"   
										+ result2);
							//Close the output stream
							out.close();
								}
							catch (Exception e){//Catch exception if any
								System.err.println("Error: " + e.getMessage());
					}
								
		}
}


		

	
	

