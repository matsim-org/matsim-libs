package playground.gregor.grips.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import net.opengis.gml.v_3_1_1.AbstractFeatureType;
import net.opengis.gml.v_3_1_1.FeaturePropertyType;
import net.opengis.wfs.v_1_1_0.FeatureCollectionType;

import org.matsim.core.utils.io.IOUtils;

import com.vividsolutions.jts.geom.Polygon;

public class EvacuationAreaDeserializer {


	public Polygon deserialize(String uri) {
		JAXBElement<FeatureCollectionType> jaxfts = (JAXBElement<FeatureCollectionType>) parseFeatureCollection(uri);
		FeatureCollectionType fts = jaxfts.getValue();
		List<FeaturePropertyType> mbr = fts.getFeatureMember();
		List<JAXBElement<? extends AbstractFeatureType>> list = null;
		Polygon p = processList(list);
		return null;
	}

	private Polygon processList(
			List<JAXBElement<? extends AbstractFeatureType>> list) {
		for (JAXBElement<? extends AbstractFeatureType>  el : list) {
			AbstractFeatureType val = el.getValue();
			System.out.println(val);
		}
		return null;
	}

	private Object parseFeatureCollection(String uri) {
		Object ret =null;
		JAXBContext jc;
		try {
			//			jc = JAXBContext.newInstance(playground.gregor.grips.jaxb.EDL.building.ObjectFactory.class);
			Class [] clazz = {playground.gregor.grips.jaxb.EDL.evacuationarea.ObjectFactory.class,net.opengis.wfs.v_1_1_0.ObjectFactory.class};
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

			URI ur = new URI(uri);
			String schema = ur.getScheme();
			if (schema != null && schema.equals("http")) {
				stream = new URL(uri).openStream();
			} else {
				stream = IOUtils.getInputstream(uri);
			}


			//			URL url = new URL("http://schemas.opengis.net/gml/3.2.1/gml.xsd");
			//			XMLSchemaFactory schemaFac = new XMLSchemaFactory();
			//			Schema s = schemaFac.newSchema(url);
			//			u.setSchema(s);
			ret =  u.unmarshal(stream);
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
