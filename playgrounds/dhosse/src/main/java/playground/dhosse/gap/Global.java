package playground.dhosse.gap;

import java.util.Random;

import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

public class Global {
	
	public static final String runID = "run8";
	
	public static final Random random = MatsimRandom.getRandom();
	
	final double countsFactor2005_2009 = 0.9883802132;
	
	private static double n = 0;
	public static final int N = 86336;
	
	//directories
	static final String smbDir = "/run/user/1007/gvfs/smb-share:server=innoz-dc01,share=innoz/";
	public static final String adminBordersDir = smbDir + "3_Allgemein/Geoinformation/Administrative_Grenzen/"; //gemeinden_2009.shp
	public static final String projectDir = smbDir + "2_MediengestützteMobilität/10_Projekte/eGAP/";
	public static final String dataDir = projectDir + "20_Datengrundlage/";
	public static final String networkDataDir = dataDir + "Netzwerk/";
	public static final String matsimDir = projectDir + "30_Modellierung/";
	public static final String matsimInputDir = matsimDir + "INPUT/";
	public static final String matsimOutputDir = matsimDir + "OUTPUT/" + Global.runID + "/output";
	
	//coordinate systems and transformations
	static final String fromCrs = "EPSG:4326";
	public static final String toCrs = "EPSG:32632";
	public final String GK4 = TransformationFactory.DHDN_GK4;
	public static final CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(fromCrs, toCrs);
	public static final CoordinateTransformation reverseCt = TransformationFactory.getCoordinateTransformation(toCrs, fromCrs);
	public static final CoordinateTransformation gk4ToUTM32N = TransformationFactory.getCoordinateTransformation(TransformationFactory.DHDN_GK4, toCrs);
	public static final CoordinateTransformation UTM32NtoGK4 = TransformationFactory.getCoordinateTransformation(toCrs, TransformationFactory.DHDN_GK4);
	
	//the activity types used in the gap scenario
	public static enum ActType{
		home,
		work,
		education,
		shop,
		leisure,
		other
	};
	
	//subpopulation classes
	public static final String USER_GROUP = "usrGroup";
	public static final String GP_CAR = "GP_CAR";
	public static final String COMMUTER = "COMMUTER";
	
	//age classes
	public static final String AGE = "AGE";
	public static final String CHILD = "CHILD";
	public static final String ADULT = "ADULT";
	public static final String PENSIONER = "PENSIONER";
	
	//sex classes
	public static final String SEX = "SEX";
	public static final String MALE = "MALE";
	public static final String FEMALE = "FEMALE";
	
	//employment
	public static final String EMPLOYMENT = "EMPLOYEMENT";
	public static final String EMPLOYED = "EMPLOYED";
	public static final String NOT_EMPLOYED = "NOT_EMPLOYED";
	
	//car availability
	public static final String CAR_AVAILABILITY = "CAR_AVAILABILITY";
	public static final String CAR_AVAIL = "CAR_AVAIL";
	public static final String NO_CAR = "NO_CAR";
	
	//driving license
	public static final String LICENSE = "LICENSE";
	public static final String HAS_LICENSE = "HAS_LICENSE";
	public static final String NO_LICENSE = "NO_LICENSE";
	
	//status of residence
	public static final String RESIDENCE = "RESIDENCE";
	public static final String INHABITANT = "INHABITANT";

	public static final String CARSHARING = "CARSHARING";
	public static final String CAR_OPTION = "CAR_OPTION";

	public static double getN() {
		return n;
	}

	public static void setN(double n) {
		Global.n = n;
	}
	
}
