/**
 *
 */
package playground.yu.visum.writer;

/**
 * @author ychen
 */
public class UserDefAtt implements Comparable<UserDefAtt> {
	/**
	 * this enumeration type contains options, whether the attribut is to
	 * document sparsely: 1- sparsely, "duenn"; 0- not sparsely, "dick". (we use
	 * now the germen version of "VISUM 9.3")
	 */
	public enum SparseDocu {
		duenn(1), dick(0);
		private final int nr;

		SparseDocu(final int nr) {
			this.nr = nr;
		}

		/**
		 * Returns a String object representing the member variable "nr" of this
		 * enum constant. The argument is converted to signed decimal
		 * representation and returned as a string.
		 * 
		 * @return a string representation of the member variable "nr" of this
		 *         enum constant.
		 */
		@Override
		public String toString() {
			return Integer.toString(nr);
		}
	};

	/**
	 * this enumeration type contains 7 types for the attribut, which user of
	 * "VISUM9.3" can choose.
	 */
	public enum DatenTyp {
		Bool("Bool"), Filename("Filename"), Int("Int"), Double("Double"), ShortLength(
				"ShortLength"), LongLength("LongLength"), Text("Text"), Duration(
				"Duration"), Timepoint("Timepoint");
		DatenTyp(final String datenTypName) {
		}
	};

	/* -------------------------MEMBER VARIABLES--------------------------- */
	private final String OBJID;
	private final String ATTID;
	private final String CODE;
	private final String NAME;
	private final DatenTyp DATENTYP;
	private final int NACHKOMMASTELLEN;
	private final SparseDocu DUENN;

	private String pattern = "##0";

	/*-----------------------CONSTRUCTOR-------------------------------*/
	/**
	 * @param objid
	 * @param attid
	 * @param code
	 * @param name
	 * @param datentyp
	 * @param nachKommaStellen
	 * @param duenn
	 */
	public UserDefAtt(final String objid, final String attid,
			final String code, final String name, final DatenTyp datentyp,
			final int nachKommaStellen, final SparseDocu duenn) {
		OBJID = objid;
		ATTID = attid;
		CODE = code;
		NAME = name;
		DATENTYP = datentyp;
		NACHKOMMASTELLEN = nachKommaStellen;
		DUENN = duenn;
		// attID = uda.getATTID();
		// datenTyp = uda.getDATENTYP();
		// nachKommaStellen = uda.getNACHKOMMASTELLEN();
		if (NACHKOMMASTELLEN > 0)
			setPattern();
	}

	/* -----------------------------GETTER------------------------------ */
	/**
	 * @return Returns the aTTID.
	 * @uml.property name="aTTID"
	 */
	public String getATTID() {
		return ATTID;
	}

	/**
	 * Returns a DatenTyp of a UserDefAtt
	 * 
	 * @return a DatenTyp of a UserDefAtt
	 * @uml.property name="dATENTYP"
	 */
	public DatenTyp getDATENTYP() {
		return DATENTYP;
	}

	/**
	 * Returns the object-ID in "UserDefAtt". e.g. "NODE", "LINK" etc.
	 * 
	 * @return object-ID in "UserDefAtt". e.g. "NODE", "LINK" etc.
	 * @uml.property name="oBJID"
	 */
	public String getOBJID() {
		return OBJID;
	}

	/**
	 * @return Returns the nACHKOMMASTELLEN.
	 * @uml.property name="nACHKOMMASTELLEN"
	 */
	public int getNACHKOMMASTELLEN() {
		return NACHKOMMASTELLEN;
	}

	/* ------------------------------OVERRIDE METHODS------------------------- */
	@Override
	public String toString() {
		return OBJID + ";" + ATTID + ";" + CODE + ";" + NAME + ";" + DATENTYP
				+ ";" + NACHKOMMASTELLEN + ";" + DUENN + "\n";
	}

	/**
	 * make UserDefAtt-object comparable
	 * 
	 * @param arg0 -
	 *            an UserDefAtt-object
	 * @return the result of the comparing between ATTIDs of 2
	 *         UserDefAtt-objects
	 */
	public int compareTo(final UserDefAtt arg0) {
		return ATTID.compareTo(arg0.ATTID);
	}

	/**
	 * @return the pattern.
	 */
	public String getPattern() {
		return pattern;
	}

	/* ----------------------------SETTER--------------------- */
	/**
	 * sets the pattern of "Benutzerdefiniert Attribut"
	 * 
	 * @param pattern
	 *            Pattern of a "Benutzerdefiniertes Attribut" TODO I'm not sure
	 *            that this function really does what it should do... -marcel
	 */
	public void setPattern() {
		StringBuffer sb = new StringBuffer(pattern);
		sb.append(".");
		for (int ii = 0; ii < NACHKOMMASTELLEN; ii++)
			sb.append("0");
		pattern = sb.toString();
	}
}
