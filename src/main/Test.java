package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import utils.FileUtils;
import utils.XMLUtils;

public class Test {
	
	public boolean compare(File report1, File report2) throws ParserConfigurationException, SAXException, IOException {
		NodeList nList=XMLUtils.getElementsByTagName(report1, "failure");
		List<String> l1=new ArrayList<String>();
		System.out.println("source");
		for(int i=0;i<nList.getLength();i++) {
			l1.add(((Element)nList.item(i)).getAttribute("type"));
			System.out.println(l1.get(i));
			System.out.println(nList.item(i).getTextContent());
		}
		
		nList=XMLUtils.getElementsByTagName(report2, "failure");
		List<String> l2=new ArrayList<String>();
		System.out.println("Target");
		for(int i=0;i<nList.getLength();i++) {
			l2.add(((Element)nList.item(i)).getAttribute("type"));
			System.out.println(l2.get(i));
			System.out.println(nList.item(i).getTextContent());
		}
		
		return l1.equals(l2);
	}
	
	
	
	public File getClassesToTest(File projectFolder) throws Exception {
		File file=new File(projectFolder,"classesList.txt");
		List<File> modules=XMLUtils.getModules(new File(projectFolder,"pom.xml"));
		FileWriter fw= new FileWriter(file);
		
		for(File module:modules) {
			List<String> classes=FileUtils.listClass(new File(module,"src/main/java"));
			for(String c: classes) {
				fw.write(c);
				fw.write("\n");
				fw.flush();
			}
		}
		fw.close();
		return file;
	}
	
	public File genarateTests(List<File> projectFiles,File classesList, int timeLimit, File outputDir) throws IOException, InterruptedException {
		File outDir=new File(outputDir,"tempTest");
		if(!outDir.exists())
			outDir.mkdirs();
		File command=new File(outDir,"generateCommand.sh");
		FileWriter fw= new FileWriter(command);
		fw.write("java -ea");
		fw.write(" -classpath jars/*");
		for(File file:projectFiles)
			fw.write(":"+file);
		fw.write(" randoop.main.Main gentests ");
		fw.write(" --classlist="+classesList);
		fw.write(" --timelimit="+timeLimit);
		fw.write(" --ignore-flaky-tests=true");
		fw.write(" --junit-output-dir="+outDir);
		fw.flush();
		fw.close();
		
		runProcess("bash "+command);
		return outDir;
		//return FileUtils.findFiles(outDir, "RegressionTest.*.java");
	}
	
	
	public void compileTests(List<File> projectFiles,File testFolder) throws IOException, InterruptedException{
		File command=new File(testFolder,"compileCommand.sh");
		FileWriter fw= new FileWriter(command);
		fw.write("javac -classpath jars/*");
		for(File file:projectFiles)
			fw.write(":"+file);
		fw.write(" $(find "+testFolder+"/* | grep .java)");
		fw.flush();
		fw.close();
		
		runProcess("bash "+command);
		//return FileUtils.findFiles(testFiles.get(0).getParentFile(), "RegressionTest.*.class");
		
	}
	
	
	
	public File runTests(List<File> projectFiles,File testFolder) throws IOException, InterruptedException {
		File XMLReport= new File(testFolder,"junit_report.xml");
		for(int i=0;XMLReport.exists();i++){
			XMLReport= new File(testFolder,"junit_report.xml"+i);
		}
		File command=new File(testFolder,"runCommand.sh");
		FileWriter fw= new FileWriter(command);
		fw.write("java -classpath jars/*:");
		fw.write(testFolder+"/.");
		for(File file:projectFiles)
			fw.write(":"+file);
		fw.write(" -Dorg.schmant.task.junit4.target="+XMLReport);
		fw.write(" barrypitman.junitXmlFormatter.Runner RegressionTest");
		fw.flush();
		fw.close();
		
		runProcess("bash "+command);
		return XMLReport;
		/*JUnitCore junit = new JUnitCore();
		List<Result> result=new ArrayList<Result>();
		for(File test:tests) {
			result.add(junit.run(Class.forName(test.getAbsolutePath())));
		}
		return result;*/
	}
	
	public void runProcess(String command) throws IOException, InterruptedException {
		
		Process p=Runtime.getRuntime().exec(command);
		BufferedReader reader =
				new BufferedReader(new InputStreamReader(p.getInputStream()));
		String s;
		while ((s=reader.readLine()) != null)
			System.out.println(s);
		p.waitFor();
		//return FileUtils.findFiles(testFiles.get(0).getParentFile(), "RegressionTest.*.class");
		
	}
}
