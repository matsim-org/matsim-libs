package city2000w;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import kid.KiDDataReader;
import kid.KiDSchema;
import kid.KiDStats;
import kid.ScheduledVehicle;
import kid.ScheduledVehicles;
import kid.Vehicle;
import kid.filter.And;
import kid.filter.HandelFilter;
import kid.filter.KernstadtFilter;
import kid.filter.LkwKleiner3Punkt5TFilter;
import kid.filter.PkwFilter;
import kid.filter.PkwGewerblichFilter;
import kid.filter.PkwPrivatFilter;
import kid.filter.SattelzugmaschinenFilter;
import kid.filter.VehicleFilter;
import kid.filter.VerarbeitendesGewerbeFilter;
import kid.filter.WeekFilter;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.IOUtils;


public class KiDDataHochrechnungsRunner {
	
	static class VehicleVisitor {
		
		private VehicleFilter filter;
		
		private String description;
		
		private BufferedWriter writer;
		
		public void setWriter(BufferedWriter writer) {
			this.writer = writer;
		}

		private double totalFahrleistung = 0.0;
		private double anzahlFahrten = 0;
		private double totChains = 0;
		private int count = 0;
		private double anzahlFahrzeuge;
		
		public VehicleVisitor(VehicleFilter filter, String description, int totNoOfVehicles) {
			super();
			this.filter = filter;
			this.description = description;
			this.anzahlFahrzeuge = totNoOfVehicles;
		}

		public void visit(ScheduledVehicle scheduledVehicle){
			if(filter.judge(scheduledVehicle.getVehicle())){
				String fzgTyp = scheduledVehicle.getVehicle().getAttributes().get(KiDSchema.VEHICLE_TYPE);
				String wochenTagTyp = scheduledVehicle.getVehicle().getAttributes().get(KiDSchema.VEHICLE_WOCHENTAGTYP);
				String datum = scheduledVehicle.getVehicle().getAttributes().get(KiDSchema.VEHICLE_DATUM);
				String kreisTyp = scheduledVehicle.getVehicle().getAttributes().get(KiDSchema.COMPANY_KREISTYP);
				String korrekturFaktor = scheduledVehicle.getVehicle().getAttributes().get(KiDSchema.VEHICLE_KORREKTURFAKTOR);
				String hochrechnungsFaktor = scheduledVehicle.getVehicle().getAttributes().get(KiDSchema.VEHICLE_HOCHRECHNUNGSFAKTOR);
				String hochrechnungsTage = scheduledVehicle.getVehicle().getAttributes().get(KiDSchema.VEHCILE_HOCHRECHNUNGSTAGE);
				String halbjahr = getHalbJahr(datum);
				String clusteredWochenTagTyp = getClusteredType(wochenTagTyp);
				Id id = makeId(fzgTyp,clusteredWochenTagTyp,halbjahr,kreisTyp);	
				String fahrleistung = scheduledVehicle.getVehicle().getAttributes().get(KiDSchema.VEHICLE_TAGESFAHRLEISTUNG);
				String anzahlFarten = scheduledVehicle.getVehicle().getAttributes().get(KiDSchema.VEHICLE_ANZAHLFAHRTEN);
				String anzahlFahrtenketten = scheduledVehicle.getVehicle().getAttributes().get(KiDSchema.VEHICLE_ANZAHLFAHRTENKETTEN);
				
				double fahrtenHochgerechnet = getTotLegs(anzahlFarten,korrekturFaktor,hochrechnungsFaktor);
				anzahlFahrten += fahrtenHochgerechnet;
				
				double kettenHochgerechnet = getTotLegs(anzahlFahrtenketten,korrekturFaktor,hochrechnungsFaktor);
				totChains += kettenHochgerechnet;
				
				double fzgLeistung = getFahrleistung(fahrleistung,korrekturFaktor,hochrechnungsFaktor);
				totalFahrleistung += fzgLeistung;
				//logger.info("fzgLeistung=" + fzgLeistung + "; fahrleistung="+fahrleistung+"; kFaktor=" + korrekturFaktor + "; hFaktor=" + hochrechnungsFaktor);
				count++;
				
			}
		}
		
