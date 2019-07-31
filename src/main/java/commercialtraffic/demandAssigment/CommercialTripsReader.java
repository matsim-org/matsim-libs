package commercialtraffic.demandAssigment;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.opencsv.CSVReader;

public class CommercialTripsReader {
	List<String[]> rawTripList;
	String csvTripFile;
	Map<String, List<CommercialTrip>> commercialTripMap;
	Map<String, List<Double>> ServiceType2ServiceDurationsMap;

	CommercialTripsReader(String csvTripFile) {
		this.csvTripFile = csvTripFile;
		this.rawTripList = new ArrayList<String[]>();
		this.commercialTripMap = new HashMap<String, List<CommercialTrip>>();
		this.ServiceType2ServiceDurationsMap = new HashMap<String, List<Double>>();

	}

	public static void main(String[] args)  {

		run();

	}

	public static void run()  {
		CommercialTripsReader tripReader = new CommercialTripsReader(
				"D:\\Thiel\\Programme\\WVModell\\WV_Modell_KIT_H\\wege.csv");
		tripReader.readVehicleCSV();
		tripReader.calcServiceDurationsPerServiceType();

	}

	public void calcServiceDurationsPerServiceType()  {
		double officeFactor = 0.0;
		double workDayDuration = 8 * 3600.0;

		for (Entry<String, List<CommercialTrip>> entry : this.commercialTripMap.entrySet()) {
			
			String serviceType = entry.getKey();
			
			switch (serviceType) {
			case "A":
				officeFactor = 0.15;
				workDayDuration = 8 * 3600.0;
				break;
				
			case "B":
				officeFactor = 0.15;
				workDayDuration = 8 * 3600.0;
				break;
				
			case "D":
				officeFactor = 0.15;
				workDayDuration = 8 * 3600.0;
				break;
				
			case "E":
				officeFactor = 0.15;
				workDayDuration = 8 * 3600.0;
				break;
				
			case "F":
				officeFactor = 0.15;
				workDayDuration = 8 * 3600.0;
				break;
				
			case "G":
				officeFactor = 0.15;
				workDayDuration = 8 * 3600.0;
				break;
				
			case "H":
				officeFactor = 0.15;
				workDayDuration = 8 * 3600.0;
				break;
				
			case "J":
				officeFactor = 0.15;
				workDayDuration = 8 * 3600.0;
				break;
				
			case "K":
				officeFactor = 0.6;
				workDayDuration = 8 * 3600.0;
				break;
			
			case "M":
				officeFactor = 0.15;
				workDayDuration = 8 * 3600.0;
				break;
				
			case "O":
				officeFactor = 0.15;
				workDayDuration = 8 * 3600.0;
				break;
				
			case "P":
				officeFactor = 0.15;
				workDayDuration = 8 * 3600.0;
				break;
				
			case "S":
				officeFactor = 0.15;
				workDayDuration = 8 * 3600.0;
				break;
				
			}
			
			
			
			List<Double> serviceDurationsPerServiceType = calcServiceDurations(workDayDuration,officeFactor, entry.getValue());
			this.ServiceType2ServiceDurationsMap.put(entry.getKey(), serviceDurationsPerServiceType);
			
			Double average = serviceDurationsPerServiceType.stream().mapToDouble(val -> val).average().orElse(0.0);
			System.out.println(entry.getKey() + " = "+average/60.0);
		}
	}

	public List<Double> calcServiceDurations(double dailyWorkDuration, double officeFactor,
			List<CommercialTrip> tripList) {

		List<Double> serviceTimeDurations = new ArrayList<Double>();

		double defaultDailyWorkDur = dailyWorkDuration;
		// Temporary values
		double travelTime = 0.0;
		int tripPerVeh = 0;

		int tripCounter = 0;
		int listSize = tripList.size();
		for (CommercialTrip trip : tripList) {
			tripCounter++;
			tripPerVeh++;

			travelTime = travelTime + trip.fahrzeit;

			if (trip.fahrtID == 1 || tripCounter == listSize) {
				while (travelTime > (dailyWorkDuration * (1.0 - officeFactor))) {
					dailyWorkDuration = dailyWorkDuration + 3600.0;
				}
				double serviceTimeDuration = ((dailyWorkDuration * (1.0 - officeFactor)) - travelTime) / tripPerVeh;
				serviceTimeDurations.add(serviceTimeDuration);
				// Reset
				travelTime = 0.0;
				tripPerVeh = 0;
				dailyWorkDuration = defaultDailyWorkDur;

			}
		}
		return serviceTimeDurations;

	}

	public void readVehicleCSV() {

		// "","ID","UnternehmensID","Zelle","Flaeche","Wirtschaftszweig","Fahrzeugtyp","Cluster","akt","Q1","Q2","Q3","Q4","Q5","Q6","Q7","Q8","Q9","Anz_Akt_W","Anz_Akt_P","Anz_Akt_T","Zweck","Art_Ziel","FN","Quellzelle","Zielzelle","Fahrtzeit","FahrtID"
		// "1",1,1,1462,0.0929,"F",2,3,1,FALSE,FALSE,FALSE,TRUE,FALSE,FALSE,FALSE,FALSE,FALSE,0,0,0,1,5,"I",1462,1149,23.1749684962706,1
		// "2",1,1,1462,0.0929,"F",2,3,1,TRUE,FALSE,FALSE,FALSE,FALSE,FALSE,FALSE,FALSE,FALSE,1,0,1,5,4,"0",1149,1462,23.1749684962706,2

		// CoordinateTransformation ct =
		// TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,
		// "EPSG:25832");

		CSVReader reader = null;
		try {
			reader = new CSVReader(new FileReader(this.csvTripFile));
			rawTripList = reader.readAll();
			for (int i = 1; i < rawTripList.size(); i++) {
				String[] lineContents = rawTripList.get(i);
				int id = Integer.parseInt(lineContents[1]);
				int unternehmensID = Integer.parseInt(lineContents[2]);
				int fahrtID = Integer.parseInt(lineContents[27]); //
				String unternehmenszelle = (lineContents[3]);
				String wirtschaftszweig = (lineContents[5]);
				int fahrzeugtyp = Integer.parseInt(lineContents[6]);
				int zweck = Integer.parseInt(lineContents[21]);
				String quellzelle = (lineContents[24]);
				String zielzelle = (lineContents[25]);
				int art_ziel = Integer.parseInt(lineContents[22]);
				double fahrzeit = Double.parseDouble(lineContents[26]) * 60.0;

				CommercialTrip commercialTrip = new CommercialTrip(id, unternehmensID, fahrtID, unternehmenszelle,
						wirtschaftszweig, fahrzeugtyp, zweck, quellzelle, zielzelle, art_ziel, fahrzeit);

				if (commercialTripMap.containsKey(wirtschaftszweig)) {
					commercialTripMap.get(wirtschaftszweig).add(commercialTrip);
				} else {
					commercialTripMap.put(wirtschaftszweig, new ArrayList<CommercialTrip>());
					commercialTripMap.get(wirtschaftszweig).add(commercialTrip);
				}

			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}
}
