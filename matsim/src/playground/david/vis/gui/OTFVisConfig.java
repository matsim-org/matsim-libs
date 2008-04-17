package playground.david.vis.gui;

import java.awt.Color;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.config.Module;
import org.matsim.config.groups.GlobalConfigGroup;

import playground.david.vis.OTFQuadFileHandler;

public class OTFVisConfig extends Module {
	public static final String GROUP_NAME = "otfvis";

	public OTFVisConfig() {
		super(GROUP_NAME);
	}

	public static final String AGENT_SIZE = "agentSize";
	public static final String MIDDLE_MOUSE_FUNC = "middleMouseFunc";
	public static final String LEFT_MOUSE_FUNC = "leftMouseFunc";
	public static final String RIGHT_MOUSE_FUNC = "rightMouseFunc";

	public static final String FILE_VERSION = "fileVersion";
	public static final String FILE_MINOR_VERSION = "fileMinorVersion";

	public static final String BIG_TIME_STEP = "bigTimeStep";
	public static final String TIME_STEP = "timeStep";

	private  float agentSize = 100.f;
	private  String middleMouseFunc = "Pan";
	private  String leftMouseFunc = "Zoom";
	private  String rightMouseFunc = "Menu";
	private int fileVersion = OTFQuadFileHandler.VERSION;
	private int fileMinorVersion = OTFQuadFileHandler.MINORVERSION;

	private int bigTimeStep = 600;
	private final int timeStep = 1;
	private String queryType = "Agent";
	private boolean multipleSelect = true;
	private Color backgroundColor = new Color(179, 179, 179, 0);
	private Color networkColor = new Color(128, 128, 255, 128);
	private float linkWidth = 30;

	private static final Logger log = Logger.getLogger(GlobalConfigGroup.class);

	@Override
	public String getValue(final String key) {
		if (AGENT_SIZE.equals(key)) {
			return Float.toString(getAgentSize());
		} else if (MIDDLE_MOUSE_FUNC.equals(key)) {
			return middleMouseFunc;
		} else if (LEFT_MOUSE_FUNC.equals(key)) {
			return leftMouseFunc;
		}  else if (RIGHT_MOUSE_FUNC.equals(key)) {
			return rightMouseFunc;
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public void addParam(final String key, final String value) {
		if (AGENT_SIZE.equals(key)) {
			agentSize = Float.parseFloat(value);
		} else if (MIDDLE_MOUSE_FUNC.equals(key)) {
			middleMouseFunc = value;
		} else if (LEFT_MOUSE_FUNC.equals(key)) {
			leftMouseFunc = value;
		}  else if (RIGHT_MOUSE_FUNC.equals(key)) {
			rightMouseFunc = value;
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	protected final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		map.put(AGENT_SIZE, getValue(AGENT_SIZE));
		map.put(LEFT_MOUSE_FUNC, getValue(LEFT_MOUSE_FUNC));
		map.put(MIDDLE_MOUSE_FUNC, getValue(MIDDLE_MOUSE_FUNC));
		map.put(RIGHT_MOUSE_FUNC, getValue(RIGHT_MOUSE_FUNC));
		return map;
	}

	/* direct access */

	public float getAgentSize() {
		return agentSize;
	}

	public void setAgentSize(float agentSize) {
		this.agentSize = agentSize;
	}

	public String getMiddleMouseFunc() {
		return middleMouseFunc;
	}

	public void setMiddleMouseFunc(String middleMouseFunc) {
		this.middleMouseFunc = middleMouseFunc;
	}

	public String getLeftMouseFunc() {
		return leftMouseFunc;
	}

	public void setLeftMouseFunc(String leftMouseFunc) {
		this.leftMouseFunc = leftMouseFunc;
	}

	public String getRightMouseFunc() {
		return rightMouseFunc;
	}

	public void setRightMouseFunc(String rightMouseFunc) {
		this.rightMouseFunc = rightMouseFunc;
	}

	/**
	 * @return the fileVersion
	 */
	public int getFileVersion() {
		return fileVersion;
	}

	/**
	 * @param fileVersion the fileVersion to set
	 */
	public void setFileVersion(int fileVersion) {
		this.fileVersion = fileVersion;
	}

	/**
	 * @return the fileMinorVersion
	 */
	public int getFileMinorVersion() {
		return fileMinorVersion;
	}

	/**
	 * @param fileMinorVersion the fileMinorVersion to set
	 */
	public void setFileMinorVersion(int fileMinorVersion) {
		this.fileMinorVersion = fileMinorVersion;
	}

	/**
	 * @return the bigTimeStep
	 */
	public int getBigTimeStep() {
		return bigTimeStep;
	}

	/**
	 * @param bigTimeStep the bigTimeStep to set
	 */
	public void setBigTimeStep(int bigTimeStep) {
		this.bigTimeStep = bigTimeStep;
	}

	/**
	 * @return the queryType
	 */
	public String getQueryType() {
		return queryType;
	}

	/**
	 * @param queryType the queryType to set
	 */
	public void setQueryType(String queryType) {
		this.queryType = queryType;
	}

	/**
	 * @return the multipleSelect
	 */
	public boolean isMultipleSelect() {
		return multipleSelect;
	}

	/**
	 * @param multipleSelect the multipleSelect to set
	 */
	public void setMultipleSelect(boolean multipleSelect) {
		this.multipleSelect = multipleSelect;
	}

	public Color getNetworkColor() {
		return this.networkColor;
	}

	public void setNetworkColor(final Color networkColor) {
		this.networkColor = new Color(networkColor.getRed(), networkColor.getGreen(), networkColor.getBlue(), 128);
	}

	public Color getBackgroundColor() {
		return this.backgroundColor;
	}

	public void setBackgroundColor(final Color bgColor) {
		this.backgroundColor = bgColor;
	}

	public float getLinkWidth() {
		return this.linkWidth;
	}

	public void setLinkWidth(final float linkWidth) {
		this.linkWidth = linkWidth;
	}

}
