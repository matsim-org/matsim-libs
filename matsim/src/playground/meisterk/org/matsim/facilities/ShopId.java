package playground.meisterk.org.matsim.facilities;

public class ShopId {
	
	public static final String SEPARATOR = "_";
	
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

	public ShopId(String shopIdString) throws java.lang.ArrayIndexOutOfBoundsException {
		
		String tokens[] = shopIdString.split(ShopId.SEPARATOR);
		
		this.retailer = tokens[0];
		this.shopType = tokens[1]; 
		this.shopDescription = tokens[2]; 
		this.businessRegion = tokens[3];
		this.postcode = tokens[4];
		this.city = tokens[5]; 
		this.street = tokens[6];
		
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
	
	public String getAddressForGeocoding() {
		return this.street + ", " + this.city + ", Schweiz";
	}

	public String getRetailer() {
		return retailer;
	}

	public String getShopType() {
		return shopType;
	}

	public String getShopDescription() {
		return shopDescription;
	}

	public String getBusinessRegion() {
		return businessRegion;
	}

	public String getPostcode() {
		return postcode;
	}

	public String getCity() {
		return city;
	}

	public String getStreet() {
		return street;
	}
	
}