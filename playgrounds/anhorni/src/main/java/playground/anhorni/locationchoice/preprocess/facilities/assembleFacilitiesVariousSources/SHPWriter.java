/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.anhorni.locationchoice.preprocess.facilities.assembleFacilitiesVariousSources;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

import playground.anhorni.choiceSetGeneration.helper.ZHFacility;

import com.vividsolutions.jts.geom.Point;

public class SHPWriter {

	private SimpleFeatureBuilder builder;

	private HashMap<Integer, String> mapBZ = new HashMap<Integer, String>();
	private HashMap<String, String> mapMZ = new HashMap<String, String>();
	private BufferedWriter statistics;

	public SHPWriter() {
		statistics =  IOUtils.getBufferedWriter("output/statistics.txt");
	}


	private void init() {
		mapBZ.put(408, "Verbraucherm�rkte  (> 2500 m2)");
		mapBZ.put(409, "Grosse Superm�rkte (1000-2499 m2)");
		mapBZ.put(410, "Kleine Superm�rkte (400-999 m2)");
		mapBZ.put(411, "Grosse Gesch�fte (100-399 m2)");
		mapBZ.put(412, "Kleine Gesch�fte (< 100 m2)");
		mapBZ.put(413, "Warenh�user");

		mapBZ.put(415, "Detailhandel mit Obst und Gem�se");
		mapBZ.put(416, "Detailhandel mit Fleisch und Fleischwaren");
		mapBZ.put(417, "Detailhandel mit Fisch und Meeresfr�chten");
		mapBZ.put(418, "Detailhandel mit Brot, Back- und S�sswaren");
		mapBZ.put(419, "Detailhandel mit Getr�nken");

		mapBZ.put(421, "Detailhandel mit Milcherzeugnissen und Eiern");
		mapBZ.put(422, "Sonstiger Fachdetailhandel mit Nahrungsmitteln, Getr�nken und Tabak");

		// ----------------------------------------

		mapMZ.put("1", "Coop");
		mapMZ.put("2", "Migros");
		mapMZ.put("3", "Denner");
		mapMZ.put("4", "Spar");
		mapMZ.put("5", "Pickpay");
		mapMZ.put("6", "Primo");
		mapMZ.put("7", "Volg");
		mapMZ.put("8", "Warenhäuser");
		mapMZ.put("9", "Supermarkt");
		mapMZ.put("10", "Bäckerei");
		mapMZ.put("11", "Metzgerei");
		mapMZ.put("12", "Grocery, fruits and vegetables");
	}


	public void write(List<Hectare> hectares)  {

		this.init();
		this.initGeometries();
		ArrayList<SimpleFeature> features = new ArrayList<SimpleFeature>();

		Iterator<Hectare> hectares_it = hectares.iterator();
		while (hectares_it.hasNext()) {
			Hectare hectare = hectares_it.next();
			Coord coord = hectare.getCoords();

			String shops = " ";
			Iterator<Integer> shops_it = hectare.getShops().iterator();
			while (shops_it.hasNext()) {
				int shop = shops_it.next();
				shops += mapBZ.get(shop) + "\t";
			}
			features.add(this.createFeature(coord, shops));
		}
		if (!features.isEmpty()) {
			ShapeFileWriter.writeGeometries(features, "output/hectares.shp");
		}
	}