		public void finish() throws IOException{
			double fzg_days = (double)KiDStats.WorkingDays_without_saturday*(double)anzahlFahrzeuge;
			if(writer != null){
				writer.write(description + "\n");
				writer.write("#Fahrzeuge (in Mio.Kfz)=" + anzahlFahrzeuge/1000000 + "\n");
				writer.write("Fahrzeugfahrleistung [in Mio.Fzkm/a]=" + totalFahrleistung/1000000 + "\n");
				writer.write("Fahrzeugfahrleistung [km/Kfz*d]=" + (totalFahrleistung)/(fzg_days) + "\n");
				writer.write("Fahrtenhäufigkeit [Fahrten/Kfz*d]=" + anzahlFahrten/(fzg_days) + "\n");
				writer.write("#Fahrtenaufkommen [in Mio]=" + anzahlFahrten/1000000 + "\n");
				writer.write("#Fahrtenketten[in Fketten/Kfz*d]=" + totChains/fzg_days + "\n");
				writer.write("\n");
			}
			System.out.println(description);
			
			System.out.println("#Fahrzeuge (in Stichprobe)="+ count);
			System.out.println("#Fahrzeuge (in Mio.Kfz)=" + anzahlFahrzeuge/1000000);
			System.out.println("Fahrzeugfahrleistung [in Mio.Fzkm/a]=" + totalFahrleistung/1000000);
			System.out.println("Fahrzeugfahrleistung [km/Kfz*d]=" + (totalFahrleistung)/(fzg_days));
			System.out.println("Fahrtenhäufigkeit [Fahrten/Kfz*d]=" + anzahlFahrten/(fzg_days));
			System.out.println("#Fahrtenaufkommen [in Mio]=" + anzahlFahrten/1000000);
			//double fzg_days = (double)(KiDStats.NoGesamt)*(double)KiDStats.WorkingDays_without_saturday;
			//System.out.println("#fzg="+ count + " fahrleistung=" + totalFahrleistung + " fzgDays=" + fzg_days + " km/tag=" + totalFahrleistung/fzg_days);
			System.out.println("#Fahrtenketten[in Fketten/Kfz*d]=" + totChains/fzg_days);
			System.out.println("");
			
		}
		
		private double getFahrzeuge(String korrekturFaktor,String hochrechnungsFaktor) {
			double kFaktor = Float.parseFloat(korrekturFaktor);
			double hFaktor = Float.parseFloat(hochrechnungsFaktor);
			return kFaktor*hFaktor;
		}

		private double getDouble(String hochrechnungsTage) {
			return Double.parseDouble(hochrechnungsTage);
		}

		private double getTotLegs(String anzahlFarten, String korrekturFaktor,String hochrechnungsFaktor) {
			int nOLegs = Integer.parseInt(anzahlFarten);
			double kFaktor = Float.parseFloat(korrekturFaktor);
			double hFaktor = Float.parseFloat(hochrechnungsFaktor);
			return (double)nOLegs*kFaktor*hFaktor;
		}

		private double getFahrleistung(String fahrleistung, String korrekturFaktor, String hochrechnungsFaktor) {
			double leistung = Float.parseFloat(fahrleistung);
			double kFaktor = Float.parseFloat(korrekturFaktor);
			double hFaktor = Float.parseFloat(hochrechnungsFaktor);
			return leistung*kFaktor*hFaktor;
		}

		private Double calculateTotVehicles(String korrekturFaktor, String hochrechnungsFaktor, String hochrechnungsTage) {
			Double hFaktor = Double.parseDouble(hochrechnungsFaktor);
			Double kFaktor = Double.parseDouble(korrekturFaktor);
			Double hTage = Double.parseDouble(hochrechnungsTage);
			return kFaktor*hFaktor/hTage;
		}

		private Id makeId(String fzgTyp, String clusteredWochenTagTyp, String halbjahr, String kreisTyp) {
			return new IdImpl(fzgTyp + "_" + clusteredWochenTagTyp + "_" +  halbjahr + "_ " + kreisTyp);
		}

		
	}
	
