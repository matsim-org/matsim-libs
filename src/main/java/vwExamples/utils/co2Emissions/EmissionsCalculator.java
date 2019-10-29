package vwExamples.utils.co2Emissions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.compress.compressors.FileNameUtil;
import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

import com.opencsv.CSVReader;

public class EmissionsCalculator {
	String emissionsDefinitionFile;
	String mileageDefinitionFile;
	List<String[]> emissionsDefinitionList = new ArrayList<String[]>();

	public enum generalModesTypes {
		car, tram, rail, drt, bus
	}

	public enum generalVehicleTyps {
		normal, hdv, ldv
	}

	public enum generalPowerTrainTypes {
		gasonline, diesel, bev, bevplus
	}

	ArrayList<CO2EmissionsEntry> co2EmissionsInputs = new ArrayList<CO2EmissionsEntry>();
	HashMap<String, Double> mileage2generalModeTypes = new HashMap<String, Double>();

	EmissionsCalculator(ArrayList<CO2EmissionsEntry> co2EmissionsInputs,
			HashMap<String, Double> mileage2generalModeTypes) {
		this.co2EmissionsInputs = co2EmissionsInputs;
		this.mileage2generalModeTypes = mileage2generalModeTypes;

	}

	EmissionsCalculator(String emissionsDefinitionFile, String mileageDefinition) {
		this.emissionsDefinitionFile = emissionsDefinitionFile;
		this.mileageDefinitionFile = mileageDefinition;
		readEmissionsDefinitionCSV();
		readMileageDefinitionCSV();

	}

	public static void main(String[] args) throws IOException {

		EmissionsCalculator emissionsCalculator = new EmissionsCalculator(
				"C:\\Users\\VWBIDGN\\Desktop\\Emissions_base.csv",
				"D:\\Matsim\\Axer\\Hannover\\ZIM\\output\\vw280_CityCommuterDRTcarOnly_20pct_1.0_2500_veh_idx0\\vw280_CityCommuterDRTcarOnly_20pct_1.0_2500_veh_idx0.mileage.csv");

		emissionsCalculator.validityCheck();
		emissionsCalculator.allocateMilages();
		emissionsCalculator.calculate();
		emissionsCalculator.writeEmissions();

	}

	public void allocateMilages() {

		for (CO2EmissionsEntry co2EmissionsInput : co2EmissionsInputs) {

			String inputMode = co2EmissionsInput.generalMode;

			if (mileage2generalModeTypes.containsKey(inputMode)) {

				double totalMileageOfGeneralMode = this.mileage2generalModeTypes.get(inputMode);
				co2EmissionsInput.setMileage(
						co2EmissionsInput.mileageShare * totalMileageOfGeneralMode * co2EmissionsInput.powerTrainShare);
			} else {
				Logger.getLogger(EmissionsCalculator.class)
						.warn("No input mileage for mode: " + inputMode + " provided");
			}
		}

	}

	public void calculate() {

		for (CO2EmissionsEntry co2EmissionsInput : co2EmissionsInputs) {

			double emissions = co2EmissionsInput.calculateEmissions();

		}

	}

	public void validityCheck() {

		// Loop over general modes: car, tram, drt, bus
		for (generalModesTypes gMode : generalModesTypes.values()) {
			String mode = gMode.toString();

			HashMap<String, Double> vehicleTypeShareMap = new HashMap<String, Double>();

			// Loop over vehicle types with a general mode
			for (generalVehicleTyps vehType : generalVehicleTyps.values()) {
				String vehicleType = vehType.toString();

				double powerTrainShare = 0;

				for (CO2EmissionsEntry co2EmissionsInput : co2EmissionsInputs) {

					if (co2EmissionsInput.generalMode.equals(gMode.toString())
							&& co2EmissionsInput.vehicleType.equals(vehicleType)) {

						powerTrainShare = powerTrainShare + co2EmissionsInput.powerTrainShare;

						if (vehicleTypeShareMap.containsKey(vehicleType)) {

							double actualShare = vehicleTypeShareMap.get(vehicleType);
							double newShare = co2EmissionsInput.mileageShare;

							if (newShare != actualShare) {
								throw new IllegalArgumentException(
										"Different vehicle type share within group: " + vehicleType);
							}
						} else {
							double newShare = co2EmissionsInput.mileageShare;
							vehicleTypeShareMap.put(vehicleType, newShare);

						}

						System.out.println("General mode: " + gMode + " || vehicle type: " + vehicleType
								+ " || mileage for vehicle type share:" + vehicleTypeShareMap.get(vehicleType)
								+ " || powerTrainShare: " + powerTrainShare);
					}

				}

				if (powerTrainShare > 1.0) {
					throw new IllegalArgumentException(
							"Powertrain share in general mode " + mode + "  > 1.0, check inputs ");
				}

			}

			if (vehicleTypeShareMap.values().stream().reduce(0.0, Double::sum) > 1.0) {
				throw new IllegalArgumentException("Sum over vehicle types > 1.0: " + mode);

			}

		}

	}

