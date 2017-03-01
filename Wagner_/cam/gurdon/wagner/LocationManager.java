package cam.gurdon.wagner;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LocationManager {
private ExecutorService executor;
private List<PlateLocation> locations;
private String path;
private int nThreads;
	
	public LocationManager(String path, int n) {
		this.path = path;
		this.nThreads = n;
		locations = new ArrayList<PlateLocation>();
	}
	
	public void add(String name){
		PlateLocation loc = getLocation( name );
		if( loc == null ){
			loc = new PlateLocation(path, name);
			loc.addImage( name );
			locations.add( loc );
		}
		else{
			loc.addImage( name );
		}
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
		
		if(nThreads>locations.size()){	//don't allocate more threads than locations
			nThreads = locations.size();
		}
		long available = Runtime.getRuntime().maxMemory();
		long total = 0L;
		for(PlateLocation pl : locations){
			total += pl.getSize();
		}
		long req = (total/locations.size()) * nThreads;
		if( available<req ){
			System.out.print( "Insufficient memory for "+locations.size()+" locations running on "+nThreads+" threads\n"+
							   (available/1024/1024)+" Mb available, "+(req/1024/1024)+" Mb required for "+locations.size()+" locations\n"+
							  " - set higher with the java -Xmx*m argument or use fewer threads\n" );
			return;
		}
		
		executor = Executors.newFixedThreadPool( nThreads );
		for(PlateLocation pl : locations){
			executor.execute( pl );
		}
		executor.shutdown();
		try {
			executor.awaitTermination(7L, TimeUnit.DAYS);
			System.out.println( "Wagner finished "+locations.size()+" locations in "+path );
		} catch (InterruptedException ie) {
			System.out.println(ie.toString());
		}
	}
	
}
