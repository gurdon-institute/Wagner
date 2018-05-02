package cam.gurdon.wagner;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;

class PlateLocation implements Runnable{
	public int row, column, field;
	private String path, locationString;
	private List<OperaTiff> images;
	private long memory;
	private boolean overwrite;
	private static final String foo = System.getProperty("file.separator");
	
	
	public PlateLocation(String path, String loc, boolean overwrite){
		this.path = path;
		this.row = OperaParser.getValue(OperaParser.Dimension.ROW, loc);
		this.column = OperaParser.getValue(OperaParser.Dimension.COLUMN, loc);
		this.field = OperaParser.getValue(OperaParser.Dimension.FIELD, loc);
		this.images = new ArrayList<OperaTiff>();
		this.memory = 0L;
		this.overwrite = overwrite;
	}
	
	//constructor for use in stack stitching organisation
	public PlateLocation(int row, int column, int field) {
		this.row = row;
		this.column = column;
		this.field = field;
	}
	
	public void addImage(String filename){
		if(images.indexOf(filename)==-1){	//if the image isn't already in the list
			images.add( new OperaTiff(filename) );
			File tempFile = new File( path+foo+filename );
			memory += tempFile.length();
			tempFile = null;
		}
	}

	public boolean matches(PlateLocation other){
		if(other.row==row&&other.column==column&&other.field==field){
			return true;
		}
		else{
			return false;
		}
	}
	public boolean matches(String loc){
		int locRow = OperaParser.getValue(OperaParser.Dimension.ROW, loc);
		int locColumn = OperaParser.getValue(OperaParser.Dimension.COLUMN, loc);
		int locField = OperaParser.getValue(OperaParser.Dimension.FIELD, loc);
		if(locRow==row&&locColumn==column&&locField==field){
			return true;
		}
		else{
			return false;
		}
	}
	
	public long getSize(){	//returns the sum of lengths in bytes of all files for this location
		return memory;
	}
	
	public String toString(){
		if(locationString==null){
			locationString = "row-"+row+" column-"+column+" field-"+field;
		}
		return locationString;
	}
	
	public void flush(){
		path = null;
		images = null;
	}
	
	@Override
	public void run() {
		try{
		File saveDir = new File(path+foo+"stacks");
		if(!saveDir.isDirectory()){
			saveDir.mkdir();
		}
		
		String savePath = path+foo+"stacks"+foo+toString()+".tiff";
		if(!overwrite&&new File(savePath).exists()){
			System.out.println(savePath+" already exists");
			flush();
			return;
		}
		
		if(images==null||images.size()==0){
			System.out.println( "No images for "+toString() );
			return;
		}
		System.out.println( "building location "+toString() );
		
		Collections.sort( images, new Comparator<OperaTiff>(){	//put in CZT (ImageJ default) order
			@Override
			public int compare(OperaTiff a, OperaTiff b) {
				if(a.frame!=b.frame){return a.frame-b.frame;}
				if(a.slice!=b.slice){return a.slice-b.slice;}
				if(a.channel!=b.channel){return a.channel-b.channel;}
				return 0;
			}
		} );
		
		Set<Integer> Cset = new HashSet<Integer>();	//get unique dimension indices in Sets
		Set<Integer> Zset = new HashSet<Integer>();
		Set<Integer> Tset = new HashSet<Integer>();
		ImageStack stack = null;
		for(OperaTiff image : images){
			Cset.add(image.channel);
			Zset.add(image.slice);
			Tset.add(image.frame);
			ImagePlus imp = null;
			imp = IJ.openImage( path+foo+image.name );
			if(stack==null){
				stack = new ImageStack(imp.getWidth(), imp.getHeight());
			}
			stack.addSlice( "c"+image.channel+" z"+image.slice+" t"+image.frame, imp.getProcessor() );
			imp.flush();
			imp.close();
		}
		if(stack.getSize()==0){
			System.out.println("No stack for "+toString());
			return;
		}
		System.out.println("Got data for "+toString());
		
		ImagePlus imageStack = new ImagePlus( toString(), stack );
		
		int n = imageStack.getStackSize();
		int C = Cset.size();
		int Z = Zset.size();
		int T = Tset.size();
		
        if ( C*Z*T < n ){
            System.out.println("C*Z*T is less than the stack size for "+toString()+" - extra images may have been added");
            return;
        }
        else if ( C*Z*T > n ){
        	System.out.println("C*Z*T is greater than the stack size for "+toString()+" - images may be missing");
        	return;
        }
        imageStack.setDimensions(C, Z, T);
        if (C>1) {
            imageStack = new CompositeImage(imageStack, CompositeImage.GRAYSCALE);
        }
        imageStack.setOpenAsHyperStack(true);
		
		IJ.saveAs(imageStack, "TIFF", path+foo+"stacks"+foo+toString()+".tiff");
		imageStack.close();
		
		System.out.println(toString()+" done");
		}catch(Exception e){ System.out.print( e.toString()+"\n"+Arrays.toString(e.getStackTrace()).replace(",","\n")+"\n");}
		finally{
			flush();
		}
	}
	
}