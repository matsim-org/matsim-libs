package playground.jjoubert.CommercialClusters;

import java.util.ArrayList;
import java.util.List;

import playground.jjoubert.Utilities.MyStringBuilder;

public class MyCommercialClusterStringBuilder extends MyStringBuilder{
	
	private String version;
	private String threshold;
	private String sample;
	private String studyAreaName;
	private String radius;
	private String minimumPoints;
	private String activityType;
	private String outputPrefix;
	
	/**
	 * 
	 * @param root
	 * @param version
	 * @param threshold
	 * @param sample
	 * @param studyAreaName
	 * @param radius the given radius used during clustering.
	 * @param minimumPoints the given minimum number of points in a cluster used during 
	 * 		clustering. 
	 * @param activityType the type of activities that were clustered. This parameter may be
	 * 		<code>minor</code>, <code>major</code> or <code>null</code>. If the activity
	 * 		type is <code>null</code>, then the output filenames will include the word 
	 * 		<i>All</i> to indicate that the clustering results include both <code>minor</code>
	 * 		and <code>major</code> activities.
	 */
	public MyCommercialClusterStringBuilder(String root, String version, String threshold, 
											String sample, String studyAreaName, float radius, 
											int minimumPoints, String activityType){
		super(root);
		this.version = version;
		this.threshold = threshold;
		this.sample = sample;
		this.studyAreaName = studyAreaName;
		this.radius = String.valueOf((int) radius);
		this.minimumPoints = String.valueOf(minimumPoints);
		
		if(activityType == null){
			this.activityType = "All";
		} else if(activityType.equalsIgnoreCase("minor")){
			this.activityType = "Minor";
		} else if(activityType.equalsIgnoreCase("major")){
			this.activityType = "Major";
		} else{
			throw new RuntimeException("Incorrect activity type provided.");
		}

		this.outputPrefix = root + studyAreaName + "/" + version + "/" + threshold 
				+ "/Sample" + sample + "/Activities/" + studyAreaName + "_" 
				+ this.activityType + "_" + this.radius + "_" + this.minimumPoints + "_";
	}

	/**
	 * Calls <code>MyShapefileReader</code> from <code>MyStringBuilder</code>.
	 * @param studyArea the name of the study area.
	 * @return the absolute path to the shapefile.
	 */
	public String getShapefilename() {
		return super.getShapefilename(studyAreaName);
	}

	/**
	 * Gets the filename of the minor activity file that will be clustered.
	 * @return the absolute path of the activity filename.
	 */
	public String getMinorActivityFilename() {
		//TODO Get rid of the threshold in the filename. It can be in the path, but not the filename!!
		return this.getRoot() + studyAreaName + "/" + version + "/" + threshold + "/Sample" + sample + "/Activities/" + studyAreaName + "_MinorLocations.txt";
	}

	/**
	 * Gets the filename of the major activity file that will be clustered.
	 * @return the absolute path of the activity filename.
	 */
	public String getMajorActivityFilename() {
		return this.getRoot() + studyAreaName + "/" + version + "/" + threshold + "/Sample" + sample + "/Activities/" + studyAreaName + "_MajorLocations.txt";
	}

	/**
	 * Gets the folder where the vehicle XML files are stored.
	 * @return the absolute path of the folder.
	 */
	public String getVehicleFoldername() {
		return this.getRoot() + "DigiCore/XML/" + version + "/" + threshold + "/Sample" + sample + "/";
	}
	
	/**
	 * Return all the <i>Social Network Analysis</i> (SNA) output filenames.
	 * @return a <code>List</code> of <code>String</code>s, each list element the absolute 
	 * 		path of the filename. The five files are in the following sequence in the list:
	 * 		<ul>
	 * 			<li> the distance adjacency;
	 * 			<li> the order adjacency;
	 * 			<li> the in-order adjacency;
	 * 			<li> the out-order adjacency;
	 * 			<li> the <i>Pajek</i> network file.
	 * 		</ul> 
	 */
	public List<String> getSnaOutputFilenameList() {
		List<String> result = new ArrayList<String>(6);
		result.add(outputPrefix + "DistanceAdjacency.txt");
		result.add(outputPrefix + "OrderAdjacency.txt");
		result.add(outputPrefix + "InOrderAdjacency.txt");
		result.add(outputPrefix + "OutOrderAdjacency.txt");
		result.add(outputPrefix + "PajekNetwork.net");
		result.add(outputPrefix + "RNetwork.txt");
		result.add(outputPrefix + "RNodeCoordinate.txt");
		return result;
	}
	

	/**
	 * Return all the <i>cluster visualisation</i> output filenames.
	 * @return a <code>List</code> of <code>String</code>s, each list element the absolute 
	 * 		path of the filename. The four files are in the following sequence in the list:
	 * 		<ul>
	 * 			<li> the points of all clustered activities;
	 * 			<li> the clusters (cluster centroid);
	 * 			<li> the lines linking clustered activity points and the cluster centroid;
	 * 			<li> the convex hull (polygon) around the clustered activity points.
	 * 		</ul> 
	 */
	public List<String> getClusterVisualisationFilenameList(){
		List<String> result = new ArrayList<String>(4);
		result.add(outputPrefix + "ETPoint.txt");
		result.add(outputPrefix + "ETCentroid.txt");
		result.add(outputPrefix + "ETLine.txt");
		result.add(outputPrefix + "ETPolygon.txt");		
		return result;
	}

	/**
	 * Gets the filename of that will contain the clustering results.
	 * @param radius the given radius used to cluster activities.
	 * @param minimumPoints the given minimum number of points in a cluster used during clustering. 
	 * @param activityType the type of activities that were clustered. This parameter may be
	 * 		<code>minor</code>, <code>major</code> or <code>null</code>. If the activity
	 * 		type is <code>null</code>, then the output filenames will include the word 
	 * 		<i>All</i> to indicate that the clustering results include both <code>minor</code>
	 * 		and <code>major</code> activities.
	 * @return the absolute path of the filename.
	 */
	public String getClusterOutputFilename() {
		return outputPrefix + "Cluster.txt";
	}

	/**
	 * Gets the filename where the <code>DJCluster</code> list will be written to as 
	 * <code>XML</code> file.
	 * @return the absolute path of the <code>XML</code> filename.
	 */
	public String getClusterXmlFilename() {
		return outputPrefix + "ClusterXML.xml";
	}
	
}
