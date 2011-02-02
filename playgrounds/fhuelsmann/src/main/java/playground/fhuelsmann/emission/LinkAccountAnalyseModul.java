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


public class LinkAccountAnalyseModul implements analyseModul{
	
	public  String findHbefaFromVisum(int roadType){

		return this.roadTypes[roadType].getHBEFA_RT_NR();
	}
	private final VisumObject[] roadTypes = new VisumObject[100];
	

	
	EmissionFactor emissionFactor = new EmissionFactor();

	private Map<Id, double[]> emissions = new TreeMap<Id,double[]>();


@Override
	public void bearbeite(double travelTime, Id linkId, double averageSpeed, int roadType, int freeVelocity, double distance, HbefaObject[][] hbefaTable) {
	
	creatRoadTypes("../../detailedEval/teststrecke/sim/inputEmissions/road_types.txt");
	int Hbefa_road_type = Integer.valueOf(findHbefaFromVisum(roadType));
	double [] inputForEmission = emissionFactor.collectInputForEmission(Hbefa_road_type, averageSpeed, distance,hbefaTable);
				
		//falls kein LinkId in der Map vorhanden ist
		
		if(this.emissions.get(linkId) == null) {
			this.emissions.put(linkId, inputForEmission);// in dem Fall muss nichts aufaddiert werden, weil es zum ersten mal Daten eingelesen werden
			System.out.println(this.emissions);

		}
		else{
		
			double [] newValue = new double[13]; // es werden hier die neuen  Daten (nach der Aufsummierung) gespeichert
			double [] oldValue = this.emissions.get(linkId); // oldValue ist die bisherige Summe
		
			for(int i=0; i<12 ; i++){
			
			newValue[i]= oldValue[i] + inputForEmission[i];
			
		}
		
		// put newValue in the Map
		
		emissions.put(linkId, newValue);
		System.out.println(this.emissions);

		
	}
	
}

// road_Types erstellen
public void creatRoadTypes(String filename){

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
	
		for(Entry<Id, double[]> LinkIdEntry : this.emissions.entrySet())	{
				for(int i=0 ; i<12 ; i++)
				result+= LinkIdEntry.getKey()+ ";" +i+ ";" + LinkIdEntry.getValue()[i] + "\n";
				
				
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
}}}


		

	
	

