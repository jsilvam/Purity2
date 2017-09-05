package main;



import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;


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
		
		git.setLocation("/tmp/Projeto/Downloads/"+commit);
		
		
		File targetFile=git.downloadCommit(commit);
		File targetFolder=ZipExtractor.extract(targetFile, new File(git.getLocation(),commit));
		
		
		File sourceFile=git.downloadCommit(parent);
		File sourceFolder=ZipExtractor.extract(sourceFile, new File(git.getLocation(),parent));
		
		
		
		System.out.println("Copiling: "+sourceFolder);
		Process p=Runtime.getRuntime().exec("cmd /C mvn -f "+sourceFolder.getAbsolutePath()+" compile");
		BufferedReader reader =
				new BufferedReader(new InputStreamReader(p.getInputStream()));
		while ((reader.readLine()) != null) {}
		p.waitFor();
		System.out.println("Estatus da compilação: "+p.exitValue());
		
		System.out.println("Copiling: "+targetFolder);
		p=Runtime.getRuntime().exec("cmd /C mvn -f "+targetFolder.getAbsolutePath()+" compile");
		reader =
				new BufferedReader(new InputStreamReader(p.getInputStream()));
		while ((reader.readLine()) != null) {}
		p.waitFor();
		System.out.println("Estatus da compilação: "+p.exitValue());
		
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
