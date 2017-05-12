package playground.santiago.landuse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;



public class BuildingsByArea {

	final String OUTPUT_PATH = "../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/landUse/SII/";	
	final String CSV_FILE = "../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/landUse/SII/2_modifiedShapeSantiagoWithInfo/OrderedBuildingsByBlock.csv";
	private final static Logger log = Logger.getLogger(BuildingsByArea.class);
	
	private BuildingsByArea() {
		
}
	
	public static void main(String[] args) {
	 BuildingsByArea buildingsByArea = new BuildingsByArea();
	 buildingsByArea.Run();
}
	
	
	private void Run(){
		

		ArrayList<String> oldKeys = new ArrayList<>(); //CMN-MZ list

		ArrayList<String> newKeys = new ArrayList<>(); //CMN-MZ-AR list
		
		Map <String , Integer> C = new LinkedHashMap <String , Integer> ();
		Map <String , Integer> D = new LinkedHashMap <String , Integer> ();
		Map <String , Integer> E = new LinkedHashMap <String , Integer> ();
		Map <String , Integer> G = new LinkedHashMap <String , Integer> ();
		Map <String , Integer> H = new LinkedHashMap <String , Integer> ();
		Map <String , Integer> I = new LinkedHashMap <String , Integer> ();
		Map <String , Integer> L = new LinkedHashMap <String , Integer> ();
		Map <String , Integer> M = new LinkedHashMap <String , Integer> ();
		Map <String , Integer> O = new LinkedHashMap <String , Integer> ();
		Map <String , Integer> P = new LinkedHashMap <String , Integer> ();
		Map <String , Integer> Q = new LinkedHashMap <String , Integer> ();
		Map <String , Integer> S = new LinkedHashMap <String , Integer> ();
		Map <String , Integer> T = new LinkedHashMap <String , Integer> ();
		Map <String , Integer> V = new LinkedHashMap <String , Integer> ();
		Map <String , Integer> W = new LinkedHashMap <String , Integer> ();
		Map <String , Integer> Z = new LinkedHashMap <String , Integer> ();


		
		try {
			
			BufferedReader br = IOUtils.getBufferedReader(CSV_FILE);
			String line = br.readLine();

			while ((line = br.readLine()) != null) {
				String entries[]=line.split(",");
				oldKeys.add(entries[3]);
	
				
				
				C.put(entries[21],Integer.parseInt(entries[6]));	D.put(entries[21],Integer.parseInt(entries[8]));
				E.put(entries[21],Integer.parseInt(entries[9]));	G.put(entries[21],Integer.parseInt(entries[12]));
				H.put(entries[21],Integer.parseInt(entries[11]));	I.put(entries[21],Integer.parseInt(entries[13]));
				L.put(entries[21],Integer.parseInt(entries[5]));	M.put(entries[21],Integer.parseInt(entries[14]));
				O.put(entries[21],Integer.parseInt(entries[15]));	P.put(entries[21],Integer.parseInt(entries[4]));
				Q.put(entries[21],Integer.parseInt(entries[7]));	S.put(entries[21],Integer.parseInt(entries[17]));
				T.put(entries[21],Integer.parseInt(entries[19]));	V.put(entries[21],Integer.parseInt(entries[16]));
				W.put(entries[21],Integer.parseInt(entries[18]));	Z.put(entries[21],Integer.parseInt(entries[10]));
				
				newKeys.add(entries[21]);
			}

			br.close();
			
			} catch (IOException e) {
			
			log.error(new Exception(e));
			
			}
		
		Set<String> setOfOldKeys = new TreeSet<String>(oldKeys);

		
		ArrayList <Double> doubleCInformation = new ArrayList <>();	ArrayList <Double> doubleDInformation = new ArrayList <>();
		ArrayList <Double> doubleEInformation = new ArrayList <>();	ArrayList <Double> doubleGInformation = new ArrayList <>();
		ArrayList <Double> doubleHInformation = new ArrayList <>();	ArrayList <Double> doubleIInformation = new ArrayList <>();
		ArrayList <Double> doubleLInformation = new ArrayList <>();	ArrayList <Double> doubleMInformation = new ArrayList <>();
		ArrayList <Double> doubleOInformation = new ArrayList <>();	ArrayList <Double> doublePInformation = new ArrayList <>();
		ArrayList <Double> doubleQInformation = new ArrayList <>();	ArrayList <Double> doubleSInformation = new ArrayList <>();
		ArrayList <Double> doubleTInformation = new ArrayList <>();	ArrayList <Double> doubleVInformation = new ArrayList <>();
		ArrayList <Double> doubleWInformation = new ArrayList <>();	ArrayList <Double> doubleZInformation = new ArrayList <>();
		
		
		
		ArrayList <Integer> intCInformation = new ArrayList <>();	ArrayList <Integer> intDInformation = new ArrayList <>();
		ArrayList <Integer> intEInformation = new ArrayList <>();	ArrayList <Integer> intGInformation = new ArrayList <>();
		ArrayList <Integer> intHInformation = new ArrayList <>();	ArrayList <Integer> intIInformation = new ArrayList <>();
		ArrayList <Integer> intLInformation = new ArrayList <>();	ArrayList <Integer> intMInformation = new ArrayList <>();
		ArrayList <Integer> intOInformation = new ArrayList <>();	ArrayList <Integer> intPInformation = new ArrayList <>();
		ArrayList <Integer> intQInformation = new ArrayList <>();	ArrayList <Integer> intSInformation = new ArrayList <>();
		ArrayList <Integer> intTInformation = new ArrayList <>();	ArrayList <Integer> intVInformation = new ArrayList <>();
		ArrayList <Integer> intWInformation = new ArrayList <>();	ArrayList <Integer> intZInformation = new ArrayList <>();
		
		

			for (int i = 0; i < newKeys.size(); i++) {
				
				
				int totalC = C.get(newKeys.get(i));	int totalD = D.get(newKeys.get(i));	int totalE = E.get(newKeys.get(i));
				int totalG = G.get(newKeys.get(i));	int totalH = H.get(newKeys.get(i));	int totalI = I.get(newKeys.get(i));
				int totalL = L.get(newKeys.get(i));	int totalM = M.get(newKeys.get(i));	int totalO = O.get(newKeys.get(i));
				int totalP = P.get(newKeys.get(i));	int totalQ = Q.get(newKeys.get(i));	int totalS = S.get(newKeys.get(i));
				int totalT = T.get(newKeys.get(i));	int totalV = V.get(newKeys.get(i));	int totalW = W.get(newKeys.get(i));
				int totalZ = Z.get(newKeys.get(i));
				
				double totalArea = getTotalAreaByOldKey ( oldKeys.get(i) , oldKeys , newKeys );
				
				doubleCInformation.add(assignBuildings(newKeys.get(i) , oldKeys , totalC , totalArea ));
				doubleDInformation.add(assignBuildings(newKeys.get(i) , oldKeys , totalD , totalArea ));
				doubleEInformation.add(assignBuildings(newKeys.get(i) , oldKeys , totalE , totalArea ));
				doubleGInformation.add(assignBuildings(newKeys.get(i) , oldKeys , totalG , totalArea ));
				doubleHInformation.add(assignBuildings(newKeys.get(i) , oldKeys , totalH , totalArea ));
				doubleIInformation.add(assignBuildings(newKeys.get(i) , oldKeys , totalI , totalArea ));
				doubleLInformation.add(assignBuildings(newKeys.get(i) , oldKeys , totalL , totalArea ));
				doubleMInformation.add(assignBuildings(newKeys.get(i) , oldKeys , totalM , totalArea ));
				doubleOInformation.add(assignBuildings(newKeys.get(i) , oldKeys , totalO , totalArea ));
				doublePInformation.add(assignBuildings(newKeys.get(i) , oldKeys , totalP , totalArea ));
				doubleQInformation.add(assignBuildings(newKeys.get(i) , oldKeys , totalQ , totalArea ));
				doubleSInformation.add(assignBuildings(newKeys.get(i) , oldKeys , totalS , totalArea ));
				doubleTInformation.add(assignBuildings(newKeys.get(i) , oldKeys , totalT , totalArea ));
				doubleVInformation.add(assignBuildings(newKeys.get(i) , oldKeys , totalV , totalArea ));
				doubleWInformation.add(assignBuildings(newKeys.get(i) , oldKeys , totalW , totalArea ));
				doubleZInformation.add(assignBuildings(newKeys.get(i) , oldKeys , totalZ , totalArea ));
				
				intCInformation.add((int) Math.floor(assignBuildings(newKeys.get(i) , oldKeys , totalC , totalArea )));
				intDInformation.add((int) Math.floor(assignBuildings(newKeys.get(i) , oldKeys , totalD , totalArea )));
				intEInformation.add((int) Math.floor(assignBuildings(newKeys.get(i) , oldKeys , totalE , totalArea )));
				intGInformation.add((int) Math.floor(assignBuildings(newKeys.get(i) , oldKeys , totalG , totalArea )));
				intHInformation.add((int) Math.floor(assignBuildings(newKeys.get(i) , oldKeys , totalH , totalArea )));
				intIInformation.add((int) Math.floor(assignBuildings(newKeys.get(i) , oldKeys , totalI , totalArea )));
				intLInformation.add((int) Math.floor(assignBuildings(newKeys.get(i) , oldKeys , totalL , totalArea )));
				intMInformation.add((int) Math.floor(assignBuildings(newKeys.get(i) , oldKeys , totalM , totalArea )));
				intOInformation.add((int) Math.floor(assignBuildings(newKeys.get(i) , oldKeys , totalO , totalArea )));
				intPInformation.add((int) Math.floor(assignBuildings(newKeys.get(i) , oldKeys , totalP , totalArea )));
				intQInformation.add((int) Math.floor(assignBuildings(newKeys.get(i) , oldKeys , totalQ , totalArea )));
				intSInformation.add((int) Math.floor(assignBuildings(newKeys.get(i) , oldKeys , totalS , totalArea )));
				intTInformation.add((int) Math.floor(assignBuildings(newKeys.get(i) , oldKeys , totalT , totalArea )));
				intVInformation.add((int) Math.floor(assignBuildings(newKeys.get(i) , oldKeys , totalV , totalArea )));
				intWInformation.add((int) Math.floor(assignBuildings(newKeys.get(i) , oldKeys , totalW , totalArea )));
				intZInformation.add((int) Math.floor(assignBuildings(newKeys.get(i) , oldKeys , totalZ , totalArea )));

		
			
			}
			
					/*Largest Remainder Method*/
			for (String s : setOfOldKeys) {
			    	
					int start = oldKeys.indexOf(s);
					int finish = oldKeys.lastIndexOf(s);
					
					int totalC = C.get(newKeys.get(start));	int totalD = D.get(newKeys.get(start));	int totalE = E.get(newKeys.get(start));
					int totalG = G.get(newKeys.get(start));	int totalH = H.get(newKeys.get(start));	int totalI = I.get(newKeys.get(start));
					int totalL = L.get(newKeys.get(start));	int totalM = M.get(newKeys.get(start));	int totalO = O.get(newKeys.get(start));
					int totalP = P.get(newKeys.get(start));	int totalQ = Q.get(newKeys.get(start));	int totalS = S.get(newKeys.get(start));
					int totalT = T.get(newKeys.get(start));	int totalV = V.get(newKeys.get(start));	int totalW = W.get(newKeys.get(start));
					int totalZ = Z.get(newKeys.get(start));
					
					
					int assignedC = 0;	int assignedD = 0;	int assignedE = 0;	int assignedG = 0;
					int assignedH = 0;	int assignedI = 0;	int assignedL = 0;	int assignedM = 0;
					int assignedO = 0;	int assignedP = 0;	int assignedQ = 0;	int assignedS = 0;
					int assignedT = 0;	int assignedV = 0;	int assignedW = 0;	int assignedZ = 0;
					
					
					
					Map <Double, Integer> differencesC = new TreeMap <Double, Integer> (Collections.reverseOrder());					
					Map <Double, Integer> differencesD = new TreeMap <Double, Integer> (Collections.reverseOrder());					
					Map <Double, Integer> differencesE = new TreeMap <Double, Integer> (Collections.reverseOrder());					
					Map <Double, Integer> differencesG = new TreeMap <Double, Integer> (Collections.reverseOrder());					
					Map <Double, Integer> differencesH = new TreeMap <Double, Integer> (Collections.reverseOrder());					
					Map <Double, Integer> differencesI = new TreeMap <Double, Integer> (Collections.reverseOrder());					
					Map <Double, Integer> differencesL = new TreeMap <Double, Integer> (Collections.reverseOrder());					
					Map <Double, Integer> differencesM = new TreeMap <Double, Integer> (Collections.reverseOrder());					
					Map <Double, Integer> differencesO = new TreeMap <Double, Integer> (Collections.reverseOrder());					
					Map <Double, Integer> differencesP = new TreeMap <Double, Integer> (Collections.reverseOrder());					
					Map <Double, Integer> differencesQ = new TreeMap <Double, Integer> (Collections.reverseOrder());					
					Map <Double, Integer> differencesS = new TreeMap <Double, Integer> (Collections.reverseOrder());					
					Map <Double, Integer> differencesT = new TreeMap <Double, Integer> (Collections.reverseOrder());					
					Map <Double, Integer> differencesV = new TreeMap <Double, Integer> (Collections.reverseOrder());					
					Map <Double, Integer> differencesW = new TreeMap <Double, Integer> (Collections.reverseOrder());					
					Map <Double, Integer> differencesZ = new TreeMap <Double, Integer> (Collections.reverseOrder());				

					
					
					for (int i=start; i<=finish; i++){
						assignedC = assignedC + intCInformation.get(i);
						assignedD = assignedD + intDInformation.get(i);
						assignedE = assignedE + intEInformation.get(i);
						assignedG = assignedG + intGInformation.get(i);
						assignedH = assignedH + intHInformation.get(i);
						assignedI = assignedI + intIInformation.get(i);
						assignedL = assignedL + intLInformation.get(i);
						assignedM = assignedM + intMInformation.get(i);
						assignedO = assignedO + intOInformation.get(i);
						assignedP = assignedP + intPInformation.get(i);
						assignedQ = assignedQ + intQInformation.get(i);
						assignedS = assignedS + intSInformation.get(i);
						assignedT = assignedT + intTInformation.get(i);
						assignedV = assignedV + intVInformation.get(i);
						assignedW = assignedW + intWInformation.get(i);
						assignedZ = assignedZ + intZInformation.get(i);
						
						
						
						differencesC.put((doubleCInformation.get(i) - intCInformation.get(i)), i);
						differencesD.put((doubleDInformation.get(i) - intDInformation.get(i)), i);
						differencesE.put((doubleEInformation.get(i) - intEInformation.get(i)), i);
						differencesG.put((doubleGInformation.get(i) - intGInformation.get(i)), i);
						differencesH.put((doubleHInformation.get(i) - intHInformation.get(i)), i);
						differencesI.put((doubleIInformation.get(i) - intIInformation.get(i)), i);
						differencesL.put((doubleLInformation.get(i) - intLInformation.get(i)), i);
						differencesM.put((doubleMInformation.get(i) - intMInformation.get(i)), i);
						differencesO.put((doubleOInformation.get(i) - intOInformation.get(i)), i);
						differencesP.put((doublePInformation.get(i) - intPInformation.get(i)), i);
						differencesQ.put((doubleQInformation.get(i) - intQInformation.get(i)), i);
						differencesS.put((doubleSInformation.get(i) - intSInformation.get(i)), i);
						differencesT.put((doubleTInformation.get(i) - intTInformation.get(i)), i);
						differencesV.put((doubleVInformation.get(i) - intVInformation.get(i)), i);
						differencesW.put((doubleWInformation.get(i) - intWInformation.get(i)), i);
						differencesZ.put((doubleZInformation.get(i) - intZInformation.get(i)), i);
					}
					
					
					ArrayList<Integer> indexesC = new ArrayList<>(differencesC.values());
					ArrayList<Integer> indexesD = new ArrayList<>(differencesD.values());
					ArrayList<Integer> indexesE = new ArrayList<>(differencesE.values());
					ArrayList<Integer> indexesG = new ArrayList<>(differencesG.values());
					ArrayList<Integer> indexesH = new ArrayList<>(differencesH.values());
					ArrayList<Integer> indexesI = new ArrayList<>(differencesI.values());
					ArrayList<Integer> indexesL = new ArrayList<>(differencesL.values());
					ArrayList<Integer> indexesM = new ArrayList<>(differencesM.values());
					ArrayList<Integer> indexesO = new ArrayList<>(differencesO.values());
					ArrayList<Integer> indexesP = new ArrayList<>(differencesP.values());
					ArrayList<Integer> indexesQ = new ArrayList<>(differencesQ.values());
					ArrayList<Integer> indexesS = new ArrayList<>(differencesS.values());
					ArrayList<Integer> indexesT = new ArrayList<>(differencesT.values());
					ArrayList<Integer> indexesV = new ArrayList<>(differencesV.values());
					ArrayList<Integer> indexesW = new ArrayList<>(differencesW.values());
					ArrayList<Integer> indexesZ = new ArrayList<>(differencesZ.values());
					
				
					int difC = totalC - assignedC; 					int difD = totalD - assignedD;
					int difE = totalE - assignedE; 					int difG = totalG - assignedG;
					int difH = totalH - assignedH; 					int difI = totalI - assignedI;
					int difL = totalL - assignedL; 					int difM = totalM - assignedM;
					int difO = totalO - assignedO; 					int difP = totalP - assignedP;
					int difQ = totalQ - assignedQ; 					int difS = totalS - assignedS;
					int difT = totalT - assignedT; 					int difV = totalV - assignedV;
					int difW = totalW - assignedW; 					int difZ = totalZ - assignedZ;
					
	
					if(assignedC!=totalC){
						int i = 0;
						while (difC!=0){

							int index=indexesC.get(i);
							intCInformation.set(index, intCInformation.get(index)+1);
							difC=difC-1;
							i=i+1;
						}

					}
						
					
					if(assignedD!=totalD){
						int i = 0;
						while (difD!=0){

							int index=indexesD.get(i);
							intDInformation.set(index, intDInformation.get(index)+1);
							difD=difD-1;
							i=i+1;
						}

					}
					
					
					
					
					if(assignedE!=totalE){
						int i = 0;
						while (difE!=0){

							int index=indexesE.get(i);
							intEInformation.set(index, intEInformation.get(index)+1);
							difE=difE-1;
							i=i+1;
						}

					}
					
					if(assignedG!=totalG){
						int i = 0;
						while (difG!=0){

							int index=indexesG.get(i);
							intGInformation.set(index, intGInformation.get(index)+1);
							difG=difG-1;
							i=i+1;
						}

					}
					
					if(assignedH!=totalH){
						int i = 0;
						while (difH!=0){

							int index=indexesH.get(i);
							intHInformation.set(index, intHInformation.get(index)+1);
							difH=difH-1;
							i=i+1;
						}

					}
					
					if(assignedI!=totalI){
						int i = 0;
						while (difI!=0){

							int index=indexesI.get(i);
							intIInformation.set(index, intIInformation.get(index)+1);
							difI=difI-1;
							i=i+1;
						}

					}
					
					if(assignedL!=totalL){
						int i = 0;
						while (difL!=0){

							int index=indexesL.get(i);
							intLInformation.set(index, intLInformation.get(index)+1);
							difL=difL-1;
							i=i+1;
						}

					}
					
					if(assignedM!=totalM){
						int i = 0;
						while (difM!=0){

							int index=indexesM.get(i);
							intMInformation.set(index, intMInformation.get(index)+1);
							difM=difM-1;
							i=i+1;
						}

					}
					
					if(assignedO!=totalO){
						int i = 0;
						while (difO!=0){

							int index=indexesO.get(i);
							intOInformation.set(index, intOInformation.get(index)+1);
							difO=difO-1;
							i=i+1;
						}

					}
					
					if(assignedP!=totalP){
						int i = 0;
						while (difP!=0){

							int index=indexesP.get(i);
							intPInformation.set(index, intPInformation.get(index)+1);
							difP=difP-1;
							i=i+1;
						}

					}
					
					if(assignedQ!=totalQ){
						int i = 0;
						while (difQ!=0){

							int index=indexesQ.get(i);
							intQInformation.set(index, intQInformation.get(index)+1);
							difQ=difQ-1;
							i=i+1;
						}

					}
					
					if(assignedS!=totalS){
						int i = 0;
						while (difS!=0){

							int index=indexesS.get(i);
							intSInformation.set(index, intSInformation.get(index)+1);
							difS=difS-1;
							i=i+1;
						}

					}
					
					if(assignedT!=totalT){
						int i = 0;
						while (difT!=0){

							int index=indexesT.get(i);
							intTInformation.set(index, intTInformation.get(index)+1);
							difT=difT-1;
							i=i+1;
						}

					}
					
					if(assignedV!=totalV){
						int i = 0;
						while (difV!=0){

							int index=indexesV.get(i);
							intVInformation.set(index, intVInformation.get(index)+1);
							difV=difV-1;
							i=i+1;
						}

					}
					
					if(assignedW!=totalW){
						int i = 0;
						while (difW!=0){

							int index=indexesW.get(i);
							intWInformation.set(index, intWInformation.get(index)+1);
							difW=difW-1;
							i=i+1;
						}

					}
					
					if(assignedZ!=totalZ){
						int i = 0;
						while (difZ!=0){

							int index=indexesZ.get(i);
							intZInformation.set(index, intZInformation.get(index)+1);
							difZ=difZ-1;
							i=i+1;
						}

					}
					


							}

		
		try {
		
			File buildingsByArea = new File(OUTPUT_PATH + "BuildingsByBlock_ByArea");
			if(!buildingsByArea.exists()) new File(OUTPUT_PATH + "BuildingsByBlock_ByArea").mkdirs();
			

		PrintWriter pwC = new PrintWriter (new FileWriter ( buildingsByArea + "/Comercio.csv" ));
		PrintWriter pwD = new PrintWriter (new FileWriter ( buildingsByArea + "/deporte.csv" ));
		PrintWriter pwE = new PrintWriter (new FileWriter ( buildingsByArea + "/educacion.csv" ));
		PrintWriter pwG = new PrintWriter (new FileWriter ( buildingsByArea + "/hotel.csv" ));
		PrintWriter pwH = new PrintWriter (new FileWriter ( buildingsByArea + "/hogar.csv" ));
		PrintWriter pwI = new PrintWriter (new FileWriter ( buildingsByArea + "/industria.csv" ));
		PrintWriter pwL = new PrintWriter (new FileWriter ( buildingsByArea + "/bodega.csv" ));
		PrintWriter pwM = new PrintWriter (new FileWriter ( buildingsByArea + "/mineria.csv" ));
		PrintWriter pwO = new PrintWriter (new FileWriter ( buildingsByArea + "/oficina.csv" ));
		PrintWriter pwP = new PrintWriter (new FileWriter ( buildingsByArea + "/adminpublica.csv" ));
		PrintWriter pwQ = new PrintWriter (new FileWriter ( buildingsByArea + "/culto.csv" ));
		PrintWriter pwS = new PrintWriter (new FileWriter ( buildingsByArea + "/salud.csv" ));
		PrintWriter pwT = new PrintWriter (new FileWriter ( buildingsByArea + "/transporte.csv" ));
		PrintWriter pwV = new PrintWriter (new FileWriter ( buildingsByArea + "/otros.csv" ));
		PrintWriter pwW = new PrintWriter (new FileWriter ( buildingsByArea + "/sitioeriazo.csv" ));
		PrintWriter pwZ = new PrintWriter (new FileWriter ( buildingsByArea + "/estacionamiento.csv" ));
		
		for (int i = 0; i < newKeys.size(); i++) {
			
		pwC.println( newKeys.get(i).concat( ";" + C.get(newKeys.get(i)) + ";" + String.valueOf( intCInformation.get(i) )  ) );	
		pwD.println( newKeys.get(i).concat( ";" + D.get(newKeys.get(i)) + ";" + String.valueOf( intDInformation.get(i) )  ) );	
		pwE.println( newKeys.get(i).concat( ";" + E.get(newKeys.get(i)) + ";" + String.valueOf( intEInformation.get(i) )  ) );	
		pwG.println( newKeys.get(i).concat( ";" + G.get(newKeys.get(i)) + ";" + String.valueOf( intGInformation.get(i) )  ) );	
		pwH.println( newKeys.get(i).concat( ";" + H.get(newKeys.get(i)) + ";" + String.valueOf( intHInformation.get(i) )  ) );	
		pwI.println( newKeys.get(i).concat( ";" + I.get(newKeys.get(i)) + ";" + String.valueOf( intIInformation.get(i) )  ) );	
		pwL.println( newKeys.get(i).concat( ";" + L.get(newKeys.get(i)) + ";" + String.valueOf( intLInformation.get(i) )  ) );	
		pwM.println( newKeys.get(i).concat( ";" + M.get(newKeys.get(i)) + ";" + String.valueOf( intMInformation.get(i) )  ) );	
		pwO.println( newKeys.get(i).concat( ";" + O.get(newKeys.get(i)) + ";" + String.valueOf( intOInformation.get(i) )  ) );	
		pwP.println( newKeys.get(i).concat( ";" + P.get(newKeys.get(i)) + ";" + String.valueOf( intPInformation.get(i) )  ) );	
		pwQ.println( newKeys.get(i).concat( ";" + Q.get(newKeys.get(i)) + ";" + String.valueOf( intQInformation.get(i) )  ) );	
		pwS.println( newKeys.get(i).concat( ";" + S.get(newKeys.get(i)) + ";" + String.valueOf( intSInformation.get(i) )  ) );	
		pwT.println( newKeys.get(i).concat( ";" + T.get(newKeys.get(i)) + ";" + String.valueOf( intTInformation.get(i) )  ) );	
		pwV.println( newKeys.get(i).concat( ";" + V.get(newKeys.get(i)) + ";" + String.valueOf( intVInformation.get(i) )  ) );	
		pwW.println( newKeys.get(i).concat( ";" + W.get(newKeys.get(i)) + ";" + String.valueOf( intWInformation.get(i) )  ) );	
		pwZ.println( newKeys.get(i).concat( ";" + Z.get(newKeys.get(i)) + ";" + String.valueOf( intZInformation.get(i) )  ) );	

			}

			pwC.close();
			pwD.close();
			pwE.close();
			pwG.close();
			pwH.close();
			pwI.close();
			pwL.close();
			pwM.close();
			pwO.close();
			pwP.close();
			pwQ.close();
			pwS.close();
			pwT.close();
			pwV.close();
			pwW.close();
			pwZ.close();
			
			


	
			
			} catch (IOException e) {
			
			log.error(new Exception(e));
			
			}

}


	private int numberOfOcurrences ( String oldKey , ArrayList<String> oldKeys ){
		
		return Collections.frequency(oldKeys, oldKey);
	
	
	}
	
	private double assignBuildings( String newKey , ArrayList<String> oldKeys , int totalBuildings , double totalArea ){
		
		

		String parts [] = newKey.split("-");
		String oldKey = parts[0].concat("-" + parts[1]);
		int area = Integer.parseInt(parts[2]);
		int frequencyOfOldKey = numberOfOcurrences ( oldKey , oldKeys );
	
		if (frequencyOfOldKey==1){
			

			return totalBuildings;

			
		}else{
			
			
			double Buildings = (area * (totalBuildings / totalArea));
			return Buildings;

				
			}
	
			
		}
	
	private double getTotalAreaByOldKey ( String oldKey , ArrayList<String> oldKeys , ArrayList<String> newKeys ){
		
		int start = oldKeys.indexOf(oldKey);
		int finish = oldKeys.lastIndexOf(oldKey);
		int totalArea=0;
		
		for (int i = start ; i<=finish ; ++i ) {
			String entries [] = newKeys.get(i).split("-");
			
			totalArea = totalArea + Integer.parseInt(entries[2]);
		}
		
		return totalArea;
		
		
	}

	}

