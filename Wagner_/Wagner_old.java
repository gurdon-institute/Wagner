import ij.*;
import ij.gui.*;
import ij.plugin.*;

import java.io.File;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Math;
import java.lang.NullPointerException;
import java.lang.String;
import java.util.ArrayList;

import java.util.Arrays;

import javax.swing.JFileChooser;

public class Wagner_old implements PlugIn{
private String path = Prefs.get("Wagner_.path", System.getProperty("user.home") );
private String savePath;
private ArrayList<ImageLocation> list;
private int Z, C, T, F, W, H;
private static final String foo = System.getProperty("file.separator");
	
	/*	
* 	opera exported TIFF filenames
*	r[0-9]{2}c[0-9]{2}f[0-9]{2}p[0-9]{2}-ch[0-9]sk[0-9]?[0-9]fk[0-9]fl[0-9].tiff
*	r - row
*	c - column
*	f - field
*	p - z-slice
*	ch - channel
*	sk - t-frame
*	fk - ???
*	fl - ???
*/

private class ImageLocation{
	public int row, column, field, channel, slice, frame;
	public String filename, filePath;
	
	public ImageLocation(String filePath, int row, int column, int field, int channel, int slice, int frame){
		this.filePath = filePath;
		this.row = row;
		this.column = column;
		this.field = field;
		this.channel = channel;
		this.slice = slice;
		this.frame = frame;
	}
	
	public ImageLocation(String path){
		this.filePath = path;
		this.filename = path.substring(path.lastIndexOf(System.getProperty("file.separator"))+1);
		int rowi = filename.indexOf("r")+1;
		int columni = filename.indexOf("c")+1;
		int fieldi = filename.indexOf("f")+1;
		int channeli = filename.indexOf("ch")+2;
		int slicei = filename.indexOf("p")+1;
		int framei = filename.indexOf("sk")+2;
		int frameiEnd = filename.indexOf("fk");
		this.row = Integer.valueOf(filename.substring(rowi, rowi+2));
		this.column = Integer.valueOf(filename.substring(columni, columni+2));
		this.field = Integer.valueOf(filename.substring(fieldi, fieldi+2));
		this.channel = (channeli==1) ? 0 : Integer.valueOf(filename.substring(channeli, channeli+1));
		this.slice = (slicei==0) ? 0 : Integer.valueOf(filename.substring(slicei, slicei+2));
		this.frame = (framei==1) ? 0 : Integer.valueOf(filename.substring(framei, frameiEnd));
	}
	
	public ImageLocation(File file){
		this(file.getAbsolutePath());
	}
	
	public boolean sameRCF(ImageLocation other){
		if(this.row==other.row&&this.column==other.column&&this.field==other.field){return true;}
		else{return false;}
	}
	
	public String toString(){
		return filePath+" row "+row+", column "+column+", field "+field+", channel "+channel+", slice "+slice+", frame "+frame;
	}
	
	public String getName(){
		return "row"+row+"_column"+column+"_field"+field;
	}
	
}

private void getDimensions(){	//get number of slices, frames, and channels for current list
	Z = 0;
	T = 0;
	C = 0;
	for(int i=0;i<list.size();i++){
		Z = Math.max(Z, list.get(i).slice);
		T = Math.max(T, list.get(i).frame);
		C = Math.max(C, list.get(i).channel);
	}
}

private void construct(ImageLocation loc){
try{
	int n = 0;
	IJ.log("Constructing "+loc.getName()+"...");
	ImageStack stack = new ImageStack();
	boolean stacked = false;
	long startTime = System.currentTimeMillis();
	
	String r = loc.row>9 ? String.valueOf(loc.row) : "0"+String.valueOf(loc.row);
	String c = loc.column>9 ? String.valueOf(loc.column) : "0"+String.valueOf(loc.column);
	String f = loc.field>9 ? String.valueOf(loc.field) : "0"+String.valueOf(loc.field);
	String fk = "1";	//mystery parts of filenames
	String fl = "1";
	
	//czt	
	for(int ci=1;ci<=C;ci++){
		for(int zi=1;zi<=Z;zi++){
			for(int ti=1;ti<=T;ti++){
				n++;
				IJ.showStatus("Constructing "+loc.getName()+" ("+n+"/"+(Z*T*C)+")");
				String p = zi>9 ? String.valueOf(zi) : "0"+String.valueOf(zi);
				String ch = String.valueOf(ci);
				String sk = String.valueOf(ti);
				String name = "r"+r+"c"+c+"f"+f+"p"+p+"-ch"+ch+"sk"+sk+"fk"+fk+"fl"+fl+".tiff";
				ImagePlus image = IJ.openImage(path+foo+name);
				if(image==null){
					if(stacked){
						IJ.log("Could not open image - "+name);
						image = new ImagePlus("empty", stack.getProcessor(stack.getSize()) );
						continue;
					}
					else{
						throw new Exception("Could not construct "+path+foo+name+" - missing first file.");
					}
				}	
				if(!stacked){
					W = image.getWidth();
					H = image.getHeight();
					stack = new ImageStack(W, H);
					stacked = true;
				}
				stack.addSlice( image.getProcessor() );
				image.close();
			}
		}
	}

	ImagePlus imp = new ImagePlus(loc.getName(), stack);
	imp = toHyperStack(imp, C, Z, T);
	File saveDir = new File(savePath);
	if(!saveDir.exists()){
		saveDir.mkdir();
	}
	IJ.saveAs(imp, "tiff", savePath+loc.getName()+".tif");
	imp.close();
	IJ.log("...done");
	
}catch(Exception e){IJ.log(e.toString()+"\n~~~~~\n"+Arrays.toString(e.getStackTrace()).replace(",","\n"));}
}


