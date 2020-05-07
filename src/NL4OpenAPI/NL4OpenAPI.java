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
import java.util.Map.Entry;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
				if(!srcFileName.contains( "." + defaultFileExtension)) {
					srcFileName += "." + defaultFileExtension;
				}
				break;
			case 2 : 
				srcFileName = args[0];
				dstFileName = args[1];
				if(!srcFileName.contains( "." + defaultFileExtension)) {
					srcFileName += "." + defaultFileExtension;
				}
				if(!dstFileName.contains( "." + defaultFileExtension)) {
					dstFileName += "." + defaultFileExtension;
				}
		}
		
		configBabel();
		openapi2NL();
                
	}
	
	public NL4OpenAPI(String newSrcFileName, String newDstFileName) {
		srcFileName = newSrcFileName;
		dstFileName = newDstFileName;
		if(!srcFileName.contains( "." + defaultFileExtension)) {
			srcFileName += "." + defaultFileExtension;
		}
		if(!dstFileName.contains( "." + defaultFileExtension)) {
			dstFileName += "." + defaultFileExtension;
		}
		
		configBabel();	
	}

	// Apply NLP to existing OpenAPI to add descriptions
	public static void openapi2NL() {
		
		// Parameters of NL4APIdocs
		HashMap<String,String> generalHashMap = new HashMap<>();
		ArrayList<HashMap<String,String>> fullParametersList = new ArrayList<HashMap<String,String>>();
		ArrayList<HashMap<String,String>> parametersList = new ArrayList<HashMap<String,String>>();
		ArrayList<HashMap<String,String>> fullPropertiesList = new ArrayList<HashMap<String,String>>();

		HashMap<String, String> apiHashMap = new HashMap<>();

		HashMap<String, String> examplesHashMap = new HashMap<>();
		
		//JSON parser object to parse read file
		JsonParser jsonParser = new JsonParser();
         
        try (FileReader reader = new FileReader(srcFileName))
        {
        	String openapiTitle = "data";
        	
            //Read JSON file
        	JsonObject obj = (JsonObject) jsonParser.parse(reader);
        	
        	JsonObject openapiInfo = (JsonObject) obj.get("info");
        	openapiTitle = openapiInfo.get("title").toString().replace("\"", "");            
    		apiHashMap.put("fileName", openapiTitle);
        	
    		String url = "localhost:8080";
    		try {
	        	JsonArray openapiServers = (JsonArray) obj.get("servers");
	        	JsonObject openapiUrls = (JsonObject) openapiServers.get(0);
	        	url = openapiUrls.get("url").getAsString();
    		} catch(Exception e) {}

    		try {
	        	JsonObject openapiComponentsProperties = (JsonObject) obj.getAsJsonObject("components")
	        			.getAsJsonObject("schemas").getAsJsonObject("mainComponent").getAsJsonObject("properties");
	    		Set<Entry<String, JsonElement>> openapiComponentsPropertiesKeys = openapiComponentsProperties.entrySet();
	    		Iterator<Entry<String, JsonElement>> openapiComponentsPropertiesKeyIterator = openapiComponentsPropertiesKeys.iterator();
	    		
	    		ArrayList<HashMap<String, String>> componentsHashMaps = new ArrayList<HashMap<String, String>>();
	    		while (openapiComponentsPropertiesKeyIterator.hasNext()) {
	            	parametersList.clear();
	            	try {
	            		String path = (String) openapiComponentsPropertiesKeyIterator.next().getKey();
		            		  
		        		HashMap<String, String> parameterHashMap = new HashMap<>();
		        		parameterHashMap.put("name", path);
		        		
		        		String example = "";
		        		try {
		        			example = openapiComponentsProperties.getAsJsonObject(path).get("example").toString().replace("\"", "");
		        		} catch(Exception e) {
		            		System.out.println(e.getMessage());
		            	}
		        		
		        		parameterHashMap.put("example", example);
	
		        		examplesHashMap.put(path, example);
		        		
		        		String type = "";
		        		try {
		        			type = openapiComponentsProperties.getAsJsonObject(path).get("type").toString().replace("\"", "");
		        		} catch(Exception e) {
		            		System.out.println(e.getMessage());
		            	}
		        		parameterHashMap.put("type", type);
		        		parameterHashMap.put("ptype", "property");
		        		parametersList.add(parameterHashMap);
		        		fullPropertiesList.add(parameterHashMap);

		        		String componentDescription = NLGenT.generateText(apiHashMap, parametersList, 2, generalHashMap, url);
		                System.out.println(componentDescription);
		                
		                // add description to new OpenAPI json
		                openapiComponentsProperties.getAsJsonObject(path).addProperty("description", componentDescription);
		                
	            	} catch(Exception e) {
	            		System.out.println(e.getMessage());
	            	}
	            }
    		} catch (Exception e) {
    			e.printStackTrace();
    		}

    		try {
	        	JsonObject openapiPaths = (JsonObject) obj.getAsJsonObject("paths");
	    		Set<Entry<String, JsonElement>> openapiPathsKeys = openapiPaths.entrySet();
	    		Iterator<Entry<String, JsonElement>> openapiPathsKeyIterator = openapiPathsKeys.iterator();
	    		ArrayList<HashMap<String, String>> parametersHashMaps = new ArrayList<HashMap<String, String>>();
			    		
	    		while (openapiPathsKeyIterator.hasNext()) {
	    			String path = (String) openapiPathsKeyIterator.next().getKey();
	    			
	                fullParametersList.clear();
	                
	                try {
		            	JsonArray openapiPathParameters = (JsonArray) openapiPaths.getAsJsonObject(path).getAsJsonObject("get")
		            			.getAsJsonArray("parameters");
		            	
		        		for (int i = 0; i < openapiPathParameters.size(); i++) {
		            	   
		            	   parametersList.clear();
		            	   
		            	   JsonObject param = (JsonObject) openapiPathParameters.get(i);
		            	   String paramName = param.get("name").toString().replace("\"", "");
		            	   		            		
		            	   HashMap<String, String> parameterHashMap = new HashMap<>();
		            	   parameterHashMap.put("name", paramName);
		
		            	   String example = "";
		            	   boolean exampleNull = false;
		            	   try {
			   	        		example = examplesHashMap.get(paramName).toString().replace("\"", "");
		  	        		} catch(Exception e) {
		  	            		//System.out.println(e.getMessage());
		  	            		exampleNull = true;
		  	        		}
		            	    if(exampleNull) {
			            	   try {				   	        		
				   	        		String exampleAux = param.get("example").toString().replace("\"", "");
				   	        		example = exampleAux;
			  	        		} catch(Exception e) {}
		            	    }
			   	        	parameterHashMap.put("example", example);
		  	        		
		  	        		String type = "";
		  	        		try {
		  	        			type = param.getAsJsonObject("schema").get("type").toString().replace("\"", "");
		  	        		} catch(Exception e) {}
		  	        		parameterHashMap.put("type", type);
		  	        		
		  	        		String in = "";
		  	        		try {
		  	        			in = param.get("in").toString().replace("\"", "");
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


		            	    boolean paramCompleted = false;
		            	    String paramCompletedDescription = "";
		            	    for(int j = 0; j < parametersHashMaps.size(); j++) {
								if(parametersHashMaps.get(j).get("name").contentEquals(paramName) &&
										parametersHashMaps.get(j).get("required").contentEquals(String.valueOf(required)) &&
										parametersHashMaps.get(j).get("in").contentEquals(in) &&
										parametersHashMaps.get(j).get("type").contentEquals(type) &&
										parametersHashMaps.get(j).get("example").contentEquals(example)) {
										paramCompleted = true;
										paramCompletedDescription = parametersHashMaps.get(j).get("description");
								}
		            	    }
		            	    
		  	        		String parameterDescription = "";
		  	        		if(!paramCompleted) {
		  	        			parameterDescription = NLGenT.generateText
		  	        					(apiHashMap, parametersList, 2, generalHashMap, url);

				        		HashMap<String, String> paramCompletedHashMap = new HashMap<>();
				        		paramCompletedHashMap.put("name", paramName);
				        		paramCompletedHashMap.put("description", parameterDescription);
				        		paramCompletedHashMap.put("required", String.valueOf(required));
				        		paramCompletedHashMap.put("in", in);
				        		paramCompletedHashMap.put("type", type);
				        		paramCompletedHashMap.put("example", example);
				        		parametersHashMaps.add(paramCompletedHashMap);
			  	                
		  	        		} else {
		  	        			parameterDescription = paramCompletedDescription;
		  	        		}
		  	        		
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
		    			String methodDescription = NLGenT.generateText(apiHashMap, fullPropertiesList, 1, generalHashMap, url);
	
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
	            String apiDescription = NLGenT.generateText(apiHashMap, fullPropertiesList, 0, generalHashMap, url);
	            System.out.println(apiDescription);
	            
                //TODO: add description to new OpenAPI json
	            obj.getAsJsonObject("info").addProperty("description", apiDescription);
                
            } catch(Exception e) {
            	e.printStackTrace();
            }
            
            //Write JSON file
            try (FileWriter file = new FileWriter(dstFileName)) {
 
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
