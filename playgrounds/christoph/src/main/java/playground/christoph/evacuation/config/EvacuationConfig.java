package playground.christoph.evacuation.config;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.christoph.evacuation.router.util.DistanceFuzzyFactorProviderFactory;

public class EvacuationConfig {
	
	private static final Logger log = Logger.getLogger(EvacuationConfig.class);
	
	public static double evacuationTime = 3600 * 8.0;
	
	public static double innerRadius = 30000.0;
	public static double outerRadius = 30500.0;
	
//	public static Coord centerCoord = new CoordImpl("683518.0","246836.0");	// Bellevue Coord
	public static Coord centerCoord = new CoordImpl("640050.0", "246256.0");	// Coordinates of KKW Goesgen
	
	public static String dhm25File = "../../matsim/mysimulations/networks/GIS/nodes_3d_dhm25.shp";
	public static String srtmFile = "../../matsim/mysimulations/networks/GIS/nodes_3d_srtm.shp";
	
	public static String householdObjectAttributesFile = "";
	
	public static List<String> evacuationArea = new ArrayList<String>();
	
	public static List<String> vehicleFleet = new ArrayList<String>();

	public static double affectedAreaTimePenaltyFactor = 1.20;
	public static double affectedAreaDistanceBuffer = 2500.0;

	public static double ptTravelTimePenaltyFactor = Double.MAX_VALUE;
	
	public static double panicShare = 0.00;
	public static double compassProbability = 0.667;
	public static boolean tabuSearch = true;

	public static double householdParticipationShare = 1.00;
	
	public static double duringLegReroutingShare = 1.00;
	
	public static enum PickupAgentBehaviour {ALWAYS, NEVER, MODEL}; 
	public static PickupAgentBehaviour pickupAgents = PickupAgentBehaviour.NEVER;
	
	public static boolean useFuzzyTravelTimes = true;
	
	public static double informAgentsRayleighSigma = 300.0;
	
	public static double capacityFactor = 1.0;
	public static double speedFactor = 1.0;
	
	/*
	 * Analysis modules
	 */
	public static boolean createEvacuationTimePicture = true;
	public static boolean countAgentsInEvacuationArea = true;
	
	/*
	 * survey based model parameter
	 */	
	public static double pickupModelAlwaysConst = 2.67;
	public static double pickupModelAlwaysAge31to60 = -0.71;
	public static double pickupModelAlwaysAge61to70 = -0.71;
	public static double pickupModelAlwaysAge71plus = 6.08;
	public static double pickupModelAlwaysHasChildren = 1.66;
	public static double pickupModelAlwaysHasDrivingLicence = 1.54;
	public static double pickupModelAlwaysIsFemale = -0.65;
	
	public static double pickupModelIfSpaceConst = 0.92;
	public static double pickupModelIfSpaceAge31to60 = -0.81;
	public static double pickupModelIfSpaceAge61to70 = -0.76;
	public static double pickupModelIfSpaceAge71plus = 6.43;
	public static double pickupModelIfSpaceHasChildren = 1.57;
	public static double pickupModelIfSpaceHasDrivingLicence = 3.00;
	public static double pickupModelIfSpaceIsFemale = -0.47;

	/*
	 * These two values are fixed so far. They might be changed in another study.
	 */
	public enum PreEvacuationTime {TIME0, TIME8, TIME16};
	public enum EvacuationReason {WATER, FIRE, CHEMICAL, ATOMIC};
	public static PreEvacuationTime leaveModelPreEvacuationTime = PreEvacuationTime.TIME0;
	public static EvacuationReason leaveModelEvacuationReason = EvacuationReason.ATOMIC;
	
	public static double leaveModelHasChildren = 0.60;
	public static double leaveModelHasDrivingLicense = 0.52;
	
	public static double leaveModelImmediatelyConst = 4.10;
	public static double leaveModelImmediatelyChemical = 1.61;
	public static double leaveModelImmediatelyAtomic = 2.08;
	public static double leaveModelImmediatelyFire = 0.59;
	public static double leaveModelImmediatelyAge31to60 = -3.12;
	public static double leaveModelImmediatelyAge61plus = -3.49;
	public static double leaveModelImmediatelyTime8 = -1.66;
	public static double leaveModelImmediatelyHouseholdUnited1 = -0.07;
	public static double leaveModelImmediatelyTime16 = -1.99;
	public static double leaveModelImmediatelyHouseholdUnited2 = -0.33;
	
	public static double leaveModelLaterConst = 3.36;
	public static double leaveModelLaterChemical = 0.982;
	public static double leaveModelLaterAtomic = 0.777;
	public static double leaveModelLaterFire = 0.297;
	public static double leaveModelLaterAge31to60 = -1.9;
	public static double leaveModelLaterAge61plus = -2.13;
	public static double leaveModelLaterTime8 = 0.458;
	public static double leaveModelLaterHouseholdUnited1 = -2.95;
	public static double leaveModelLaterTime16 = 0.275;
	public static double leaveModelLaterHouseholdUnited2 = -1.69;
	
	
	public static Coord getRescueCoord() {
		return new CoordImpl(centerCoord.getX() + 50000.0, centerCoord.getY() + 50000.0);
	}
	
	public static void printConfig() {
		log.info("evacuation start time:\t" + evacuationTime);
		log.info("inner radius:\t" + innerRadius);
		log.info("outer radius:\t" + outerRadius);
		log.info("center coordinate:\t" + centerCoord.toString());
		log.info("dhm 25 file:\t" + dhm25File);
		log.info("srtm file:\t" + srtmFile);
		log.info("household object attributes file:\t" + householdObjectAttributesFile);
		
		for (String string : evacuationArea) {
			log.info("evacuation area file:\t" + string);
		}

		for (String string : vehicleFleet) {
			log.info("vehicle fleet file:\t" + string);
		}

		log.info("affected area time penalty factor:\t" + affectedAreaTimePenaltyFactor);
		log.info("affected area distance buffer:\t" + affectedAreaDistanceBuffer);
		log.info("pt travel time penalty factor:\t" + ptTravelTimePenaltyFactor);
		log.info("panic share:\t" + panicShare);
		log.info("compass probability:\t" + compassProbability);
		log.info("tabu search:\t" + tabuSearch);
		log.info("household participation share:\t" + householdParticipationShare);
		log.info("during leg re-routing share:\t" + duringLegReroutingShare);
		log.info("agent pickup behaviour:\t" + pickupAgents.toString());
		log.info("use fuzzy travel times:\t" + useFuzzyTravelTimes);
		log.info("use lookup map for fuzzy travel times:\t" + DistanceFuzzyFactorProviderFactory.useLookupMap);
		log.info("create evacuation time picture:\t" + createEvacuationTimePicture);
		log.info("count agents in evacuation are:\t" + countAgentsInEvacuationArea);
		log.info("sigma for inform-agents Rayleigh function:\t" + informAgentsRayleighSigma);
		log.info("Network capacity factor:\t" + capacityFactor);
		log.info("Network speed factor:\t" + speedFactor);
	}
}
