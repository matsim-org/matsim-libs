package org.matsim.application.prepare.freight.tripGeneration;

import org.matsim.api.core.v01.population.Person;

public class LongDistanceFreightUtils {

	static void writeCommonAttributesV1(Person person, TripRelation tripRelation, String tripRelationId){
		LongDistanceFreightUtils.setFreightSubpopulation(person);
		LongDistanceFreightUtils.setTripRelationIndex(person, tripRelationId);
		LongDistanceFreightUtils.setPreRunMode(person, tripRelation);
		LongDistanceFreightUtils.setMainRunMode(person, tripRelation);
		LongDistanceFreightUtils.setPostRunMode(person, tripRelation);
		LongDistanceFreightUtils.setOriginalOriginCell(person, tripRelation);
		LongDistanceFreightUtils.setOriginCellMainRun(person, tripRelation);
		LongDistanceFreightUtils.setDestinationCellMainRun(person, tripRelation);
		LongDistanceFreightUtils.setFinalDestinationCell(person, tripRelation);
		LongDistanceFreightUtils.setGoodsTypeMainRun(person, tripRelation);
		LongDistanceFreightUtils.setTonsPerYearMainRun(person, tripRelation);
	}
	static void writeCommonAttributesV2(Person person, TripRelation tripRelation, String tripRelationId){
		LongDistanceFreightUtils.setFreightSubpopulation(person);
		LongDistanceFreightUtils.setTripRelationIndex(person, tripRelationId);
		LongDistanceFreightUtils.setPreRunMode(person, tripRelation);
		LongDistanceFreightUtils.setMainRunMode(person, tripRelation);
		LongDistanceFreightUtils.setPostRunMode(person, tripRelation);
		LongDistanceFreightUtils.setOriginalOriginCell(person, tripRelation);
		LongDistanceFreightUtils.setOriginCellMainRun(person, tripRelation);
		LongDistanceFreightUtils.setDestinationCellMainRun(person, tripRelation);
		LongDistanceFreightUtils.setFinalDestinationCell(person, tripRelation);
		LongDistanceFreightUtils.setGoodsTypePreRun(person, tripRelation);
		LongDistanceFreightUtils.setGoodsTypeMainRun(person, tripRelation);
		LongDistanceFreightUtils.setGoodsTypePostRun(person, tripRelation);
		LongDistanceFreightUtils.setTonsPerYearPreRun(person, tripRelation);
		LongDistanceFreightUtils.setTonsPerYearMainRun(person, tripRelation);
		LongDistanceFreightUtils.setTonsPerYearPostRun(person, tripRelation);
		LongDistanceFreightUtils.setTonKMPerYearPreRun(person, tripRelation);
		LongDistanceFreightUtils.setTonKMPerYearMainRun(person, tripRelation);
		LongDistanceFreightUtils.setTonKMPerYearPostRun(person, tripRelation);
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
	static void setOriginalOriginCell(Person person, TripRelation tripRelation) {
		person.getAttributes().putAttribute("original_origin_cell", tripRelation.getOriginalOriginCell());
	}
	static void setOriginCellMainRun(Person person, TripRelation tripRelation) {
		person.getAttributes().putAttribute("origin_cell_main_run", tripRelation.getOriginalCellMainRun());
	}
	static void setDestinationCellMainRun(Person person, TripRelation tripRelation) {
		person.getAttributes().putAttribute("destination_cell_main_run", tripRelation.getDestinationCellMainRun());
	}
	static void setFinalDestinationCell(Person person, TripRelation tripRelation) {
		person.getAttributes().putAttribute("final_destination_cell", tripRelation.getFinalDestinationCell());
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
