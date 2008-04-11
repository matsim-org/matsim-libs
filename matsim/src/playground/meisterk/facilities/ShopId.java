package playground.meisterk.facilities;

public class ShopId {
	
	private static final String SEPARATOR = "_";
	
	private String retailer = null;
	private String shopType = null;
	private String shopDescription = null;
	private String businessRegion = null;
	private String postcode = null;
	private String city = null;
	private String street = null;
	
	public ShopId(String retailer, String shopType, String shopDescription,
			String businessRegion, String postcode, String city, String street) {
		super();
		this.retailer = retailer;
		this.shopType = shopType;
		this.shopDescription = shopDescription;
		this.businessRegion = businessRegion;
		this.postcode = postcode;
		this.city = city;
		this.street = street;
	}


	public String getShopId() {
		
		String shopId = new String("");

		String[] attributes = new String[]{
				this.retailer, 
				this.shopType, 
				this.shopDescription, 
				this.businessRegion,
				this.postcode,
				this.city, 
				this.street
				};
		
		for (String str : attributes) {
			shopId = shopId.concat(str);
			if (!str.equals(attributes[attributes.length - 1])) {
				shopId = shopId.concat(ShopId.SEPARATOR);
			}
		}
		
		return shopId;
		
	}
	
	public String getAddress() {
		return this.street + ", " + this.postcode + " " + this.city + ", Schweiz";
	}
	
}