package commercialtraffic.companyGeneration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeWriter;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import com.opencsv.CSVReader;

import commercialtraffic.demandAssigment.CommercialTripsReader;
import ft.utils.ctDemandPrep.Company;
import ft.utils.ctDemandPrep.Demand4CompanyClass;
import ft.utils.ctDemandPrep.DemandGenerator;

public class CompanyGenerator {

	String csvVehiclefile;
	String csvAddCompanyFolder;
	String networkFile;
	File[] addCompanyfiles;
	DemandGenerator companyLocations;
	String companyFolder;
	String zoneSHP;
	String outputpath;
	String carrierOutputPath;
	Network network;
	Network carNetwork;
	// This map contains our companies and their carriers. In this case each company
	// is its own carrier. String Key = companyId
	public Map<String, CommericalCompany> commercialCompanyMap = new HashMap<String, CommericalCompany>();
	List<String[]> vehicleList;
	Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	CommercialTripsReader tripReader;
	String ctTripsFile;
	String serviceTimeDistributions;
	Map<String, Set<Integer>> vehBlackListIds;

	public CompanyGenerator(String csvVehiclefile, String csvAddCompanyFolder, String ctTripsFile,
			String serviceTimeDistributions, String networkFile, String companyFolder, String zoneSHP,
			String outputpath, String carrierOutputPath, Map<String, Set<Integer>> vehBlackListIds) {
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
		this.csvVehiclefile = csvVehiclefile;
		this.csvAddCompanyFolder = csvAddCompanyFolder;
		this.addCompanyfiles = new File(csvAddCompanyFolder).listFiles();
		this.ctTripsFile = ctTripsFile;
		this.networkFile = networkFile;
		this.serviceTimeDistributions = serviceTimeDistributions;
		this.companyFolder = companyFolder;
		this.zoneSHP = zoneSHP;
		this.outputpath = outputpath;
		this.network = scenario.getNetwork();
		this.carrierOutputPath = carrierOutputPath;
		this.vehBlackListIds = vehBlackListIds;
		// Filter Network to get only Car-links
		NetworkFilterManager networkFilter = new NetworkFilterManager(network);
		networkFilter.addLinkFilter(l -> l.getAllowedModes().contains(TransportMode.car));
		carNetwork = networkFilter.applyFilters();
		// TODO: Obviously not needed?!
		// CommercialTripsReader tripReader = new CommercialTripsReader(ctTripsFile,
		// serviceTimeDistributions);
		// tripReader.run();

	}

	public static void main(String[] args) {

	}

	public void initalize() {
		// CompanyGenerator demand = new
		// CompanyGenerator("D:\\Thiel\\Programme\\WVModell\\WV_Modell_KIT_H\\fahrzeuge.csv",
		// "D:\\Thiel\\Programme\\WVModell\\WV_Modell_KIT_H\\wege.csv",
		// "D:\\Thiel\\Programme\\WVModell\\ServiceDurCalc\\Distributions\\",
		// "D:\\Thiel\\Programme\\MatSim\\01_HannoverModel_2.0\\Network\\00_Final_Network\\network_editedPt.xml.gz",
		// "D:\\Thiel\\Programme\\WVModell\\00_Eingangsdaten\\Unternehmen\\",
		// "D:\\Thiel\\Programme\\WVModell\\00_Eingangsdaten\\Zellen\\FNP_Merged\\baseShapeH.shp",
		// "D:\\Thiel\\Programme\\WVModell\\00_Eingangsdaten\\",
		// "D:\\Thiel\\Programme\\WVModell\\01_MatSimInput\\Carrier\\");

		this.loadCompanyLocations();
		this.readVehicleCSV();
		// this.writeCarriers();

	}

	public void loadCompanyLocations() {

		// companyFolder =
		// "D:\\Thiel\\Programme\\WVModell\\00_Eingangsdaten\\Unternehmen\\";
		// zoneSHP =
		// "D:\\Thiel\\Programme\\WVModell\\00_Eingangsdaten\\Zellen\\FNP_Merged\\baseShapeH.shp";
		// outputpath = "D:\\Thiel\\Programme\\WVModell\\00_Eingangsdaten\\";

		companyLocations = new DemandGenerator(companyFolder, zoneSHP, outputpath);
		companyLocations.getFilesperCompanyClass();
		for (String keys : companyLocations.filesPerCompanyClass.keySet()) {
			// String dummyDemandFile = demand.getFile(i);

			Demand4CompanyClass d = new Demand4CompanyClass(companyLocations.filesPerCompanyClass.get(keys), null,
					companyLocations.zoneMap);

			d.readDemandCSV();

			companyLocations.demand4CompanyClass2List.put(d.getCompanyClass(), d);

		}
		companyLocations.getCompanyClassesPerZone();
	}

	public Id<Link> infereCompanyLink(String companyClass, String zone) {
		Demand4CompanyClass comClass = companyLocations.demand4CompanyClass2List.get(companyClass);
		if (comClass.zone2CompanyMap.get(zone) == null) {
			System.out.println("Zone not found:" + zone);

		}

		ArrayList<Company> companyPerCompanyClassAndZone = comClass.zone2CompanyMap.get(zone);

		Company company = companyPerCompanyClassAndZone.remove(0);

		return NetworkUtils.getNearestLink(carNetwork, company.coord).getId();
	}

