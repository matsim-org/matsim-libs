package playground.wrashid.PSF2.vehicle.energyConsumption;

import org.matsim.api.core.v01.Id;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.vehicles.VehicleType;

public class EnergyConsumptionTableTest extends MatsimTestCase {
	
	public void testBasic() {
		String pathVehicleEnergyConsumptionTable =super.getPackageInputDirectory() + "VehicleEnergyConsumptionRegressionTable.txt";
		EnergyConsumptionTable energyConsumtpionTable=new EnergyConsumptionTable(pathVehicleEnergyConsumptionTable);
		double linkLengthInMeters=1000;
		
		// trying to use first line in file.
		Id<VehicleType> vehicleClassId=Id.create(1, VehicleType.class);
		double linkFreeSpeedInKmPerHour=30.0;
		double linkFreeSpeedInMetersPerSecond=linkFreeSpeedInKmPerHour*1000/3600;
		double averageDrivenSpeedOnLinkInKmPerHour=25.0;
		
		
		double expectedEnergyConsumptionInJoule= 19614276.98 +	-1339507.714*averageDrivenSpeedOnLinkInKmPerHour +	26096.13036*averageDrivenSpeedOnLinkInKmPerHour*averageDrivenSpeedOnLinkInKmPerHour;
		double actualEnergyConsumptionInJoule=energyConsumtpionTable.getEnergyConsumptionInJoule(vehicleClassId, averageDrivenSpeedOnLinkInKmPerHour,linkFreeSpeedInMetersPerSecond ,linkLengthInMeters);
		
		assertEquals(expectedEnergyConsumptionInJoule,actualEnergyConsumptionInJoule,1.0);
	}    
}
