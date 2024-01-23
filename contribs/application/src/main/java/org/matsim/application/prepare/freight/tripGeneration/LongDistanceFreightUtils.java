package org.matsim.application.prepare.freight.tripGeneration;

import org.matsim.api.core.v01.population.Person;

import java.util.List;
import java.util.Map;

public class LongDistanceFreightUtils {

	public enum TransportType {
		FTL, LTL
	}

	private final static Map <TransportType, List<Integer>> transportTypeMap = Map.of(
			TransportType.FTL, List.of(10, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 71, 72, 32, 33, 80, 90, 100, 110, 120, 160, 170, 190, 200),
			TransportType.LTL, List.of(40, 50, 60, 130, 140, 150, 180)
	);

	/** Returns the transport type ('FTL' = full truck load; 'LTL' = less than truck load) for a given goods type
	 * @param goodsType
	 * @return
	 */
	public static TransportType getTransportType(int goodsType) {
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
		setPreRunMode(person, tripRelation);
		setMainRunMode(person, tripRelation);
		setPostRunMode(person, tripRelation);
		setOriginOriginCell(person, tripRelation);
		setOriginCellMainRun(person, tripRelation);
		setDestinationCellMainRun(person, tripRelation);
		setDestinationCell(person, tripRelation);
		setGoodsTypeMainRun(person, tripRelation);
		setTonsPerYearMainRun(person, tripRelation);
	}
	static void writeCommonAttributesV2(Person person, TripRelation tripRelation, String tripRelationId){
		writeCommonAttributesV1(person, tripRelation, tripRelationId);
		setOriginTerminal(person, tripRelation);
		setDestinationTerminal(person, tripRelation);
		setOriginTerminal(person, tripRelation);
		setDestinationTerminal(person, tripRelation);
		setGoodsTypePreRun(person, tripRelation);
		setGoodsTypePostRun(person, tripRelation);
		setTransportType(person, tripRelation);
		setTonsPerYearPreRun(person, tripRelation);
		setTonsPerYearPostRun(person, tripRelation);
		setTonKMPerYearPreRun(person, tripRelation);
		setTonKMPerYearMainRun(person, tripRelation);
		setTonKMPerYearPostRun(person, tripRelation);
	}

	private static void setTransportType(Person person, TripRelation tripRelation) {
		person.getAttributes().putAttribute("transport_type_main-run", String.valueOf(getTransportType(Integer.parseInt(tripRelation.getGoodsTypeMainRun()))));
	}

	static void setFreightSubpopulation(Person person) {
		person.getAttributes().putAttribute("subpopulation", "freight");
	}

	static void setTripRelationIndex(Person person, String tripRelationId) {
		person.getAttributes().putAttribute("trip_relation_index", tripRelationId);
	}
	static void setPreRunMode(Person person, TripRelation tripRelation) {
		person.getAttributes().putAttribute("mode_pre-run", tripRelation.getModePreRun());
	}
	static void setMainRunMode(Person person, TripRelation tripRelation) {
		person.getAttributes().putAttribute("mode_main-run", tripRelation.getModeMainRun());
	}
	static void setPostRunMode(Person person, TripRelation tripRelation) {
		person.getAttributes().putAttribute("mode_post-run", tripRelation.getModePostRun());
	}
	static void setOriginOriginCell(Person person, TripRelation tripRelation) {
		person.getAttributes().putAttribute("origin_cell", tripRelation.getOriginCell());
	}
	static void setOriginCellMainRun(Person person, TripRelation tripRelation) {
		person.getAttributes().putAttribute("origin_cell_main_run", tripRelation.getOriginCellMainRun());
	}
	static void setDestinationCellMainRun(Person person, TripRelation tripRelation) {
		person.getAttributes().putAttribute("destination_cell_main_run", tripRelation.getDestinationCellMainRun());
	}
	static void setDestinationCell(Person person, TripRelation tripRelation) {
		person.getAttributes().putAttribute("destination_cell", tripRelation.getDestinationCell());
	}
	static void setOriginTerminal(Person person, TripRelation tripRelation) {
		person.getAttributes().putAttribute("origin_terminal", tripRelation.getOriginTerminal());
	}
	static void setDestinationTerminal(Person person, TripRelation tripRelation) {
		person.getAttributes().putAttribute("destination_terminal", tripRelation.getDestinationTerminal());
	}
	static void setGoodsTypePreRun(Person person, TripRelation tripRelation) {
		person.getAttributes().putAttribute("goods_type_pre-run", tripRelation.getGoodsTypePreRun());
	}
	static void setGoodsTypeMainRun(Person person, TripRelation tripRelation) {
		person.getAttributes().putAttribute("goods_type_main-run", tripRelation.getGoodsTypeMainRun());
	}
	static void setGoodsTypePostRun(Person person, TripRelation tripRelation) {
		person.getAttributes().putAttribute("goods_type_post-run", tripRelation.getGoodsTypePostRun());
	}
	static void setTonsPerYearPreRun(Person person, TripRelation tripRelation) {
		person.getAttributes().putAttribute("tons_per_year_pre-run", tripRelation.getTonsPerYearPreRun());
	}
	static void setTonsPerYearMainRun(Person person, TripRelation tripRelation) {
		person.getAttributes().putAttribute("tons_per_year_main-run", tripRelation.getTonsPerYearMainRun());
	}
	static void setTonsPerYearPostRun(Person person, TripRelation tripRelation) {
		person.getAttributes().putAttribute("tons_per_year_post-run", tripRelation.getTonsPerYearPostRun());
	}
	static void setTonKMPerYearPreRun(Person person, TripRelation tripRelation) {
		person.getAttributes().putAttribute("tonKM_per_year_pre-run", tripRelation.getTonKMPerYearPreRun());
	}
	static void setTonKMPerYearMainRun(Person person, TripRelation tripRelation) {
		person.getAttributes().putAttribute("tonKM_per_year_main-run", tripRelation.getTonKMPerYearMainRun());
	}
	static void setTonKMPerYearPostRun(Person person, TripRelation tripRelation) {
		person.getAttributes().putAttribute("tonKM_per_year_post-run", tripRelation.getTonKMPerYearPostRun());
	}
}
