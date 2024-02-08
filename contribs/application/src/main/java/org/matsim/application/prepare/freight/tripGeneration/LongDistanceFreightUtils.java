package org.matsim.application.prepare.freight.tripGeneration;

import org.matsim.api.core.v01.population.Person;

import java.util.List;
import java.util.Map;

public class LongDistanceFreightUtils {

	public enum TransportType {
		FTL, LTL
	}

	public enum LongDistanceTravelMode {
		road, train, ship, unallocated
	}

	private final static Map <TransportType, List<Integer>> transportTypeMap = Map.of(
			TransportType.FTL, List.of(10, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 71, 72, 32, 33, 80, 90, 100, 110, 120, 160, 170, 190, 200),
			TransportType.LTL, List.of(40, 50, 60, 130, 140, 150, 180)
	);

	/** Returns the transport type ('FTL' = full truck load; 'LTL' = less than truck load) for a given goods type
	 * @param goodsType
	 * @return
	 */
	public static TransportType findTransportType(int goodsType) {
		for (Map.Entry<TransportType, List<Integer>> entry : transportTypeMap.entrySet()) {
			if (entry.getValue().contains(goodsType)) {
				return entry.getKey();
			}
		}
		throw new IllegalArgumentException("Goods type " + goodsType + " not found in transportTypeMap");
	}

	static void writeCommonAttributesV1(Person person, TripRelation tripRelation, String tripRelationId){
		setFreightSubpopulation(person);
		setTripRelationIndex(person, tripRelationId);
		setModePreRun(person, tripRelation);
		setModeMainRun(person, tripRelation);
		setModePostRun(person, tripRelation);
		setOriginCell(person, tripRelation);
		setOriginCellMainRun(person, tripRelation);
		setDestinationCellMainRun(person, tripRelation);
		setDestinationCell(person, tripRelation);
		setGoodsTypePreRun(person, tripRelation);
		setGoodsTypeMainRun(person, tripRelation);
		setGoodsTypePostRun(person, tripRelation);
		setTonsPerYearMainRun(person, tripRelation);
	}
	static void writeCommonAttributesV2(Person person, TripRelation tripRelation, String tripRelationId){
		writeCommonAttributesV1(person, tripRelation, tripRelationId);
		setOriginTerminal(person, tripRelation);
		setDestinationTerminal(person, tripRelation);
		setOriginTerminal(person, tripRelation);
		setDestinationTerminal(person, tripRelation);
		setTransportType(person, tripRelation);
		setTonsPerYearPreRun(person, tripRelation);
		setTonsPerYearPostRun(person, tripRelation);
		setTonKMPerYearPreRun(person, tripRelation);
		setTonKMPerYearMainRun(person, tripRelation);
		setTonKMPerYearPostRun(person, tripRelation);
	}

	private static void setTransportType(Person person, TripRelation tripRelation) {
		person.getAttributes().putAttribute("transport_type", String.valueOf(findTransportType(Integer.parseInt(tripRelation.getGoodsTypeMainRun()))));
	}
	private static void setFreightSubpopulation(Person person) {
		person.getAttributes().putAttribute("subpopulation", "freight");
	}
	private static void setTripRelationIndex(Person person, String tripRelationId) {
		person.getAttributes().putAttribute("trip_relation_index", tripRelationId);
	}

	private static void setModePreRun(Person person, TripRelation tripRelation) {
		setMode(person, tripRelation.getModePreRun(), "mode_pre-run");
	}

	static LongDistanceTravelMode getModePreRun(Person person) {
		return LongDistanceTravelMode.valueOf(person.getAttributes().getAttribute("mode_pre-run").toString());
	}

	private static void setModeMainRun(Person person, TripRelation tripRelation) {
		setMode(person, tripRelation.getModeMainRun(), "mode_main-run");
	}

	static LongDistanceTravelMode getModeMainRun(Person person) {
		return LongDistanceTravelMode.valueOf(person.getAttributes().getAttribute("mode_main-run").toString());
	}

	private static void setModePostRun(Person person, TripRelation tripRelation) {
		setMode(person, tripRelation.getModePostRun(), "mode_post-run");
	}

