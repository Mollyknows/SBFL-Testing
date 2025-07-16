package sbflTestingProject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class testingProject {
	public static void main(String [] args) throws IOException {
		String outputFileName = "program_output.txt";
		PrintWriter writer = new PrintWriter(new FileWriter(outputFileName));

		//Get folder containing testing data
		File folder = new File("NewCoverageData");
		System.out.println("abs path of folder: " + folder.getAbsolutePath());
		System.out.println(folder.getName());

		//iterate through files
		File[] files = folder.listFiles();
		double totalPass = 0.0;
		double totalFail = 0.0;
		ArrayList<String> filenames = new ArrayList<>();

		

		Map<String, Map<String, double[]>> masterMethodList = new HashMap<>();
		for (File file : files) {
			System.out.println("\n--- Reading file: " + file.getName() + " ---");
			filenames.add(file.getName());
			try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
				String line;
				int i=0;
				//Array that contains data about the tests
				String[] fileInfo;
				boolean testPass=false;
				while ((line = reader.readLine()) != null) {
					//get class name from first line
					if (i==0) {
						String[] testInfo = line.split("[.]");
						String[] testNameDetails = testInfo[testInfo.length-1].split(" ");

						//System.out.println("testClassname: " + testInfo[testInfo.length-2] + " testname: " + testNameDetails[0] + " testpassed: " + testNameDetails[1]);
						if (testNameDetails[1].contains("true")) {
							testPass = true;
							//increment total passing tests
							totalPass++;
						}
						else {
							//increment total failing tests
							totalFail++;
						}
					}
					else if (!line.trim().isEmpty()) {
						//Get Class and Method Name
						//System.out.println(testPass);
						String[] splitOnColon = line.split(":");
						String methodName = splitOnColon[1];
						String[] splitOnBackslash = splitOnColon[0].split("/");
						String className = splitOnBackslash[splitOnBackslash.length-1];
						//System.out.println("class: " + className);
						//System.out.println("method: " + methodName);

						//if class doesn't exist in hash map, add class to hash map with method
						if (masterMethodList.get(className) == null) {
							Map <String, double[]> newClassMethod = new HashMap<>();
							double[] initArray = new double [2];
							newClassMethod.put(methodName, initArray);
							masterMethodList.put(className, newClassMethod);
							//System.out.println(masterMethodList.get(className).get(methodName)[0]);
						}
						//if class exists, but method does not add method to hash map 
						else if(masterMethodList.get(className).get(methodName) == null){
							double[] initArray = new double [2];
							masterMethodList.get(className).put(methodName, initArray);
							//System.out.println(masterMethodList.get(className).get(methodName)[0]);
						}
						//if class and method exists add one to pass ([0]) or fail ([1]) based on result
						if (masterMethodList.get(className).get(methodName) != null) {
							if (testPass) {
								masterMethodList.get(className).get(methodName)[0]++;
							} else {
								masterMethodList.get(className).get(methodName)[1]++;
							}
						}
					}
					i++;
				}
			} catch (IOException e) {
				System.err.println("Error reading file " + file.getName() + ": " + e.getMessage());
			}
		}	
		ArrayList<LinkedList<Object>> finalList = new ArrayList<>();
		//Calculate the 4 SBFL scores for each method and add them to a linked list sorted by Tarantula Score
		for(String testedClass : masterMethodList.keySet()) {
			for(String testedMethod : masterMethodList.get(testedClass).keySet()){
				LinkedList<Object> currentList = new LinkedList<>();
				double methodPass = masterMethodList.get(testedClass).get(testedMethod)[0];
				double methodFail = masterMethodList.get(testedClass).get(testedMethod)[1];
				currentList.add(testedClass); //index 0
				currentList.add(testedMethod); //index 1
				currentList.add(methodPass); //index 2
				currentList.add(methodFail); //index 3
				double tarantulaCalc = 0.0;
				if (methodFail > 0) {
					tarantulaCalc = calcTarantula(methodFail, totalFail, methodPass, totalPass); 
				}
				currentList.add(tarantulaCalc); //index 4
				currentList.add(calcSBI(methodFail, methodPass)); //index 5
				currentList.add(calcJaccard(methodFail, totalFail, methodPass)); //index 6
				currentList.add(calcOchiai(methodFail, totalFail, methodPass)); //index 7
				
				finalList.add(currentList);

				
			}
		}
		
		Collections.sort(finalList, new Comparator<LinkedList<Object>>(){
			@Override
			public int compare(LinkedList<Object> list1, LinkedList<Object> list2) {
				if (!(list1.get(4) instanceof Double) || !(list2.get(4) instanceof Double)) {
					return 0;
				}
				Double tarantula1 = (Double) list1.get(4);
                Double tarantula2 = (Double) list2.get(4);

                // For descending order, compare(b, a)
                return Double.compare(tarantula2, tarantula1);
				
			}
			
		});
		
        
		// output final data
		
		writer.println("Total Pass: " + totalPass);
		writer.println("Total Fail: " + totalFail);
		writer.println("test files set to 'fail': 4, 7, 21, 27, 32" );
		writer.println("List ordered by Descending order of Tarantula Formula result: ");
		for (LinkedList<Object> output : finalList) {
			writer.println("=====");
			writer.println("TestedClass: " + output.get(0));
			writer.println("TestedMethod: " + output.get(1));
			writer.println("Method Pass: " + output.get(2));
			writer.println("Method Fail: " + output.get(3));
			writer.println("Tarantula Formula:" + output.get(4));
			writer.println("SBI Formula:" + output.get(5));
			writer.println("Jaccard Formula:" + output.get(6));
			writer.println("Ochiai Formula:" + output.get(7));

		}
		
		//Print all reviewed filenames for review's sake
		writer.println("=====All reviewed files=====");
		for (String filename : filenames) {
			writer.println(filename);
		}
		writer.flush();
		writer.close();
	}

	public static double calcTarantula(double methodFail, double totalFail, double methodPass, double totalPass) {  
		return ((methodFail/totalFail)/((methodFail/totalFail)+(methodPass/totalPass)));
	}

	public static double calcSBI(double methodFail, double methodPass) {
		return (methodFail/(methodFail + methodPass));
	}

	public static double calcJaccard(double methodFail, double totalFail, double methodPass) {
		return (methodFail/(totalFail + methodPass));
	}

	public static double calcOchiai(double methodFail, double totalFail, double methodPass) {
		return (methodFail/Math.sqrt(totalFail *(methodPass + methodFail)));
	}
}
