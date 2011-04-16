/* *********************************************************************** *
 * project: org.matsim.*
 * InundationDataFromNetcdfReaderII.java
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
package playground.gregor.otf.readerwriter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.evacuation.flooding.FloodingInfo;

import playground.gregor.MY_STATIC_STUFF;
import playground.gregor.flooding.TriangularMeshSimplifier;
import playground.gregor.otf.readerwriter.InundationData.InundationGeometry;
import playground.gregor.otf.readerwriter.InundationData.Polygon;
import playground.gregor.otf.readerwriter.InundationData.Quad;
public class InundationDataFromNetcdfReaderII {


	private static final Logger log = Logger.getLogger(InundationDataFromNetcdfReaderII.class);


	public static final float[] cvdeep = new float[] { 0.f, 0.f, 1.f };
//	public static final float[] cdeep = new float[] { 0.f, 0.1f, 0.9f };
//	public static final float[] cumid = new float[] { 0.f, 0.2f, 0.8f };
//	public static final float[] clmid = new float[] { 0.f, 0.3f, 0.7f };
	public static final float[] clow = new float[] { 0.f, 0.4f, 0.6f };
	public static final float[] cvlow = new float[] { 0.f, 0.9f, 0.7f };



	private  double offsetEast = 0;
	private  double offsetNorth = 0;

	private InundationData inundationData;

	private TreeMap<Double, float[]> colorTree;
	private int seriesLength;

	private QuadTree<Integer> coordinateQuadTree;


	public InundationDataFromNetcdfReaderII(double on, double oe) {
		this.offsetNorth = on;
		this.offsetEast = oe;
		System.out.println(on + " " + oe );

	}

	public InundationData createData() {
		this.inundationData = new InundationData();

		init();
		this.inundationData.colorMapping = this.colorTree;
		this.inundationData.seriesLength = this.seriesLength;
		try {
			writeToFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return this.inundationData;
	}



	private void writeToFile() throws IOException {


		String fileName = MY_STATIC_STUFF.SWW_ROOT + "/flooding.dat";
		ObjectOutputStream o = new ObjectOutputStream(new FileOutputStream(fileName));
		o.writeObject(this.inundationData);
		o.close();


	}

	private void init() {
		this.inundationData.powerLookUp = new double[3];

		double z = 1;
		for (int i = 0; i < this.inundationData.powerLookUp.length; i++) {
			this.inundationData.powerLookUp[i] = (z *= 4);
		}

		createColorTabel();
		HashMap<Integer,ArrayList<InundationGeometry>> mapping = new HashMap<Integer,ArrayList<InundationGeometry>>();
		QuadTree<InundationGeometry> triangles = getBasicGeometries(mapping);


		this.inundationData.floodingData.put(this.inundationData.powerLookUp[0], triangles);
//		this.inundationData.floodingData.put(this.inundationData.powerLookUp[1], triangles);
		for (int i = 1; i < this.inundationData.powerLookUp.length; i++) {
			//			QuadTree<InundationGeometry> tmp = scaleTriangles(triangles,mapping);
			QuadTree<InundationGeometry> tmp =  scaleGeometries(triangles,mapping);
//			tmp =  scaleGeometries(tmp,mapping);
			//			tmp =  scaleTriangles(tmp,mapping);
			//			tmp =  scaleTriangles(tmp,mapping);
			double zoom = this.inundationData.powerLookUp[i];
			this.inundationData.floodingData.put(zoom, tmp);
			triangles = tmp;
		}

		cleanup();
	}

	private void cleanup() {
		log.info("Coordinates before cleanup:" + this.inundationData.xcoords.length);

		this.coordinateQuadTree.clear();
		ArrayList<Float> xcoords = new ArrayList<Float>();
		ArrayList<Float> ycoords = new ArrayList<Float>();
		ArrayList<float []> walshs = new ArrayList<float[]>();
		int newIdx = 0;
		for (QuadTree<InundationGeometry> tmp : this.inundationData.floodingData.values()) {
			for (InundationGeometry g : tmp.values()) {
				int maxCount = g.getCoords().length;
				for (int i = 0; i < maxCount; i++) {
					float x = this.inundationData.xcoords[g.getCoords()[i]];
					float y = this.inundationData.ycoords[g.getCoords()[i]];
					Collection<Integer> coord;
					int cIdx;
					if ( (coord = this.coordinateQuadTree.get(x, y, 0.01)).size() == 0) {
						cIdx = newIdx++;
						xcoords.add(x);
						ycoords.add(y);
						float[] coeffs = this.inundationData.walshs[g.getCoords()[i]];
						walshs.add(coeffs);
						this.coordinateQuadTree.put(x , y, cIdx);
					} else {
						cIdx = coord.iterator().next();
					}
					g.getCoords()[i] = cIdx;

				}
			}
		}
		this.inundationData.walshs = new float [walshs.size()][InundationData.RES+1];
		for (int i = 0; i < walshs.size(); i++) {
			this.inundationData.walshs[i] = walshs.get(i);
		}
		walshs = null;
		this.inundationData.xcoords = new float [xcoords.size()];
		for (int i = 0; i < xcoords.size(); i++) {
			this.inundationData.xcoords[i] = xcoords.get(i);
		}
		xcoords = null;
		this.inundationData.ycoords = new float [ycoords.size()];
		for (int i = 0; i < ycoords.size(); i++) {
			this.inundationData.ycoords[i] = ycoords.get(i);
		}
		ycoords = null;	
		log.info("Coordinates after cleanup:" + this.inundationData.xcoords.length);
	}

	private QuadTree<InundationGeometry> getBasicGeometries(HashMap<Integer, ArrayList<InundationGeometry>> mapping) {
		int coordIdx = 0;
		double maxX = 10000000;
		double maxY = 10000000;
		double minX = -10000000;
		double minY = -10000000;
		QuadTree<InundationGeometry> ret= new QuadTree<InundationGeometry>(minX,minY,maxX,maxY);
		HashMap<Integer,ArrayList<InundationGeometry>> newMapping = new HashMap<Integer, ArrayList<InundationGeometry>>();
		this.coordinateQuadTree = new QuadTree<Integer>(minX,minY,maxX,maxY);
		ArrayList<Float> xcoords = new ArrayList<Float>();
		ArrayList<Float> ycoords = new ArrayList<Float>();
		ArrayList<float []> walshs = new ArrayList<float[]>();
		
//		String aoi = MY_STATIC_STUFF.SWW_ROOT + "/aoi.shp";
		for (int i = 0; i < MY_STATIC_STUFF.SWW_COUNT; i++) {
			String file = MY_STATIC_STUFF.SWW_ROOT + "/" + MY_STATIC_STUFF.SWW_PREFIX + i + MY_STATIC_STUFF.SWW_SUFFIX;
//			BasicInundationGeometryLoader reader = new BasicInundationGeometryLoader(file);
//			ConvexMeshSimplifier reader = new ConvexMeshSimplifier(file);//,aoi);
			TriangularMeshSimplifier reader = new TriangularMeshSimplifier(file);
			List<List<FloodingInfo>> fgis = reader.getInundationGeometries();
			reader = null;

			Queue<List<FloodingInfo>> queue = new ConcurrentLinkedQueue<List<FloodingInfo>>();
			queue.addAll(fgis);
			fgis = null;

			while (queue.size() > 0) {
				List<FloodingInfo> fgi = queue.poll();

				Polygon p = new Polygon(this.inundationData);
				p.coordsIdx = new int [fgi.size()];
				int idx = 0;
				double x = 0., y = 0.;
				for (FloodingInfo fi : fgi) {
					int cIdx;
					Collection<Integer> coord;
					if ( (coord = this.coordinateQuadTree.get(fi.getCoordinate().x - this.offsetEast, fi.getCoordinate().y - this.offsetNorth, 0.01)).size() == 0){
						cIdx = coordIdx++;
						x += fi.getCoordinate().x - this.offsetEast;
						y += fi.getCoordinate().y - this.offsetNorth;
						xcoords.add((float) (fi.getCoordinate().x - this.offsetEast));
						ycoords.add((float) (fi.getCoordinate().y - this.offsetNorth));
						float[] coeffs = getWalshCoefs(fi.getFloodingSeries());
						walshs.add(coeffs);
						this.coordinateQuadTree.put(fi.getCoordinate().x - this.offsetEast, fi.getCoordinate().y - this.offsetNorth, cIdx);
					} else {
						cIdx = coord.iterator().next();
						x += xcoords.get(cIdx);
						y += ycoords.get(cIdx);
					}
					p.coordsIdx[idx++] = cIdx;
					
				}	
				x /= p.coordsIdx.length;
				y /= p.coordsIdx.length;
				ret.put(x, y, p);
				for (int j = 0; j < p.coordsIdx.length; j++) {
					ArrayList<InundationGeometry> tmp = newMapping.get(p.coordsIdx[j]);
					if (tmp == null) {
						tmp = new ArrayList<InundationGeometry>();
						newMapping.put(p.coordsIdx[j], tmp);
					}
					tmp.add(p);
				}


			}

		}

		this.inundationData.walshs = new float [walshs.size()][InundationData.RES+1];
		for (int i = 0; i < walshs.size(); i++) {
			this.inundationData.walshs[i] = walshs.get(i);
		}
		walshs = null;
		this.inundationData.xcoords = new float [xcoords.size()];
		for (int i = 0; i < xcoords.size(); i++) {
			this.inundationData.xcoords[i] = xcoords.get(i);
		}
		xcoords = null;
		this.inundationData.ycoords = new float [ycoords.size()];
		for (int i = 0; i < ycoords.size(); i++) {
			this.inundationData.ycoords[i] = ycoords.get(i);
		}
		ycoords = null;	
		mapping.clear();
		mapping.putAll(newMapping);
		this.coordinateQuadTree.clear();
		for (InundationGeometry ig : ret.values()) {


			for (int i = 0; i < ig.getCoords().length; i++) {
				this.coordinateQuadTree.put(this.inundationData.xcoords[ig.getCoords()[i]], this.inundationData.ycoords[ig.getCoords()[i]], ig.getCoords()[i]);
			}


		}

		return ret;
	}




	private QuadTree<InundationGeometry> scaleGeometries(
			QuadTree<InundationGeometry> triangles,
			HashMap<Integer, ArrayList<InundationGeometry>> mapping) {
		QuadTree<InundationGeometry> ret = new QuadTree<InundationGeometry>(triangles.getMinEasting(),triangles.getMinNorthing(),triangles.getMaxEasting(),triangles.getMaxNorthing());
		HashSet<InundationGeometry> removed = new HashSet<InundationGeometry>();
		HashMap<Integer,ArrayList<InundationGeometry>> newMapping = new HashMap<Integer, ArrayList<InundationGeometry>>();
		int toGo = triangles.values().size();
		for (InundationGeometry p : triangles.values()) {

			if (removed.contains(p)) {
				continue;
			}
			int maxCount = p.getCoords().length-1;
			HashSet<InundationGeometry> neighbors = new HashSet<InundationGeometry>();
			for (int i = 0; i <= maxCount; i++) {
				neighbors.addAll(mapping.get(p.getCoords()[i]));
			}

			HashSet<InundationGeometry> newNeighbors = new HashSet<InundationGeometry>();
			for (InundationGeometry ig : neighbors) {
				if (!removed.contains(ig)) {
					newNeighbors.add(ig);
				}
			}

			if (newNeighbors.size() == 0) {
				continue;
			}
			double maxX = 0;
			double maxY = 0;
			double minX = Double.POSITIVE_INFINITY;
			double minY = Double.POSITIVE_INFINITY;
			for (InundationGeometry n : newNeighbors) {
				removed.add(n);
				maxCount = n.getCoords().length-1;
				for (int i = 0; i <= maxCount; i++){
					float x = this.inundationData.xcoords[n.getCoords()[i]];
					float y = this.inundationData.ycoords[n.getCoords()[i]];
					maxX = maxX > x ? maxX : x + 3;
					minX = minX < x ? minX : x - 3;
					maxY = maxY > y ? maxY : y + 3;
					minY = minY < y ? minY : y - 3;
				}
			}
			try {
				int c1 = this.coordinateQuadTree.get(maxX, maxY);
				int c2 = this.coordinateQuadTree.get(maxX, minY);
				int c3 = this.coordinateQuadTree.get(minX, minY);
				int c4 = this.coordinateQuadTree.get(minX, maxY);
				Quad q = new Quad(this.inundationData);
				q.coordsIdx[0] = c1;
				q.coordsIdx[1] = c2;
				q.coordsIdx[2] = c3;
				q.coordsIdx[3] = c4;
				double x = (maxX + minX)/2;
				double y = (maxY + minY)/2;
				ret.put(x, y, q);
				for (int i = 0; i <= 3; i++) {
					ArrayList<InundationGeometry> tmp = newMapping.get(q.coordsIdx[i]);
					if (tmp == null) {
						tmp = new ArrayList<InundationGeometry>();
						newMapping.put(q.coordsIdx[i], tmp);
					}
					tmp.add(q);
				}

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}




		}

		mapping.clear();
		mapping.putAll(newMapping);
		this.coordinateQuadTree.clear();
		for (InundationGeometry ig : ret.values()) {


			for (int i = 0; i < ig.getCoords().length; i++) {
				this.coordinateQuadTree.put(this.inundationData.xcoords[ig.getCoords()[i]], this.inundationData.ycoords[ig.getCoords()[i]], ig.getCoords()[i]);
			}


		}
		return ret;
	}





	
	private float[] getWalshCoefs(List<Double> floodingSeries) {
		float []  coeffs = new float [InundationData.RES+1];
		this.seriesLength = Math.max(this.seriesLength, floodingSeries.size());
		coeffs[InundationData.RES] = floodingSeries.size();
		//TODO tmp res
		for (int idx = 0; idx < floodingSeries.size(); idx++) {
			double d = floodingSeries.get(idx);
			if (d > 0.05) {
				coeffs[InundationData.RES] = idx;
				break;
			}
		}
		for (int k = 0; k < InundationData.RES; k++) {
			double tmp1 = 0;
			double tmp2 = 0;
			for (int i = (int)coeffs[InundationData.RES]; i < floodingSeries.size(); i++ ) {
				int idx = (int) Math.round(((double)(i-(int)coeffs[InundationData.RES])/(floodingSeries.size()-(int)coeffs[InundationData.RES])) * (InundationData.RES-1));
				tmp1 += floodingSeries.get(i) * InundationData.walsh8[k][idx];
				tmp2 +=  InundationData.walsh8[k][idx] * InundationData.walsh8[k][idx];
			}
			coeffs[k] = (float)(tmp1/tmp2);
		}


		return coeffs;
	}



	private void createColorTabel() {
		TreeMap<Double, float []> colorTree = new TreeMap<Double, float []>();

		double d = 0.05;
		for (d = 0.05; d < 0.5; d += 0.01) {
			double w1 = 1- (0.5 - d) / 0.45;
			double w2 = 1 - w1;
			colorTree.put(d, new float [] {(float) (clow[0] * w1 + cvlow[0]* w2),(float) (clow[1] * w1 + cvlow[1]* w2),(float) (clow[2] * w1 + cvlow[2]* w2),(float) (d+0.5)});
		}
		for (d = 0.5; d < 4; d += .05) {
			double w1 = (d - 0.2) / 3.8;
			double w2 = 1 - w1;
			colorTree.put(d, new float [] {(float) (cvdeep[0] * w1 + clow[0]* w2),(float) (cvdeep[1] * w1 + clow[1]* w2),(float) (cvdeep[2] * w1 + clow[2]* w2),1});
		}
		//		for (d = 1; d < 1.5; d += 0.1) {
		//			double w1 = 1 - (1.5 - d) / 1;
		//			double w2 = 1 - w1;
		//			colorTree.put(d, new float [] {(float) (cumid[0] * w1 + clmid[0]* w2),(float) (cumid[1] * w1 + clmid[1]* w2),(float) (cumid[2] * w1 + clmid[2]* w2),1});
		//			idx++;
		//		}
		//		System.out.println("IDX:" + idx);
		//
		//		for (d = 1.5; d < 2; d += 0.1) {
		//			double w1 = 1 - (2 - d) / 0.5;
		//			double w2 = 1 - w1;
		//			colorTree.put(d, new float [] {(float) (cdeep[0] * w1 + cumid[0]* w2),(float) (cdeep[1] * w1 + cumid[1]* w2),(float) (cdeep[2] * w1 + cumid[2]* w2),1});
		//			idx++;
		//		}
		//		System.out.println("IDX:" + idx);
		//		for (d = 2; d < 4; d += 0.1) {
		//			double w1 = 1 - (4 - d) / 2;
		//			double w2 = 1 - w1;
		//
		//			colorTree.put(d, new float [] {(float) (cvdeep[0] * w1 + cdeep[0]* w2),(float) (cvdeep[1] * w1 + cdeep[1]* w2),(float) (cvdeep[2] * w1 + cdeep[2]* w2),1});
		//			idx++;
		//		}
		colorTree.put(3., new float [] {(cvdeep[0]),(cvdeep[1]),(cvdeep[2]),1});
		this.colorTree = colorTree;

	}

	
	public static void main(String [] args){
		double on = 9870000.0;
		double oe = 643000.0;
		 
		new InundationDataFromNetcdfReaderII(on, oe).createData();
	}




}

