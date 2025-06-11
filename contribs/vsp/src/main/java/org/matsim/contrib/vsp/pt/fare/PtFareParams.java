package org.matsim.contrib.vsp.pt.fare;

import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Map;

public abstract class PtFareParams extends ReflectiveConfigGroup {
	public static final String FARE_ZONE_SHP = "fareZoneShp";
	public static final String ORDER = "order";
	public static final String TRANSACTION_PARTNER = "transactionPartner";
	public static final String DESCRIPTION = "description";

	private int order;

	//nullable. If null, it will be ignored.
	private String fareZoneShp;
	private String transactionPartner;
	private String description;

	public PtFareParams(String name) {
		super(name);
	}

	@Override
	public Map<String, String> getComments() {
		var map = super.getComments();
		map.put(FARE_ZONE_SHP, "Shp file with fare zone(s). This parameter is only used for PtFareCalculationModel 'fareZoneBased'.");
		map.put(ORDER, "Order of this fare calculation in the list of fare calculations. Lower values mean to be evaluated first.");
		map.put(TRANSACTION_PARTNER, "The transaction partner for the fare calculation. This is used in the PersonMoneyEvent.");
		map.put(DESCRIPTION, "Description of the fare zone.");
		return map;
	}

	@StringGetter(FARE_ZONE_SHP)
	public String getFareZoneShp() {
		return fareZoneShp;
	}

	@StringSetter(FARE_ZONE_SHP)
	public void setFareZoneShp(String fareZoneShp) {
		this.fareZoneShp = fareZoneShp;
	}

	@StringGetter(ORDER)
	public int getOrder() {
		return order;
	}

	@StringSetter(ORDER)
	public void setOrder(int order) {
		this.order = order;
	}

	@StringGetter(TRANSACTION_PARTNER)
	public String getTransactionPartner() {
		return transactionPartner;
	}

	@StringSetter(TRANSACTION_PARTNER)
	public void setTransactionPartner(String transactionPartner) {
		this.transactionPartner = transactionPartner;
	}

	@StringGetter(DESCRIPTION)
	public String getDescription() {
		return description;
	}

	@StringSetter(DESCRIPTION)
	public void setDescription(String description) {
		this.description = description;
	}
}