	private static Logger logger = Logger.getLogger(KiDDataHochrechnungsRunner.class);
	
	
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
//		And filters = new And(new WeekFilter(), new PkwGewerblichFilter());
		And filters = new And(new WeekFilter(), new PkwFilter());
//		filters.addFilter(new KernstadtFilter());
		kidReader.setVehicleFilter(new WeekFilter());
		kidReader.run();
		
		BufferedWriter writer = IOUtils.getBufferedWriter("output/kidStats.txt");
		
		Collection<VehicleVisitor> visitors = new ArrayList<VehicleVisitor>();
		initVisitors(visitors,writer);
				
		for(ScheduledVehicle vehicle : vehicles.getScheduledVehicles().values()){
			for(VehicleVisitor visitor : visitors){
				visitor.visit(vehicle);
			}
		}
		for(VehicleVisitor visitor : visitors){
			visitor.finish();
		}
		writer.close();
		
	}

	private static void initVisitors(Collection<VehicleVisitor> visitors, BufferedWriter writer) {
		
		
		And filters = new And(new WeekFilter(), new PkwGewerblichFilter());
		VehicleVisitor pkwGew_week = new VehicleVisitor(filters, "Pkw-Gewerblich, Mo-Fr", KiDStats.NoPkw_gewerblich);
		pkwGew_week.setWriter(writer);
		visitors.add(pkwGew_week);
		
		And anotherFilter = new And(new WeekFilter(), new PkwPrivatFilter());
		VehicleVisitor pkwPriv_week = new VehicleVisitor(anotherFilter, "Pkw-Private, Mo-Fr", KiDStats.NoPkw_private);
		pkwPriv_week.setWriter(writer);
		visitors.add(pkwPriv_week);
		
		And kl3_5_Filter = new And(new WeekFilter(), new LkwKleiner3Punkt5TFilter());
		VehicleVisitor kl3_5_week = new VehicleVisitor(kl3_5_Filter, "Lkw <3.5t, Mo-Fr", KiDStats.NoLkw_kl3punkt5);
		kl3_5_week.setWriter(writer);
		visitors.add(kl3_5_week);
		
		And kl3_5_G_Filter = new And(new WeekFilter(), new LkwKleiner3Punkt5TFilter());
		kl3_5_G_Filter.addFilter(new HandelFilter());
		VehicleVisitor kl3_5_G_week = new VehicleVisitor(kl3_5_G_Filter, "Lkw <3.5t, Handel (G), Mo-Fr", KiDStats.NoLkw_kl3punkt5_G);
		kl3_5_G_week.setWriter(writer);
		visitors.add(kl3_5_G_week);
		
		And kl3_5_K1_Filter = new And(new WeekFilter(), new LkwKleiner3Punkt5TFilter());
		kl3_5_K1_Filter.addFilter(new KernstadtFilter());
		VehicleVisitor kl3_5_K1_week = new VehicleVisitor(kl3_5_K1_Filter, "Lkw <3.5t, K1, Mo-Fr", KiDStats.NoLkw_kl3punkt5_K1);
		kl3_5_K1_week.setWriter(writer);
		visitors.add(kl3_5_K1_week);
		
		And kl3_5_G_K1_Filter = new And(new WeekFilter(), new LkwKleiner3Punkt5TFilter());
		kl3_5_G_K1_Filter.addFilter(new HandelFilter());
		kl3_5_G_K1_Filter.addFilter(new KernstadtFilter());
		VehicleVisitor kl3_5_G_K1_week = new VehicleVisitor(kl3_5_G_K1_Filter, "Lkw <3.5t, Handel (G), K1, Mo-Fr", KiDStats.NoLkw_kl3punkt5_K1_G);
		kl3_5_G_K1_week.setWriter(writer);
		visitors.add(kl3_5_G_K1_week);
		
		And kl3_5_D_K1_Filter = new And(new WeekFilter(), new LkwKleiner3Punkt5TFilter());
		kl3_5_D_K1_Filter.addFilter(new VerarbeitendesGewerbeFilter());
		kl3_5_D_K1_Filter.addFilter(new KernstadtFilter());
		VehicleVisitor kl3_5_D_K1_week = new VehicleVisitor(kl3_5_D_K1_Filter, "Lkw <3.5t, Verarb.Gewerbe (D), K1, Mo-Fr", KiDStats.NoLkw_kl3punkt5_K1_D);
		kl3_5_D_K1_week.setWriter(writer);
		visitors.add(kl3_5_D_K1_week);
		
		And sattelFilter = new And(new WeekFilter(), new SattelzugmaschinenFilter());
		VehicleVisitor sattel_week = new VehicleVisitor(sattelFilter, "Sattelzugmaschine, Mo-Fr", KiDStats.NoSattelzugmaschinen);
		sattel_week.setWriter(writer);
		visitors.add(sattel_week);
		
		And sattelHandelFilter = new And(new WeekFilter(), new SattelzugmaschinenFilter());
		sattelHandelFilter.addFilter(new HandelFilter());
		VehicleVisitor sattelHandel_week = new VehicleVisitor(sattelHandelFilter, "Sattelzugmaschine, Handel (G), Mo-Fr", KiDStats.NoSattelzugmaschinen_G);
		sattelHandel_week.setWriter(writer);
		visitors.add(sattelHandel_week);
		
		And weekPkwGewerblichWz = new And(new WeekFilter(), new PkwGewerblichFilter());
		weekPkwGewerblichWz.addFilter(new VerarbeitendesGewerbeFilter());
		VehicleVisitor pkwGew_D_week = new VehicleVisitor(weekPkwGewerblichWz, "Pkw-Gewerblich, Verarb.Gewerbe (D), Mo-Fr",KiDStats.NoPkw_gewerblich_D);
		pkwGew_D_week.setWriter(writer);
		visitors.add(pkwGew_D_week);
		
		And weekPkwGewerblichHandel = new And(new WeekFilter(), new PkwGewerblichFilter());
		weekPkwGewerblichHandel.addFilter(new HandelFilter());
		VehicleVisitor pkwGew_G_week = new VehicleVisitor(weekPkwGewerblichHandel, "Pkw-Gewerblich, Handel (G), Mo-Fr",KiDStats.NoPkw_gewerblich_G);
		pkwGew_G_week.setWriter(writer);
		visitors.add(pkwGew_G_week);
		
		And weekPkwGew_K1 = new And(new WeekFilter(), new PkwGewerblichFilter());
//		weekPkwGew_D_K1.addFilter(new VerarbeitendesGewerbeFilter());
		weekPkwGew_K1.addFilter(new KernstadtFilter());
		VehicleVisitor pkwGew_K1_week = new VehicleVisitor(weekPkwGew_K1, "Pkw-Gewerblich, K1, Mo-Fr", KiDStats.NoPkw_gewerblich_K1);
		pkwGew_K1_week.setWriter(writer);
		visitors.add(pkwGew_K1_week);
		
		And weekPkwGew_D_K1 = new And(new WeekFilter(), new PkwGewerblichFilter());
		weekPkwGew_D_K1.addFilter(new VerarbeitendesGewerbeFilter());
		weekPkwGew_D_K1.addFilter(new KernstadtFilter());
		VehicleVisitor weekPkwGew_D_K1_week = new VehicleVisitor(weekPkwGew_D_K1, "Pkw-Gewerblich, Verarb.Gewerbe (D), K1, Mo-Fr", KiDStats.NoPkw_gewerblich_K1_D);
		weekPkwGew_D_K1_week.setWriter(writer);
		visitors.add(weekPkwGew_D_K1_week);
		
		And weekPkwGew_G_K1 = new And(new WeekFilter(), new PkwGewerblichFilter());
		weekPkwGew_G_K1.addFilter(new HandelFilter());
		weekPkwGew_G_K1.addFilter(new KernstadtFilter());
		VehicleVisitor weekPkwGew_G_K1_week = new VehicleVisitor(weekPkwGew_G_K1, "Pkw-Gewerblich, Handel (G), K1, Mo-Fr", KiDStats.NoPkw_gewerblich_K1_G);
		weekPkwGew_G_K1_week.setWriter(writer);
		visitors.add(weekPkwGew_G_K1_week);
		
	}

	private static void close(BufferedWriter writer) {
		try {
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
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
