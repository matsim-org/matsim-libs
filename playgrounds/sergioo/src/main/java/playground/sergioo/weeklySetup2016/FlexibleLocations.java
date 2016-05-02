package playground.sergioo.weeklySetup2016;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkNode;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;

public class FlexibleLocations {
	
	private enum SimpleCategory {
		
		SHOP(new Category[]{Category.CLOTHING_STORE, Category.BICYCLE_STORE, Category.HOME_GOODS_STORE, Category.JEWELRY_STORE, Category.PET_STORE, Category.HARDWARE_STORE, Category.FURNITURE_STORE, Category.PHARMACY, Category.GROCERY_OR_SUPERMARKET, Category.LIQUOR_STORE, Category.CONVENIENCE_STORE, Category.FLORIST, Category.CAR_DEALER, Category.SHOE_STORE, Category.STORE, Category.BAKERY, Category.DEPARTMENT_STORE, Category.BOOK_STORE, Category.ELECTRONICS_STORE, Category.ART_GALLERY}),
		SHOP_HIGH(new Category[]{Category.SHOPPING_MALL}),
		BUSINESS(new Category[]{Category.FINANCE, Category.LAWYER, Category.REAL_ESTATE_AGENCY, Category.EMBASSY, Category.ATM, Category.ACCOUNTING, Category.CASINO, Category.CITY_HALL, Category.INSURANCE_AGENCY, Category.TRAVEL_AGENCY, Category.BANK, Category.COURTHOUSE, Category.LOCAL_GOVERNMENT_OFFICE}),
		NEED(new Category[]{Category.SHOPPING_MALL, Category.FIRE_STATION, Category.GAS_STATION, Category.ROOFING_CONTRACTOR, Category.PLUMBER, Category.STORAGE, Category.CAR_RENTAL, Category.PAINTER, Category.MOVING_COMPANY, Category.FUNERAL_HOME, Category.POLICE, Category.POST_OFFICE, Category.GENERAL_CONTRACTOR, Category.CAR_WASH, Category.ELECTRICIAN, Category.CAR_REPAIR, Category.LAUNDRY, Category.LOCKSMITH, Category.VETERINARY_CARE}), 
		EAT(new Category[]{Category.CAFE, Category.FOOD, Category.MEAL_DELIVERY, Category.RESTAURANT, Category.BAKERY, Category.MEAL_TAKEAWAY}),
		FUN(new Category[]{Category.BAR, Category.CASINO, Category.NIGHT_CLUB}),
		SPORT(new Category[]{Category.NATURAL_FEATURE, Category.PARK, Category.AMUSEMENT_PARK, Category.SPA, Category.CAMPGROUND, Category.BEAUTY_SALON, Category.STADIUM, Category.BOWLING_ALLEY}),
		CULTURAL(new Category[]{Category.ZOO, Category.AQUARIUM, Category.MUSEUM, Category.MOVIE_THEATER, Category.MOVIE_RENTAL, Category.ART_GALLERY, Category.LIBRARY}),
		HEALTH(new Category[]{Category.DOCTOR, Category.HOSPITAL, Category.PHARMACY, Category.DENTIST, Category.PHYSIOTHERAPIST, Category.HAIR_CARE, Category.BEAUTY_SALON, Category.HEALTH, Category.SPA}),
		RELIGION(new Category[]{Category.CEMETERY, Category.PLACE_OF_WORSHIP, Category.HINDU_TEMPLE, Category.SYNAGOGUE, Category.MOSQUE, Category.CHURCH});
		private Category[] categories;
		
