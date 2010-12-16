package playground.andreas.fggeoinfo;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.andreas.utils.ana.filterActsPerShape.FilterActsPerShape;

public class FGGeoInfo {

	/**
	 * Generate demand for geoinfo project
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		
		Gbl.startMeasurement();
		
		String inputDir = "d:\\Berlin\\berlin-fggeoinfo\\30_Run_20_percent\\20101214_run793_794\\";
		String networkFile = inputDir + "network_modified_20100806_added_BBI_AS_cl.xml.gz";
		String inPlansFile = "d:\\Berlin\\berlin-sharedsvn\\plans\\baseplan_10x_900s.xml.gz";
		
		double scaleFactor = 0.20; // 20%
		String bbiDemandInFile = inputDir + "Anreise_Autobahnauffahrten_20100804.csv";
		String timeStructure = inputDir + "Analyse_Fluege_20100804.csv";
		String oldDemandTXLSXFoutFile = "oldDemandTXLSXF.xml.gz";
		String newDemandBBIoutFile = "newDemandBBI.xml.gz";
		
		String movedBackgroundDemand = "movedBGdemand.xml.gz";
		String shapeFile = "d:/Berlin/berlin-fggeoinfo/10_Eingangsdaten/20101007_bezirke_GK4/Bezirke_Polygon_GK4.shp";
		String actTypeOne = "work";
		String actTypeTwo = "home";
		String resultsOutFile = "workHomeStats.txt";
		
		Coord coordBBI = new CoordImpl(4604545.48760, 5805194.68221);
		Coord coordTXL = new CoordImpl(4588068.19422, 5824668.31998);
		Coord coordSXF = new CoordImpl(4603377.91673, 5807538.81303);
		
		Coord minSXF = new CoordImpl(4602500.000, 5806200.000);
		Coord maxSXF = new CoordImpl(4604000.000, 5807400.000);
				
		Coord minTXL = new CoordImpl(4586900.000, 5824500.000);
		Coord maxTXL = new CoordImpl(4588800.000, 5826300.000);
		
		// Old coordinates
//		Coord minSXF = new CoordImpl(4599269.481, 5801657.79);
//		Coord maxSXF = new CoordImpl(4606127.877, 5807724.832);
		
//		Coord minTXL = new CoordImpl(4585293.203, 5824791.757);
//		Coord maxTXL = new CoordImpl(4589791.816, 5826794.636);
		
		// Generate population from passengers
		BBIextraDemand bbi = new BBIextraDemand(bbiDemandInFile, timeStructure,
				inputDir + oldDemandTXLSXFoutFile, inputDir + newDemandBBIoutFile,
				scaleFactor, coordBBI, coordTXL, coordSXF);
		bbi.createDemand();
		
		// Merge old demand with unchanged background demand
		MergePopulations.mergePopulations(networkFile, inPlansFile, inputDir + oldDemandTXLSXFoutFile,
				inputDir + "completeOldDemandTXLSXF.xml.gz");
		
		// Move background demand towards BBI and merge it with new demand
		FilterPersonActs.filterPersonActs(networkFile, inPlansFile, inputDir + movedBackgroundDemand,
				minSXF, maxSXF, minTXL, maxTXL, coordBBI, inputDir, "movedActs.kmz");		
		MergePopulations.mergePopulations(networkFile, inputDir + movedBackgroundDemand, inputDir + newDemandBBIoutFile,
				inputDir + "completeNewDemandBBI.xml.gz");		
		
		// Filter acts based on location - where do employees of BBI live
		FilterActsPerShape.run(networkFile, inputDir + "completeNewDemandBBI.xml.gz", shapeFile, coordBBI, coordBBI, actTypeOne, actTypeTwo, inputDir + resultsOutFile);
		
		Gbl.printElapsedTime();
	}

}
