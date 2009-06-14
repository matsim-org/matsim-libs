package playground.gregor.flooding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.vividsolutions.jts.geom.Coordinate;

public class FloodingLine {

	Map<Integer, ArrayList<Integer>> coordIdxTriangleIdxMapping = null;
	private String netcdf;
	private List<FloodingInfo> fis;
	private Map<Integer, Integer> idxMapping;
	private List<int[]> triangles;
	private HashSet<Integer> processCoordIdxs;
	private FloodingReader fr = null;

	public FloodingLine(FloodingReader fr) {
		this.fr = fr;
	}

	public FloodingLine(String netcdf) {
		this.netcdf = netcdf;
	}

	List<ArrayList<Coordinate>> getFloodLine(int timestep) {
		if (this.coordIdxTriangleIdxMapping == null) {
			init();
		}

		this.processCoordIdxs = new HashSet<Integer>();
		int count = 0;

		List<ArrayList<Coordinate>> coords = new ArrayList<ArrayList<Coordinate>>();

		for (int[] triangle : this.triangles) {
			if (floodLineTriangle(triangle, timestep)) {
				// for (int i = 0; i < 3; i++) {
				// Integer fiIdx = this.idxMapping.get(triangle[i]);
				// if (this.processCoordIdxs.contains(fiIdx)) {
				// continue;
				// }
				// FloodingInfo fi = this.fis.get(fiIdx);
				// coords.add(fi.getCoordinate());
				// }
				coords.add(processTriangle(triangle, timestep));
				count++;
			}
		}
		System.out.println("time:" + timestep + " flooded:" + count);
		return coords;
	}

	private ArrayList<Coordinate> processTriangle(int[] triangle, int timestep) {
		ArrayList<Coordinate> coords = new ArrayList<Coordinate>();
		for (int i = 0; i < 3; i++) {
			Integer fiIdx = triangle[i];
			if (this.processCoordIdxs.contains(fiIdx)) {
				continue;
			}
			FloodingInfo fi = this.fis.get(this.idxMapping.get(fiIdx));
			if (fi.getFloodingTime() <= timestep) {
				coords.add(fi.getCoordinate());
				this.processCoordIdxs.add(fiIdx);
				Integer tmp = fiIdx;
				while (fiIdx != null) {
					fiIdx = getNextIndex(fiIdx, coords, timestep);
				}
				Collections.reverse(coords);
				fiIdx = tmp;
				while (fiIdx != null) {
					fiIdx = getNextIndex(fiIdx, coords, timestep);
				}
				return coords;
			}
		}
		return coords;

	}

	private Integer getNextIndex(Integer fiIdx, List<Coordinate> coords,
			int time) {
		ArrayList<Integer> nTri = this.coordIdxTriangleIdxMapping.get(fiIdx);
		HashSet<Integer> nTriSet = new HashSet<Integer>();
		nTriSet.addAll(nTri);
		for (Integer triIdx : nTri) {
			int[] triangle = this.triangles.get(triIdx);
			for (int i = 0; i < 3; i++) {
				fiIdx = triangle[i];
				if (this.idxMapping.get(fiIdx) == null
						|| this.processCoordIdxs.contains(triangle[i])) {
					continue;
				}
				FloodingInfo fi = this.fis.get(this.idxMapping.get(fiIdx));
				if (fi.getFloodingTime() > time) {
					continue;
				}
				ArrayList<Integer> tmp = this.coordIdxTriangleIdxMapping
						.get(fiIdx);
				int flCommTri = 0;
				for (Integer t : tmp) {
					if (!nTriSet.contains(t)) {
						continue;
					}
					int[] tmpTri = this.triangles.get(t);
					if (floodLineTriangle(tmpTri, time)) {
						coords.add(fi.getCoordinate());
						this.processCoordIdxs.add(fiIdx);
						return fiIdx;
					}
					if (triangleFlooded(tmpTri, time)) {
						if (flCommTri++ == 1) {
							break;
						}
					}
				}
				if (flCommTri < 2) {
					coords.add(fi.getCoordinate());
					this.processCoordIdxs.add(fiIdx);
					return fiIdx;
				}
			}
		}

		return null;
	}

	private void init() {
		if (this.fr == null) {
			this.fr = new FloodingReader(this.netcdf, true);
		}
		this.idxMapping = this.fr.getIdxMapping();
		this.triangles = this.fr.getTriangles();
		this.fis = this.fr.getFloodingInfos();
		this.coordIdxTriangleIdxMapping = new TreeMap<Integer, ArrayList<Integer>>();
		for (int i = 0; i < this.triangles.size(); i++) {
			int[] triangle = this.triangles.get(i);
			for (int j = 0; j < 3; j++) {
				int coordIdx = triangle[j];
				ArrayList<Integer> list = this.coordIdxTriangleIdxMapping
						.get(coordIdx);
				if (list == null) {
					list = new ArrayList<Integer>();
					this.coordIdxTriangleIdxMapping.put(coordIdx, list);
				}
				list.add(i);
			}
		}

	}

	private boolean floodLineTriangle(int[] triangle, int time) {
		boolean flooded = false;
		boolean unflooded = false;
		for (int i = 0; i < 3; i++) {
			Integer fiIdx = this.idxMapping.get(triangle[i]);
			if (fiIdx == null) {
				return false;
			}
			FloodingInfo fi = this.fis.get(fiIdx);
			if (fi.getFloodingTime() <= time) {
				flooded = true;
			} else {
				unflooded = true;
			}
		}
		if (flooded == unflooded && unflooded == true) {
			return true;
		}
		return false;
	}

	private boolean triangleFlooded(int[] triangle, int time) {
		for (int i = 0; i < 3; i++) {
			Integer fiIdx = this.idxMapping.get(triangle[i]);
			if (fiIdx == null) {
				return false;
			}
			FloodingInfo fi = this.fis.get(fiIdx);
			if (fi.getFloodingTime() <= time) {
				return true;
			}
		}
		return false;
	}

	public static void main(String[] args) {
		String netcdf = "../../inputs/flooding/flooding_old.sww";
		FloodingLine fl = new FloodingLine(netcdf);
		for (int i = 0; i < 120; i++) {
			fl.getFloodLine(i);
		}
	}

}
