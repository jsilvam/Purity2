package main;



import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import utils.FileUtils;
import utils.GithubDownloader;
import utils.ZipExtractor;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import saferefactor.core.Parameters;
import saferefactor.core.Report;
import saferefactor.core.SafeRefactor;
import saferefactor.core.SafeRefactorException;
import saferefactor.core.SafeRefactorImp;
import saferefactor.core.util.Project;



public class Purity {
	
	private String urlRepository;
	
	public Purity(String urlRepository){
		this.urlRepository=urlRepository;
	}
	
	public void setUrlRepository(String urlRepository){
		this.urlRepository=urlRepository;
	}
	
	public String getUrlRepository(){
		return this.urlRepository;
	}
	

	public int check(String commit, String parent) throws Exception{
		
		
		GithubDownloader git=new GithubDownloader(urlRepository);
		git.setLocation(git.getLocation()+"/"+commit);
		
		
		File sourceFile=git.downloadCommit(parent);
		File sourceFolder=ZipExtractor.extract(sourceFile, new File(git.getLocation(),parent));
		
		//File targetFile=git.downloadCommit(commit);
		//File targetFolder=ZipExtractor.extract(targetFile, new File(git.getLocation(),commit));
		
		
		//compilar o projeto
		//List<File> compiledSourceProject=this.compileProject(sourceFolder);
		this.compileProject(sourceFolder);
		
		//List<File> compiledTargetProject=this.compileProject(targetFolder);
		
		
		//System.out.println(runSafeRefactor(sourceFolder,targetFolder));
		
		//Listar Classes
		File classesToTest=this.getClassesToTest(sourceFolder);
		
		
		
		List<File> tests=this.genarateTests(sourceFolder, classesToTest, 10 );
		//List<File> compiledTests=compileTests(sourceFolder,tests);
		//System.out.println(compiledTests);
		
		System.exit(0);
		
		
		
		
		
		
		return 0;
	}
	
	private void compileProject(File projectFolder) throws Exception{
		Invoker invoker = new DefaultInvoker();
		if(System.getProperty("os.name").contains("Linux"))
			invoker.setMavenHome(new File("/usr/share/maven"));
		else
			invoker.setMavenHome(new File("C:\\Program Files\\apache-maven-3.5.0"));
		
		InvocationRequest request = new DefaultInvocationRequest();
		request.setPomFile( new File( projectFolder,"pom.xml" ) );
		request.setGoals( Arrays.asList( "compile" , "dependency:copy-dependencies") );
		invoker.execute( request );
		
		//encontrar o projeto compilado
		/*List<File> modules=FileUtils.getModules(new File(projectFolder,"pom.xml"));
		List<File> result=new ArrayList<File>();
		for(File module:modules) {
			File folder=new File(module,"target");
			if(folder.exists())
				result.add(FileUtils.findSingleFile(folder, ".*[^(sources)].jar"));
		}
		return result;*/
	}
	
	
	private File getClassesToTest(File projectFolder) throws Exception {
		File file=new File(projectFolder,"classesList.txt");
		List<File> modules=FileUtils.getModules(new File(projectFolder,"pom.xml"));
		FileWriter fw= new FileWriter(file);
		
		for(File module:modules) {
			List<String> classes=FileUtils.listClass(new File(module,"src/main/java"));
			for(String c: classes) {
				fw.write(c);
				fw.write("\n");
				fw.flush();
			}
			System.out.println(module.getName());
		}
		fw.close();
		return file;
	}
	
	
	private List<File> genarateTests(File projectFolder,File classesList, int timeLimit) throws Exception {
		File command=new File(projectFolder,"command.sh");
		FileWriter fw= new FileWriter(command);
		fw.write("pwd");
		fw.write("\njava -ea");
		fw.write(" -classpath lib/randoop-all-3.1.5.jar");
		List<File> modules=FileUtils.getModules(new File(projectFolder,"pom.xml"));
		for(File file:modules) {
			File f=new File(file,"target");
			if(f.exists()) {
				fw.write(":"+file+"/dependency/*");
				fw.write(":"+file+"/classes/*");
			}
		}
		fw.write(" randoop.main.Main gentests ");
		fw.write(" --classlist="+classesList);
		fw.write(" --timelimit="+timeLimit);
		fw.write(" --junit-output-dir="+projectFolder);
		fw.flush();
		fw.close();
		
		
		Process p=Runtime.getRuntime().exec("bash "+command);
		BufferedReader reader =
				new BufferedReader(new InputStreamReader(p.getInputStream()));
		String s;
		while ((s=reader.readLine()) != null)
			System.out.println(s);
		p.waitFor();
		return FileUtils.findFiles(projectFolder, "RegressionTest.*.java");
	}
	
