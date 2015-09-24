/**
 * 
 */
package playground.dgrether.koehlerstrehlersignal.conversion;

import java.io.File;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.Time;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.koehlerstrehlersignal.demand.PopulationToOd;
import playground.dgrether.koehlerstrehlersignal.demand.ZoneBuilder;
import playground.dgrether.koehlerstrehlersignal.network.NetLanesSignalsShrinker;
import playground.dgrether.koehlerstrehlersignal.run.Cottbus2KS2010;
import playground.dgrether.signalsystems.utils.DgScenarioUtils;
import playground.dgrether.utils.zones.DgZones;

/**
 * Class to convert a MATSim scenario into KS format.
 * 
 * @author tthunig
 */
public class TtMatsim2KS2015 {

	private static final Logger log = Logger.getLogger(Cottbus2KS2010.class);

	private static final String shapeFileDirectoryName = "shapes/";
	
	private static final CoordinateReferenceSystem CRS = MGC
			.getCRS(TransformationFactory.WGS84_UTM33N);	
	
	/**
	 * Runs the conversion process and writes the BTU output file.
	 * 
	 * @param signalSystemsFilename
	 * @param signalGroupsFilename
	 * @param signalControlFilename
	 * @param networkFilename
	 * @param lanesFilename
	 * @param populationFilename
	 * @param startTime
	 * @param endTime
	 * @param signalsBoundingBoxOffset outside this envelope all crossings stay unexpanded
	 * @param cuttingBoundingBoxOffset
	 * @param freeSpeedFilter the minimal free speed value for the interior link filter in m/s
	 * @param useFreeSpeedTravelTime a flag for dijkstras cost function: if true, dijkstra will use the free speed travel time, if false, dijkstra will use the travel distance as cost function
	 * @param maximalLinkLength restricts the NetworkSimplifier. Double.MAX_VALUE is no restriction.
	 * @param matsimPopSampleSize 1.0 means a 100% sample
	 * @param ksModelCommoditySampleSize 1.0 means that 1 vehicle is equivalent to 1 unit of flow
	 * @param minCommodityFlow only commodities with at least this demand will be optimized in the BTU model
	 * @param simplifyNetwork use network simplifier if true
	 * @param cellsX number of cells in x direction
	 * @param cellsY number of cells in y direction
	 * @param scenarioDescription
	 * @param dateFormat
	 * @param outputDirectory
	 * @throws Exception
	 */
	public static void convertMatsim2KS(String signalSystemsFilename,
			String signalGroupsFilename, String signalControlFilename,
			String networkFilename, String lanesFilename,
			String populationFilename, double startTime, double endTime,
			double signalsBoundingBoxOffset, double cuttingBoundingBoxOffset,
			double freeSpeedFilter, boolean useFreeSpeedTravelTime,
			double maximalLinkLength, double matsimPopSampleSize,
			double ksModelCommoditySampleSize, double minCommodityFlow,
			boolean simplifyNetwork,
			int cellsX, int cellsY, String scenarioDescription,
			String dateFormat, String outputDirectory) throws Exception{
		
		// init some variables
		String spCost = "tt";
		if (!useFreeSpeedTravelTime) spCost = "dist";
		outputDirectory += dateFormat + "_minflow_" + minCommodityFlow + "_time" 
				+ startTime + "-" + endTime + "_speedFilter" + freeSpeedFilter + "_SP_" + spCost 
				+ "_cBB" + cuttingBoundingBoxOffset + "_sBB" + signalsBoundingBoxOffset + "/";
		String ksModelOutputFilename = "ks2010_model_" + Double.toString(minCommodityFlow) + "_"
				+ Double.toString(startTime) + "_" + Double.toString(cuttingBoundingBoxOffset) + ".xml";
		
		// run
		OutputDirectoryLogging.initLoggingWithOutputDirectory(outputDirectory);
		String shapeFileDirectory = createShapeFileDirectory(outputDirectory);
		Scenario fullScenario = DgScenarioUtils.loadScenario(networkFilename,
				populationFilename, lanesFilename, signalSystemsFilename,
				signalGroupsFilename, signalControlFilename);

		// reduce the size of the scenario
		NetLanesSignalsShrinker scenarioShrinker = new NetLanesSignalsShrinker(
				fullScenario, CRS);
		scenarioShrinker.shrinkScenario(outputDirectory, shapeFileDirectory,
				cuttingBoundingBoxOffset, freeSpeedFilter,
				useFreeSpeedTravelTime, maximalLinkLength, simplifyNetwork);

		// create the geometry for zones. The geometry itself is not used, but
		// the object serves as container for the link -> link OD pairs
		ZoneBuilder zoneBuilder = new ZoneBuilder(CRS);
		DgZones zones = zoneBuilder.createAndWriteZones(
				scenarioShrinker.getShrinkedNetwork(),
				scenarioShrinker.getCuttingBoundingBox(), cellsX, cellsY,
				shapeFileDirectory);

		// match population to the small network and convert to od, results are
		// stored in the DgZones object
		PopulationToOd pop2od = new PopulationToOd();
		pop2od.setMatsimPopSampleSize(matsimPopSampleSize);
		pop2od.setOriginalToSimplifiedLinkMapping(scenarioShrinker
				.getOriginalToSimplifiedLinkIdMatching());
		pop2od.convertPopulation2OdPairs(zones, fullScenario.getNetwork(),
				fullScenario.getPopulation(), CRS,
				scenarioShrinker.getShrinkedNetwork(),
				scenarioShrinker.getCuttingBoundingBox(), startTime, endTime,
				shapeFileDirectory);

		// convert to KoehlerStrehler2010 file format
		M2KS2010Converter converter = new M2KS2010Converter(
				scenarioShrinker.getShrinkedNetwork(),
				scenarioShrinker.getShrinkedLanes(),
				scenarioShrinker.getShrinkedSignals(),
				signalsBoundingBoxOffset, CRS);
		String description = createDescription(cellsX, cellsY, startTime,
				endTime, cuttingBoundingBoxOffset, matsimPopSampleSize,
				ksModelCommoditySampleSize, minCommodityFlow);
		converter.setKsModelCommoditySampleSize(ksModelCommoditySampleSize);
		converter.setMinCommodityFlow(minCommodityFlow);
		converter.convertAndWrite(outputDirectory, shapeFileDirectory,
				ksModelOutputFilename, scenarioDescription, description, zones, startTime,
				endTime);

		printStatistics(cellsX, cellsY, cuttingBoundingBoxOffset, startTime,
				endTime);
		log.info("output ist written to " + outputDirectory);
		OutputDirectoryLogging.closeOutputDirLogging();
	}
	
