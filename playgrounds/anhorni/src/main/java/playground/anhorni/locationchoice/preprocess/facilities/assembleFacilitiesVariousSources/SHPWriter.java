package playground.anhorni.locationchoice.preprocess.facilities.assembleFacilitiesVariousSources;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;

import playground.anhorni.choiceSetGeneration.helper.ZHFacility;

import com.vividsolutions.jts.geom.Point;

public class SHPWriter {

	private FeatureType featureType;

	private HashMap<Integer, String> mapBZ = new HashMap<Integer, String>();
	private HashMap<String, String> mapMZ = new HashMap<String, String>();
	private BufferedWriter statistics;

	public SHPWriter() {

	try {
		statistics =  IOUtils.getBufferedWriter("output/statistics.txt");

	} catch (final IOException e) {
		Gbl.errorMsg(e);
}
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
		mapMZ.put("8", "Warenh�user");
		mapMZ.put("9", "Supermarkt");
		mapMZ.put("10", "B�ckerei");
		mapMZ.put("11", "Metzgerei");
		mapMZ.put("12", "Grocery, fruits and vegetables");
	}


	public void write(List<Hectare> hectares)  {

		this.init();
		this.initGeometries();
		ArrayList<Feature> features = new ArrayList<Feature>();

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
		try {
			if (!features.isEmpty()) {
				ShapeFileWriter.writeGeometries(features, "output/hectares.shp");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeNelsonFacilities(List<ZHFacility> facilities)  {

		this.init();
		this.initGeometries();
		ArrayList<Feature> featuresCoop = new ArrayList<Feature>();
		ArrayList<Feature> featuresMigros = new ArrayList<Feature>();
		ArrayList<Feature> featuresDenner = new ArrayList<Feature>();
		ArrayList<Feature> featuresSpar = new ArrayList<Feature>();
		ArrayList<Feature> featuresPickPay = new ArrayList<Feature>();
		ArrayList<Feature> featuresPrimo = new ArrayList<Feature>();
		ArrayList<Feature> featuresVolg = new ArrayList<Feature>();
		ArrayList<Feature> featuresWarehouses = new ArrayList<Feature>();
		ArrayList<Feature> featuresSupermarkets = new ArrayList<Feature>();
		ArrayList<Feature> featuresDiv = new ArrayList<Feature>();

		Iterator<ZHFacility> facilities_it = facilities.iterator();
		while (facilities_it.hasNext()) {
			ZHFacility facility = facilities_it.next();
			Coord coord = facility.getExactPosition();

			String shop = facility.getId() +"\t" + mapMZ.get(facility.getRetailerID().toString()) +"\t" +
				facility.getName() + "\t" + facility.getSize_descr();

			if (facility.getRetailerID().compareTo(new IdImpl("1")) == 0) {
				featuresCoop.add(this.createFeature(coord, shop));
			}
			else if (facility.getRetailerID().compareTo(new IdImpl("2")) == 0) {
				featuresMigros.add(this.createFeature(coord, shop));
			}
			else if (facility.getRetailerID().compareTo(new IdImpl("3")) == 0) {
				featuresDenner.add(this.createFeature(coord, shop));
			}
			else if (facility.getRetailerID().compareTo(new IdImpl("4")) == 0) {
				featuresSpar.add(this.createFeature(coord, shop));
			}
			else if (facility.getRetailerID().compareTo(new IdImpl("5")) == 0) {
				featuresPickPay.add(this.createFeature(coord, shop));
			}
			else if (facility.getRetailerID().compareTo(new IdImpl("6")) == 0) {
				featuresPrimo.add(this.createFeature(coord, shop));
			}
			else if (facility.getRetailerID().compareTo(new IdImpl("7")) == 0) {
				featuresVolg.add(this.createFeature(coord, shop));
			}
			else if (facility.getRetailerID().compareTo(new IdImpl("8")) == 0) {
				featuresWarehouses.add(this.createFeature(coord, shop));
			}
			else if (facility.getRetailerID().compareTo(new IdImpl("9")) == 0) {
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
		ArrayList<Feature> featuresCoop = new ArrayList<Feature>();
		ArrayList<Feature> featuresMigros = new ArrayList<Feature>();
		ArrayList<Feature> featuresDenner = new ArrayList<Feature>();
		ArrayList<Feature> featuresSpar = new ArrayList<Feature>();
		ArrayList<Feature> featuresPickPay = new ArrayList<Feature>();
		ArrayList<Feature> featuresPrimo = new ArrayList<Feature>();
		ArrayList<Feature> featuresVolg = new ArrayList<Feature>();
		ArrayList<Feature> featuresWarehouses = new ArrayList<Feature>();
		ArrayList<Feature> featuresSupermarkets = new ArrayList<Feature>();

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
		AttributeType [] attr = new AttributeType[2];
		attr[0] = AttributeTypeFactory.newAttributeType("Point", Point.class);
		attr[1] = AttributeTypeFactory.newAttributeType("Types", String.class);

		try {
			this.featureType = FeatureTypeBuilder.newFeatureType(attr, "point");
		} catch (SchemaException e) {
			e.printStackTrace();
		}
	}

	private Feature createFeature(Coord coord, String types) {

		Feature feature = null;

		try {
			feature = this.featureType.create(new Object [] {MGC.coord2Point(coord), types});
		} catch (IllegalAttributeException e) {
			e.printStackTrace();
		}
		return feature;
	}
}