    private ImagePlus toHyperStack(ImagePlus imp, int C, int Z, int T) {
        int n = imp.getStackSize();
        if (n==1 || imp.getBitDepth()==24){
            throw new IllegalArgumentException("Non-RGB stack required");
        }
        if (C*Z*T!=n){
            throw new IllegalArgumentException("C*Z*T not equal stack size");
        }
        imp.setDimensions(C, Z, T);
      
        (new HyperStackConverter()).shuffle(imp, 5);	//0 is TZC (private field for some reason)
        ImagePlus imp2 = imp;
        if (C>1) {
            imp2 = new CompositeImage(imp, CompositeImage.GRAYSCALE);
        }
        imp2.setOpenAsHyperStack(true);
        return imp2;
    }


	
	public void run(String _){
	try{
		JFileChooser chooser = new JFileChooser(path);
		chooser.setDialogTitle("Exported Opera TIFF directory...");
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int choice = chooser.showDialog(null, "OK");
		if(choice != JFileChooser.APPROVE_OPTION){return;}
		path = chooser.getSelectedFile().getAbsolutePath();
		savePath = path+foo+"stacks"+foo;
		Prefs.set("Wagner_.path", path);
		
		
		File[] files = new File(path).listFiles();
		list = new ArrayList<ImageLocation>();
		for(File f : files){
			if(f.getName().matches( "r[0-9]{2}c[0-9]{2}f[0-9]{2}p[0-9]{2}-ch[0-9]sk[0-9]?[0-9]fk[0-9]fl[0-9].tiff" )){
				list.add(new ImageLocation(f));
			}
		}
		if(list.size()==0){
			IJ.error("No exported Opera TIFFs found in "+path);
		}
		
		File[] exist = new File(path+foo+"stacks"+foo).listFiles();
		ArrayList<ImageLocation> doneLocations = new ArrayList<ImageLocation>();
		if(exist!=null){
			for(int i=0;i<exist.length;i++){
				if(exist[i].getName().matches( "^r[0-9]{2}c[0-9]{2}f[0-9]{2}\\.tif" )){
						doneLocations.add( new ImageLocation(exist[i]) );
				}
			}
		}
		getDimensions();
		IJ.log("Constructing stacks from "+path);
		for(int f=0;f<files.length;f++){
			if(files[f].getName().matches( ".*?\\.tiff" )){
				boolean done = false;
				
				ImageLocation imgLoc = new ImageLocation(path+foo+files[f]);
				for(int i=0;i<doneLocations.size();i++){
					if(imgLoc.sameRCF(doneLocations.get(i))){
						done = true;
						break;
					}
				}
				
				if(!done){
					construct(imgLoc);
					doneLocations.add(imgLoc);
				}
			}
		}
		IJ.log(path+" finished");
		
	}catch(Exception e){IJ.log(e.toString()+"\n~~~~~\n"+Arrays.toString(e.getStackTrace()).replace(",","\n"));}
	}
	
}