	private static String createDescription(int cellsX, int cellsY,
			double startTime, double endTime, double boundingBoxOffset,
			double matsimPopSampleSize, double ksModelCommoditySampleSize,
			double minCommodityFlow) {
		String description = "offset: " + boundingBoxOffset + " cellsX: "
				+ cellsX + " cellsY: " + cellsY + " startTimeSec: " + startTime
				+ " endTimeSec: " + endTime;
		description += " matsimPopsampleSize: " + matsimPopSampleSize
				+ " ksModelCommoditySampleSize: " + ksModelCommoditySampleSize;
		description += " minimum flow of commodities to be included in conversion: "
				+ minCommodityFlow;
		return description;
	}

	private static void printStatistics(int cellsX, int cellsY,
			double boundingBoxOffset, double startTime, double endTime) {
		log.info("Number of Cells:");
		log.info("  X " + cellsX + " Y " + cellsY);
		log.info("Bounding Box: ");
		log.info("  Offset: " + boundingBoxOffset);
		log.info("Time: ");
		log.info("  startTime: " + startTime + " " + Time.writeTime(startTime));
		log.info("  endTime: " + endTime + " " + Time.writeTime(endTime));
	}

	private static String createShapeFileDirectory(String outputDirectory) {
		String shapeDir = outputDirectory + shapeFileDirectoryName;
		File outdir = new File(shapeDir);
		outdir.mkdir();
		return shapeDir;
	}
}
