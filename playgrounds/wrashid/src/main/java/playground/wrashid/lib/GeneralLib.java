package playground.wrashid.lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.StringTokenizer;

import javax.xml.parsers.ParserConfigurationException;

import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.KmlType;
import net.opengis.kml._2.ObjectFactory;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.KmlNetworkWriter;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.CH1903LV03toWGS84;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.vis.kml.KMZWriter;
import org.xml.sax.SAXException;

import playground.wrashid.lib.obj.StringMatrix;
import playground.wrashid.lib.obj.list.Lists;

public class GeneralLib {

	/*
	 * Reads the population from the plans file.
	 * 
	 * Note: use the other method with the same name, if this poses problems.
	 */
	public static Scenario readScenario(String plansFile, String networkFile) {
		ScenarioImpl scenario = new ScenarioImpl();
		Population population = scenario.getPopulation();

		NetworkImpl network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkFile);

		PopulationReader popReader = new MatsimPopulationReader(scenario);
		popReader.readFile(plansFile);

		return scenario;
	}
	
	/*
	 * Reads the population from the plans file.
	 * 
	 */
	public static Scenario readScenario(String plansFile, String networkFile, String facilititiesPath) {
		ScenarioImpl sc = new ScenarioImpl();

		sc.getConfig().setParam("plans", "inputPlansFile", plansFile);
		sc.getConfig().setParam("network", "inputNetworkFile", networkFile);
		sc.getConfig().setParam("facilities", "inputFacilitiesFile", facilititiesPath);

		ScenarioLoaderImpl sl = new ScenarioLoaderImpl(sc);

		sl.loadScenario();

		return sc;
	}
	
	/*
	 * Reads the network from the network file.
	 */
	public static NetworkImpl readNetwork(String networkFile) {
		ScenarioImpl sc = new ScenarioImpl();

		sc.getConfig().setParam("network", "inputNetworkFile", networkFile);

		ScenarioLoaderImpl sl = new ScenarioLoaderImpl(sc);

		sl.loadScenario();

		return sc.getNetwork();
	}
	
	public static void writeNetwork(Network network, String outputNetworkFileName){
		new NetworkWriter(network).write(outputNetworkFileName);
	}
	

	/*
	 * Write the population to the specified file.
	 */
	public static void writePopulation(Population population, Network network, String plansFile) {
		MatsimWriter populationWriter = new PopulationWriter(population, network);

		populationWriter.write(plansFile);
	}

	/**
	 * @param facilitiesFile
	 * @return
	 */
	public static ActivityFacilitiesImpl readActivityFacilities(String facilitiesFile) {
		ScenarioImpl scenario = new ScenarioImpl();
		ActivityFacilitiesImpl facilities = scenario.getActivityFacilities();
		new MatsimFacilitiesReader(scenario).readFile(facilitiesFile);
		return facilities;
	}

	
	
	
	/*
	 * Write the facilities to the specified file.
	 */
	public static void writeActivityFacilities(ActivityFacilitiesImpl facilities, String facilitiesFile) {
		new FacilitiesWriter(facilities).write(facilitiesFile);
	}

	/**
	 * Write out a list of Strings
	 * 
	 * after each String in the list a "\n" is added.
	 * 
	 * @param list
	 * @param fileName
	 */
	public static void writeList(ArrayList<String> list, String fileName) {
		try {
			FileOutputStream fos = new FileOutputStream(fileName);
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fos);

			char[] charArray=Lists.getCharsOfAllArrayItemsWithNewLineCharacterInbetween(list);
			outputStreamWriter.write(charArray);			
			
			outputStreamWriter.flush();
			outputStreamWriter.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * TODO: write test!!!!
	 * 
	 * @param matrix
	 * @param numberOfRows
	 * @param numberOfColumns
	 * @return
	 */
	public static double[][] trimMatrix(double[][] matrix, int numberOfRows, int numberOfColumns) {
		double newMatrix[][] = new double[numberOfRows][numberOfColumns];

		for (int i = 0; i < numberOfRows; i++) {
			for (int j = 0; j < numberOfColumns; j++) {
				newMatrix[i][j] = matrix[i][j];
			}
		}

		return newMatrix;
	}

	/**
	 * if headerLine=null, then add no line at top of file. "\n" is added at end
	 * of first line by this method.
	 * 
	 * matrix[numberOfRows][numberOfColumns]
	 * 
	 * @param matrix
	 * @param fileName
	 * @param headerLine
	 */
	public static void writeMatrix(double[][] matrix, String fileName, String headerLine) {
		ArrayList<String> list = new ArrayList<String>();

		if (headerLine != null) {
			list.add(headerLine);
		}

		for (int i = 0; i < matrix.length; i++) {
			String line = "";
			for (int j = 0; j < matrix[0].length - 1; j++) {
				line += matrix[i][j];
				line += "\t";
			}
			line += matrix[i][matrix[0].length - 1];
			list.add(line);
		}

		writeList(list, fileName);
	}

	/**
	 * reads in data from a file.
	 * 
	 * 
	 * @param numberOfRows
	 * @param numberOfColumns
	 * @param ignoreFirstLine
	 * @return
	 */
	public static double[][] readMatrix(int numberOfRows, int numberOfColumns, boolean ignoreFirstLine, String fileName) {

		double[][] matrix = new double[numberOfRows][numberOfColumns];

		try {

			FileReader fr = new FileReader(fileName);

			BufferedReader br = new BufferedReader(fr);
			String line;
			StringTokenizer tokenizer;
			String token;

			if (ignoreFirstLine) {
				br.readLine();
			}

			line = br.readLine();
			int rowId = 0;
			while (line != null) {
				tokenizer = new StringTokenizer(line);

				for (int i = 0; i < numberOfColumns; i++) {
					token = tokenizer.nextToken();
					double parsedNumber = Double.parseDouble(token);
					matrix[rowId][i] = parsedNumber;
				}

				if (tokenizer.hasMoreTokens()) {
					// if there are more columns than expected, throw an
					// exception
					throw new RuntimeException("the number of columns is wrong");
				}

				line = br.readLine();
				rowId++;
			}
			if (rowId != numberOfRows) {
				throw new RuntimeException("the number of rows is wrong");
			}

		} catch (RuntimeException e) {
			// just forward the runtime exception
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			
			
			throw new RuntimeException("Error reading the matrix from the file");
		}

		return matrix;
	}

	public static double[][] invertMatrix(double[][] matrix) {
		int firstDimentionOfResultMatrix = matrix[0].length;
		int secondDimentionOfResultMatrix = matrix.length;

		double[][] resultMatrix = new double[firstDimentionOfResultMatrix][secondDimentionOfResultMatrix];

		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix[0].length; j++) {
				resultMatrix[j][i] = matrix[i][j];
			}
		}

		return resultMatrix;
	}

	/**
	 * TODO This method
	 * 
	 * @param fileName
	 * @return
	 */
	public static String getFirstLineOfFile(String fileName) {
		return null;
	}

	// TODO: write tests.
	// create target directory, if it does not exist.
	public static void copyDirectory(String sourceDirectoryPath, String targetDirectoryPath) {

		File sourceDirectory = new File(sourceDirectoryPath);
		File targetDirectory = new File(targetDirectoryPath);

		try {
			if (sourceDirectory.isDirectory()) {
				// do recursion
				if (!targetDirectory.exists()) {
					targetDirectory.mkdir();
				}

				String[] children = sourceDirectory.list();
				for (int i = 0; i < children.length; i++) {
					copyDirectory(sourceDirectoryPath + "\\" + children[i], targetDirectory + "\\" + children[i]);
				}
			} else {

				// copy the file
				copyFile(sourceDirectory.getAbsolutePath(), targetDirectory.getAbsolutePath());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * TODO: write test.
	 * 
	 * @param sourceFilePath
	 * @param targetFilePath
	 */
	public static void copyFile(String sourceFilePath, String targetFilePath) {
		File sourceFile = new File(sourceFilePath);
		File targetFile = new File(targetFilePath);

		try {
			// copy the file
			InputStream in = new FileInputStream(sourceFile);
			OutputStream out = new FileOutputStream(targetFile);
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			in.close();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		copyDirectory("C:\\tmp\\abcd", "C:\\tmp\\aaab");
	}

	public static double[][] initializeMatrix(double[][] matrix) {
		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix[0].length; j++) {
				matrix[i][j] = 0;
			}
		}
		return matrix;
	}

	/**
	 * energyUsageStatistics[number of values][number of functions]
	 * 
	 * 
	 * @param fileName
	 * @param matrix
	 * @param title
	 * @param xLabel
	 * @param yLabel
	 */
	public static void writeHubGraphic(String fileName, double[][] matrix, String title, String xLabel, String yLabel) {
		int numberOfXValues = matrix.length;
		int numberOfFunctions = matrix[0].length;
		String[] seriesLabels= new String[numberOfFunctions];

		double[] time = new double[numberOfXValues];

		for (int i = 0; i < numberOfXValues; i++) {
			time[i] = i * 900;
		}

		for (int i = 0; i < numberOfFunctions; i++) {
			seriesLabels[i]= "hub-" + i;
		}

		writeGraphic(fileName, matrix, title, xLabel, yLabel, seriesLabels, time);
	}
	
	public static void writeGraphic(String fileName, double[][] matrix, String title, String xLabel, String yLabel, String[] seriesLabels, double[] xValues){
		XYLineChart chart = new XYLineChart(title, xLabel, yLabel);

		int numberOfXValues = matrix.length;
		int numberOfFunctions = matrix[0].length;

		for (int i = 0; i < numberOfFunctions; i++) {
			double[] series = new double[numberOfXValues];
			for (int j = 0; j < numberOfXValues; j++) {
				series[j] = matrix[j][i];
			}
			chart.addSeries(seriesLabels[i], xValues, series);
		}

		// chart.addMatsimLogo();
		chart.saveAsPng(fileName, 800, 600);
		
		
		if (GlobalRegistry.doPrintGraficDataToConsole){
			printGraphicDataToConsole(fileName, matrix, title, xLabel, yLabel, seriesLabels, xValues);
		}
	}
	
	public static void printGraphicDataToConsole(String fileName, double[][] matrix, String title, String xLabel, String yLabel, String[] seriesLabels, double[] xValues){
		int numberOfXValues = matrix.length;
		int numberOfFunctions = matrix[0].length;
		
		System.out.println("===================================================");
		
		System.out.println("title:" + title);
		System.out.println("xLabel:" + xLabel);
		System.out.println("yLabel:" + yLabel);
		
		System.out.println("----------------------------------------------------");
		
		System.out.print("xValues");
		
		for (int i=0;i<numberOfFunctions;i++){
			System.out.print("\t"+seriesLabels[i]);
		}
		
		System.out.print("\n");
		System.out.println("----------------------------------------------------");
		
		for (int i = 0; i < numberOfXValues; i++) {
			System.out.print(xValues[i]);
			
			for (int j = 0; j < numberOfFunctions; j++) {
				System.out.print("\t"+matrix[i][j]);
			}
			System.out.print("\n");
		}
		
		
		System.out.println("==================================================");
	}
	

	public static double[][] scaleMatrix(double[][] matrix, double scalingFactor) {
		int numberOfRows = matrix.length;
		int numberOfColumns = matrix[0].length;

		double[][] resultMatrix = new double[numberOfRows][numberOfColumns];

		for (int i = 0; i < numberOfRows; i++) {
			for (int j = 0; j < numberOfColumns; j++) {
				resultMatrix[i][j] = matrix[i][j] * scalingFactor;
			}
		}

		return resultMatrix;
	}

	/**
	 * If time is > 60*60*24 [seconds], it will be projected into next day, e.g.
	 * time=60*60*24+1=1
	 * 
	 * even if time is negative, it is turned into a positive time by adding
	 * number of seconds of day into it consecutively
	 * 
	 * @param time
	 * @return
	 */
	public static double projectTimeWithin24Hours(double time) {
		double secondsInOneDay = 60 * 60 * 24;

		while (time < 0) {
			time += secondsInOneDay;
		}

		if (time < secondsInOneDay) {
			return time;
		} else {
			return ((time / secondsInOneDay) - (Math.floor(time / secondsInOneDay))) * secondsInOneDay;
		}
	}

	/**
	 * 24 hour check is performed (no projection required as pre-requisite).
	 * 
	 * @param startIntervalTime
	 * @param endIntervalTime
	 * @return
	 */
	public static double getIntervalDuration(double startIntervalTime, double endIntervalTime) {
		double secondsInOneDay = 60 * 60 * 24;
		startIntervalTime = projectTimeWithin24Hours(startIntervalTime);
		endIntervalTime = projectTimeWithin24Hours(endIntervalTime);

		if (startIntervalTime < endIntervalTime) {
			return endIntervalTime - startIntervalTime;
		} else {
			return endIntervalTime + (secondsInOneDay - startIntervalTime);
		}

	}

	/**
	 * Interval start and end are inclusive.
	 * 
	 * @param startIntervalTime
	 * @param endIntervalTime
	 * @param timeToCheck
	 * @return
	 */
	public static boolean isIn24HourInterval(double startIntervalTime, double endIntervalTime, double timeToCheck) {
		errorIfNot24HourProjectedTime(startIntervalTime);
		errorIfNot24HourProjectedTime(endIntervalTime);
		errorIfNot24HourProjectedTime(timeToCheck);

		if (startIntervalTime < endIntervalTime && timeToCheck >= startIntervalTime && timeToCheck <= endIntervalTime) {
			return true;
		}

		if (startIntervalTime > endIntervalTime && (timeToCheck >= startIntervalTime || timeToCheck <= endIntervalTime)) {
			return true;
		}

		return false;
	}

	public static void errorIfNot24HourProjectedTime(double time) {
		double secondsInOneDay = 60 * 60 * 24;

		if (time >= secondsInOneDay) {
			throw new Error("time not projected within 24 hours!");
		}
	}

	public static double getDistance(Coord coord, Link link) {
		return GeneralLib.getDistance(coord, link.getCoord());
	}

	public static double getDistance(Coord coordA, Coord coordB) {
		double xDiff = coordA.getX() - coordB.getX();
		double yDiff = coordA.getY() - coordB.getY();
		return Math.sqrt(xDiff * xDiff + yDiff * yDiff);
	}

	/**
	 * TODO: write test.
	 * 
	 * @param persons
	 * @param outputPlansFileName
	 * @param network
	 */
	public static void writePersons(Collection<? extends Person> persons, String outputPlansFileName, NetworkImpl network) {
		PopulationWriter popWriter = new PopulationWriter(new PopulationImpl(null), network);
		popWriter.writeStartPlans(outputPlansFileName);

		for (Person person : persons) {
			popWriter.writePerson(person);
		}

		popWriter.writeEndPlans();
	}

	/**
	 * copy the person and the selected plan of the person
	 * @param person
	 * @return
	 */
	public static Person copyPerson(Person person) {
		PersonImpl newPerson=new PersonImpl(person.getId());
		PlanImpl newPlan=new PlanImpl();
		newPlan.copyPlan(person.getSelectedPlan());
		newPlan.setPerson(newPerson);
		newPerson.addPlan(newPlan);
		newPerson.setSelectedPlan(newPlan);
		newPerson.removeUnselectedPlans();
		return newPerson;
	}
	
	
	public static void convertMATSimNetworkToKmz(String matsimNetworkFileName, String outputKmzFileName) throws IOException {
		Network network = readNetwork(matsimNetworkFileName);

		ObjectFactory kmlObjectFactory = new ObjectFactory();
		KMZWriter kmzWriter = new KMZWriter(outputKmzFileName);
		
		KmlType mainKml = kmlObjectFactory.createKmlType();
		DocumentType mainDoc = kmlObjectFactory.createDocumentType();
		mainKml.setAbstractFeatureGroup(kmlObjectFactory.createDocument(mainDoc));
		
		//KmlNetworkWriter kmlNetworkWriter = new KmlNetworkWriter(network, new AtlantisToWGS84(), kmzWriter, mainDoc);
		KmlNetworkWriter kmlNetworkWriter = new KmlNetworkWriter(network,new CH1903LV03toWGS84(), kmzWriter, mainDoc);
		//KmlNetworkWriter kmlNetworkWriter = new KmlNetworkWriter(network,TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84_UTM35S, TransformationFactory.WGS84), kmzWriter, mainDoc);
		
		mainDoc.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(kmlNetworkWriter.getNetworkFolder()));
		
		kmzWriter.writeMainKml(mainKml);
		kmzWriter.close();
	}
	
	public static Network convertOsmNetworkToMATSimNetwork(String osmNetworkFile) throws SAXException, ParserConfigurationException, IOException{
		Scenario sc = new ScenarioImpl();
		Network net = sc.getNetwork();

		CoordinateTransformation ct = new WGS84toCH1903LV03();

		OsmNetworkReader reader = new OsmNetworkReader(net,ct);
		reader.setKeepPaths(true);
		reader.parse(osmNetworkFile);

		new NetworkCleaner().run(net);
		return net;
	}
	
	//TODO: there are some classes (e.g. playground.wrashid.PSF.data.HubLinkMapping and HubPriceInfo, which could
	// be refactored by calling this method.
	public static StringMatrix readStringMatrix(String fileName){
		StringMatrix matrix=new StringMatrix();
		
		try {
			
			FileReader fr = new FileReader(fileName);
			
			BufferedReader br = new BufferedReader(fr);
			String line;
			StringTokenizer tokenizer;
			line = br.readLine();
			while (line != null) {
				LinkedList<String> row=new LinkedList<String>();
				
				tokenizer = new StringTokenizer(line);
				
				while (tokenizer.hasMoreTokens()){
					row.add(tokenizer.nextToken());
				}
				
				matrix.addRow(row);
				
				line = br.readLine();
			}
		} catch (Exception e){
			e.printStackTrace();
			
			throw new Error("Error reading the file: " + fileName);
		}		
				
		
		return matrix;
	}
		
	/**
	 * TODO: move method to approporaite place where the data structures are located.
	 * @param hm
	 */
	public static void printHashmapToConsole(HashMap hm){
		for (Object key:hm.keySet()){
			System.out.println(key.toString() +"\t"+hm.get(key));
		}	
	}
	
}
