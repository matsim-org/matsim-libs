package energy.consumption;


public class EnergyConsumptionModelBerlinLow extends
		AbstractEnergyConsumptionModelBerlin {

	@Override
	protected void SetDrivingType() {
		this.drivingType="MEDIUM";
	}

}
