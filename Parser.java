package leagueDataRetrieval;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Parser {
	
	
	  List<String> data = new ArrayList<String>();
	  
	  private Path fFilePath;
	  
	  public Parser(String aFileName){
	    this.fFilePath = Paths.get(aFileName);
	    try {
			this.processLineByLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	  }
	  

	  protected final void processLineByLine() throws IOException {

	    try (Scanner scanner =  new Scanner(fFilePath, "UTF-8")){

	    	while (scanner.hasNextLine() ){
	    		
	    	  data.add(scanner.nextLine());
	    	  
	    	}		 	    	    
	    }
	  }
	  
	  
	  public List<String> getData(){	  
		  return data;
	  }
	  
		 
		  
}