package definitiondumper;

public class AbcResult {
	
	private String filename;
	private String text;
	
	public AbcResult(String filename, String text) {
		this.filename = filename;
		this.text = text;
	}
	
	public String getFilename() {
		return filename;
	}
	public String getText() {
		return text;
	}
	
	@Override
	public String toString() {
		return "filename: " + filename + "\n" + text;
	}
	
}
