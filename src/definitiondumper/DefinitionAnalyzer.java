package definitiondumper;

import java.io.File;
import java.util.ArrayList;

import flash.swf.TagHandler;
import flash.swf.tags.DoABC;
import flash.swf.tools.CustomAbcPrinter;

public class DefinitionAnalyzer extends TagHandler {
	
	private ArrayList<AbcResult> result = new ArrayList<AbcResult>();

	@Override
	public void doABC(DoABC tag) {
		
		String filename = tag.name.replace('/', File.separatorChar) + ".as";;
		try {
			result.add(new AbcResult(filename, new CustomAbcPrinter(tag.abc).toString()));
		} catch (Exception e) {
			System.out.println("Could not generate dump for: " + filename);
			e.printStackTrace();
		}
		
	}
	
	public AbcResult[] getResult(){
		return result.toArray(new AbcResult[result.size()]);
	}
	
}