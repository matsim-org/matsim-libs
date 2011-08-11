package playground.gregor.grips.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;


import net.opengis.gml.v_3_1_1.AbstractFeatureType;
import net.opengis.wfs.v_1_1_0.FeatureCollectionType;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.io.IOUtils;

import playground.gregor.grips.helper.Building;
import playground.gregor.grips.jaxb.EDL.building.BuildingType;


public class BuildingsDeserializer {

	public List<Building> deserialize(String file) {
		JAXBElement<FeatureCollectionType> jaxfts = parseFeatureCollection(file);
		FeatureCollectionType fts = jaxfts.getValue();
		List<JAXBElement<? extends AbstractFeatureType>> list = fts.getFeatureMembers().getFeature();
		List<Building> blds = processList(list);
		return null;
	}

	private List<Building> processList(
			List<JAXBElement<? extends AbstractFeatureType>> list) {

		for (JAXBElement<? extends AbstractFeatureType>  el : list) {
			AbstractFeatureType val = el.getValue();
			if (val instanceof BuildingType) {
				System.out.println(((BuildingType)val).getFloorplan());
			} else {
				throw new RuntimeException("Unexpected feature type:" + val + "! Aboarding...");
			}
		}
		return null;
	}

	private JAXBElement<FeatureCollectionType> parseFeatureCollection(
			String file) {

		JAXBElement<FeatureCollectionType> ret;
		JAXBContext jc;
		try {
			//			jc = JAXBContext.newInstance(playground.gregor.grips.jaxb.EDL.building.ObjectFactory.class);
			Class [] clazz = {playground.gregor.grips.jaxb.EDL.building.ObjectFactory.class,net.opengis.wfs.v_1_1_0.ObjectFactory.class};
			jc = JAXBContext.newInstance(clazz);
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
		Unmarshaller u;
		try {
			u = jc.createUnmarshaller();
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
		InputStream stream = null;
		try {
			stream = IOUtils.getInputstream(file);
			//			URL url = new URL("http://schemas.opengis.net/gml/3.2.1/gml.xsd");
			//			XMLSchemaFactory schemaFac = new XMLSchemaFactory();
			//			Schema s = schemaFac.newSchema(url);
			//			u.setSchema(s);
			ret =  (JAXBElement<FeatureCollectionType>) u.unmarshal(stream);
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (stream != null) { stream.close();	}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		return ret;
	}


}