	public void writeNelsonFacilities(List<ZHFacility> facilities)  {

		this.init();
		this.initGeometries();
		ArrayList<SimpleFeature> featuresCoop = new ArrayList<SimpleFeature>();
		ArrayList<SimpleFeature> featuresMigros = new ArrayList<SimpleFeature>();
		ArrayList<SimpleFeature> featuresDenner = new ArrayList<SimpleFeature>();
		ArrayList<SimpleFeature> featuresSpar = new ArrayList<SimpleFeature>();
		ArrayList<SimpleFeature> featuresPickPay = new ArrayList<SimpleFeature>();
		ArrayList<SimpleFeature> featuresPrimo = new ArrayList<SimpleFeature>();
		ArrayList<SimpleFeature> featuresVolg = new ArrayList<SimpleFeature>();
		ArrayList<SimpleFeature> featuresWarehouses = new ArrayList<SimpleFeature>();
		ArrayList<SimpleFeature> featuresSupermarkets = new ArrayList<SimpleFeature>();
		ArrayList<SimpleFeature> featuresDiv = new ArrayList<SimpleFeature>();

		Iterator<ZHFacility> facilities_it = facilities.iterator();
		while (facilities_it.hasNext()) {
			ZHFacility facility = facilities_it.next();
			Coord coord = facility.getExactPosition();

			String shop = facility.getId() +"\t" + mapMZ.get(facility.getRetailerID().toString()) +"\t" +
				facility.getName() + "\t" + facility.getSize_descr();

			if (facility.getRetailerID().compareTo(Id.create("1", Person.class)) == 0) {
				featuresCoop.add(this.createFeature(coord, shop));
			}
			else if (facility.getRetailerID().compareTo(Id.create("2", Person.class)) == 0) {
				featuresMigros.add(this.createFeature(coord, shop));
			}
			else if (facility.getRetailerID().compareTo(Id.create("3", Person.class)) == 0) {
				featuresDenner.add(this.createFeature(coord, shop));
			}
			else if (facility.getRetailerID().compareTo(Id.create("4", Person.class)) == 0) {
				featuresSpar.add(this.createFeature(coord, shop));
			}
			else if (facility.getRetailerID().compareTo(Id.create("5", Person.class)) == 0) {
				featuresPickPay.add(this.createFeature(coord, shop));
			}
			else if (facility.getRetailerID().compareTo(Id.create("6", Person.class)) == 0) {
				featuresPrimo.add(this.createFeature(coord, shop));
			}
			else if (facility.getRetailerID().compareTo(Id.create("7", Person.class)) == 0) {
				featuresVolg.add(this.createFeature(coord, shop));
			}
			else if (facility.getRetailerID().compareTo(Id.create("8", Person.class)) == 0) {
				featuresWarehouses.add(this.createFeature(coord, shop));
			}
			else if (facility.getRetailerID().compareTo(Id.create("9", Person.class)) == 0) {
				featuresSupermarkets.add(this.createFeature(coord, shop));
			}
			else {
				featuresDiv.add(this.createFeature(coord, shop));
			}

		}
		try {
			if (!featuresCoop.isEmpty()) {
				ShapeFileWriter.writeGeometries(featuresCoop, "output/nelsonFacilitiesCoop.shp");
				statistics.write("Nelson Coop: "+ featuresCoop.size() +"\n");
			}
			if (!featuresMigros.isEmpty()) {
				ShapeFileWriter.writeGeometries(featuresMigros, "output/nelsonFacilitiesMigros.shp");
				statistics.write("Nelson Mirgos: "+ featuresMigros.size() +"\n");
			}
			if (!featuresDenner.isEmpty()) {
				ShapeFileWriter.writeGeometries(featuresDenner, "output/nelsonFacilitiesDenner.shp");
				statistics.write("Nelson Denner: "+ featuresDenner.size() +"\n");
			}
			if (!featuresSpar.isEmpty()) {
				ShapeFileWriter.writeGeometries(featuresSpar, "output/nelsonFacilitiesSpar.shp");
				statistics.write("Nelson Spar: "+ featuresSpar.size() +"\n");
			}
			if (!featuresPickPay.isEmpty()) {
				ShapeFileWriter.writeGeometries(featuresPickPay, "output/nelsonFacilitiesPickPay.shp");
				statistics.write("Nelson PickPay: "+ featuresPickPay.size() +"\n");
			}
			if (!featuresPrimo.isEmpty()) {
				ShapeFileWriter.writeGeometries(featuresPrimo, "output/nelsonFacilitiesPrimo.shp");
				statistics.write("Nelson Primo: "+ featuresPrimo.size() +"\n");
			}
			if (!featuresVolg.isEmpty()) {
				ShapeFileWriter.writeGeometries(featuresVolg, "output/nelsonFacilitiesVolg.shp");
				statistics.write("Nelson Volg: "+ featuresVolg.size() +"\n");
			}
			if (!featuresWarehouses.isEmpty()) {
				ShapeFileWriter.writeGeometries(featuresWarehouses, "output/nelsonFacilitiesWarehouses.shp");
				statistics.write("Nelson Warehouses: "+ featuresWarehouses.size() +"\n");
			}
			if (!featuresSupermarkets.isEmpty()) {
				ShapeFileWriter.writeGeometries(featuresSupermarkets, "output/nelsonFacilitiesSupermarkets.shp");
				statistics.write("Nelson Supermarkets: "+ featuresSupermarkets.size() +"\n");
			}
			if (!featuresDiv.isEmpty()) {
				ShapeFileWriter.writeGeometries(featuresDiv, "output/nelsonFacilitiesDiv.shp");
				statistics.write("Nelson Div: "+ featuresDiv.size() +"\n");
			}
			statistics.newLine();
			statistics.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeDatapulsFacilities(List<ZHFacilityComposed> facilities)  {

		this.init();
		this.initGeometries();
		ArrayList<SimpleFeature> featuresCoop = new ArrayList<SimpleFeature>();
		ArrayList<SimpleFeature> featuresMigros = new ArrayList<SimpleFeature>();
		ArrayList<SimpleFeature> featuresDenner = new ArrayList<SimpleFeature>();
		ArrayList<SimpleFeature> featuresSpar = new ArrayList<SimpleFeature>();
		ArrayList<SimpleFeature> featuresPickPay = new ArrayList<SimpleFeature>();
		ArrayList<SimpleFeature> featuresPrimo = new ArrayList<SimpleFeature>();
		ArrayList<SimpleFeature> featuresVolg = new ArrayList<SimpleFeature>();
		ArrayList<SimpleFeature> featuresWarehouses = new ArrayList<SimpleFeature>();
		ArrayList<SimpleFeature> featuresSupermarkets = new ArrayList<SimpleFeature>();

		Iterator<ZHFacilityComposed> facilities_it = facilities.iterator();
		while (facilities_it.hasNext()) {
			ZHFacilityComposed facility = facilities_it.next();
			Coord coord = facility.getCoords();

			String shop = facility.getId() +"\t" + facility.getName();

			if (facility.getRetailerCategory().equals("1")) {
				featuresCoop.add(this.createFeature(coord, shop));
			}
			else if (facility.getRetailerCategory().equals("2")) {
				featuresMigros.add(this.createFeature(coord, shop));
			}
			else if (facility.getRetailerCategory().equals("3")) {
				featuresDenner.add(this.createFeature(coord, shop));
			}
			else if (facility.getRetailerCategory().equals("4")) {
				featuresSpar.add(this.createFeature(coord, shop));
			}
			else if (facility.getRetailerCategory().equals("5")) {
				featuresPickPay.add(this.createFeature(coord, shop));
			}
			else if (facility.getRetailerCategory().equals("6")) {
				featuresPrimo.add(this.createFeature(coord, shop));
			}
			else if (facility.getRetailerCategory().equals("7")) {
				featuresVolg.add(this.createFeature(coord, shop));
			}
			else if (facility.getRetailerCategory().equals("8")) {
				featuresWarehouses.add(this.createFeature(coord, shop));
			}
			else if (facility.getRetailerCategory().equals("9")) {
				featuresSupermarkets.add(this.createFeature(coord, shop));
			}
		}
		try {
			if (!featuresCoop.isEmpty()) {
				ShapeFileWriter.writeGeometries(featuresCoop, "output/datapulsFacilitiesCoop.shp");
				statistics.write("Datapuls Coop: "+ featuresCoop.size() +"\n");
			}
			if (!featuresMigros.isEmpty()) {
				ShapeFileWriter.writeGeometries(featuresMigros, "output/datapulsFacilitiesMigros.shp");
				statistics.write("Datapuls Migros: "+ featuresMigros.size() +"\n");
			}
			if (!featuresDenner.isEmpty()) {
				ShapeFileWriter.writeGeometries(featuresDenner, "output/datapulsFacilitiesDenner.shp");
				statistics.write("Datapuls Denner: "+ featuresDenner.size() +"\n");
			}
			if (!featuresSpar.isEmpty()) {
				ShapeFileWriter.writeGeometries(featuresSpar, "output/datapulsFacilitiesSpar.shp");
				statistics.write("Datapuls Spar: "+ featuresSpar.size() +"\n");
			}
			if (!featuresPickPay.isEmpty()) {
				ShapeFileWriter.writeGeometries(featuresPickPay, "output/datapulsFacilitiesPickPay.shp");
				statistics.write("Datapuls PickPay: "+ featuresPickPay.size() +"\n");
			}
			if (!featuresPrimo.isEmpty()) {
				ShapeFileWriter.writeGeometries(featuresPrimo, "output/datapulsFacilitiesPrimo.shp");
				statistics.write("Datapuls Primo: "+ featuresPrimo.size() +"\n");
			}
			if (!featuresVolg.isEmpty()) {
				ShapeFileWriter.writeGeometries(featuresVolg, "output/datapulsFacilitiesVolg.shp");
				statistics.write("Datapuls Volg: "+ featuresVolg.size() +"\n");
			}
			if (!featuresWarehouses.isEmpty()) {
				ShapeFileWriter.writeGeometries(featuresWarehouses, "output/datapulsFacilitiesWarehouses.shp");
				statistics.write("Datapuls Warehouses: "+ featuresWarehouses.size() +"\n");
			}
			if (!featuresSupermarkets.isEmpty()) {
				ShapeFileWriter.writeGeometries(featuresSupermarkets, "output/datapulsFacilitiesSupermarkets.shp");
				statistics.write("Datapuls Supermarkets: "+ featuresSupermarkets.size() +"\n");
			}
			statistics.newLine();
			statistics.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void finish() {
		try {
			statistics.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	private void initGeometries() {
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setName("point");
		b.add("location", Point.class);
		b.add("Types", String.class);
		this.builder = new SimpleFeatureBuilder(b.buildFeatureType());
	}

	private SimpleFeature createFeature(Coord coord, String types) {
		return this.builder.buildFeature(null, new Object [] {MGC.coord2Point(coord), types});
	}
}
