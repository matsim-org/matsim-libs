/* *********************************************************************** *
 * project: org.matsim.*
 * GraphBuilder.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.survey.ivt2009.graph.io;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TObjectIntHashMap;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.math.stat.StatUtils;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;

import playground.johannes.sna.gis.CRSUtils;
import playground.johannes.sna.graph.Vertex;
import playground.johannes.sna.math.Discretizer;
import playground.johannes.sna.math.FixedSampleSizeDiscretizer;
import playground.johannes.sna.math.Histogram;
import playground.johannes.sna.math.LinearDiscretizer;
import playground.johannes.sna.snowball.SampledGraphProjection;
import playground.johannes.sna.snowball.SampledVertexDecorator;
import playground.johannes.sna.util.TXTWriter;
import playground.johannes.socialnetworks.graph.social.SocialPerson;
import playground.johannes.socialnetworks.snowball2.io.SampledGraphProjMLWriter;
import playground.johannes.socialnetworks.snowball2.spatial.SpatialSampledGraphProjectionBuilder;
import playground.johannes.socialnetworks.statistics.Correlations;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseEdge;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraphBuilder;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseVertex;
import playground.johannes.socialnetworks.survey.ivt2009.graph.io.AlterTableReader.VertexRecord;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class GraphBuilder {
	
	public static final Logger logger = Logger.getLogger(GraphBuilder.class);
	
	private SocialSparseGraphBuilder builder = new SocialSparseGraphBuilder(CRSUtils.getCRS(4326));
	
	private SpatialSampledGraphProjectionBuilder<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> projBuilder
		= new SpatialSampledGraphProjectionBuilder<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>();
	
	private Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	
	private GeometryFactory geoFacotry = new GeometryFactory();

	private ErrorLogger errLogger;
	
	private SocialSparseGraph graph;
	
	private Map<SocialSparseVertex, SampledVertexDecorator<SocialSparseVertex>> projMap;
	
	private SampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> proj;
	
	private Map<String, SocialSparseVertex> idMap;
	
	public SampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> buildGraph(
			List<String> alterTables, List<String> egoTables, List<String> sqlDumps) throws IOException {
		errLogger = new ErrorLogger();
		/*
		 * Load raw data.
		 */
		AlterTableReader alterReader = new AlterTableReader(alterTables);
		EgoTableReader egoReader = new EgoTableReader(egoTables);
		SQLDumpReader sqlReader = new SQLDumpReader(sqlDumps);
		/*
		 * Build the raw graph and a sampled projection.
		 */
		graph = builder.createGraph();
		proj = projBuilder.createGraph(graph);
		/*
		 * Create the vertices.
		 */
		projMap = new HashMap<SocialSparseVertex, SampledVertexDecorator<SocialSparseVertex>>();
		idMap = new HashMap<String, SocialSparseVertex>();
		
		for(Entry<String, VertexRecord> entry : alterReader.getVertices().entrySet()) {
			VertexRecord vRecord = entry.getValue();
			/*
			 * Extract the home location.
			 */
			Point point;
			if(vRecord.isEgo) {
				point = sqlReader.getEgoLocation(vRecord.egoSQLId);
				if(point == null) {
					/*
					 * try getting coordinates via google
					 */
					logger.info("Requesting google server for coordinates.");
					point = egoReader.getEgoLocation(vRecord.id);
				}
			} else {
				point = sqlReader.getAlterLocation(vRecord.alterKeys);
			}
			if(point == null) {
				errLogger.logNoCoordinate(vRecord.isEgo);
//				point = geoFacotry.createPoint(new Coordinate(0, 0));
			}
			/*
			 * Create a vertex and its projection.
			 */
			SocialSparseVertex vertex = builder.addVertex(graph, createPerson(vRecord, sqlReader), point);
			SampledVertexDecorator<SocialSparseVertex> vProj = projBuilder.addVertex(proj, vertex);
			/*
			 * If it is an ego set the snowball attributes.
			 */
			if(vRecord.isEgo) {
				vProj.sample(infereIterationSampled(new Integer(vRecord.id)));
				vProj.detect(vProj.getIterationSampled() - 1);
			}
			
			projMap.put(vertex, vProj);
			idMap.put(vRecord.id, vertex);
//			recordMap.put(vRecord.id, vRecord);
		}
		/*
		 * Create the edges.
		 */
		for(Tuple<VertexRecord, VertexRecord> edge : alterReader.getEdges()) {
			SocialSparseVertex v1 = idMap.get(edge.getFirst().id);
			SocialSparseVertex v2 = idMap.get(edge.getSecond().id);
			SocialSparseEdge socialEdge = builder.addEdge(graph, v1, v2);
			/*
			 * Check if we have double edges.
			 */
			if(socialEdge != null) {
				SampledVertexDecorator<SocialSparseVertex> vProj1 = projMap.get(v1);
				SampledVertexDecorator<SocialSparseVertex> vProj2 = projMap.get(v2);
			
				projBuilder.addEdge(proj, vProj1, vProj2, socialEdge);
				/*
				 * Set the snowball attributes if it is not an ego.
				 */
				if(!vProj1.isSampled()) {
					if(vProj1.isDetected())
						/*
						 * If this vertex is already detected check if the adjacent vertex has been sampled earlier.
						 */
						vProj1.detect(Math.min(vProj1.getIterationDetected(), vProj2.getIterationSampled()));
					else
						vProj1.detect(vProj2.getIterationSampled());
				}
				
				if(!vProj2.isSampled()) {
					if(vProj2.isDetected())
						/*
						 * If this vertex is already detected check if the adjacent vertex has been sampled earlier.
						 */
						vProj2.detect(Math.min(vProj2.getIterationDetected(), vProj1.getIterationSampled()));
					else
						vProj2.detect(vProj1.getIterationSampled());
				}
				/*
				 * add edge attributes
				 */
				VertexRecord rec1 = edge.getFirst();
				VertexRecord rec2 = edge.getSecond();
				double freq = 0;
				if(rec1.isEgo) {
					freq = sqlReader.getF2FFrequencey(rec1.egoSQLId, rec2.alterKeys.get(rec1.egoSQLId));
				} else {
					freq = sqlReader.getF2FFrequencey(rec2.egoSQLId, rec1.alterKeys.get(rec2.egoSQLId));
				}
				socialEdge.setFrequency(freq);
				
				socialEdge.setType(sqlReader.getEdgeType(rec1, rec2));
				
			} else {
				errLogger.logDoubleEdge();
			}
		}
		/*
		 * Sociogram
		 */
		loadSociogramData(alterReader.getVertices().values(), sqlReader);
		
		logger.info(errLogger.toString());
		return proj;
	}
	
	private SocialPerson createPerson(VertexRecord record, SQLDumpReader sqlData) {
		PersonImpl matsimPerson = new PersonImpl(Id.create(record.id, Person.class));
		SocialPerson person = new SocialPerson(matsimPerson);
		
		int age;
		if(record.isEgo)
			age = sqlData.getEgoAge(record.egoSQLId);
		else
			age = sqlData.getAlterAge(record.alterKeys);
		
		if(age < 0)
			errLogger.logNoAge(record.isEgo);
		else
			matsimPerson.setAge(age);
		
		String sex = sqlData.getSex(record);
		if(sex != null)
			matsimPerson.setSex(sex);
		else
			errLogger.logNoSex(record.isEgo);
		
		if(record.isEgo)
			matsimPerson.setLicence(sqlData.getLicense(record));
		
		if(record.isEgo)
			matsimPerson.setCarAvail(sqlData.getCarAvail(record));
		
		person.setCitizenship(sqlData.getCitizenship(record));
		person.setEducation(sqlData.getEducation(record));
		person.setIncome(sqlData.getIncome(record));
		person.setCivilStatus(sqlData.getCivilStatus(record));
		return person;
	}
	
	private Integer infereIterationSampled(Integer id) {
		if(id >= 0 && id <= 1000)
			return 0;
		else if(id > 1000 && id <= 10000)
			return 1;
		else if(id > 10000 && id <= 100000)
			return 2;
		else if(id > 100000 && id <= 400000)
			return 3;
		else if(id > 400000)
			return 4;
		else {
			logger.warn(String.format("Cannot infere sampling iteration (%1$s)", id));
			return null;
		}
	}
	
	private void loadSociogramData(Collection<VertexRecord> records, SQLDumpReader sqlData) {
		logger.info("Loading sociogram data...");
		Map<String, VertexRecord> map = sqlData.getFullAlterKeyMappping(records);
		
		TObjectIntHashMap<Vertex> rawDegrees = new TObjectIntHashMap<Vertex>();
		for(Vertex v : proj.getVertices()) {
			rawDegrees.put(v, v.getNeighbours().size());
		}
		
		int edgecnt = 0;
		int doublecnt = 0;
		int egoEdge = 0;
		
		Set<Vertex> notOkVertices = new HashSet<Vertex>();
		Set<Vertex> okVertices = new HashSet<Vertex>();
		DescriptiveStatistics notOkStats = new DescriptiveStatistics();
		DescriptiveStatistics okStats = new DescriptiveStatistics();
		
		DescriptiveStatistics numDistr = new DescriptiveStatistics();
		DescriptiveStatistics numDistrNoZero = new DescriptiveStatistics();
		DescriptiveStatistics sizeDistr = new DescriptiveStatistics();
		
		TDoubleArrayList sizeValues = new TDoubleArrayList();
		TDoubleArrayList kSizeValues = new TDoubleArrayList();
		TDoubleArrayList numValues = new TDoubleArrayList();
		TDoubleArrayList numValues2 = new TDoubleArrayList();
		TDoubleArrayList kNumValues = new TDoubleArrayList();
		
		for(VertexRecord record : records) {
			if(record.isEgo) {
			List<Set<String>> cliques = sqlData.getCliques(record);
			numDistr.addValue(cliques.size());
			
			Vertex v = idMap.get(record.id);
			numValues.add(cliques.size());
			kNumValues.add(v.getNeighbours().size());
			
			if(!cliques.isEmpty())
				numDistrNoZero.addValue(cliques.size());
			
			for(Set<String> clique : cliques) {
					sizeDistr.addValue(clique.size());
					sizeValues.add(clique.size());
					kSizeValues.add(rawDegrees.get(projMap.get(v)));
					numValues2.add(cliques.size());
					List<SocialSparseVertex> vertices = new ArrayList<SocialSparseVertex>(clique.size());
					for (String alter : clique) {
						VertexRecord r = map.get(record.egoSQLId + alter);
						if (r != null) {
							SocialSparseVertex vertex = idMap.get(r.id);
							if (vertex != null) {
								vertices.add(vertex);
							} else {
								logger.warn("Vertex not found.");
							}
						} else {
							logger.warn("Record not found.");
						}
					}

				for(int i = 0; i < vertices.size(); i++) {
					for(int j = i+1; j < vertices.size(); j++) {
						SampledVertexDecorator<SocialSparseVertex> vProj1 = projMap.get(vertices.get(i));
						SampledVertexDecorator<SocialSparseVertex> vProj2 = projMap.get(vertices.get(j));
						if (!vProj1.isSampled() && !vProj2.isSampled()) {
													
							if (Math.random() < 0.62) {
								SocialSparseEdge socialEdge = builder.addEdge(graph, vertices.get(i), vertices.get(j));
								if (socialEdge != null) {
									projBuilder.addEdge(proj, vProj1, vProj2, socialEdge);
									edgecnt++;
									
									if (vProj1.isSampled() || vProj2.isSampled()) {
										egoEdge++;
										if (vProj1.isSampled())
											notOkVertices.add(vProj1);
										else
											notOkVertices.add(vProj2);
									}
									
								} else {
									doublecnt++;
									if (vProj1.isSampled())
										okVertices.add(vProj1);
									else if (vProj2.isSampled())
										okVertices.add(vProj2);
								}
							}
						}
					}
				}
			}
		}
		}
		
		for(Vertex v : okVertices)
			okStats.addValue(rawDegrees.get(v));
		
		for(Vertex v: notOkVertices)
			notOkStats.addValue(rawDegrees.get(v));
		try {
			
		TDoubleDoubleHashMap hist = Histogram.createHistogram(okStats, new LinearDiscretizer(1), false);
		TXTWriter.writeMap(hist, "k", "n", "/Users/jillenberger/Work/socialnets/data/ivt2009/11-2011/augmented/k_ok.txt");
		
		TDoubleDoubleHashMap hist2 = Histogram.createHistogram(notOkStats, new LinearDiscretizer(1), false);
		TXTWriter.writeMap(hist2, "k", "n", "/Users/jillenberger/Work/socialnets/data/ivt2009/11-2011/augmented/k_notok.txt");
		
		TDoubleDoubleHashMap ratio = new TDoubleDoubleHashMap();
		double[] keys = hist.keys();
		for(double k : keys) {
			double val1 = hist2.get(k);
			double val2 = hist.get(k);
			
			ratio.put(k, val1/(val2+val1));
		}
		TXTWriter.writeMap(ratio, "k", "p", "/Users/jillenberger/Work/socialnets/data/ivt2009/11-2011/augmented/k_ratio.txt");
		
			logger.info("Mean num of cliques: " + numDistrNoZero.getMean());
			logger.info("Mean size: " + sizeDistr.getMean());
			logger.info("Median num of cliques: " + StatUtils.percentile(numDistrNoZero.getValues(), 50));
			logger.info("Median size: " + StatUtils.percentile(sizeDistr.getValues(), 50));
			
			TDoubleDoubleHashMap histNum = Histogram.createHistogram(numDistrNoZero, FixedSampleSizeDiscretizer.create(numDistrNoZero.getValues(), 2, 20), true);
			Histogram.normalize(histNum);
			TXTWriter.writeMap(histNum, "num", "freq", "/Users/jillenberger/Work/socialnets/data/ivt2009/11-2011/augmented/numCliques.txt");
			
			TDoubleDoubleHashMap histSize = Histogram.createHistogram(sizeDistr, FixedSampleSizeDiscretizer.create(sizeDistr.getValues(), 2, 20), true);
			Histogram.normalize(histSize);
			TXTWriter.writeMap(histSize, "size", "freq", "/Users/jillenberger/Work/socialnets/data/ivt2009/11-2011/augmented/numPersons.txt");
			
			Discretizer discretizer = FixedSampleSizeDiscretizer.create(kSizeValues.toNativeArray(), 20, 20);
			TDoubleArrayList valuesX = new TDoubleArrayList();
			for(int i = 0; i < kSizeValues.size(); i++) {
				valuesX.add(discretizer.discretize(kSizeValues.get(i)));
			}
			
			Correlations.writeToFile(Correlations.mean(valuesX.toNativeArray(), sizeValues.toNativeArray()),
					"/Users/jillenberger/Work/socialnets/data/ivt2009/11-2011/augmented/size_k.txt", "k", "size");
			
			discretizer = FixedSampleSizeDiscretizer.create(kNumValues.toNativeArray(), 20, 20);
			valuesX = new TDoubleArrayList();
			for(int i = 0; i < kNumValues.size(); i++) {
				valuesX.add(discretizer.discretize(kNumValues.get(i)));
			}
			
			Correlations.writeToFile(Correlations.mean(valuesX.toNativeArray(), numValues.toNativeArray()), 
					"/Users/jillenberger/Work/socialnets/data/ivt2009/11-2011/augmented/num_k.txt", "k", "n");
			
			Correlations.writeToFile(Correlations.mean(numValues2.toNativeArray(), sizeValues.toNativeArray()), 
					"/Users/jillenberger/Work/socialnets/data/ivt2009/11-2011/augmented/size_num.txt", "num", "size");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.info(String.format("Inserted %1$s edges, %2$s edges already present.", edgecnt, doublecnt));
		logger.info(String.format("Inserted %1$s edges between at least one ego.", egoEdge));
	}
	
	private class ErrorLogger {
		
		private int noEgoCoords;
		
		private int noAlterCoords;
		
		private int noEgoAge;
		
		private int noAlterAge;
		
		private int doubleEdges;
		
		private int noEgoSex;
		
		private int noAlterSex;
		
		public void logNoCoordinate(boolean isEgo) {
			if(isEgo)
				noEgoCoords++;
			else
				noAlterCoords++;
		}
		
		public void logDoubleEdge() {
			doubleEdges++;
		}
		
		public void logNoAge(boolean isEgo) {
			if(isEgo)
				noEgoAge++;
			else
				noAlterAge++;
		}
		
		public void logNoSex(boolean isEgo) {
			if(isEgo)
				noEgoSex++;
			else
				noAlterSex++;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("The following warnings occurred:\n");
			builder.append(String.valueOf(noEgoCoords));
			builder.append(" egos without coordinates\n");
			builder.append(String.valueOf(noAlterCoords));
			builder.append(" alters without coordinates\n");
			builder.append(String.valueOf(noEgoAge));
			builder.append(" egos without age\n");
			builder.append(String.valueOf(noAlterAge));
			builder.append(" alters without age\n");
			builder.append(String.valueOf(doubleEdges));
			builder.append(" double edges\n");
			builder.append(String.valueOf(noEgoSex));
			builder.append(" egos without sex\n");
			builder.append(String.valueOf(noAlterSex));
			builder.append(" alters without sex\n");
			return builder.toString();
		}
	}
	
	public static void main(String args[]) throws IOException {
		GraphBuilder builder = new GraphBuilder();
		
		ArrayList<String> alterTables = new ArrayList<String>();
		alterTables.add("/Users/jillenberger/Work/socialnets/data/ivt2009/11-2011/raw/alters1.txt");
		alterTables.add("/Users/jillenberger/Work/socialnets/data/ivt2009/11-2011/raw/alters2.txt");
		
		
		ArrayList<String> egoTables = new ArrayList<String>();
//		egoTables.add("/Users/jillenberger/Work/socialnets/data/ivt2009/01-2011/egos1.txt");
//		egoTables.add("/Users/jillenberger/Work/socialnets/data/ivt2009/09-2010/egos2.txt");
		
		ArrayList<String> sqlDumps = new ArrayList<String>();
		sqlDumps.add("/Users/jillenberger/Work/socialnets/data/ivt2009/11-2011/raw/snowball.csv");
		
		SampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> graph = builder.buildGraph(alterTables, egoTables, sqlDumps);
		SampledGraphProjMLWriter writer = new SampledGraphProjMLWriter(new SocialSparseGraphMLWriter());
//		writer.write(graph, "/Users/jillenberger/Work/socialnets/data/ivt2009/11-2011/graph/graph.graphml");
//		writer.write(graph, "/Users/jillenberger/Work/socialnets/data/ivt2009/09-2010/graph/sociogram/graph.graphml");
	}
}
