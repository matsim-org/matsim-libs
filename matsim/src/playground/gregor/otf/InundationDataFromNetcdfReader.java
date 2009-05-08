package playground.gregor.otf;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.matsim.core.utils.collections.QuadTree;

import playground.gregor.collections.gnuclasspath.TreeMap;
import playground.gregor.flooding.FloodingInfo;
import playground.gregor.flooding.FloodingReader;
import playground.gregor.otf.InundationData.Quad;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

public class InundationDataFromNetcdfReader {

	// private final String file = "../../inputs/networks/flooding.sww";
//	private final String file = "test/input/playground/gregor/data/flooding.sww";
	private final String file = "/home/laemmel/devel/inputs/flooding/SZ_r018M_m003_092_12_mw9.00_03h__P0_8.sww";

	public static final float[] cvdeep = new float[] { 0.f, 0.f, 1.f };
	public static final float[] cdeep = new float[] { 0.f, 0.1f, 0.9f };
	public static final float[] cumid = new float[] { 0.f, 0.2f, 0.8f };
	public static final float[] clmid = new float[] { 0.f, 0.3f, 0.7f };
	public static final float[] clow = new float[] { 0.f, 0.4f, 0.6f };
	public static final float[] cvlow = new float[] { 0.f, 0.9f, 0.7f };
	
	private static final String BASE = "/home/laemmel/devel/inputs/flooding/flooding0";

//	private final HashMap<Integer, byte[]> tableMapping = new HashMap<Integer, byte[]>(100000);
//	private final HashMap<Double, ArrayList<Integer>> indexing = new HashMap<Double, ArrayList<Integer>>();
	// private FloodingReader fr;

	private final double offsetEast;
	private final double offsetNorth;

	private InundationData inundationData;

//	private final int mappings = 0;

	private Envelope envelope;

//	private final int id = 0;

//	private TreeMap<Double, Byte> colorMapping;
	private TreeMap<Double, float[]> colorTree;
	private int seriesLength;

	public InundationDataFromNetcdfReader(double on, double oe) {
		this.offsetNorth = on;
		this.offsetEast = oe;

	}

	public InundationData createData() {
		// this.fr = new FloodingReader(this.file);
		this.inundationData = new InundationData();

		init();
//		this.inundationData.tableMapping = this.tableMapping;
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


		//		OutputStream a = new ByteArrayOutputStream();
		ObjectOutputStream o = new ObjectOutputStream(new FileOutputStream("test.dat"));
		o.writeObject(this.inundationData);
		o.close();
		//		FileWriter fw = new FileWriter("test.dat");

		//		OutputStreamWriter out = new OutputStreamWriter(o);


	}

