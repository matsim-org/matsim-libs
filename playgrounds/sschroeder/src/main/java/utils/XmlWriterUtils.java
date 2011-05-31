package utils;

public class XmlWriterUtils {
	
	public static String tabs(int nOfTabs) { 
		String tabs = null;
		for(int i=0;i<nOfTabs;i++){
			if(tabs == null){
				tabs = "\t";
			}
			else{
				tabs += "\t";
			}
		}
		return tabs; 
	}
	
	public static String quotation(){
		return "\"";
	}
	
	public static String newLine() {
		return "\n";
	}
	
	public static String inQuotation(String things){
		return quotation() + things + quotation();
	}

	public static String inQuotation(int intValue) {
		return quotation() + intValue + quotation();
	}

	public static String inQuotation(Double value) {
		return quotation() + value + quotation();
	}

}