	public void writeEmissions() {

		String fileName = ".co2Emissions.csv";
		String scenarioName = new File(this.mileageDefinitionFile).getParentFile().getName();
		String path = new File(this.mileageDefinitionFile).getParentFile().toString();

		NumberFormat formatter = new DecimalFormat("#0.00");
		BufferedWriter bw = IOUtils.getBufferedWriter(path + "//" + scenarioName + fileName);

		try {

			bw.write("generalMode;vehicleType;powerTrain;co2Emissions;mileage");
			bw.newLine();

			for (CO2EmissionsEntry co2EmissionsInput : co2EmissionsInputs) {
				String row = co2EmissionsInput.generalMode + ";" + co2EmissionsInput.vehicleType + ";"
						+ co2EmissionsInput.powerTrain + ";" + formatter.format(co2EmissionsInput.co2Emissions) + ";"
						+ formatter.format(co2EmissionsInput.mileage);

				bw.write(row);
				bw.newLine();
			}

			bw.flush();
			bw.close();

		} catch (IOException e) {

			e.printStackTrace();
		}
	}

	public void readEmissionsDefinitionCSV() {

		// generalMode vehicleType powerTrainType mileageShare powerTrainShare
		// tailPipeEmissionsPerKm energyConsomptionPer100Km energyProducationPerKwH
		// upstreamChainFactor

		CSVReader reader = null;
		try {

			reader = new CSVReader(new FileReader(this.emissionsDefinitionFile));

			// reader = new CSVReader(new FileReader(this.emissionsDefinitionFile),";");
			emissionsDefinitionList = reader.readAll();
			for (int i = 1; i < emissionsDefinitionList.size(); i++) {
				String[] lineContents = emissionsDefinitionList.get(i);
				String generalMode = (lineContents[0]); // generalMode,
				String vehicleType = (lineContents[1]); // vehicleType,
				String powerTrainType = (lineContents[2]); // powerTrainType,
				double mileageShare = Double.parseDouble(lineContents[3]); // mileageShare,
				double powerTrainShare = Double.parseDouble(lineContents[4]); // powerTrainShare,
				double tailPipeEmissionsPerKm = Double.parseDouble(lineContents[5]); // tailPipeEmissionsPerKm,
				double energyConsomptionPer100Km = Double.parseDouble(lineContents[6]); // energyConsomptionPer100Km
				double energyProducationPerKwH = Double.parseDouble(lineContents[7]); // energyProducationPerKwH
				double upstreamChainFactor = Double.parseDouble(lineContents[8]); // upstreamChainFactor

				CO2EmissionsEntry entry = new CO2EmissionsEntry(generalMode, vehicleType, powerTrainType, mileageShare,
						powerTrainShare, tailPipeEmissionsPerKm, energyConsomptionPer100Km, energyProducationPerKwH,
						upstreamChainFactor);

				this.co2EmissionsInputs.add(entry);

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

	public void readMileageDefinitionCSV() {

		HashSet<String> knownModes = new HashSet<String>();

		for (generalModesTypes gMode : generalModesTypes.values()) {
			knownModes.add(gMode.toString());
		}

		CSVReader reader = null;
		try {

			// reader = new CSVReader(new FileReader(this.emissionsDefinitionFile));

			reader = new CSVReader(new FileReader(this.mileageDefinitionFile), ';');
			// reader = new CSVReader(new FileReader(this.emissionsDefinitionFile),";");
			emissionsDefinitionList = reader.readAll();
			for (int i = 1; i < emissionsDefinitionList.size(); i++) {
				String[] lineContents = emissionsDefinitionList.get(i);
				String generalMode = (lineContents[0]); // generalMode,
				Double mileageKM = Double.parseDouble(lineContents[1]); // mileageKM,

				if (knownModes.contains(generalMode)) {
					mileage2generalModeTypes.put(generalMode, mileageKM);
				} else {
					Logger.getLogger(EmissionsCalculator.class)
							.warn("Mileage definition uses unknown general mode: " + generalMode);
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
