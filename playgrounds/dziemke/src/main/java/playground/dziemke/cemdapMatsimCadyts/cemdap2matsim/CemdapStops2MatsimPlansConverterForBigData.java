package playground.dziemke.cemdapMatsimCadyts.cemdap2matsim;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;

public class CemdapStops2MatsimPlansConverterForBigData {

	private static final Logger log = Logger.getLogger(CemdapStops2MatsimPlansConverter.class);
	
	private static int numberOfFirstCemdapOutputFile = 100;
	private static int numberOfPlans = 1;
	
	private static String tazShapeFile = "C:/Users/gthunig/SVN/shared-svn/projects/cemdapMatsimCadyts/scenario/shapefiles/gemeindenLOR_DHDN_GK4.shp";
	private static String cemdapDataRoot = "C:/Users/gthunig/CEMDAP/cemdap_output/";
	private static String outputRoot = "C:/Users/gthunig/SVN/shared-svn/projects/cemdapMatsimCadyts/scenario/cemdap2matsim/";
	
	public static void main(String[] args) {
		
		try {		

			convertData(outputRoot, 100, false);
			gunzipAll5Plans(outputRoot, 100);
			joinSplitDataFromOneRun(outputRoot, 100);
			
			convertData(outputRoot, 101, false);
			gunzipAll5Plans(outputRoot, 101);
			joinSplitDataFromOneRun(outputRoot, 101);
			
			convertData(outputRoot, 102, false);
			gunzipAll5Plans(outputRoot, 102);
			joinSplitDataFromOneRun(outputRoot, 102);
			
			convertData(outputRoot, 103, false);
			gunzipAll5Plans(outputRoot, 103);
			joinSplitDataFromOneRun(outputRoot, 103);
			
			convertData(outputRoot, 104, true);
			gunzipAll5Plans(outputRoot, 104);
			joinSplitDataFromOneRun(outputRoot, 104);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void convertData(String outputRoot, int numberOfFirstCemdapOutputFile, boolean addStayHomePlan) throws IOException {
		
		CemdapStops2MatsimPlansConverter converter = new CemdapStops2MatsimPlansConverter(tazShapeFile, cemdapDataRoot);
				
		converter.setNumberOfFirstCemdapOutputFile(numberOfFirstCemdapOutputFile);
		converter.setNumberOfPlans(numberOfPlans);
		converter.setAddStayHomePlan(addStayHomePlan);
		
		for (int i=1; i<=5; i++) {
			log.info("Start converting " + i);
			converter.setCemdapStopFilename("stops.out" + i);
			converter.setOutputDirectory(outputRoot + numberOfFirstCemdapOutputFile + "_" + i + "/");
			converter.convert();
		}
		
	}
	
	public static void gunzipAll5Plans(String outputRoot, int numberOfFirstCemdapOutputFile) {
		for (int i=1; i<=5; i++) {
			String rootDirectory = outputRoot + numberOfFirstCemdapOutputFile + "_" + i + "/";
			gunzipIt(rootDirectory + "plans.xml.gz", rootDirectory + "plans.xml");
		}
	}
	
	public static void joinSplitDataFromOneRun(String outputRoot, int numberOfFirstCemdapOutputFile) throws IOException {
		log.info("Start joining plans");
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputRoot + numberOfFirstCemdapOutputFile));
		for (int i=1; i <=5; i++) {
			log.info("joinSplitDataFromOneRun: Read " + numberOfFirstCemdapOutputFile + "_" + i);
			BufferedReader reader = new BufferedReader(new FileReader(outputRoot + numberOfFirstCemdapOutputFile + "_" + i + "/plans.xml"));
			if (i != 1) {
				//skip start
				while (!reader.readLine().equals("<population>"));
			}
			String line = reader.readLine();
			while (!line.equals("</population>")) {
				writer.write(line);
				writer.newLine();
				line = reader.readLine();
			}
			if (i == 5) {
				writer.write(line);
				writer.close();
			} else {
				writer.newLine();
			}
			reader.close();
		}
	}
	
