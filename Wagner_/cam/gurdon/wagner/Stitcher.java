package cam.gurdon.wagner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.HyperStackConverter;
import ij.process.Blitter;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

public class Stitcher {
	private static final String foo = System.getProperty("file.separator");
	ArrayList<Well> wells;
	int[][] arrange;
	String stackPath;
	
	class Well{
		private int row, col, maxf;
		private ArrayList<Integer> fields;
		private Well(int r, int c){
			row = r;
			col = c;
			fields = new ArrayList<Integer>();
		}
		private void addField(int field){
			if(fields.contains(field)){
				System.out.println("Well row "+row+" column "+col+" already contains field "+field);
			}
			else{
				fields.add(field);
				maxf = Math.max(maxf, field);
			}
		}
		String getTitle(int field){
			return "row-"+row+" column-"+col+" field-"+field+".tiff";
		}
		private int getNFields(){
			return fields.size();
		}
	}

	public Stitcher(List<PlateLocation> locations, String path){
		try{
			stackPath = path+foo+"stacks"+foo;
			ArrayList<int[]> wellrcs = new ArrayList<int[]>();
			wells = new ArrayList<Well>();
			for(PlateLocation loc:locations){
				int[] rc = new int[]{loc.row,loc.column};
				boolean got = false;
				for(int[] pair:wellrcs){
					if(pair[0]==rc[0]&&pair[1]==rc[1]){
						got = true;
						break;
					}
				}
				if(!got){
					wellrcs.add(rc);
					wells.add(new Well(rc[0], rc[1]));
				}
			}
			for(Well well:wells){
				for(PlateLocation loc:locations){
					if(loc.row==well.row&&loc.column==well.col){
						well.addField(loc.field);
					}
				}
			}
			Arranger arranger = new Arranger(wells.get(0).maxf, this);
			
		}catch(Exception e){ System.out.print( e.toString()+"\n"+Arrays.toString(e.getStackTrace()).replace(",","\n"));}
	}
	
	//default constuctor for testing
	public Stitcher() {
		
	}

	public void stitch(int[][] arrange, double overlapPercent) {
		
		double olF = (100d-overlapPercent)/100d;
		
		for(int w=0;w<wells.size();w++){
			Well well = wells.get(w);
			if(well.getNFields()<2) continue;
			ImagePlus[][] tiles = new ImagePlus[arrange.length][arrange[0].length];
			int totalW = 0;
			int totalH = 0;
			int W = 0;
			int H = 0;
			int C = 0;
			int Z = 0;
			int T = 0;
			for (int y = 0; y < arrange.length; y++) {
				for (int x = 0; x < arrange[y].length; x++) {
					int f = arrange[y][x];
					if(f==0) continue;
					tiles[y][x] = IJ.openImage( stackPath+well.getTitle(f) );
					totalW = (int) Math.max(totalW, tiles[y][x].getWidth()*(x+1)*olF);
					totalH = (int) Math.max(totalH, tiles[y][x].getHeight()*(y+1)*olF);
					W = Math.max(W, tiles[y][x].getWidth());
					H = Math.max(H, tiles[y][x].getHeight());
					C = Math.max(C, tiles[y][x].getNChannels());
					Z = Math.max(Z, tiles[y][x].getNSlices());
					T = Math.max(T, tiles[y][x].getNFrames());
				}
			}
			
			//sort out overlap lengths for 1 or >1 tiles
			if(arrange.length>1) totalH += (totalH-(totalH*olF))/2d;
			else totalH += (totalH-(totalH*olF));
			if(arrange[0].length>1) totalW += (totalW-(totalW*olF))/2d;
			else totalW += (totalW-(totalW*olF));
			
			ImageStack stack = new ImageStack((int)(totalW), (int)(totalH));
			
			for(int z=0;z<Z;z++){
				ImageProcessor ip = new ShortProcessor((int)(totalW), (int)(totalH));
				for (int y = 0; y < tiles.length; y++) {
					for (int x = 0; x < tiles[y].length; x++) {
						ImageProcessor tp = null;
						if(tiles[y][x]==null) continue;
						if(C>1||Z>1||T>1){
							tp = tiles[y][x].getImageStack().getProcessor(z+1);
						}
						else{
							tp = tiles[y][x].getProcessor();
						}
						int xp = (int)( x*tiles[y][x].getWidth()*olF );
						int yp = (int)( y*tiles[y][x].getHeight()*olF );
						//ip.copyBits(tp, xp, yp, Blitter.AVERAGE);
						ip.copyBits(tp, xp, yp, Blitter.MAX);
					}
				}
				stack.addSlice("slice "+(z+1), ip);
			}
			
			Z /= C;
			Z /= T;
			ImagePlus mosaic = new ImagePlus("mosaic", stack);
			if(C>1||Z>1||T>1){
				mosaic = HyperStackConverter.toHyperStack(mosaic, C, Z, T, "xyczt", "composite");
			}
			IJ.saveAs(mosaic, "TIFF", stackPath+"mosaic");
			System.out.println("Saved stitched mosaic in "+stackPath);
mosaic.show();	//TEST
			//mosaic.close();
		}
	}
	
}
