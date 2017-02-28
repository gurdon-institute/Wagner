import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;

public class Charles {
private ExecutorService executor;
private List<PlateLocation> locations;
private String path;
	
	public Charles(String path, int nThreads) {
		this.path = path;
		executor = Executors.newFixedThreadPool( nThreads );
		locations = new ArrayList<PlateLocation>();
	}
	public Charles(){
		
	}
	
	public void add(String file){
		PlateLocation loc = getLocation( file );
		if( loc == null ){
			loc = new PlateLocation(path, file);
			loc.addFile( file );
			locations.add( loc );
		}
		else{
			loc.addFile( file );
		}
		//System.out.println( file.getName()+" -> "+loc.toString() );
	}
	
	public PlateLocation getLocation( String str ){
		for(PlateLocation loc : locations){
			if(loc.matches(str)){
				return loc;
			}
		}
		return null;
	}

	public void execute(){
		for(PlateLocation pl : locations){
			executor.execute( pl );
			//System.out.println( "execute : "+pl.toString() );
		}
		executor.shutdown();
		try {
			executor.awaitTermination(7L, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			JOptionPane.showMessageDialog(null, "Execution Interrupted", "Error", JOptionPane.ERROR_MESSAGE);
		}
		finally{
			System.out.println( "Wagner finished "+locations.size()+" locations in "+path );
		}
	}
	
}
