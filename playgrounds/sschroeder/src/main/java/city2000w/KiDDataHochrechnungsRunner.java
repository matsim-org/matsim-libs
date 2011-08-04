package city2000w;

import java.io.FileNotFoundException;
import java.io.IOException;

import kid.KiDDataReader;
import kid.KiDSchema;
import kid.KiDStats;
import kid.ScheduledVehicle;
import kid.ScheduledVehicles;
import kid.Vehicle;
import kid.filter.And;
import kid.filter.KernstadtFilter;
import kid.filter.LkwKleiner3Punkt5TFilter;
import kid.filter.VehicleFilter;
import kid.filter.WeekFilter;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;





public class KiDDataHochrechnungsRunner {
	
	private static Logger logger = Logger.getLogger(KiDDataHochrechnungsRunner.class);
	
	public static int NOFSATTELZUGMASCHINEN = 175500;
	
	

	
	public static int NOFARBEITSTAGE_MO_FR = 252;
	
	public static class MobileVehicleFilter implements VehicleFilter{

		public boolean judge(Vehicle vehicle) {
			int mobilityIndex = Integer.parseInt(vehicle.getAttributes().get(KiDSchema.VEHICLE_MOBILITY));
			if(mobilityIndex == 1){
				return true;
			}
			return false;
		}
		
	}
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		ScheduledVehicles vehicles = new ScheduledVehicles();
		KiDDataReader kidReader = new KiDDataReader(vehicles);
		
		String directory = "/Volumes/projekte/2000-Watt-City/Daten/KiD/";
		kidReader.setVehicleFile(directory + "KiD_2002_Fahrzeug-Datei.txt");
		kidReader.setTransportChainFile(directory + "KiD_2002_Fahrtenketten-Datei.txt");
		kidReader.setTransportLegFile(directory + "KiD_2002_(Einzel)Fahrten-Datei.txt");
		//kidReader.getVehicleFilter().add(new SattelzugmaschinenFilter());
//		kidReader.getVehicleFilter().add(new WeekFilter());
//		kidReader.getVehicleFilter().add(new LkwGroesser3Punkt5TFilter());
		And and = new And(new WeekFilter(), new LkwKleiner3Punkt5TFilter());
		and.addFilter(new KernstadtFilter());
		kidReader.setVehicleFilter(and);
		kidReader.run();
		
