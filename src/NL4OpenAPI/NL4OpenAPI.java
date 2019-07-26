package NL4OpenAPI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import Generator.NLGenT;

public class NL4OpenAPI {
	

	public static String srcFileName = "openapi";
	public static String dstFileName = "openapiNL";
	public static String defaultFileExtension = "json";
	public static boolean babelFolderExisted = false, babelFilesExisted = false;
	
	public static void main(String[] args) {
		
		switch (args.length) {
			case 1 : 
				srcFileName = args[0];
				break;
			case 2 : 
				srcFileName = args[0];
				dstFileName = args[1];
		}
		
		configBabel();
		openapi2NL();
                
	}

	// Apply NLP to existing OpenAPI to add descriptions
	private static void openapi2NL() {
		
		// Parameters of NL4APIdocs
		ArrayList<HashMap<String,String>> fullParametersList = new ArrayList<HashMap<String,String>>();
		ArrayList<HashMap<String,String>> parametersList = new ArrayList<HashMap<String,String>>();
		ArrayList<HashMap<String,String>> fullPropertiesList = new ArrayList<HashMap<String,String>>();

		HashMap<String, String> apiHashMap = new HashMap<>();

		HashMap<String, String> examplesHashMap = new HashMap<>();
		
		//JSON parser object to parse read file
		JsonParser jsonParser = new JsonParser();
         
        try (FileReader reader = new FileReader(srcFileName + "." + defaultFileExtension))
        {
        	String openapiTitle = "data";
        	
            //Read JSON file
        	JsonObject obj = (JsonObject) jsonParser.parse(reader);
        	
        	JsonObject openapiInfo = (JsonObject) obj.get("info");
        	openapiTitle = openapiInfo.get("title").toString();            
    		apiHashMap.put("fileName", openapiTitle);

    		try {
	        	JsonObject openapiComponentsProperties = (JsonObject) obj.getAsJsonObject("components")
	        			.getAsJsonObject("schemas").getAsJsonObject("mainComponent").getAsJsonObject("properties");
	    		Set<String> openapiComponentsPropertiesKeys = openapiComponentsProperties.keySet();
	    		Iterator<String> openapiComponentsPropertiesKeyIterator = openapiComponentsPropertiesKeys.iterator();
	    		
	    		while (openapiComponentsPropertiesKeyIterator.hasNext()) {
	            	parametersList.clear();
	            	try {
	            		String path = (String) openapiComponentsPropertiesKeyIterator.next();
	            		  
		        		HashMap<String, String> parameterHashMap = new HashMap<>();
		        		parameterHashMap.put("name", path);
		        		
		        		String example = "";
		        		try {
		        			example = openapiComponentsProperties.getAsJsonObject(path).get("example").toString();
		        		} catch(Exception e) {
		            		System.out.println(e.getMessage());
		            	}
		        		
		        		parameterHashMap.put("example", example);
	
		        		examplesHashMap.put(path, example);
		        		
		        		String type = "";
		        		try {
		        			type = openapiComponentsProperties.getAsJsonObject(path).get("type").toString();
		        		} catch(Exception e) {
		            		System.out.println(e.getMessage());
		            	}
		        		parameterHashMap.put("type", type);
		        		parameterHashMap.put("ptype", "property");
		        		parametersList.add(parameterHashMap);
		        		fullPropertiesList.add(parameterHashMap);
		
		                String parameterDescription = NLGenT.generateText(apiHashMap, parametersList, 2);
		                System.out.println(parameterDescription);
		                
		                //TODO: add description to new OpenAPI json
		                openapiComponentsProperties.getAsJsonObject(path).addProperty("description", parameterDescription);
		                
	            	} catch(Exception e) {
	            		System.out.println(e.getMessage());
	            	}
	            }
    		} catch (Exception e) {
    			e.printStackTrace();
    		}

    		try {
	        	JsonObject openapiPaths = (JsonObject) obj.getAsJsonObject("paths");
	    		Set<String> openapiPathsKeys = openapiPaths.keySet();
	    		Iterator<String> openapiPathsKeyIterator = openapiPathsKeys.iterator();
	    		
	    		while (openapiPathsKeyIterator.hasNext()) {
	    			String path = (String) openapiPathsKeyIterator.next();
	    			
	                fullParametersList.clear();
	                
	                try {
		            	JsonArray openapiPathParameters = (JsonArray) openapiPaths.getAsJsonObject(path).getAsJsonObject("get")
		            			.getAsJsonArray("parameters");
		        		for (int i = 0; i < openapiPathParameters.size(); i++) {
		            	   
		            	   parametersList.clear();
		            	   
		            	   JsonObject param = (JsonObject) openapiPathParameters.get(i);
		            	   String paramName = param.get("name").toString();
		            	   HashMap<String, String> parameterHashMap = new HashMap<>();
		            	   parameterHashMap.put("name", paramName);
		
		            	   String example = "";
		            	   try {
			   	        		example = examplesHashMap.get(paramName);
			   	        		if(example != null) {
				   	        		parameterHashMap.put("example", example);
			   	        		} else {
			   	        			parameterHashMap.put("example", "");
			   	        		}
		  	        		} catch(Exception e) {
		  	            		System.out.println(e.getMessage());
		  	        		}
		  	        		
		  	        		String type = "";
		  	        		try {
		  	        			type = param.getAsJsonObject("schema").get("type").toString();
		  	        		} catch(Exception e) {
		  	            		System.out.println(e.getMessage());
		  	            	}
		  	        		parameterHashMap.put("type", type);
		  	        		
		  	        		String in = "";
		  	        		try {
		  	        			in = param.get("in").toString();
		  	        		} catch(Exception e) {
		  	            		System.out.println(e.getMessage());
		  	            	}
		  	        		parameterHashMap.put("in", in);
		  	        		
		  	        		boolean required = false;
		  	        		try {
		  	        			required = param.get("required").getAsBoolean();
		  	        		} catch(Exception e) {
		  	            		//System.out.println(e.getMessage());
		  	            		required = false;
		  	            	}
		  	        		parameterHashMap.put("required", String.valueOf(required));
		  	        		
		  	        		parameterHashMap.put("ptype", "parameter");
		  	        		parametersList.add(parameterHashMap);
		  	        		fullParametersList.add(parameterHashMap);
		
		  	                
		  	                String parameterDescription = NLGenT.generateText(apiHashMap, parametersList, 2);
		  	                System.out.println(parameterDescription);
		  	                
		  	                //TODO: add description to new OpenAPI json
		  	                param.addProperty("description", parameterDescription);
			                
		            	}
	                } catch (Exception e) {
	                	e.printStackTrace();
	                }
	    			
		    		String methodName = "";
		        	try{
		        		methodName = path.split("/")[1];
		        	} catch(Exception e) {
		        		methodName = "/";
		        	}
		    		apiHashMap.put("methodName", methodName);
		    		
		    		try {
		                String methodDescription = NLGenT.generateText(apiHashMap, fullPropertiesList, 1);
	
		                //TODO: add description to new OpenAPI json
		                obj.getAsJsonObject("paths").getAsJsonObject(path).getAsJsonObject("get")
		                	.addProperty("description", methodDescription);
		                
		    		} catch (Exception e) {
		    			e.printStackTrace();
		    		}
		            fullParametersList.clear();
	    		}

    		} catch (Exception e) {
    			e.printStackTrace();
    		}
    		

            
            try {
	            String apiDescription = NLGenT.generateText(apiHashMap, fullPropertiesList, 0);
	            System.out.println(apiDescription);
	            
                //TODO: add description to new OpenAPI json
	            obj.getAsJsonObject("info").addProperty("description", apiDescription);
                
            } catch(Exception e) {
            	e.printStackTrace();
            }
            
            //Write JSON file
            try (FileWriter file = new FileWriter(dstFileName + "." + defaultFileExtension)) {
 
	            file.write(toPrettyFormat(obj.toString()));
	            file.flush();
	 
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
 
        } catch (FileNotFoundException e) {
        	System.out.println("File not found");
        } catch (IOException e) {
        	System.out.println("Error reading file");
        }
		
		
		try {
			File file = new File("config");
			deleteDir(file);
		} catch(Exception e) {
        	System.out.println("Error deleting configuration files");
		}
	}

	// Create Babel configuration files
	private static void configBabel() {
		File folder = new File("config");
		if (folder.isDirectory()) {
		   babelFolderExisted = true;
		} else {
			File dir = new File("config");
			dir.mkdir();
		}
		
		try {
			InputStream src = NL4OpenAPI.class.getResourceAsStream("/babelfy.properties");
			File f = new File("config/babelfy.properties");
			if(f.exists()) {
				babelFilesExisted = true;
			} else {
				Files.copy(src, Paths.get("config/babelfy.properties"));
			}
		} catch (Exception e) {
        	System.out.println("Error creating configuration files");
		}
		
		try {
			InputStream src = NL4OpenAPI.class.getResourceAsStream("/babelfy.var.properties");
			File f = new File("config/babelfy.var.properties");
			if(f.exists()) {
				babelFilesExisted = true;
			} else {
			    Files.copy(src, Paths.get("config/babelfy.var.properties"));
			}
		} catch (Exception e) {
        	System.out.println("Error creating configuration files");
		}
		
		try {
			InputStream src = NL4OpenAPI.class.getResourceAsStream("/babelnet.properties");
			File f = new File("config/babelnet.properties");
			if(f.exists()) {
				babelFilesExisted = true;
			} else {
			    Files.copy(src, Paths.get("config/babelnet.properties"));
			}
		} catch (Exception e) {
        	System.out.println("Error creating configuration files");
		}
		
		try {
			InputStream src = NL4OpenAPI.class.getResourceAsStream("/babelnet.var.properties");
			File f = new File("config/babelnet.var.properties");
			if(f.exists()) {
				babelFilesExisted = true;
			} else {
			    Files.copy(src, Paths.get("config/babelnet.var.properties"));
			}
		} catch (Exception e) {
        	System.out.println("Error creating configuration files");
		}
		
		try {
			InputStream src = NL4OpenAPI.class.getResourceAsStream("/jlt.properties");
			File f = new File("config/jlt.properties");
			if(f.exists()) {
				babelFilesExisted = true;
			} else {
			    Files.copy(src, Paths.get("config/jlt.properties"));
			}
		} catch (Exception e) {
        	System.out.println("Error creating configuration files");
		}
		
		try {
			InputStream src = NL4OpenAPI.class.getResourceAsStream("/jlt.var.properties");
			File f = new File("config/jlt.var.properties");
			if(f.exists()) {
				babelFilesExisted = true;
			} else {
			    Files.copy(src, Paths.get("config/jlt.var.properties"));
			}
		} catch (Exception e) {
        	System.out.println("Error creating configuration files");
		}
		
		try {
			InputStream src = NL4OpenAPI.class.getResourceAsStream("/ser.properties");
			File f = new File("config/ser.properties");
			if(f.exists()) {
				babelFilesExisted = true;
			} else {
			    Files.copy(src, Paths.get("config/ser.properties"));
			}
		} catch (Exception e) {
        	System.out.println("Error creating configuration files");
		}
	}
	
	// Pretty JSON result
	public static String toPrettyFormat(String jsonString) 
	{
	      JsonParser parser = new JsonParser();
	      JsonObject json = parser.parse(jsonString).getAsJsonObject();

	      Gson gson = new GsonBuilder().setPrettyPrinting().create();
	      String prettyJson = gson.toJson(json);

	      return prettyJson;
	}
	
	// Remove Babel configuration files
	private static void deleteDir(File file) {
		File[] contents = file.listFiles();
	    if (contents != null) {
	        for (File f : contents) {
	            if (! Files.isSymbolicLink(f.toPath())) {
	                deleteDir(f);
	            }
	        }
	    }

		if(file.isDirectory() && file.getName().contentEquals("config") && !babelFolderExisted) {
			file.delete();
		} else if(file.isFile() && !babelFilesExisted && (
				file.getName().contentEquals("babelfy.properties") || file.getName().contentEquals("babelfy.var.properties") || 
				file.getName().contentEquals("babelnet.properties") || file.getName().contentEquals("babelnet.var.properties") || 
				file.getName().contentEquals("jlt.properties") || file.getName().contentEquals("jlt.var.properties") || 
				file.getName().contentEquals("ser.properties"))){
			file.delete();
		}
	}
	
}