		private SimpleCategory(Category[] categories) {
			this.categories = categories;
		}
		private static Set<SimpleCategory> getSimpleCategories(Category category) {
			Set<SimpleCategory> simpleCategories = new HashSet<>();
			for(SimpleCategory simpleCategory:SimpleCategory.values())
				for(Category cat:simpleCategory.categories)
					if(cat==category)
						simpleCategories.add(simpleCategory);
			return simpleCategories;
		}
		
	}
	private enum Category {
		PET_STORE,
		ELECTRICIAN,
		INSURANCE_AGENCY,
		PLACE_OF_WORSHIP,
		TRAVEL_AGENCY,
		VETERINARY_CARE,
		MEAL_DELIVERY,
		CAR_REPAIR,
		LOCKSMITH,
		JEWELRY_STORE,
		BANK,
		BAR,
		COURTHOUSE,
		STADIUM,
		PHYSIOTHERAPIST,
		BAKERY,
		SYNAGOGUE,
		HAIR_CARE,
		SHOPPING_MALL,
		DEPARTMENT_STORE,
		BOWLING_ALLEY,
		MOSQUE,
		BOOK_STORE,
		CHURCH,
		ELECTRONICS_STORE,
		LOCAL_GOVERNMENT_OFFICE,
		ART_GALLERY,
		GENERAL_CONTRACTOR,
		LIBRARY,
		LAUNDRY,
		MOVIE_RENTAL,
		HOME_GOODS_STORE,
		CEMETERY,
		NIGHT_CLUB,
		CAR_WASH,
		CAMPGROUND,
		RESTAURANT,
		SHOE_STORE,
		POST_OFFICE,
		HEALTH,
		STORE,
		FOOD,
		MEAL_TAKEAWAY,
		POLICE,
		MOVIE_THEATER,
		BEAUTY_SALON,
		NATURAL_FEATURE,
		CITY_HALL,
		CAR_DEALER,
		CASINO,
		ACCOUNTING,
		FUNERAL_HOME,
		FLORIST,
		ATM,
		HINDU_TEMPLE,
		CONVENIENCE_STORE,
		SPA,
		LIQUOR_STORE,
		GROCERY_OR_SUPERMARKET,
		DENTIST,
		MOVING_COMPANY,
		PAINTER,
		AMUSEMENT_PARK,
		CAFE,
		MUSEUM,
		PHARMACY,
		CAR_RENTAL,
		GYM,
		EMBASSY,
		BICYCLE_STORE,
		FURNITURE_STORE,
		STORAGE,
		REAL_ESTATE_AGENCY,
		HARDWARE_STORE,
		CLOTHING_STORE,
		PLUMBER,
		ZOO,
		HOSPITAL,
		ROOFING_CONTRACTOR,
		PARK,
		LAWYER,
		GAS_STATION,
		FIRE_STATION,
		AQUARIUM,
		DOCTOR,
		FINANCE;
		private static Category getCategory(String cat) {
			for(Category category:Category.values())
				if(cat.toUpperCase().equals(category.name()))
					return category;
			return null;
		}
	}
	private static class Location {
		private String postalCode;
		private Set<String> places = new HashSet<>();
		private Coord coord;
		private Map<SimpleCategory, Integer> simpleCategories = new HashMap<>();
		private Set<Category> categories = new HashSet<>();
		
