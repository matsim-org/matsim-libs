package playground.christoph.evacuation.config;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;

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
	
	/*
	 * Analysis modules
	 */
	public static boolean createEvacuationTimePicture = true;
	public static boolean countAgentsInEvacuationArea = true;
	
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
		log.info("create evacuation time picture:\t" + createEvacuationTimePicture);
		log.info("count agents in evacuation are:\t" + countAgentsInEvacuationArea);
		log.info("sigma for inform-agents Rayleigh function:\t" + informAgentsRayleighSigma);
	}
}
