package playground.balac.aam.config;

import org.matsim.core.config.ReflectiveConfigGroup;


public class AMMConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "MovingPathways";
	
	private String travelingMovingPathways = null;
	
	private String constantMovingPathways = null;
	
	private String movingPathwaysInputFile = null;
	
	private boolean useMovingPathways = false;
	
	private String statsFileName = null;


	
	public AMMConfigGroup() {
		super(GROUP_NAME);
	}
	
	@StringGetter( "statsFileName" )
	public String getStatsFileName() {
		return this.statsFileName;
	}

	@StringSetter( "statsFileName" )
	public void setStatsFileName(final String statsFileName) {
		this.statsFileName = statsFileName;
	}
	
	@StringGetter( "travelingMovingPathways" )
	public String getUtilityOfTravelling() {
		return this.travelingMovingPathways;
	}

	@StringSetter( "travelingMovingPathways" )
	public void setUtilityOfTravelling(final String travelingMovingPathways) {
		this.travelingMovingPathways = travelingMovingPathways;
	}

	@StringGetter( "constantMovingPathways" )
	public String constantMovingPathways() {
		return this.constantMovingPathways;
	}

	@StringSetter( "constantMovingPathways" )
	public void setConstantMovingPathways(final String constantMovingPathways) {
		this.constantMovingPathways = constantMovingPathways;
	}		
	
	@StringGetter( "movingPathwaysInputFile" )
	public String getvehiclelocations() {
		return this.movingPathwaysInputFile;
	}

	@StringSetter( "movingPathwaysInputFile" )
	public void setvehiclelocations(final String movingPathwaysInputFile) {
		this.movingPathwaysInputFile = movingPathwaysInputFile;
	}
	
	@StringGetter( "useMovingPathways" )
	public boolean useMovingPathways() {
		return this.useMovingPathways;
	}

	@StringSetter( "useMovingPathways" )
	public void setUseOneWayCarsharing(final boolean useMovingPathways) {
		this.useMovingPathways = useMovingPathways;
	}
	
}