	private static void setMode(Person person, String mode, String attributeName) {
		LongDistanceTravelMode longDistanceTravelMode = switch (mode) {
			case "0" -> LongDistanceTravelMode.unallocated;
			case "1" -> LongDistanceTravelMode.train;
			case "2" -> LongDistanceTravelMode.road;
			case "3" -> LongDistanceTravelMode.ship;
			default -> throw new IllegalArgumentException("Mode " + mode + " not found");
		};
		person.getAttributes().putAttribute(attributeName, longDistanceTravelMode);
	}
	static LongDistanceTravelMode getModePostRun(Person person) {
		return LongDistanceTravelMode.valueOf(person.getAttributes().getAttribute("mode_post-run").toString());
	}
	private static void setOriginCell(Person person, TripRelation tripRelation) {
		person.getAttributes().putAttribute("origin_cell", tripRelation.getOriginCell());
	}
	static String getOriginCell(Person person) {
		return person.getAttributes().getAttribute("origin_cell").toString();
	}
	private static void setOriginCellMainRun(Person person, TripRelation tripRelation) {
		person.getAttributes().putAttribute("origin_cell_main_run", tripRelation.getOriginCellMainRun());
	}
	static String getOriginCellMainRun(Person person) {
		return person.getAttributes().getAttribute("origin_cell_main_run").toString();
	}
	private static void setDestinationCellMainRun(Person person, TripRelation tripRelation) {
		person.getAttributes().putAttribute("destination_cell_main_run", tripRelation.getDestinationCellMainRun());
	}
	static String getDestinationCellMainRun(Person person) {
		return person.getAttributes().getAttribute("destination_cell_main_run").toString();
	}
	private static void setDestinationCell(Person person, TripRelation tripRelation) {
		person.getAttributes().putAttribute("destination_cell", tripRelation.getDestinationCell());
	}
	static String getDestinationCell(Person person) {
		return person.getAttributes().getAttribute("destination_cell").toString();
	}
	private static void setOriginTerminal(Person person, TripRelation tripRelation) {
		person.getAttributes().putAttribute("origin_terminal", tripRelation.getOriginTerminal());
	}
	static String getOriginTerminal(Person person) {
		return person.getAttributes().getAttribute("origin_terminal").toString();
	}
	private static void setDestinationTerminal(Person person, TripRelation tripRelation) {
		person.getAttributes().putAttribute("destination_terminal", tripRelation.getDestinationTerminal());
	}
	static String getDestinationTerminal(Person person) {
		return person.getAttributes().getAttribute("destination_terminal").toString();
	}
	private static void setGoodsTypeMainRun(Person person, TripRelation tripRelation) {
		person.getAttributes().putAttribute("goods_type_main-run", tripRelation.getGoodsTypeMainRun());
	}
	static int getGoodsTypeMainRun(Person person) {
		return Integer.parseInt(person.getAttributes().getAttribute("goods_type_main-run").toString());
	}
	private static void setGoodsTypePreRun(Person person, TripRelation tripRelation) {
		person.getAttributes().putAttribute("goods_type_pre-run", tripRelation.getGoodsTypePreRun());
	}

	static int getGoodsTypePreRun(Person person) {
		return Integer.parseInt(person.getAttributes().getAttribute("goods_type_pre-run").toString());
	}
	private static void setGoodsTypePostRun(Person person, TripRelation tripRelation) {
		person.getAttributes().putAttribute("goods_type_post-run", tripRelation.getGoodsTypePostRun());
	}
	static int getGoodsTypePostRun(Person person) {
		return Integer.parseInt(person.getAttributes().getAttribute("goods_type_post-run").toString());
	}
	private static void setTonsPerYearPreRun(Person person, TripRelation tripRelation) {
		person.getAttributes().putAttribute("tons_per_year_pre-run", tripRelation.getTonsPerYearPreRun());
	}
	static double getTonsPerYearPreRun(Person person) {
		return Double.parseDouble(person.getAttributes().getAttribute("tons_per_year_pre-run").toString());
	}
	private static void setTonsPerYearMainRun(Person person, TripRelation tripRelation) {
		person.getAttributes().putAttribute("tons_per_year_main-run", tripRelation.getTonsPerYearMainRun());
	}
	static double getTonsPerYearMainRun(Person person) {
		return Double.parseDouble(person.getAttributes().getAttribute("tons_per_year_main-run").toString());
	}
	private static void setTonsPerYearPostRun(Person person, TripRelation tripRelation) {
		person.getAttributes().putAttribute("tons_per_year_post-run", tripRelation.getTonsPerYearPostRun());
	}
	static double getTonsPerYearPostRun(Person person) {
		return Double.parseDouble(person.getAttributes().getAttribute("tons_per_year_post-run").toString());
	}
	private static void setTonKMPerYearPreRun(Person person, TripRelation tripRelation) {
		person.getAttributes().putAttribute("tonKM_per_year_pre-run", tripRelation.getTonKMPerYearPreRun());
	}
	private static void setTonKMPerYearMainRun(Person person, TripRelation tripRelation) {
		person.getAttributes().putAttribute("tonKM_per_year_main-run", tripRelation.getTonKMPerYearMainRun());
	}
	private static void setTonKMPerYearPostRun(Person person, TripRelation tripRelation) {
		person.getAttributes().putAttribute("tonKM_per_year_post-run", tripRelation.getTonKMPerYearPostRun());
	}
	static void setTripType(Person person, String tripType) {
		person.getAttributes().putAttribute("trip_type", tripType);
	}
	static String getTripType(Person person) {
		return person.getAttributes().getAttribute("trip_type").toString();
	}
}