	private void init() {
		this.inundationData.powerLookUp = new double[3];

		double z = 0.25;
		for (int i = 0; i < 3; i++) {
			this.inundationData.powerLookUp[i] = (z *= 4);
		}

		createColorTabel();

		// this.envelope = this.fr.getEnvelope();

//		Collection<CompressedFloodingInfo> infos = getFloodingInfos();
//		Collection<CompressedFloodingInfo> infos = getWalshFloodingInfos();
		Collection<Triangle> infos = getTriangles();
		double minX = -2000
		* (Math.abs(this.envelope.getMaxX()) - this.offsetEast);
		double maxX = 2000 * (Math.abs(this.envelope.getMaxX()) - this.offsetEast);
		double minY = -2000
		* (Math.abs(this.envelope.getMaxY()) - this.offsetNorth);
		double maxY = 2000 * (Math.abs(this.envelope.getMaxY()) - this.offsetNorth);
		QuadTree<InundationData.Quad> quadsTree = new QuadTree<InundationData.Quad>(minX, minY, maxX, maxY);
		
		System.out.println(this.envelope);

		QuadTree<Triangle> infoTree = new QuadTree<Triangle>(minX, minY, maxX, maxY);

		double baseRes = 1.;
		fillInfoTree(infoTree, infos, baseRes);
		// this.fr = null; //free memory
		fillQuadsTree(quadsTree, infoTree); //, this.colorMapping);
		this.inundationData.floodingData.put(1., quadsTree);
//		baseRes = 2.;
		for (int i = 1; i < this.inundationData.powerLookUp.length; i++) {
			double zoom = this.inundationData.powerLookUp[i];
			QuadTree<Triangle> tmp = new QuadTree<Triangle>(minX, minY, maxX, maxY);
			QuadTree<Quad> quads = new QuadTree<Quad>(minX, minY, maxX, maxY);
			fillInfoTree(tmp, infoTree.values(), Math.max(6,baseRes * zoom));
			System.out.println(tmp.size());
			fillQuadsTree(quads, tmp); //, this.colorMapping);
			this.inundationData.floodingData.put(zoom, quads);
			infoTree = tmp;
		}

		// for (FloodingInfo fi : this.fr.getFloodingInfos()) {
		//			
		//			
		// }

	}

//	private Collection<CompressedFloodingInfo> getWalshFloodingInfos() {
//		Collection<CompressedFloodingInfo> info = new ArrayList<CompressedFloodingInfo>(
//				550000);
//		double maxX = 0;
//		double maxY = 0;
//		double minX = Double.POSITIVE_INFINITY;
//		double minY = Double.POSITIVE_INFINITY;
//		for (int i = 1; i < 2; i++) {
//			String file = BASE + i + ".sww";
////			 file = this.file;
//			FloodingReader r = new FloodingReader(file);
//			
//			Envelope e = r.getEnvelope();
//			maxX = maxX > e.getMaxX() ? maxX : e.getMaxX();
//			maxY = maxY > e.getMaxY() ? maxY : e.getMaxY();
//			minX = minX < e.getMinX() ? minX : e.getMinX();
//			minY = minY < e.getMinY() ? minY : e.getMinY();
//			for (FloodingInfo fi : r.getFloodingInfos()) {
//
//				float[] coeffs = getWalshCoefs(fi.getFloodingSeries());
//				//				int colorKey = createTableMapping(colors);
//				if (coeffs[InundationData.RES] >= fi.getFloodingSeries().size()-1) {
//					continue;
//				}
//				CompressedFloodingInfo ci = new CompressedFloodingInfo();
//				//				ci.colorTableKey = colorKey;
//				ci.walshCoeffs = coeffs;
//				ci.x = (float) (fi.getCoordinate().x - this.offsetEast);
//				ci.y = (float) (fi.getCoordinate().y - this.offsetNorth);
//				info.add(ci);
//			}
//			r = null;
//		}
//		this.envelope = new Envelope(maxX, minX, maxY, minY);
//		return info;
//	}
	
	private Collection<Triangle> getTriangles() {
		Collection<Triangle> info = new ArrayList<Triangle>();
		double maxX = 0;
		double maxY = 0;
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		for (int i = 1; i < 2; i++) {
//			String file = BASE + i + ".sww";
			 String file = this.file;
			FloodingReader r = new FloodingReader(file);
			List<int []> triangles = r.getTriangles();
			Map<Integer,Integer> mapping = r.getIdxMapping();
			List<FloodingInfo> infos = r.getFloodingInfos();
			Envelope e = r.getEnvelope();
			r = null;
			maxX = maxX > e.getMaxX() ? maxX : e.getMaxX();
			maxY = maxY > e.getMaxY() ? maxY : e.getMaxY();
			minX = minX < e.getMinX() ? minX : e.getMinX();
			minY = minY < e.getMinY() ? minY : e.getMinY();
			int count = triangles.size();
			for (int [] tri : triangles) {
				if (count-- % 1000 == 0) {
					System.out.println(count);
				}
				
				Triangle t = new Triangle();
				for (int j = 0; j < 3; j++) {
					Integer idx = mapping.get(tri[j]);
					if (idx == null) {
						t = null;
						break;
					}
					
					FloodingInfo fi = infos.get(idx);
					float[] coeffs = getWalshCoefs(fi.getFloodingSeries());
					switch (j) {
					case 0:
						t.awalsh = coeffs;
						t.xa = (float) fi.getCoordinate().x;
						t.ya = (float) fi.getCoordinate().y;
						break;
					case 1 :
						t.bwalsh = coeffs;
						t.xb = (float) fi.getCoordinate().x;
						t.yb = (float) fi.getCoordinate().y;
						break;
					case 2 :
						t.cwalsh = coeffs;
						t.xc = (float) fi.getCoordinate().x;
						t.yc = (float) fi.getCoordinate().y;
						break;
					}
					
					
				}	
				if (t != null){
					info.add(t);
				}
			}
			
		}
		this.envelope = new Envelope(maxX, minX, maxY, minY);
		return info;
	}