	public static void joinAll5Plans(String[] planFileNames, String outputPlanFile) throws IOException {
		log.info("Start joining plan files");
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputPlanFile));
		
		BufferedReader[] readers = new BufferedReader[5];
		String[] currentLines = new String[5];
		for (int i=0; i < readers.length; i++) {
			readers[i] = new BufferedReader(new FileReader(planFileNames[i]));
			currentLines[i] = readers[i].readLine();
		}
		
		//copy start from one reader 	<- inside the loop
		//skip readers to first person 	<- inside the loop
		
		//loop: join person plans -> only one selected plan
		while (!currentLines[0].equals("</population>")) {
			
			//copy text from one reader until start of a person or end of file
			while (!currentLines[0].contains("person id=") && !currentLines[0].equals("</population>")) {
				writer.write(currentLines[0]);
				currentLines[0] = readers[0].readLine();
			}
			
			if (!currentLines[0].equals("</population>")) {
				
				//skip other readers until start of person
				for (int i = 1; i < readers.length; i++) {
					while (!currentLines[i].contains("person id=")) {
						currentLines[i] = readers[i].readLine();
					}
				}
				
				//copy start of person from one person
				writer.write(currentLines[0]);
				currentLines[0] = readers[0].readLine();
				
				//skip start of person at other readers
				for (int i = 1; i < readers.length; i++) {
					currentLines[i] = readers[i].readLine();
				}
				
				//copy start of selected plan from one reader (so it will not be deselected in the loop)
				writer.write(currentLines[0]);
				currentLines[0] = readers[0].readLine();
				
				//do for all readers
				for (int i = 0; i < readers.length; i++) {
					
					//copy lines from until end of person
					while (!currentLines[i].contains("</person>")) {
						
						//if another plan is selected, deselect it
						if (currentLines[i].contains("<plan selected=\"yes\">")) {
							currentLines[i] = currentLines[i].replaceAll("yes", "no");
						}
						
						writer.write(currentLines[i]);
						currentLines[i] = readers[i].readLine();
					}
				}
			}
			
		}
		
		//copy end from one reader 	<- inside the loop
		
		//close all readers
		for (BufferedReader reader : readers) {
			reader.close();
		}
		
		//close writer
		writer.close();
	}
	
	public static void isIdListSorted(ArrayList<String> ids) {
		String previousId = "0";
		boolean notSorted = false;
		int i = 0;
		for (String id : ids) {
			if (i < 20) {
				System.out.println(id);
				i++;
			}
			if (!greaterThan(id, previousId)) {
				System.out.println();
				System.out.println("Not sorted!!!");
				System.out.println("previousId = " + previousId);
				System.out.println("currentId = " + id);
				System.out.println();
				notSorted = true;
			}
			previousId = id;
		}
		System.out.println("End of List");
		System.out.println();
		if (notSorted) {
			System.out.println("Not Sorted");
		} else {
			System.out.println("Sorted");
		}
	}
	
	public static void areThereDublicants(ArrayList<String> ids) {
		ArrayList<String> duplicants = new ArrayList<>();
		for (int i=0; i<ids.size()-1; i++) {
			for (int i2=i+1; i2<ids.size(); i++) {
				if (ids.get(i).equals(ids.get(i2))) {
					duplicants.add(ids.get(i));
				}
			}
		}
		if (duplicants.isEmpty()) {
			System.out.println("No duplicants.");
		} else {
			System.out.println("There are duplicants.");
			for (String duplicant : duplicants) {
				System.out.println(duplicant);
			}
		}
	}
	
	public static boolean greaterThan(String x, String y) {
		long longX = Long.parseLong(x);
		long longY = Long.parseLong(y);
		int compare = Long.compare(longX, longY);
		if (compare > 0) {
			return true;
		} else {
			return false;
		}
	}
	
	public static ArrayList<String> listIds() throws IOException {
		log.info("Start listing ids");
		ArrayList<String> ids = new ArrayList<String>();
		for (int i=1; i <=5; i++) {
			String plansFile = outputRoot + numberOfFirstCemdapOutputFile + "_" + i + "/plans.xml";
			log.info("Trying to read " + plansFile);
			FileReader fileReader = new FileReader(plansFile);
			BufferedReader reader = new BufferedReader(fileReader);
			//skip start
			String line = reader.readLine();
			while (!line.equals("<population>")) {
				System.out.println(line);
				line = reader.readLine();
			};
			while (!line.equals("</population>")) {
				if (line.startsWith("<person id=")) {
					String id = line.substring(12, line.length()-2);
					System.out.println(id);
					ids.add(id);
				}
				line = reader.readLine();
			}
			reader.close();
		}
		return ids;
	}
	
	/**
     * GunZip it
     */
    public static void gunzipIt(String inputGzipFile, String outputFile){

     byte[] buffer = new byte[1024];

     try{

    	 log.info("Try to unzip " + inputGzipFile);
    	 
    	 GZIPInputStream gzis =
    		new GZIPInputStream(new FileInputStream(inputGzipFile));

    	 FileOutputStream out =
            new FileOutputStream(outputFile);

        int len;
        while ((len = gzis.read(buffer)) > 0) {
        	out.write(buffer, 0, len);
        }

        gzis.close();
    	out.close();

    	log.info("Unzip successful");
    	
    }catch(IOException ex){
       ex.printStackTrace();
    }
   }
}
