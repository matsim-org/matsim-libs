package vwExamples.utils.co2Emissions;

public class CO2EmissionsEntry {
	String generalMode;
	String vehicleType;
	String powerTrain;
	Double mileage = 0.0;
	Double mileageShare;
	double tailPipeEmissions;
	double energyConsomptionPer100Km;
	double energyProducationPerKwH;
	double upstreamChainFactor;
	double powerTrainShare;
	double co2Emissions = 0.0;

	public CO2EmissionsEntry(String generalMode, String vehicleType, String powerTrain, double mileageShare,
			double powerTrainShare, double tailPipeEmissions, double energyConsomptionPer100Km,
			double energyProducationPerKwH, double upstreamChainFactor) {
		this.generalMode = generalMode;
		this.vehicleType = vehicleType;
		this.powerTrain = powerTrain;
		this.mileageShare = mileageShare;
		this.powerTrainShare = powerTrainShare;
		this.tailPipeEmissions = tailPipeEmissions;
		this.energyConsomptionPer100Km = energyConsomptionPer100Km;
		this.energyProducationPerKwH = energyProducationPerKwH;
		this.upstreamChainFactor = upstreamChainFactor;
	}

	public void setMileage(Double mileage) {
		this.mileage = mileage;
	}

	public double calculateEmissions() {

		double co2Emissions = this.mileage * ((this.tailPipeEmissions * upstreamChainFactor) + // Combustion engine
				(this.energyConsomptionPer100Km / 100.0 * this.energyProducationPerKwH) // Electric engine
		) / 1000000.0;
		this.co2Emissions = co2Emissions;
		return co2Emissions;

	}

}
