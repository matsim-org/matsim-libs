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

package playground.jbischoff.taxi.berlin.demand;

import java.util.*;

import org.geotools.feature.simple.*;
import org.matsim.api.core.v01.*;
import org.matsim.contrib.zone.Zone;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.tabularFileParser.*;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.*;

import playground.jbischoff.taxi.berlin.data.BeelineDistanceExractor;


public class EnergyDemand2ShpWriter
{

    /**
     * @param args
     */

    Scenario scenario;
    private SimpleFeatureBuilder builder;
    private BeelineDistanceExractor bde;
    private GeometryFactory geofac;
    private List<Entry> fromtos = new ArrayList<>();


    public static void main(String[] args)
    {
        EnergyDemand2ShpWriter dsw = new EnergyDemand2ShpWriter();
        dsw.readFile("C:/Users/Joschka/Documents/shared-svn/papers/2014/ecabTaxiData/data/shp/tsl/flows.csv");
        dsw.writeShape("C:/Users/Joschka/Documents/shared-svn/papers/2014/ecabTaxiData/data/shp/tsl/flows.shp");
        
    }


  


    private void readFile(String inputFile) {
    	
            TabularFileParserConfig config = new TabularFileParserConfig();
            config.setDelimiterTags(new String[] { ","});
            config.setFileName(inputFile);
            config.setCommentTags(new String[] { "#" });
            new TabularFileParser().parse(config, new TabularFileHandler() {
				
				@Override
				public void startRow(String[] row) {
					
					fromtos.add(new Entry(row[0], row[1], Double.parseDouble(row[2])));
					
				}
			});
        
	}





	private void writeShape(String outfile)
    {

    	Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();

        for (Entry e: this.fromtos) {
           
        		Coord from = bde.getZoneCentroid(Id.create(e.getFromLocation(), Zone.class));
        		Coord to = bde.getZoneCentroid(Id.create(e.getToLocation(),Zone.class));
        		double xarrowoffset = -10.;
        		double yarrowoffset = -10.;
        		if (to.getX()-from.getX()<0) xarrowoffset = 10.0;
        		if (to.getY()-from.getY()<0) yarrowoffset = 10.0;

            Coord arrowend = new Coord(to.getX() + xarrowoffset, to.getY() + yarrowoffset);
        		
                LineString ls = this.geofac.createLineString(new Coordinate[] {
                        MGC.coord2Coordinate(from),
                        MGC.coord2Coordinate(to),
                        MGC.coord2Coordinate(arrowend) });
                Object[] attribs = new Object[5];
                attribs[0] = ls;
                attribs[1] = e.getFromLocation().toString();
                attribs[2] = e.getToLocation().toString();
                attribs[3] = e.getValue();
                attribs[4] = 0.1 * e.getValue();
                String ftid = e.getFromLocation().toString()+"_"+e.getToLocation().toString();
                SimpleFeature sf = this.builder.buildFeature(ftid, attribs);
                features.add(sf);
            
        }
        ShapeFileWriter.writeGeometries(features, outfile);

    }

  
    public EnergyDemand2ShpWriter()
    {
        bde = new BeelineDistanceExractor();
        scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        this.geofac = new GeometryFactory();
        initFeatureType();
    }


    private void initFeatureType()
    {
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("flows");
        typeBuilder.setCRS(MGC.getCRS(TransformationFactory.WGS84_UTM33N));
        typeBuilder.add("the_geom", LineString.class);
        typeBuilder.add("fromId", String.class);
        typeBuilder.add("toId", String.class);
        
        typeBuilder.add("amount", Double.class);
        typeBuilder.add("visWidth", Double.class);

        this.builder = new SimpleFeatureBuilder(typeBuilder.buildFeatureType());
    }

}


class Entry{
	private String fromLocation;
	private String toLocation;
	private double value;
	
	protected Entry(String fromLocation, String toLocation, double value) {
		this.fromLocation = fromLocation;
		this.toLocation = toLocation;
		this.value = value;
	}

	public String getFromLocation() {
		return fromLocation;
	}

	public String getToLocation() {
		return toLocation;
	}

	public double getValue() {
		return value;
	}
	
	
}	
	
	
