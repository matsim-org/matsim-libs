package playground.santiago.landuse;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;



public class BuildingsByBlock {

	final String OUTPUT_PATH = "../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/landUse/SII/";	
	final String BUILDINGS_FILE = "../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/landUse/SII/infoSII/BRORGA2441N_00000_2014.csv";
	


	private final static Logger log = Logger.getLogger(BuildingsByBlock.class);
	
	private BuildingsByBlock() {}
	
	
	public static void main(String[] args) {
	BuildingsByBlock landUse = new BuildingsByBlock();
	landUse.Run();
	}
	
	private void Run(){
		getBuildingsByBlock( BUILDINGS_FILE , OUTPUT_PATH );
	}
	
	private void getBuildingsByBlock( String utilFile , String outputPath ){
		
		Map <String , Integer> comercioByBlock = new HashMap <String , Integer> ();
		Map <String , Integer> deporteByBlock = new HashMap <String , Integer> ();
		Map <String , Integer> educacionByBlock = new HashMap <String , Integer> ();
		Map <String , Integer> hotelByBlock = new HashMap <String , Integer> ();
		Map <String , Integer> hogarByBlock = new HashMap <String , Integer> ();
		Map <String , Integer> industriaByBlock = new HashMap <String , Integer> ();
//		Map <String , Integer> bienComunByBlock = new HashMap <String , Integer> ();
		Map <String , Integer> bodegaByBlock = new HashMap <String , Integer> ();
		Map <String , Integer> mineriaByBlock = new HashMap <String , Integer> ();
		Map <String , Integer> oficinaByBlock = new HashMap <String , Integer> ();
		Map <String , Integer> adminPublicaByBlock = new HashMap <String , Integer> ();
		Map <String , Integer> cultoByBlock = new HashMap <String , Integer> ();
		Map <String , Integer> saludByBlock = new HashMap <String , Integer> ();
		Map <String , Integer> transporteByBlock = new HashMap <String , Integer> ();
		Map <String , Integer> otrosByBlock = new HashMap <String , Integer> ();
		Map <String , Integer> sitioEriazoByBlock = new HashMap <String , Integer> ();
		Map <String , Integer> estacionamientosByBlock = new HashMap <String , Integer> ();


		int numberLines = 0;		
		
		try {
			
			BufferedReader br = IOUtils.getBufferedReader(utilFile);
			String line;

			while ((line = br.readLine()) != null) {
				numberLines = numberLines + 1;
			}

			br.close();
			
			} catch (IOException e) {
			
			log.error(new Exception(e));
			
			}
		
		
		String [][] prueba = new String [numberLines][2];
			
		try {
			BufferedReader br = IOUtils.getBufferedReader(utilFile);
			String line;
			int i=0;
			while ((line = br.readLine()) != null) {
				String entries[] = line.split(";");
				prueba[i][0]=entries[3];
				prueba[i][1]=entries[4];
				i=i+1;
			}
		} catch (IOException e) {
			
			log.error(new Exception(e));
		}
		
		
	int C=0;	int D=0;	int E=0;	int G=0;	int H=0;	
	int I=0;	int L=0;	int M=0;	int O=0;	int P=0;	
	int Q=0;	int S=0;	int T=0;	int V=0;	int W=0;	
	int Z=0;  //int K=0;
	
	
		for (int i=0; i<numberLines-1; ++i){
			
			/*An ordered utilFile is assumed*/
			
			String actualKey = prueba [i][0];
			String nextKey = prueba [i+1][0];
			
			if(nextKey.equals(actualKey)){

				switch ( prueba[i][1] ) {
				
				case "C":
					C=C+1;
					break;
				case "D":
					D=D+1;
					break;
				case "E":
					E=E+1;
					break;
				case "G":
					G=G+1;
					break;
				case "H":
					H=H+1;
					break;
				case "I":
					I=I+1;
					break;
//				case "K":
//					K=K+2;
//					break;
				case "L":
					L=L+1;
					break;
				case "M":
					M=M+1;
					break;
				case "O":
					O=O+1;
					break;
				case "P":
					P=P+1;
					break;
				case "Q":
					Q=Q+1;
					break;
				case "S":
					S=S+1;
					break;
				case "T":
					T=T+1;
					break;
				case "V":
					V=V+1;
					break;
				case "W":
					W=W+1;
					break;
				case "Z":
					Z=Z+1;
					break;
							
				}				
				
			} else {
				
				comercioByBlock.put(actualKey,C);
				deporteByBlock.put(actualKey,D);
				educacionByBlock.put(actualKey,E);
				hotelByBlock.put(actualKey,G);
				hogarByBlock.put(actualKey,H);
				industriaByBlock.put(actualKey,I);
//				bienComunByBlock.put(actualKey,K);
				bodegaByBlock.put(actualKey,L);
				mineriaByBlock.put(actualKey,M);
				oficinaByBlock.put(actualKey,O);
				adminPublicaByBlock.put(actualKey,P);
				cultoByBlock.put(actualKey,Q);
				saludByBlock.put(actualKey,S);
				transporteByBlock.put(actualKey,T);
				otrosByBlock.put(actualKey,V);
				sitioEriazoByBlock.put(actualKey,W);
				estacionamientosByBlock.put(actualKey,Z);
				
				C=0;	D=0;		E=0;		G=0;
				H=0;	I=0;		L=0;		M=0;	
				O=0;	P=0;		Q=0;		S=0;	
				T=0;	V=0;		W=0;		Z=0;
//				K=0;
				
				
				
			}
			
			
		}
		
		try {
			
			File buildingsByBlock = new File(outputPath + "Buildings by Block");				
			if(!buildingsByBlock.exists()) new File(outputPath + "Buildings by Block").mkdirs();				
			PrintWriter pwC = new PrintWriter (new FileWriter ( buildingsByBlock + "/comercio.csv" ));
			PrintWriter pwD = new PrintWriter (new FileWriter ( buildingsByBlock + "/deporte.csv" ));
			PrintWriter pwE = new PrintWriter (new FileWriter ( buildingsByBlock + "/educacion.csv" ));
			PrintWriter pwG = new PrintWriter (new FileWriter ( buildingsByBlock + "/hotel.csv" ));
			PrintWriter pwH = new PrintWriter (new FileWriter ( buildingsByBlock + "/hogar.csv" ));
			PrintWriter pwI = new PrintWriter (new FileWriter ( buildingsByBlock + "/industria.csv" ));
//			PrintWriter pwK = new PrintWriter (new FileWriter ( buildingsByBlock + "/biencomun.csv" ));
			PrintWriter pwL = new PrintWriter (new FileWriter ( buildingsByBlock + "/bodega.csv" ));
			PrintWriter pwM = new PrintWriter (new FileWriter ( buildingsByBlock + "/mineria.csv" ));
			PrintWriter pwO = new PrintWriter (new FileWriter ( buildingsByBlock + "/oficina.csv" ));
			PrintWriter pwP = new PrintWriter (new FileWriter ( buildingsByBlock + "/adminpublica.csv" ));
			PrintWriter pwQ = new PrintWriter (new FileWriter ( buildingsByBlock + "/culto.csv" ));
			PrintWriter pwS = new PrintWriter (new FileWriter ( buildingsByBlock + "/salud.csv" ));
			PrintWriter pwT = new PrintWriter (new FileWriter ( buildingsByBlock + "/transporte.csv" ));
			PrintWriter pwV = new PrintWriter (new FileWriter ( buildingsByBlock + "/otros.csv" ));
			PrintWriter pwW = new PrintWriter (new FileWriter ( buildingsByBlock + "/sitioeriazo.csv" ));
			PrintWriter pwZ = new PrintWriter (new FileWriter ( buildingsByBlock + "/estacionamiento.csv" ));
			
			
	
			for (Map.Entry<String, Integer> entry : comercioByBlock.entrySet()) {
				pwC.println( entry.getKey().concat(";"+entry.getValue()) );	

		}
			pwC.close();
			
			
			for (Map.Entry<String, Integer> entry : deporteByBlock.entrySet()) {
				pwD.println( entry.getKey().concat(";"+entry.getValue()) );	

		}
			pwD.close();
			
			for (Map.Entry<String, Integer> entry : educacionByBlock.entrySet()) {
				pwE.println( entry.getKey().concat(";"+entry.getValue()) );	

		}
			pwE.close();
			
			for (Map.Entry<String, Integer> entry : hotelByBlock.entrySet()) {
				pwG.println( entry.getKey().concat(";"+entry.getValue()) );	

		}
			pwG.close();
			
			for (Map.Entry<String, Integer> entry : hogarByBlock.entrySet()) {
				pwH.println( entry.getKey().concat(";"+entry.getValue()) );	

		}
			pwH.close();
			
			for (Map.Entry<String, Integer> entry : industriaByBlock.entrySet()) {
				pwI.println( entry.getKey().concat(";"+entry.getValue()) );	

		}
			pwI.close();
			
//			for (Map.Entry<String, Integer> entry : bienComunByBlock.entrySet()) {
//				pwK.println( entry.getKey().concat(";"+entry.getValue()) );	
//
//		}
//			pwK.close();
			
			for (Map.Entry<String, Integer> entry : bodegaByBlock.entrySet()) {
				pwL.println( entry.getKey().concat(";"+entry.getValue()) );	

		}
			pwL.close();
			
			for (Map.Entry<String, Integer> entry : mineriaByBlock.entrySet()) {
				pwM.println( entry.getKey().concat(";"+entry.getValue()) );	

		}
			pwM.close();
			
			for (Map.Entry<String, Integer> entry : oficinaByBlock.entrySet()) {
				pwO.println( entry.getKey().concat(";"+entry.getValue()) );	

		}
			pwO.close();
			
			for (Map.Entry<String, Integer> entry : adminPublicaByBlock.entrySet()) {
				pwP.println( entry.getKey().concat(";"+entry.getValue()) );	

		}
			pwP.close();
			
			for (Map.Entry<String, Integer> entry : cultoByBlock.entrySet()) {
				pwQ.println( entry.getKey().concat(";"+entry.getValue()) );	

		}
			pwQ.close();
			
			for (Map.Entry<String, Integer> entry : saludByBlock.entrySet()) {
				pwS.println( entry.getKey().concat(";"+entry.getValue()) );	

		}
			pwS.close();
			
			for (Map.Entry<String, Integer> entry : transporteByBlock.entrySet()) {
				pwT.println( entry.getKey().concat(";"+entry.getValue()) );	

		}
			pwT.close();
			
			for (Map.Entry<String, Integer> entry : otrosByBlock.entrySet()) {
				pwV.println( entry.getKey().concat(";"+entry.getValue()) );	

		}
			pwV.close();
			
			for (Map.Entry<String, Integer> entry : sitioEriazoByBlock.entrySet()) {
				pwW.println( entry.getKey().concat(";"+entry.getValue()) );	

		}
			pwW.close();
			
			for (Map.Entry<String, Integer> entry : estacionamientosByBlock.entrySet()) {
				pwZ.println( entry.getKey().concat(";"+entry.getValue()) );	

		}
			pwZ.close();

		} catch (IOException e) {
			log.error(new Exception(e));
		}
		
	
		
	}
	
	
	
}