	private List<File> genarateTests(List<File> projectFiles,File classesList, int timeLimit, File outputDir) throws IOException, InterruptedException {
		File command=new File(outputDir,"command.sh");
		FileWriter fw= new FileWriter(command);
		fw.write("pwd");
		fw.write("\njava -ea");
		fw.write(" -classpath lib/randoop-all-3.1.5.jar");
		for(File file:projectFiles)
			fw.write(":"+file);
		fw.write(" randoop.main.Main gentests ");
		fw.write(" --classlist="+classesList);
		fw.write(" --timelimit="+timeLimit);
		fw.write(" --junit-output-dir="+outputDir);
		fw.flush();
		fw.close();
		
		
		Process p=Runtime.getRuntime().exec("bash "+command);
		BufferedReader reader =
				new BufferedReader(new InputStreamReader(p.getInputStream()));
		String s;
		while ((s=reader.readLine()) != null)
			System.out.println(s);
		p.waitFor();
		return FileUtils.findFiles(outputDir, "RegressionTest.*.java");
	}
	
	
	private List<File> compileTests(List<File> projectFiles,List<File> testFiles) throws IOException, InterruptedException{
		String command="javac -classpath lib/junit-4.12.jar";
		
		for(File file:projectFiles)
			command+=":"+file;
		
		for(File file:testFiles)
			command+=" "+file;
		
		System.out.println(command);
		
		Process p=Runtime.getRuntime().exec(command);
		BufferedReader reader =
				new BufferedReader(new InputStreamReader(p.getInputStream()));
		String s;
		while ((s=reader.readLine()) != null)
			System.out.println(s);
		p.waitFor();
		return FileUtils.findFiles(testFiles.get(0).getParentFile(), "RegressionTest.*.class");
		
	}
	
	private List<Result> runTests(List<File> tests) throws ClassNotFoundException {
		JUnitCore junit = new JUnitCore();
		List<Result> result=new ArrayList<Result>();
		for(File test:tests) {
			result.add(junit.run(Class.forName(test.getAbsolutePath())));
		}
		return result;
	}
	
	private File prepareLib(File projectFolder) throws IOException, MavenInvocationException {
		File lib=new File(projectFolder,"library");
		lib.mkdir();
		
		Invoker invoker = new DefaultInvoker();
		if(System.getProperty("os.name").contains("Linux"))
			invoker.setMavenHome(new File("/usr/share/maven"));
		else
			invoker.setMavenHome(new File("C:\\Program Files\\apache-maven-3.5.0"));
		
		InvocationRequest request = new DefaultInvocationRequest();
		request.setPomFile( new File( projectFolder,"pom.xml" ) );
		request.setGoals( Arrays.asList( "dependency:copy-dependencies") );
		invoker.execute( request );
		
		List<File> jars=FileUtils.findFiles(projectFolder, ".*[^(sources)].jar");
		for(File f:jars) {
			File dest=new File(lib,f.getName());
			FileUtils.copyFile(f, dest);
		}
		return lib;
	}
	
	private boolean runSafeRefactor(File sourceFolder, File targetFolder) throws Exception {
		List<File> sourceModules=FileUtils.getModules(new File(sourceFolder,"pom.xml"));
		List<File> targetModules=FileUtils.getModules(new File(targetFolder,"pom.xml"));
		if(sourceModules.size()!=targetModules.size())
			return false;
		
		System.out.println("Preparing lib");
		File sourceLib=prepareLib(sourceFolder);
		File targetLib=prepareLib(targetFolder);
		
		for(File sourceModule: sourceModules) {
			File targetModule=new File(targetFolder.getAbsolutePath() + sourceModule.getAbsolutePath().substring(sourceFolder.getAbsolutePath().length()-1));
			if(!targetModule.exists())
				return false;
			if(!(new File(sourceModule,"target")).exists() || !(new File(targetModule,"target")).exists())
				continue;
			
			
			Project source= new Project();
			source.setProjectFolder(sourceModule);
			source.setBuildFolder(new File(sourceModule,"target/classes"));
			source.setSrcFolder(new File(sourceFolder, "src\\main\\java"));
			source.setLibFolder(sourceLib);
			
			Project target= new Project();
			source.setProjectFolder(targetModule);
			source.setBuildFolder(new File(targetModule,"target/classes"));
			source.setSrcFolder(new File(targetFolder, "src\\main\\java"));
			source.setLibFolder(targetLib);
			
			Parameters parameters = new Parameters();
			parameters.setTimeLimit(120);
			//parameters.setCheckCoverage(true);
			parameters.setCompileProjects(true);
			SafeRefactor sr = new SafeRefactorImp(source, target,parameters);
			
			try {
				sr.checkTransformation();
			} catch (SafeRefactorException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(0);
			}
			
			Report result=sr.getReport();
			
			if(!result.isRefactoring())
				return false;
			
		}
		
		
		
		
		
		
		return true;
	}
	
	private void deleteDirectory(File dir){
		File[] contents=dir.listFiles();
		for(File f: contents){
			if(f.isDirectory())
				deleteDirectory(f);
			else
				f.delete();
		}
		dir.delete();
	}
	
}