	private float[] getWalshCoefs(List<Double> floodingSeries) {
		float []  coeffs = new float [InundationData.RES+1];
		this.seriesLength = Math.max(this.seriesLength, floodingSeries.size());
		coeffs[InundationData.RES] = floodingSeries.size();
		for (int idx = 0; idx < floodingSeries.size(); idx++) {
			double d = floodingSeries.get(idx);
			if (d > 0.05) {
				coeffs[InundationData.RES] = idx;
				break;
			}
		}
//		coeffs[InundationData.RES] = 0;
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

//	private Collection<CompressedFloodingInfo> getFloodingInfos() {
//		Collection<CompressedFloodingInfo> info = new ArrayList<CompressedFloodingInfo>(
//				550000);
//		double maxX = 0;
//		double maxY = 0;
//		double minX = Double.POSITIVE_INFINITY;
//		double minY = Double.POSITIVE_INFINITY;
//		for (int i = 1; i < 9; i++) {
//			String file = BASE + i + ".sww";
//			// file = this.file;
//			FloodingReader r = new FloodingReader(file);
//			Envelope e = r.getEnvelope();
//			maxX = maxX > e.getMaxX() ? maxX : e.getMaxX();
//			maxY = maxY > e.getMaxY() ? maxY : e.getMaxY();
//			minX = minX < e.getMinX() ? minX : e.getMinX();
//			minY = minY < e.getMinY() ? minY : e.getMinY();
//			for (FloodingInfo fi : r.getFloodingInfos()) {
//
//				byte[] colors = getColors(fi.getFloodingSeries());
//				int colorKey = createTableMapping(colors);
//
//				CompressedFloodingInfo ci = new CompressedFloodingInfo();
//				ci.colorTableKey = colorKey;
//				ci.x = (float) (fi.getCoordinate().x - this.offsetEast);
//				ci.y = (float) (fi.getCoordinate().y - this.offsetNorth);
//				info.add(ci);
//			}
//			r = null;
//		}
//		this.envelope = new Envelope(maxX, minX, maxY, minY);
//		return info;
//	}

//	private byte[] getColors(List<Double> floodingSeries) {
//		byte[] colors = new byte[floodingSeries.size()];
//		short idx = 0;
//		for (Double flooding : floodingSeries) {
//			colors[idx++] = this.colorMapping.floorEntry(flooding).getValue();
//		}
//		return colors;
//	}

	private void createColorTabel() {
//		this.inundationData.colorTable = new float[4][128];
//		TreeMap<Double, Byte> colorMapping = new TreeMap<Double, Byte>();
		TreeMap<Double, float []> colorTree = new TreeMap<Double, float []>();
		byte idx = 0;
		
//		colorMapping.put(0., idx);
		idx++;

		double d = 0.05;
		for (d = 0.05; d < 0.2; d += 0.01) {
			double w1 = 1 - (0.2 - d) / 0.15;
			double w2 = 1 - w1;
			colorTree.put(d, new float [] {(float) (clow[0] * w1 + cvlow[0]* w2),(float) (clow[1] * w1 + cvlow[1]* w2),(float) (clow[2] * w1 + cvlow[2]* w2),(float) w1});
			idx++;
		}
		System.out.println("IDX:" + idx);
		for (d = 0.2; d < 1; d += .1) {
			double w1 = 1 - (1 - d) / 0.8;
			double w2 = 1 - w1;
			colorTree.put(d, new float [] {(float) (clmid[0] * w1 + clow[0]* w2),(float) (clmid[1] * w1 + clow[1]* w2),(float) (clmid[2] * w1 + clow[2]* w2),1});
			idx++;
		}
		System.out.println("IDX:" + idx);
		for (d = 1; d < 1.5; d += 0.1) {
			double w1 = 1 - (1.5 - d) / 1;
			double w2 = 1 - w1;
			colorTree.put(d, new float [] {(float) (cumid[0] * w1 + clmid[0]* w2),(float) (cumid[1] * w1 + clmid[1]* w2),(float) (cumid[2] * w1 + clmid[2]* w2),1});
			idx++;
		}
		System.out.println("IDX:" + idx);

		for (d = 1.5; d < 2; d += 0.1) {
			double w1 = 1 - (2 - d) / 0.5;
			double w2 = 1 - w1;
			colorTree.put(d, new float [] {(float) (cdeep[0] * w1 + cumid[0]* w2),(float) (cdeep[1] * w1 + cumid[1]* w2),(float) (cdeep[2] * w1 + cumid[2]* w2),1});
			idx++;
		}
		System.out.println("IDX:" + idx);
		for (d = 2; d < 4; d += 0.1) {
			double w1 = 1 - (4 - d) / 2;
			double w2 = 1 - w1;

			colorTree.put(d, new float [] {(float) (cvdeep[0] * w1 + cdeep[0]* w2),(float) (cvdeep[1] * w1 + cdeep[1]* w2),(float) (cvdeep[2] * w1 + cdeep[2]* w2),1});
			idx++;
		}
		System.out.println("IDX:" + idx);
				colorTree.put(3., new float [] {(cvdeep[0]),(cvdeep[1]),(cvdeep[2]),1});
		this.colorTree = colorTree;
		
	}

	private void fillQuadsTree(QuadTree<Quad> quadsTree,
			QuadTree<Triangle> infoTree) {

		System.out.println("processing:" + infoTree.values().size()
				+ " FloodingInfos");

		for (Triangle fi : infoTree.values()) {
			// short [] colors = new short[fi.getFloodingSeries().size()];
			// short idx = 0;
			// for (Double d : fi.getFloodingSeries()) {
			// // time += 60;
			// colors[idx++] = colorMapping.floorEntry(d).getValue();
			//
			// }
			// createTableMapping(colors);

//			Coordinate nearest = getNearestCoord(new Coordinate(fi.xa, fi.ya),
//					infoTree);
//			if (nearest == null) {
//				continue;
//			}
//			double distA = nearest.distance(new Coordinate(fi.x, fi.y));
			// Coordinate tmp = getNearestCoord(nearest,infoTree);
			// double distB = nearest.distance(tmp);
//			double diff = distA + 1; // - distB/2;

			Quad q = new Quad();
//			q.col = fi.colorTableKey;
			q.awalsh = fi.awalsh;
			q.bwalsh = fi.bwalsh;
			q.cwalsh = fi.cwalsh;
			q.xa = fi.xa;
			q.ya = fi.ya;
			q.xb = fi.xb;
			q.yb = fi.yb;
			q.xc = fi.xc;
			q.yc = fi.yc;
//			q.diff = (float) diff;
			// q.a = new Coordinate(fi.getCoordinate().x +
			// diff,fi.getCoordinate().y + diff,0);
			// q.b = new Coordinate(fi.getCoordinate().x -
			// diff,fi.getCoordinate().y + diff,0);
			// q.c = new Coordinate(fi.getCoordinate().x -
			// diff,fi.getCoordinate().y - diff,0);
			// q.d = new Coordinate(fi.getCoordinate().x +
			// diff,fi.getCoordinate().y - diff,0);
			quadsTree.put(fi.xa, fi.ya, q);
		}
		System.out.println("Quads:" + quadsTree.size());
//		int toGo = quadsTree.size();
//
//		for (Quad quad : quadsTree.values()) {
//			if (toGo-- % 1000 == 0) {
//				System.out.println("toGo:" + toGo);
//			}
////			quad.acol = quadsTree.get(quad.x + quad.diff, quad.y + quad.diff).col;
////			quad.bcol = quadsTree.get(quad.x + quad.diff, quad.y - quad.diff).col;
////			quad.ccol = quadsTree.get(quad.x - quad.diff, quad.y - quad.diff).col;
////			quad.dcol = quadsTree.get(quad.x - quad.diff, quad.y + quad.diff).col;
//			
//			quad.awalsh = quadsTree.get(quad.x + quad.diff, quad.y + quad.diff).walsh;
//			quad.bwalsh = quadsTree.get(quad.x + quad.diff, quad.y - quad.diff).walsh;
//			quad.cwalsh = quadsTree.get(quad.x - quad.diff, quad.y - quad.diff).walsh;
//			quad.dwalsh = quadsTree.get(quad.x - quad.diff, quad.y + quad.diff).walsh;
//			// quad.acol = quad.col;
//			// quad.bcol = quad.col;
//			// quad.ccol = quad.col;
//			// quad.dcol = quad.col;
//		}
//		for (Quad quad : quadsTree.values()) {
//			quad.walsh = null;
//		}
	}

//	private int createTableMapping(byte[] colors) {
//		int start = 0;
//		double key = 0;
//		for (; start < colors.length; start++) {
//			if (colors[start] > 0.) {
//				if (start + 5 >= colors.length) {
//					key = start;
//				} else {
//					key = start + (double) colors[start + 1] / 10
//					+ (double) colors[start + 2] / 100
//					+ (double) colors[start + 3] / 1000
//					+ (double) colors[start + 4] / 10000
//					+ (double) colors[start + 5] / 100000;
//				}
//				break;
//			}
//		}
//		ArrayList<Integer> lookup = this.indexing.get(key);
//		if (lookup == null) {
//			lookup = new ArrayList<Integer>();
//			this.indexing.put(key, lookup);
//			this.tableMapping.put(this.id, colors);
//			lookup.add(this.id);
//			this.id++;
//			if (this.id < 0) {
//				System.err.println("smaller then 0: " + this.id);
//			}
//			return (this.id - 1);
//		}
//
//		for (Integer index : lookup) {
//			byte[] e = this.tableMapping.get(index);
//			boolean found = true;
//			for (short i = 0; i < colors.length; i++) {
//				if (colors[i] != e[i]) {
//					found = false;
//					break;
//				}
//			}
//			if (found) {
//				if (this.mappings++ % 1000 == 0) {
//					System.err
//					.println("Identical tables found: "
//							+ this.mappings
//							+ " compression ratio:"
//							+ ((double) this.mappings / (this.mappings + this.tableMapping
//									.size()))
//									+ " processed:"
//									+ (this.tableMapping.size() + this.mappings));
//				}
//				return index;
//			}
//
//		}
//		lookup.add(this.id);
//		this.tableMapping.put(this.id, colors);
//		this.id++;
//		if (this.id < 0) {
//			System.err.println("smaller then 0: " + this.id);
//		}
//		return (this.id - 1);
//		// System.out.println("created new Mapping: " +
//		// this.tableMapping.size());
//
//	}

	private void fillInfoTree(QuadTree<Triangle> infoTree,
			Collection<Triangle> infos, double d) {
		System.out.println(infoTree.size());
		
		for (Triangle fi : infos) {

			if (infoTree.get(fi.xa, fi.ya, d).size() != 0) {
				continue;
			}
			infoTree.put(fi.xa, fi.ya, fi);
		}

	}

	private Coordinate getNearestCoord(Coordinate c,
			QuadTree<CompressedFloodingInfo> infoTree) {
		Collection<CompressedFloodingInfo> fi = infoTree.get(c.x, c.y, 20);
		if (fi.size() < 2) {
			return null;
		}
		double minDist = 21;
		Coordinate nearest = null;

		for (CompressedFloodingInfo t : fi) {
			double cdist = (new Coordinate(t.x, t.y)).distance(c);
			if (cdist > 0 && cdist < minDist) {
				minDist = cdist;
				nearest = new Coordinate(t.x, t.y);
			}
		}
		return nearest;
	}

	private static class CompressedFloodingInfo {
		public float[] walshCoeffs;
		float x;
		float y;
		int colorTableKey;
	}
	
	private static class Triangle {
		float xa;
		float xb;
		float xc;
		float ya;
		float yb;
		float yc;
		public float[] awalsh;
		public float[] bwalsh;
		public float[] cwalsh;
	}
}
