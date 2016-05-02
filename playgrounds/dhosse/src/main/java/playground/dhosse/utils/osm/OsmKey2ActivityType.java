package playground.dhosse.utils.osm;

import java.util.Set;

import org.matsim.core.utils.collections.CollectionUtils;


public class OsmKey2ActivityType {

	//SHOP
	//grocery
	public static final Set<String> groceryShops = CollectionUtils.stringToSet(
			"alcohol,bakery,beverages,butcher,cheese,chocolate,coffee,confectionery," +
			"convenience,deli,dairy,farm,greengrocer,pasta,pastry,seafood,tea,wine," +
			"supermarket");
	
	//misc
	public static final Set<String> miscShops = CollectionUtils.stringToSet(
		"department_store,general,kiosk,mall,supermarket,baby_goods,bag,boutique,clothes,"+
		"fabric,fashion,jewelry,leather,shoes,tailor,watches,charity,second_hand,"+
		"variety_store,beauty,chemist,cosmetics,erotic,hearing_aids,herbalist,"+
		"medical_supply,nutrition_supplements,optician,perfumery,bathroom_furnishing,doityourself,"+
		"electrical,energy,florist,garden_centre,garden_furniture,gas,glaziery,hardware,"+
		"locksmith,paint,trade,antiques,bed,candles,carpet,curtain,furniture,"+
		"interior_design,kitchen,lamps,window_blind,computer,electronics,hifi,mobile_phone,"+
		"radiotechnics,vacuum_cleaner,bicycle,car,car_repair,car_parts,fishing,free_flying,"+
		"hunting,motorcycle,outdoor,scuba_diving,sports,tyres,swimming_pool,art,craft,"+
		"frame,games,model,music,musical_instrument,photo,trophy,video,video_games,"+
		"anime,books,gift,lottery,newsagent,stationery,ticket,copyshop,dry_cleaning,"+
		"e-cigarette,funeral_directors,laundry,money_lender,pawnroker,pet,pyrotechnics,"+
		"religion,tobacco,toys,travel_agency,weapons");
	
	//EDUCATION
	public static final Set<String> education = CollectionUtils.stringToSet(
		"college,kindergarten,school,university");
	
	//LEISURE
	public static final Set<String> leisure = CollectionUtils.stringToSet(
			"arts_centre,brothel,casino,cinema,community_centre,gambling,nightclub,planetarium,social_centre,"
			+ "theatre,"
			+ "adult_gaming_centre,amusement_arcade,beach_resort,bandstand,bird_hide,dance,dog_park,firepit,"
			+ "fishing,garden,golf_course,hackerspace,ice_rink,marina,miniature_golf,nature_reserve,park,"
			+ "pitch,playground,slipway,sports_centre,stadium,summer_camp,swimming_pool,swimming_area,track,"
			+ "water_park,wildlife_hide");
	
	//OTHER
	public static final Set<String> otherPlaces = CollectionUtils.stringToSet(
		"hairdresser,massage,tattoo,library,public_bookcase,music_school,driving_school,"
		+ "bar,bbq,biergarten,cafe,drinking_water,fast_food,food_court,ice_cream,pub,restaurant,"
		+ "atm,bank,bureau_de_change,credit_institution,microfinance,microfinance_bank,"
		+ "clinic,dentist,doctors,hospital,pharmacy,veterinary");
	
}
