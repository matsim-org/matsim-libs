package org.matsim.contrib.emissions;

public enum HbefaTechnology {
	BIFUEL_CNG_PETROL("bifuel CNG/petrol"),
	BIFUEL_LPG_PETROL("bifuel LPG/petrol"),
	DIESEL("diesel"),
	ELECTRICITY("electricity"),
	FLEX_FUEL_E85("flex-fuel E85"),
	FUEL_CELL("FuelCell"),
	LCV("LCV"),
	PETROL_2S("petrol (2S)"),
	PETROL_4S("petrol (4S)"),
	PLUG_IN_HYBRID_DIESEL_ELECTRIC("Plug-in Hybrid diesel/electric"),
	PLUG_IN_HYBRID_PETROL_ELECTRIC("Plug-in Hybrid petrol/electric");

	public final String id;

	HbefaTechnology(String id){
		this.id = id;
	}
}
