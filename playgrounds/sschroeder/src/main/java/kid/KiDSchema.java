/**
 * 
 */
package kid;

/**
 * @author stefan
 *
 */
public class KiDSchema {
	
	public final static String VEHICLE_ID = "K00";
	public final static String VEHICLE_TYPE = "K01";
	public static String VEHICLE_MOBILITY = "K30";
	public static String VEHICLE_ANZAHLFAHRTEN = "K31";
	public static String VEHICLE_LOCATION_GEO_LONG = "K24d";
	public static String VEHICLE_LOCATION_GEO_LAT = "K24e";
	
	public static String VEHICLE_TAGESFAHRLEISTUNG = "K34";
	
	public static String COMPANY_SECTOR = "H01"; //Halter
	public static String COMPANY_KREISTYP = "H04";
	public static String COMPANY_EMPLOYEES = "H05";
	
	public static String COMPANY_FUHRPARK_PKW = "H06b";
	public static String COMPANY_FUHRPARK_LKW = "H06d";
	
	public static String VEHICLE_WOCHENTAG = "K22e";
	public static String VEHICLE_WOCHENTAGTYP = "K22f"; //Typ1 -> {Mo}, Typ2 -> {Di,Mi,Do}, Typ3 -> {Fr}, Typ4 -> {Sa}, Typ5 -> {So, Feiertag}
	
	public static String VEHICLE_DATUM = "K22d";
	
	public static String VEHICLE_HOCHRECHNUNGSFAKTOR = "K90";
	public static String VEHICLE_KORREKTURFAKTOR = "K91";
	public static String VEHCILE_HOCHRECHNUNGSTAGE = "K92";
	
	
	
	
	public static String CHAIN_LENGTH = "T05";
	
	public static String LEGID_OF_FIRSTLEG = "F00";
	public static String LEG_LENGTH = "F14";
	public static String LEG_QUELLADRESSE_GEO_LONG = "F02d";
	public static String LEG_QUELLADRESSE_GEO_LAT = "F02e";
	
	public static String LEG_ZIELADRESSE_GEO_LONG = "F08d";
	public static String LEG_ZIELADRESSE_GEO_LAT = "F08e";
	
	public static String LEG_SOURCE_LOCATIONTYPE = "F03";
	public static String LEG_DESTINATION_LOCATIONTYPE = "F09";
	public static String LEG_DEPARTURETIME = "F04";
	public static String LEG_ARRIVALTIME = "F10a";
	public static String LEG_ARRIVALDATE = "F10b";
	public static String LEG_PURPOSE = "F07a";

}
