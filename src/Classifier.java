import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.Integer;
import java.lang.Math;
import java.util.Arrays;

public class Classifier {

	static List<HashMap<String, Integer>> classifier = new ArrayList<HashMap<String, Integer>>();
	static List<String> ids = new ArrayList<String>();
	static int[] totalCounts = new int[15];
	static int[] lowestCounts = new int[15];
	final static double threshold = -80.00;
	
	public static void main(String[] args) throws IOException {
		//gets files for language models and input from arguments
		File folder = new File (args[0]);
		String text = args[1];

		File[] langModels = folder.listFiles();

		for (File f : langModels) {
        	build(f);
        }
        
        boolean unknown;
        if (args[2].equals("unknown")) {
        	unknown = true;
        } else {
        	unknown = false;
        }

        File toClassify = new File(text);
        classify(toClassify, unknown);
	}

	public static void build(File file) throws IOException {
		String name = file.getName();
		String code = name.substring(0, 3); //gets language code from file name
		ids.add(code);
		//to align indices; indices are aligned to simplify retrieval
		int index = ids.indexOf(code); 
		HashMap<String, Integer> model = new HashMap<String, Integer>();

		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "ISO_8859_1"));

		String line;

		int lowCount = 0;
        
        while ((line = reader.readLine()) != null) {
        	String[] entry = line.split("\t"); 
        	//strips punctuation
        	String word = entry[0].replaceAll("[.,!¡¥$£¿;:¹²³()\"\'—–\\-\\/\\«\\»]", "");
        	int count = Integer.valueOf(entry[1]);
        	//adds word and count to map
        	model.put(word, count); 
        	//keeps track of total word count at aligned index in array
        	totalCounts[index] = totalCounts[index] + count; 
        	//keeps track of lowest word count
        	if (lowCount == 0 || count < lowCount) {
        		lowCount = count;
        	}
        }

        //adds lowest count to aligned index in array
        lowestCounts[index] = lowCount;

        //adds map to arraylist
        classifier.add(model);
	}

	public static void classify(File file, boolean unknown) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "ISO_8859_1"));

		String line;
        
        while ((line = reader.readLine()) != null) {
        	String[] splitId = line.split("\t");
        	//gets example identifier
        	String exId = splitId[0];
        	String examp = splitId[1];
        	//splits example into tokens
        	String[] tokens = splitId[1].split(" ");
        	//to store log-prob scores for each language for this example
        	double[] logProbs = new double[classifier.size()];
        	for (String s : tokens) {
        		String stripped = s.replaceAll("[.,!¡¥$£¿;:¹²³()\\[\\]\"\'—–\\-\\/\\«\\»]", "");
	        	//calculates probabilities for each language
	        	for (int i = 0; i < classifier.size(); i++) {
	        		int wordCount;
	        		//this word is known; count is from language model
	        		if (classifier.get(i).containsKey(stripped)) {
	        			wordCount = classifier.get(i).get(stripped);
	        		} else { //this word is unseen; count is lowest count
	        			wordCount = lowestCounts[i];
	        		}
	        		//calculates log-prob of current language
	        		double prob = wordCount/(double) totalCounts[i];
	        		double logProb = Math.log10(prob);
	        		logProbs[i] = logProbs[i] + logProb;
	        	}
	        }

			//print results to stdout
	        int bestInd = 0; //index of highest log-prob score for this example
	        System.out.println(exId + "\t" + examp); //prints identifier and example text
	        //prints log-probs for each language
	        for (int i = 0; i < logProbs.length; i++) {
	        	System.out.println(ids.get(i) + "\t" + logProbs[i]);
	        	//tracks highest score
	        	if (logProbs[i] > logProbs[bestInd]) {
	        		bestInd = i;
	        	}
	        }

	        //if using threshold 
	        String result = "";
	        if (unknown && logProbs[bestInd] < threshold) {
	        	//prints unknown if under threshold
	        	result = "unk"; 
	        } else {
	        	//prints best score if above; also if threshold is not being used
	        	result = ids.get(bestInd); 
	        }
	        
	        System.out.println("result\t" + result);
        }
	}


}