		double totalFahrleistung = 0.00;
		double totLegs = 0;
		double totChains = 0;
		int count = 0;
		for(ScheduledVehicle vehicle : vehicles.getScheduledVehicles().values()){
			String fzgTyp = vehicle.getVehicle().getAttributes().get(KiDSchema.VEHICLE_TYPE);
			String wochenTagTyp = vehicle.getVehicle().getAttributes().get(KiDSchema.VEHICLE_WOCHENTAGTYP);
			String datum = vehicle.getVehicle().getAttributes().get(KiDSchema.VEHICLE_DATUM);
			String kreisTyp = vehicle.getVehicle().getAttributes().get(KiDSchema.COMPANY_KREISTYP);
			String korrekturFaktor = vehicle.getVehicle().getAttributes().get(KiDSchema.VEHICLE_KORREKTURFAKTOR);
			String hochrechnungsFaktor = vehicle.getVehicle().getAttributes().get(KiDSchema.VEHICLE_HOCHRECHNUNGSFAKTOR);
			String hochrechnungsTage = vehicle.getVehicle().getAttributes().get(KiDSchema.VEHCILE_HOCHRECHNUNGSTAGE);
			String halbjahr = getHalbJahr(datum);
			String clusteredWochenTagTyp = getClusteredType(wochenTagTyp);
			Id id = makeId(fzgTyp,clusteredWochenTagTyp,halbjahr,kreisTyp);
			
			String fahrleistung = vehicle.getVehicle().getAttributes().get(KiDSchema.VEHICLE_TAGESFAHRLEISTUNG);
			
			String anzahlFarten = vehicle.getVehicle().getAttributes().get(KiDSchema.VEHICLE_ANZAHLFAHRTEN);
			
			double fahrtenHochgerechnet = getTotLegs(anzahlFarten,korrekturFaktor,hochrechnungsFaktor);
			totLegs += fahrtenHochgerechnet/NOFARBEITSTAGE_MO_FR;
			logger.info("id=" + vehicle.getVehicle().getId() + "; #fahrten=" + anzahlFarten + " hochgrechnet=" + fahrtenHochgerechnet 
					+ " proTag=" + fahrtenHochgerechnet/NOFARBEITSTAGE_MO_FR);
			
			String anzahlFahrtenketten = vehicle.getVehicle().getAttributes().get(KiDSchema.VEHICLE_ANZAHLFAHRTENKETTEN);
			double kettenHochgerechnet = getTotLegs(anzahlFahrtenketten,korrekturFaktor,hochrechnungsFaktor);
			totChains += kettenHochgerechnet/NOFARBEITSTAGE_MO_FR;
			
			double fzgLeistung = getFahrleistung(fahrleistung,korrekturFaktor,hochrechnungsFaktor);
			totalFahrleistung += fzgLeistung;
			//logger.info("fzgLeistung=" + fzgLeistung + "; fahrleistung="+fahrleistung+"; kFaktor=" + korrekturFaktor + "; hFaktor=" + hochrechnungsFaktor);
			count++;
			
		}
		double fzg_days = (double)KiDStats.WorkingDays_without_saturday*(double)KiDStats.NoLkw_kl3punkt5;
		System.out.println("#fzg="+ count + " fahrleistung[in Mio]=" + totalFahrleistung/1000000 + " fzgDays=" + fzg_days + " km/tag=" + totalFahrleistung/fzg_days);
		System.out.println("totFahrten[in Mio]=" + totLegs/1000000 + " avgLegs=" + totLegs/((double)KiDStats.NoLkw_gr3punkt5*(double)KiDStats.WorkingDays_without_saturday));
		//double fzg_days = (double)(KiDStats.NoGesamt)*(double)KiDStats.WorkingDays_without_saturday;
		//System.out.println("#fzg="+ count + " fahrleistung=" + totalFahrleistung + " fzgDays=" + fzg_days + " km/tag=" + totalFahrleistung/fzg_days);
		System.out.println("ketten[in 1000]=" + totChains/1000);
		
	}

	private static double getDouble(String hochrechnungsTage) {
		return Double.parseDouble(hochrechnungsTage);
	}

	private static double getTotLegs(String anzahlFarten, String korrekturFaktor,String hochrechnungsFaktor) {
		int nOLegs = Integer.parseInt(anzahlFarten);
		double kFaktor = Float.parseFloat(korrekturFaktor);
		double hFaktor = Float.parseFloat(hochrechnungsFaktor);
		return (double)nOLegs*kFaktor*hFaktor;
	}

	private static double getFahrleistung(String fahrleistung, String korrekturFaktor, String hochrechnungsFaktor) {
		double leistung = Float.parseFloat(fahrleistung);
		double kFaktor = Float.parseFloat(korrekturFaktor);
		double hFaktor = Float.parseFloat(hochrechnungsFaktor);
		return leistung*kFaktor*hFaktor;
	}

	private static Double calculateTotVehicles(String korrekturFaktor, String hochrechnungsFaktor, String hochrechnungsTage) {
		Double hFaktor = Double.parseDouble(hochrechnungsFaktor);
		Double kFaktor = Double.parseDouble(korrekturFaktor);
		Double hTage = Double.parseDouble(hochrechnungsTage);
		return kFaktor*hFaktor/hTage;
	}

	private static Id makeId(String fzgTyp, String clusteredWochenTagTyp, String halbjahr, String kreisTyp) {
		return new IdImpl(fzgTyp + "_" + clusteredWochenTagTyp + "_" +  halbjahr + "_ " + kreisTyp);
	}

	private static String getClusteredType(String wochenTagTyp) {
		if(wochenTagTyp.equals("1") || wochenTagTyp.equals("3") ){
			return "1";
		}
		else if(wochenTagTyp.equals("2")){
			return "2";
		}
		return "3";
		
	}

	private static String getHalbJahr(String datum) {
		datum = datum.trim();
		String monat = datum.substring(3, 5);
		if(monat.equals("10")){
			return "2";
		}
		monat = monat.replace("0", "");
		int monatsNr = Integer.parseInt(monat);
		if(monatsNr <= 6){
			return "1";
		}
		else{
			return "2";
		}
		
	}

}
