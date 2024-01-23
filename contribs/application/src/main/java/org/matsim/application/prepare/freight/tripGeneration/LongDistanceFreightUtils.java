package org.matsim.application.prepare.freight.tripGeneration;

import org.matsim.api.core.v01.population.Person;

public class LongDistanceFreightUtils {

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
		setTonsPerYearPreRun(person, tripRelation);
		setTonsPerYearPostRun(person, tripRelation);
		setTonKMPerYearPreRun(person, tripRelation);
		setTonKMPerYearMainRun(person, tripRelation);
		setTonKMPerYearPostRun(person, tripRelation);
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