	public void addCustomCompany() {

		CSVReader reader = null;
		// vehID,CompanyId,LocationX,LocationY,zone,companyClass,vehicleType,active,compOpening,compClosing,vehOpening,vehClosing,ServiceDur
		if (addCompanyfiles.length > 0) {
			for (File file : addCompanyfiles) {
				try {
					reader = new CSVReader(new FileReader(file));
					vehicleList = reader.readAll();
					for (int i = 1; i < vehicleList.size(); i++) {
						String[] lineContents = vehicleList.get(i);
						int vehicleId = Integer.parseInt(lineContents[0]);
						String companyId = (lineContents[1]);
						double companyX = Double.parseDouble(lineContents[2]);
						double companyY = Double.parseDouble(lineContents[3]);
						Coord companyCoord = new Coord(companyX, companyY);

						// String zone = (lineContents[4]);
						String companyClass = lineContents[5];
						int vehicleType = Integer.parseInt(lineContents[6]);
						// TODO: Model two separate fleets
						boolean active;
						if (Integer.parseInt(lineContents[7]) == 1) {
							active = true;
						} else {
							active = false;
						}
						double compOpening = Double.parseDouble(lineContents[8]);
						double compClosing = Double.parseDouble(lineContents[9]);
						double vehOpening = Double.parseDouble(lineContents[10]);
						double vehClosing = Double.parseDouble(lineContents[11]);
						double servDur = Double.parseDouble(lineContents[12]);
						;

						// TODO Fix opening and closing times
						if (commercialCompanyMap.containsKey(companyId)) {
							// Add only not filtered and active new vehicle

							commercialCompanyMap.get(companyId).addVehicle(
									commercialCompanyMap.get(companyId).companyLinkId, vehicleType, vehOpening,
									vehClosing);

						} else {
							// Create company
							Id<Link> companyLinkId = NetworkUtils.getNearestLink(carNetwork, companyCoord).getId();

							// TODO
							CommericalCompany commericalCompany = new CommericalCompany(companyId, compOpening,
									compClosing, servDur, companyClass, companyLinkId);
							// Add only not filtered and active new vehicle

							commericalCompany.addVehicle(companyLinkId, vehicleType, vehOpening, vehClosing);

							commercialCompanyMap.put(companyId, commericalCompany);

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
	}

	public void readVehicleCSV() {

		// "","ID","UnternehmensID","Zelle","Flaeche","Wirtschaftszweig","Fahrzeugtyp","Cluster","akt"
		// "1",1,1,1462,0.0929,"F",2,3,0
		// "2",2,1,1462,0.0929,"F",4,2,0

		// CoordinateTransformation ct =
		// TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,
		// "EPSG:25832");

		addCustomCompany();

		CSVReader reader = null;

		try {
			reader = new CSVReader(new FileReader(this.csvVehiclefile));
			vehicleList = reader.readAll();
			for (int i = 1; i < vehicleList.size(); i++) {
				String[] lineContents = vehicleList.get(i);
				int vehicleId = Integer.parseInt(lineContents[1]);
				String companyId = (lineContents[2]);
				String zone = (lineContents[3]);
				String companyClass = lineContents[5];
				int vehicleType = Integer.parseInt(lineContents[6]);
				// TODO: Model two separate fleets
				boolean active;
				if (Integer.parseInt(lineContents[8]) == 1) {
					active = true;
				} else {
					active = false;
				}
				;
				// TODO Fix opening and closing times
				if (commercialCompanyMap.containsKey(companyId)) {
					// Add only not filtered and active new vehicle
					if ((vehBlackListIds.isEmpty()) || (!vehBlackListIds.get(companyClass).contains(vehicleId))) {
						if (active) {
							commercialCompanyMap.get(companyId).addVehicle(

									commercialCompanyMap.get(companyId).companyLinkId, vehicleType, 6.5 * 3600.0,
									17.5 * 3600.0);
						}
					}
				} else {
					// Create company
					Id<Link> companyLinkId = infereCompanyLink(companyClass, zone);

					// TODO
					CommericalCompany commericalCompany = new CommericalCompany(companyId, 6.0 * 3600.0, 18.0 * 3600.0,
							300.0, companyClass, companyLinkId);
					// Add only not filtered and active new vehicle
					if ((vehBlackListIds.isEmpty()) || (!vehBlackListIds.get(companyClass).contains(vehicleId))) {
						if (active) {
							commericalCompany.addVehicle(companyLinkId, vehicleType, 6.5 * 3600.0, 17.5 * 3600.0);
						}
					}
					commercialCompanyMap.put(companyId, commericalCompany);

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

	public void writeCarriers() {
		Carriers carriers = new Carriers();

		for (Entry<String, CommericalCompany> commercialCompanyEntry : commercialCompanyMap.entrySet()) {
			// String companyId=commercialCompanyEntry.getKey();
			if (commercialCompanyEntry.getValue().carrier.getCarrierCapabilities().getCarrierVehicles().isEmpty()) {
				// Delete Companies without vehicles
				continue;
			}
if (commercialCompanyEntry.getValue().carrier.getId().toString().contains("grocery")) {
			carriers.addCarrier(commercialCompanyEntry.getValue().carrier);}

		}
		new CarrierPlanXmlWriterV2(carriers).write(carrierOutputPath + "carrier_definition.xml");
		new CarrierVehicleTypeWriter(CarrierVehicleTypes.getVehicleTypes(carriers))
				.write(carrierOutputPath + "carrier_vehicletypes.xml");

	}

}
