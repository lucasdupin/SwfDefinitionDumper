import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import definitiondumper.AbcResult;
import definitiondumper.DefinitionDumper;


public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		if (args.length == 0) {
			printHelp();
			return;
		}
		
		//Get the file
		File fileToParse = new File(args[0]);
		//Parse it
		AbcResult[] result = new DefinitionDumper(fileToParse).dump();
		
		//Save or print?
		if(args.length > 1) {
			//Save
			try{
				for (AbcResult abcResult : result) {
					String filePath = args[1] + File.separator + abcResult.getFilename();
					(new File(filePath)).getParentFile().mkdirs();
					FileWriter fWriter = new FileWriter(filePath);
					BufferedWriter bWriter = new BufferedWriter(fWriter);
					bWriter.write(abcResult.getText());
					bWriter.close();
				}
			} catch (Exception e) {
				System.out.println("Error: " + e.getMessage());
			}
			
		} else {
			//Print
			for (AbcResult abcResult : result){
				System.out.println(abcResult);
				System.out.println();
			}
		}
		

	}
	
	public static void printHelp() {
		System.out.println("SwfDefinitionDumper by Lucas Dupin and Fernando França");
		System.out.println();
		System.out.println("First argument must be the file to parse");
		System.out.println("Second argument, if present, will the the path where to save the result");
	}

}
