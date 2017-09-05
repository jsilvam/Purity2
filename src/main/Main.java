 package main;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Scanner;

public class Main {
	
	
	
	private static void check(String repositoryUrl) throws Exception {
		
		//String dir  = "/home/jaziel/Dropbox/UFCG/Projeto/Dados/CSVs/Refatoramentos"; //Linux
		String dir  = "C:\\Users\\Jaziel Moreira\\Dropbox\\UFCG\\Projeto\\Dados\\CSVs\\Refatoramentos"; //Windows
		String aux=repositoryUrl.substring(repositoryUrl.lastIndexOf("/")+1);
		
		Purity p=new Purity(repositoryUrl);
		Scanner in = new Scanner(new FileReader(dir+"/Part 1/"+aux+".csv")).useDelimiter(";");
		
		String commit="";
		int cont=0;
		
		in.nextLine();
		while(in.hasNext()) {
			in.next();
			aux=in.next();
			if(!aux.equals(commit)) {
				commit=aux;
				String parent=in.next();
				p.check(commit, parent);
				
			}
			in.nextLine();
		}
		in.close();
	}

	public static void main(String[] args) throws Exception {

		
		check("https://github.com/jopt-simple/jopt-simple");

	}
}
