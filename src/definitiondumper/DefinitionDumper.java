package definitiondumper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import flash.swf.TagDecoder;

public class DefinitionDumper {

	public DefinitionAnalyzer printer;
	public File file;
	
	public DefinitionDumper(File file) {
		
		this.file = file;
		printer = new DefinitionAnalyzer();
		
	}
	
	public AbcResult[] dump() {
		

			TagDecoder decoder;
			try {
				decoder = new TagDecoder(new FileInputStream(file));
				decoder.parse(printer);
			} catch (FileNotFoundException e) {
				System.out.println("Error: " + e.getMessage());
			} catch (IOException e) {
				System.out.println("Error: " + e.getMessage());
			}
			
		
		return printer.getResult();
		
	}
}
