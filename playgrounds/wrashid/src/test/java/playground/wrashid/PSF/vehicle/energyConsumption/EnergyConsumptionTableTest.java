package playground.wrashid.PSF.vehicle.energyConsumption;

import java.util.Set;

import junit.framework.TestCase;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.PSF.ParametersPSF;
import playground.wrashid.PSF.ParametersPSFMutator;
import playground.wrashid.PSF.PSS.PSSControler;
import playground.wrashid.PSF.data.HubPriceInfo;
import playground.wrashid.PSF.energy.AfterSimulationListener;
import playground.wrashid.PSF.energy.charging.ChargeLog;
import playground.wrashid.PSF.energy.charging.ChargingTimes;

public class EnergyConsumptionTableTest extends MatsimTestCase {
	
	public void testBasic() {
		String pathVehicleEnergyConsumptionTable =super.getPackageInputDirectory() + "VehicleEnergyConsumptionRegressionTable.txt";
		EnergyConsumptionTable energyConsumtpionTable=new EnergyConsumptionTable(pathVehicleEnergyConsumptionTable);
		double linkLengthInMeters=1000;
		
		// trying to use first line in file.
		Id vehicleClassId=new IdImpl(1);
		double linkFreeSpeedInKmPerHour=30.0;
		double linkFreeSpeedInMetersPerSecond=linkFreeSpeedInKmPerHour*1000/3600;
		double averageDrivenSpeedOnLinkInKmPerHour=25.0;
		
		
		double expectedEnergyConsumptionInJoule= 19614276.98 +	-1339507.714*averageDrivenSpeedOnLinkInKmPerHour +	26096.13036*averageDrivenSpeedOnLinkInKmPerHour*averageDrivenSpeedOnLinkInKmPerHour;
		double actualEnergyConsumptionInJoule=energyConsumtpionTable.getEnergyConsumptionInJoule(vehicleClassId, averageDrivenSpeedOnLinkInKmPerHour,linkFreeSpeedInMetersPerSecond ,linkLengthInMeters);
		
		assertEquals(expectedEnergyConsumptionInJoule,actualEnergyConsumptionInJoule,1.0);
	}    
}
