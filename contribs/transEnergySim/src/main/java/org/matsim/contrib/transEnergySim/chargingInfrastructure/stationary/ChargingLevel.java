package org.matsim.contrib.transEnergySim.chargingInfrastructure.stationary;

public class ChargingLevel {

	String name;
	
	public ChargingLevel(String name){
		this.name=name;
	} 
	
	public ChargingLevel(ChargingLevel chargingLevel){
		this.name=chargingLevel.getName();
	} 
	
	public String getName(){
		return name;
	}
	
	@Override
    public boolean equals(Object otherObject) {
 
        if (otherObject == this) {
            return true;
        }
 
        if (!(otherObject instanceof ChargingLevel)) {
            return false;
        }
         
        ChargingLevel otherChargingLevel = (ChargingLevel) otherObject;
         
        return getName().equalsIgnoreCase(otherChargingLevel.getName());
    }
	
	
	public static final ChargingLevel LEVEL_1;
	public static final ChargingLevel LEVEL_2_J1772;
	public static final ChargingLevel LEVEL_3;
	public static final ChargingLevel LEVEL_4;
	public static final ChargingLevel DC_CHAdeMO;
	public static final ChargingLevel DC_Combo;
	
	static {
		LEVEL_1=new ChargingLevel("LEVEL_1");
		LEVEL_2_J1772=new ChargingLevel("LEVEL_2_J1772");
		LEVEL_3=new ChargingLevel("LEVEL_3");
		LEVEL_4=new ChargingLevel("LEVEL_4");
		DC_CHAdeMO=new ChargingLevel("DC_CHAdeMO");
		DC_Combo=new ChargingLevel("DC_Combo");
    }
	
}
