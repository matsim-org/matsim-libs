package playground.anhorni.locationchoice.preprocess.facilities.assembleFacilitiesVariousSources;

import org.matsim.core.utils.geometry.CoordImpl;

public class ZHFacilityComposed {
	
	private String id = "-1";
	private double x = -1;
	private double y = -1;
	private String retailerCategory ="-1";
	private String name = "-1";
	
	private String PLZ = "-1";
	private String city = "-1";
	private String street = "-1";
	private String HNR = "-1";
	private String sizeCategory = "-1";
	private double size = -1;
	private String shopType = "-1";
	private String desc = "-1";
	private int parkingLots = -1;
	private double parkingCostsPerHour = -1;
	
	private double opentimes[][] = {{-1,-1,-1,-1}, 
									{-1,-1,-1,-1},
									{-1,-1,-1,-1},
									{-1,-1,-1,-1},
									{-1,-1,-1,-1},
									{-1,-1,-1,-1},
									{-1,-1,-1,-1}};
	
	
	public ZHFacilityComposed(String id, String retailerCategory, String name, String street, String hnr,
			String plz, String city, double x, double y, String shopType, String desc) {
		super();
		this.id = id;
		this.retailerCategory = retailerCategory;
		this.name = name;
		this.street = street;
		HNR = hnr;
		PLZ = plz;
		this.city = city;
		this.x = x;
		this.y = y;
		this.shopType = shopType;
		this.desc = desc;
	}
	
	
	public String getId() {
		return id;
	}
	public String getRetailerCategory() {
		return retailerCategory;
	}

	public void setRetailerCategory(String retailerCategory) {
		this.retailerCategory = retailerCategory;
	}

	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getStreet() {
		return street;
	}
	public void setStreet(String street) {
		this.street = street;
	}
	public String getHNR() {
	 return this.HNR;		
	}
	public void setHNR(String hnr) {
		HNR = hnr;
	}
	public String getPLZ() {
		return PLZ;
	}
	public void setPLZ(String plz) {
		PLZ = plz;
	}
	public String getCity() {
		return city;
	}
	public void setCity(String city) {
		this.city = city;
	}
	public double getX() {
		return x;
	}
	public void setX(double x) {
		this.x = x;
	}
	public double getY() {
		return y;
	}
	public void setY(double y) {
		this.y = y;
	}

	public CoordImpl getCoords() {
		return new CoordImpl(this.x,this.y);
	}


	public String getSizeCategory() {
		return sizeCategory;
	}


	public void setSizeCategory(String sizeCategory) {
		this.sizeCategory = sizeCategory;
	}


	public double getSize() {
		return size;
	}


	public void setSize(double size) {
		this.size = size;
	}


	public String getShopType() {
		return shopType;
	}


	public void setShopType(String shopType) {
		this.shopType = shopType;
	}


	public double getHrsWeek() {
		double hrs = 0.0;
		for (int i = 0; i < 7; i++) {
			for (int j = 0; j < 4; j = j + 2) {
				hrs += opentimes[i][j + 1] - opentimes[i][j];
			}
		}
		return hrs;
	}

	public int getParkingLots() {
		return parkingLots;
	}


	public void setParkingLots(int parkingLots) {
		this.parkingLots = parkingLots;
	}


	public double getParkingCostsPerHour() {
		return parkingCostsPerHour;
	}


	public void setParkingCostsPerHour(double parkingCostsPerHour) {
		this.parkingCostsPerHour = parkingCostsPerHour;
	}


	public double[][] getOpentimes() {
		return opentimes;
	}


	public void setOpentimes(double[][] opentimes) {
		this.opentimes = opentimes;
	}


	public String getDesc() {
		return desc;
	}


	public void setDesc(String desc) {
		this.desc = desc;
	}
}
