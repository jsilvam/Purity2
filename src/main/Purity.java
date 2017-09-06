package main;



import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;

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
		
		git.setLocation("c:/tmp/Projeto/Downloads/"+commit);
		

		File sourceFile=git.downloadCommit(parent);
		File sourceFolder=ZipExtractor.extract(sourceFile, new File(git.getLocation(),parent));
		
		System.out.println(sourceFolder.getAbsolutePath());
		InvocationRequest request = new DefaultInvocationRequest();
		request.setPomFile( new File( sourceFolder,"pom.xml" ) );
		request.setGoals( Arrays.asList( "compile" ) );	
		
		Invoker invoker = new DefaultInvoker();
		invoker.setMavenHome(new File("C:\\Program Files\\apache-maven-3.5.0"));
		invoker.execute( request );
		
	
		
		
		/*Process p=Runtime.getRuntime().exec("mvn -f "+sourceFolder.getAbsolutePath()+" clean");
		BufferedReader reader =
				new BufferedReader(new InputStreamReader(p.getInputStream()));
		String s;
		while ((s=reader.readLine()) != null) {System.out.println(s);}
		p.waitFor();
		System.out.println("Estatus da compilação: "+p.exitValue());*/
		
		
		
		File targetFile=git.downloadCommit(commit);
		File targetFolder=ZipExtractor.extract(targetFile, new File(git.getLocation(),commit));
		
		System.out.println(targetFolder.getAbsolutePath());
		request = new DefaultInvocationRequest();
		request.setPomFile( new File( sourceFolder,"pom.xml" ) );
		request.setGoals( Arrays.asList( "compile" ) );	
		
		invoker.execute(request);
		
		/*
		System.out.println("Copiling: "+targetFolder);
		p=Runtime.getRuntime().exec("mvn -f "+targetFolder.getAbsolutePath()+" compile");
		reader =
				new BufferedReader(new InputStreamReader(p.getInputStream()));
		while ((reader.readLine()) != null) {}
		p.waitFor();
		System.out.println("Estatus da compilação: "+p.exitValue());
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
