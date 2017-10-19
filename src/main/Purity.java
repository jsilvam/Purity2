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
import utils.XMLUtils;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;



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
		Test test=new Test();
		git.setLocation(git.getLocation()+"/"+commit);
		
		
		File sourceFile=git.downloadCommit(parent);
		File sourceFolder=ZipExtractor.extract(sourceFile, new File(git.getLocation(),parent));
		
		System.out.println(sourceFolder.getAbsolutePath());
		
		
		//compilar o projeto
		List<File> compiledProject=this.compileProject(sourceFolder);
		
		//Listar Classes
		File classesToTest=test.getClassesToTest(sourceFolder);
		
		
		
		File testsFolder=test.genarateTests(compiledProject, classesToTest, 10, git.getLocation());
		test.compileTests(compiledProject,testsFolder);
		//List<File> compiledTests=compileTests(compiledProject,testsFolder);
		//System.out.println(compiledTests);
		
		
		
		File report=test.run(compiledProject,testsFolder);
		System.exit(0);
		
		return 0;
	}
	
	private List<File> compileProject(File projectFolder) throws Exception{
		List<File> modules=XMLUtils.getModules(new File(projectFolder,"pom.xml"));
		XMLUtils.addPlugins(modules);
		
		Invoker invoker = new DefaultInvoker();
		if(System.getProperty("os.name").contains("Linux"))
			invoker.setMavenHome(new File("/usr/share/maven"));
		else
			invoker.setMavenHome(new File("C:\\Program Files\\apache-maven-3.5.0"));
		
		InvocationRequest request = new DefaultInvocationRequest();
		request.setPomFile( new File( projectFolder,"pom.xml" ) );
		request.setGoals( Arrays.asList( "install" , "-DskipTests") );
		invoker.execute( request );
		
		//encontrar o projeto compilado
		List<File> result=new ArrayList<File>();
		for(File module:modules) {
			File folder=new File(module,"target");
			if(folder.exists())
				result.add(FileUtils.findSingleFile(folder, ".*jar-with-dependencies.jar"));
		}
		return result;
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
