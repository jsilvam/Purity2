package main;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public abstract class ZipExtractor {

	public static File extract(File arquivo,File outputFolder) throws Exception{
		final int BUFFER = 2048;
    	if(!outputFolder.exists()){
    		outputFolder.mkdir();
    	}
    	System.out.println("Extrating to "+outputFolder);
		FileInputStream fis = new FileInputStream( arquivo );
		BufferedInputStream bis = new BufferedInputStream( fis, BUFFER );
		ZipInputStream zis = new ZipInputStream( bis );
		ZipEntry entrada;
		File f;
		FileOutputStream fos;
		BufferedOutputStream dest;
		zis.getNextEntry();
		while( (entrada = zis.getNextEntry()) != null ) {
		  int bytesLidos = 0;
		  byte dados[] = new byte[BUFFER];
		  //grava o arquivo em disco
		  int index=entrada.getName().indexOf("/");
		  f=new File(outputFolder.getPath()+entrada.getName().substring(index));
		  if(entrada.isDirectory()){
			  f.mkdir();
			  continue;
		  }
		  fos = new FileOutputStream(f);
		  dest = new BufferedOutputStream(fos, BUFFER);
		  while( (bytesLidos = zis.read(dados, 0, BUFFER)) != -1 ) {
			  dest.write( dados, 0, bytesLidos );
		  }
		  dest.flush();
		  dest.close();
		  fos.close();
		}
		zis.close();
		bis.close();
		fis.close();
		
		return outputFolder;
	}

}
