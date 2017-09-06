package main;



import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import randoop.main.Main;

import utils.FileUtils;
import utils.GithubDownloader;
import utils.ZipExtractor;

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
		
		
		if(System.getProperty("os.name").contains("Linux"))
			git.setLocation("/tmp/Projeto/Downloads/"+commit);
		else
			git.setLocation("c:/tmp/Projeto/Downloads/"+commit);
		
		
		

		File sourceFile=git.downloadCommit(parent);
		File sourceFolder=ZipExtractor.extract(sourceFile, new File(git.getLocation(),parent));
		
		System.out.println(sourceFolder.getAbsolutePath());
		
		//compilar o projeto
		Invoker invoker = new DefaultInvoker();
		if(System.getProperty("os.name").contains("Linux"))
			invoker.setMavenHome(new File("/usr/share/maven"));
		else
			invoker.setMavenHome(new File("C:\\Program Files\\apache-maven-3.5.0"));
		
		InvocationRequest request = new DefaultInvocationRequest();
		request.setPomFile( new File( sourceFolder,"pom.xml" ) );
		request.setGoals( Arrays.asList( "package" ) );
		invoker.execute( request );
		
		//encontrar o projeto compilado
		File f=FileUtils.find(sourceFolder, ".*[^(sources)].jar");
		
		//Listar Classes
		FileWriter fw= new FileWriter(new File(sourceFolder,"classesList.txt"));
		List<String> classes=FileUtils.listClass(new File(sourceFolder,"src/main/java"));
		for(String c: classes) {
			fw.write(c);
			fw.write("\n");
			fw.flush();
		}
		fw.close();
		
		
		System.out.println(f);
		System.out.println(sourceFolder);
		
		
		Process p=Runtime.getRuntime().exec("java -ea -classpath "
				+ f.getAbsolutePath()+":/tmp/Projeto/randoop-all-3.1.5.jar "
				+ "randoop.main.Main gentests --classlist="+sourceFolder +"classesList.txt --timelimit=10");
		BufferedReader reader =
				new BufferedReader(new InputStreamReader(p.getInputStream()));
		while ((reader.readLine()) != null) {}
		p.waitFor();
		System.out.println(p.exitValue());
		
		

		
		System.exit(0);
		
		
		
		
		/*Process p=Runtime.getRuntime().exec("mvn -f "+sourceFolder.getAbsolutePath()+" clean");
		BufferedReader reader =
				new BufferedReader(new InputStreamReader(p.getInputStream()));
		String s;
		while ((s=reader.readLine()) != null) {System.out.println(s);}
		p.waitFor();
		System.out.println("Estatus da compila��o: "+p.exitValue());*/
		
		
		
		File targetFile=git.downloadCommit(commit);
		File targetFolder=ZipExtractor.extract(targetFile, new File(git.getLocation(),commit));
		
		System.out.println(targetFolder.getAbsolutePath());
		request = new DefaultInvocationRequest();
		request.setPomFile( new File( targetFolder,"pom.xml" ) );
		request.setGoals( Arrays.asList( "package" ) );	
		
		invoker.execute(request);
		
		/*
		System.out.println("Copiling: "+targetFolder);
		p=Runtime.getRuntime().exec("mvn -f "+targetFolder.getAbsolutePath()+" compile");
		reader =
				new BufferedReader(new InputStreamReader(p.getInputStream()));
		while ((reader.readLine()) != null) {}
		p.waitFor();
		System.out.println("Estatus da compila��o: "+p.exitValue());
		*/
		return 0;
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
