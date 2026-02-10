package org.matsim.contrib.drt.extension.operations.operationFacilities;

import jakarta.validation.constraints.PositiveOrZero;
import org.matsim.contrib.common.util.ReflectiveConfigGroupWithConfigurableParameterSets;
import org.matsim.core.config.ConfigGroup;

import java.net.URL;

/**
 * @author nkuehnel / MOIA
 */
public class OperationFacilitiesParams extends ReflectiveConfigGroupWithConfigurableParameterSets {

	public static final String SET_NAME = "operationFacilities";

	@Parameter
	@Comment("path to operation facility xml")
	private String operationFacilityInputFile;

	@Parameter
	@Comment("Buffer time added to both ends of a reservation window for checking facility availability. Since travel times" +
			"may be dynamic, the arrival and departure times are stochastic, which can lead to overlap conflicts. Adding a buffer" +
			"to the availability check of reservation time windows can alleviate the issue. In [s]")
	@PositiveOrZero
	private double reservationBufferTime = 5 * 60;

	public OperationFacilitiesParams() {
		super(SET_NAME);
	}

	public URL getOperationFacilityInputUrl(URL context) {
		return getOperationFacilityInputFile() == null ?
				null :
				ConfigGroup.getInputFileURL(context, getOperationFacilityInputFile());
	}

	public String getOperationFacilityInputFile() {
		return operationFacilityInputFile;
	}

	public void setOperationFacilityInputFile(String operationFacilityInputFile) {
		this.operationFacilityInputFile = operationFacilityInputFile;
	}

	public double getReservationBufferTime() {
		return reservationBufferTime;
	}

	public void setReservationBufferTime(double reservationBufferTime) {
		this.reservationBufferTime = reservationBufferTime;
	}
}