		private Location(String postalCode, Coord coord) {
			super();
			this.postalCode = postalCode;
			this.coord = coord;
		}
		private void addPlace(String place) {
			places.add(place);
		}
		private void addCategory(Category category) {
			categories.add(category);
			for(SimpleCategory simpleCategory:SimpleCategory.getSimpleCategories(category)) {
				Integer num = simpleCategories.get(simpleCategory);
				if(num==null)
					num = 0;
				simpleCategories.put(simpleCategory, num+1);
			}
		}
		
	}
	private static final Integer EAT_THRESHOLD = 5;
	private static final Integer SHOP_THRESHOLD = 10;
	private static final double WALK_SPEED = 4.0*1000/3600;
	private static final double WALK_BL = 1.3;
	
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, SQLException, NoConnectionException {
		DataBaseAdmin dataBaseAdminP  = new DataBaseAdmin(new File(args[0]));
		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM48N);
		Map<String, Coord> postalCodes = new HashMap<>();
		ResultSet result = dataBaseAdminP.executeQuery("SELECT postcode_char,x_coord,y_coord FROM ro_postcodes.postcodes");
		while(result.next())
			postalCodes.put(result.getString("postcode_char"), coordinateTransformation.transform(new Coord(result.getDouble("x_coord"), result.getDouble("y_coord"))));
		DataBaseAdmin dataBaseAdmin  = new DataBaseAdmin(new File(args[1]));
		Set<String> categories = new HashSet<>();
		Map<String, Location> locations = new HashMap<>();
		result = dataBaseAdmin.executeQuery("SELECT place_id,postal_code,place_name,lng,lat,categories FROM c_factual.google_places WHERE country='Singapore'");
		while(result.next()) {
			String catA = result.getString("categories");
			String[] cats = catA.substring(1, catA.length()-1).split(",");
			for(String cat:cats) {
				categories.add(cat);
				Category category = Category.getCategory(cat);
				if(category!=null) {
					String postalCode = result.getString("postal_code");
					Location location = locations.get(postalCode);
					if(location == null) {
						Coord coord = postalCodes.get(postalCode);
						if(coord==null || coord.getX()<0 || coord.getY()<0) {
							if(coord!=null)
								System.out.println(postalCode);
							coord = coordinateTransformation.transform(new Coord(result.getDouble("lng"), result.getDouble("lat")));
						}
						location = new Location(postalCode, coord);
						locations.put(postalCode, location);
					}
					location.addCategory(category);
					location.addPlace(result.getString("place_id"));
				}
			}
		}
		locations.remove(null);
		Network allNetwork = NetworkUtils.createNetwork();
		new MatsimNetworkReader(allNetwork).readFile(args[2]);
		Set<String> carMode = new HashSet<String>();
		carMode.add("car");
		NetworkImpl network = (NetworkImpl) NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(allNetwork).filter(network, carMode);
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new TransitScheduleReader(scenario).readFile(args[3]);
		TransitRouterNetwork networkPT = TransitRouterNetwork.createFromSchedule(scenario.getTransitSchedule(), config.transitRouter().getMaxBeelineWalkConnectionDistance());
		PrintWriter writer = new PrintWriter(args[4]);
		for(Location location:locations.values()) {
			boolean mall = location.simpleCategories.containsKey(SimpleCategory.SHOP_HIGH);
			if(mall) {
				Integer shopN = location.simpleCategories.get(SimpleCategory.SHOP);
				if(shopN==null)
					shopN = 0;
				location.simpleCategories.put(SimpleCategory.SHOP_HIGH, location.simpleCategories.get(SimpleCategory.SHOP_HIGH)+shopN);
			}
			for(Entry<SimpleCategory, Integer> simpleCategory:location.simpleCategories.entrySet()) {
				String simpleCategoryText = simpleCategory.getKey().name();
				if(simpleCategory.getKey() == SimpleCategory.EAT) {
					if(simpleCategory.getValue()>EAT_THRESHOLD)
						simpleCategoryText = "EAT_HIGH";
					else
						simpleCategoryText = "EAT_LOW";
				}
				else if(simpleCategory.getKey() == SimpleCategory.SHOP) {
					if(!mall && simpleCategory.getValue()>SHOP_THRESHOLD)
						simpleCategoryText = "SHOP_HIGH";
					else if(!mall)
						simpleCategoryText = "SHOP_LOW";
				}
				if(!simpleCategoryText.equals("SHOP")) {
					Node carNode = ((NetworkImpl)network).getNearestNode(location.coord);
					TransitRouterNetworkNode ptNode = networkPT.getNearestNode(location.coord);
					writer.println(location.postalCode+","+location.coord.getX()+","+location.coord.getY()+","+carNode.getId().toString()+","+CoordUtils.calcEuclideanDistance(location.coord, carNode.getCoord())*WALK_BL/WALK_SPEED+","+ptNode.getStop().getStopFacility().getId().toString()+","+CoordUtils.calcEuclideanDistance(location.coord, ptNode.getCoord())*WALK_BL/WALK_SPEED+","+simpleCategoryText+","+simpleCategory.getValue());
				}
			}
		}
		writer.close();
		for(String cat:categories)
			System.out.println(cat);
	}

}
