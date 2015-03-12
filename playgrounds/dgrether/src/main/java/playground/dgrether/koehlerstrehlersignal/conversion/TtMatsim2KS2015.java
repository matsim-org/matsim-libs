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

	// input files
	private String signalSystemsFilename;
	private String signalGroupsFilename;
	private String signalControlFilename;
	
	private String networkFilename;
	private String lanesFilename;
	private String populationFilename;

	// conversion properties
	
	/* parameters for the time interval*/
	private double startTime = 0.0 * 3600.0;
	private double endTime = 23.99 * 3600.0;
	/* parameters for the network area */
	private double signalsBoundingBoxOffset = Double.MAX_VALUE; // outside this envelope all crossings stay unexpanded
	private double cuttingBoundingBoxOffset = Double.MAX_VALUE;
	/* parameters for the interior link filter */
	private double freeSpeedFilter = 1.0; // the minimal free speed value for the interior link filter in m/s
	private boolean useFreeSpeedTravelTime = true; // a flag for dijkstras cost function: if true, dijkstra will use the free speed travel time, if false, dijkstra will use the travel distance as cost function
	private double maximalLinkLength = Double.MAX_VALUE; // restricts the NetworkSimplifier. Double.MAX_VALUE is no restriction.
	/* parameters for the demand filter */
	private double matsimPopSampleSize = 1.0; // 100% sample
	private double ksModelCommoditySampleSize = 1.0; // 1 vehicle is equivalent to 1 unit of flow
	private double minCommodityFlow = 1.0; // only commodities with at least this demand will be optimized in the BTU model
	private int cellsX = 5; // number of cells in x direction
	private int cellsY = 5;	// number of cells in y direction
	/* other parameters */
	private String scenarioDescription;
	private String spCost;
	
	// output
	private String dateFormat; // e.g. 2015-02-24
	private String outputDirectory;
	private String ksModelOutputFilename;
	
	private static String shapeFileDirectoryName = "shapes/";
	
	private static final CoordinateReferenceSystem CRS = MGC
			.getCRS(TransformationFactory.WGS84_UTM33N);
	
	
	/* --------------------- constructors -----------------------------*/
	
	/**
	 * constructor using some fields
	 * 
	 * @param signalSystemsFilename
	 * @param signalGroupsFilename
	 * @param signalControlFilename
	 * @param networkFilename
	 * @param lanesFilename
	 * @param populationFilename
	 * @param startTime the start time of the simulation
	 * @param endTime the end time of the simulation
	 * @param scenarioDescription the description for the ks model
	 * @param outputDirectory
	 * @param dateFormat the date in format 2015-02-24
	 */
	public TtMatsim2KS2015(String signalSystemsFilename,
			String signalGroupsFilename, String signalControlFilename,
			String networkFilename, String lanesFilename,
			String populationFilename, double startTime, double endTime,
			String scenarioDescription, String outputDirectory, String dateFormat) {
		
		super();
		this.signalSystemsFilename = signalSystemsFilename;
		this.signalGroupsFilename = signalGroupsFilename;
		this.signalControlFilename = signalControlFilename;
		this.networkFilename = networkFilename;
		this.lanesFilename = lanesFilename;
		this.populationFilename = populationFilename;
		this.startTime = startTime;
		this.endTime = endTime;
		this.scenarioDescription = scenarioDescription;
		this.outputDirectory = outputDirectory;
		this.dateFormat = dateFormat;
	}
	
	/**
	 * constructor using a lot of fields
	 * 
	 * @param signalSystemsFilename
	 * @param signalGroupsFilename
	 * @param signalControlFilename
	 * @param networkFilename
	 * @param lanesFilename
	 * @param populationFilename
	 * @param startTime the start time of the simulation
	 * @param endTime the end time of the simulation
	 * @param signalsBoundingBoxOffset crossings outside the envelop including all 
	 * signals plus this offset will not be expanded in the KS model
	 * @param cuttingBoundingBoxOffset links outside the envelop including all signals
	 * plus this offset will not be contained in the KS model
	 * @param freeSpeedFilter the minimal free speed value for the interior link filter in m/s
	 * (links with this free speed won't be removed by the interior link filter)
	 * @param useFreeSpeedTravelTime if true, the interior link filter won't remove 
	 * links lying on shortest path between signals regarding travel time, if false, it
	 * won't remove links lying on shortest path regarding distance
	 * @param minCommodityFlow commodities with a lower flow value are skipped in the KS model
	 * @param scenarioDescription the description for the ks model
	 * @param outputDirectory
	 * @param dateFormat the date in format 2015-02-24
	 */
	public TtMatsim2KS2015(String signalSystemsFilename,
			String signalGroupsFilename, String signalControlFilename,
			String networkFilename, String lanesFilename,
			String populationFilename, double startTime, double endTime,
			double signalsBoundingBoxOffset, double cuttingBoundingBoxOffset,
			double freeSpeedFilter, boolean useFreeSpeedTravelTime,
			double minCommodityFlow, String scenarioDescription, 
			String outputDirectory, String dateFormat) {
		
		this(signalSystemsFilename, signalGroupsFilename, signalControlFilename, networkFilename, 
				lanesFilename, populationFilename, startTime, endTime, scenarioDescription, outputDirectory, dateFormat);
		
		this.signalsBoundingBoxOffset = signalsBoundingBoxOffset;
		this.cuttingBoundingBoxOffset = cuttingBoundingBoxOffset;
		this.freeSpeedFilter = freeSpeedFilter;
		this.useFreeSpeedTravelTime = useFreeSpeedTravelTime;
		this.minCommodityFlow = minCommodityFlow;
	}
	
	/**
	 * constructor using all fields
	 * 
	 * @param signalSystemsFilename
	 * @param signalGroupsFilename
	 * @param signalControlFilename
	 * @param networkFilename
	 * @param lanesFilename
	 * @param populationFilename
	 * @param startTime the start time of the simulation
	 * @param endTime the end time of the simulation
	 * @param signalsBoundingBoxOffset crossings outside the envelop including all 
	 * signals plus this offset will not be expanded in the KS model
	 * @param cuttingBoundingBoxOffset links outside the envelop including all signals
	 * plus this offset will not be contained in the KS model
	 * @param freeSpeedFilter the minimal free speed value for the interior link filter in m/s
	 * (links with this free speed won't be removed by the interior link filter)
	 * @param useFreeSpeedTravelTime if true, the interior link filter won't remove 
	 * links lying on shortest path between signals regarding travel time, if false, it
	 * won't remove links lying on shortest path regarding distance
	 * @param maximalLinkLength
	 * @param matsimPopSampleSize
	 * @param ksModelCommoditySampleSize
	 * @param minCommodityFlow commodities with a lower flow value are skipped in the KS model
	 * @param cellsX
	 * @param cellsY
	 * @param scenarioDescription the description for the ks model
	 * @param outputDirectory
	 * @param dateFormat the date in format 2015-02-24
	 */
	public TtMatsim2KS2015(String signalSystemsFilename,
			String signalGroupsFilename, String signalControlFilename,
			String networkFilename, String lanesFilename,
			String populationFilename, double startTime, double endTime,
			double signalsBoundingBoxOffset, double cuttingBoundingBoxOffset,
			double freeSpeedFilter, boolean useFreeSpeedTravelTime,
			double maximalLinkLength, double matsimPopSampleSize,
			double ksModelCommoditySampleSize, double minCommodityFlow,
			int cellsX, int cellsY, String scenarioDescription,
			String dateFormat, String outputDirectory) {

		this(signalSystemsFilename, signalGroupsFilename, signalControlFilename, networkFilename, 
				lanesFilename, populationFilename, startTime, endTime, signalsBoundingBoxOffset, 
				cuttingBoundingBoxOffset, freeSpeedFilter, useFreeSpeedTravelTime,
				minCommodityFlow, scenarioDescription, outputDirectory, dateFormat);
		
		this.maximalLinkLength = maximalLinkLength;
		this.matsimPopSampleSize = matsimPopSampleSize;
		this.ksModelCommoditySampleSize = ksModelCommoditySampleSize;
		this.cellsX = cellsX;
		this.cellsY = cellsY;
	}

	/* -------------------------------------------------------------- */

	
	/**
	 * Initializes the fields that can be set automatically.
	 */
	private void init(){
		spCost = "tt";
		if (!useFreeSpeedTravelTime) spCost = "dist";
		outputDirectory += dateFormat + "_minflow_" + minCommodityFlow + "_morning_peak_speedFilter" + freeSpeedFilter + "_SP_" + spCost 
				+ "_cBB" + cuttingBoundingBoxOffset + "_sBB" + signalsBoundingBoxOffset + "/";
		ksModelOutputFilename = "ks2010_model_" + Double.toString(minCommodityFlow) + "_"
				+ Double.toString(startTime) + "_" + Double.toString(cuttingBoundingBoxOffset) + ".xml";
	}	
	
	/**
	 * Runs the conversion process and writes the BTU output file.
	 * 
	 * @throws Exception 
	 */
	public void convertMatsim2KS() throws Exception{
		
		init();
		
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
				useFreeSpeedTravelTime, maximalLinkLength);

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
