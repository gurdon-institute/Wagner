package cam.gurdon.wagner;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.plugin.RGBStackConverter;
import ij.plugin.ZProjector;
import ij.process.ColorProcessor;


public class Arranger extends JFrame implements ActionListener{
	private static final long serialVersionUID = 4736578849569548535L;

	private static final int size = 100;
	private static final Font LABELFONT = new Font(Font.SANS_SERIF, Font.PLAIN, 16);
	private static final int MID = -1, MAX = -2;
	private static final String[] WRAP_ORDERS = {"ordered", "1-centre snake", "1-centre wrap", "opera special"};
	
	private Stitcher stitcher;
	private ArrangerCanvas canvas;
	private JSpinner overlapSpinner;
	private double overlapPercent = Prefs.get("Wagner.overlapPercent", 10d);
	private JComboBox<String> wrap;
	
	private class Pos extends Rectangle{
		private static final long serialVersionUID = 6623369613273386733L;
		
		int field;
		Image thumb;
		String path;
		
		private Pos(int x, int y, int f, String path, Image thumb){
			super(x, y, size, size);
			this.field = f;
			this.path = path;
			this.thumb = thumb;
		}
		public void setThumb(Image thumb) {
			this.thumb = thumb;
		}
		public String toString(){
			return "Pos: x="+x+" y="+y+" width="+width+" height="+height+" field="+field;
		}
	}
	
	private class ArrangerCanvas extends JPanel implements MouseListener, MouseMotionListener{
		private static final long serialVersionUID = 1230765523317260099L;
		
		private Dimension dim = new Dimension(0, 0);
		private ArrayList<Pos> list;
		private Pos moving;
		
		private ArrangerCanvas(){
			super();
			list = new ArrayList<Pos>();
			addMouseListener(this);
			addMouseMotionListener(this);
		}
		
		private void drawPos(Graphics2D g, Pos pos){
			
			int half = size/2;
			FontMetrics fm = g.getFontMetrics();
			
			g.drawImage(pos.thumb, pos.x, pos.y, this);
			g.setColor( pos==moving?Color.MAGENTA:Color.YELLOW );
			g.draw(pos);
			
			String labelStr = String.valueOf(pos.field);
			
			g.setColor(Color.WHITE);
			int x = pos.x+half-fm.stringWidth(labelStr)/2;
			int y = pos.y+half;
		    g.drawString(labelStr, x-1, y-1);
		    g.drawString(labelStr, x-1, y+1);
		    g.drawString(labelStr, x+1, y-1);
		    g.drawString(labelStr, x+1, y+1);
		    g.setColor(Color.BLACK);
		    g.drawString(labelStr, x, y);
		}
		
		@Override
		public void paintComponent(Graphics g1d){
			try{
				Graphics2D g = (Graphics2D) g1d;
				g.setFont(LABELFONT);
				
				g.setColor(Color.GRAY);
				g.fillRect(0, 0, dim.width, dim.height);
				
				g.setColor(Color.BLACK);
				for(int x=0;x<dim.width;x+=size){
					g.drawLine(x, 0, x, dim.height);
				}
				for(int y=0;y<dim.height;y+=size){
					g.drawLine(0, y, dim.width, y);
				}
				
				
				for(Pos pos : list){
					if(pos==moving) continue;	//skip moving Pos and draw it last (on top)
					drawPos(g, pos);
				}
				if(moving!=null){
					drawPos(g, moving);
				}
				
			}catch(Exception e){ System.out.print( e.toString()+"\n"+Arrays.toString(e.getStackTrace()).replace(",","\n")+"\n");}
		}
		
		private void calculate(){ //recursive calculation of positions
			int width = size;
			int height = size;
			for(Pos pos:list){
				int x = pos.x;
				int y = pos.y;
				int xx = Math.round(x/size)*size;
				int yy = Math.round(y/size)*size;
				for(int i=0;i<list.size();i++){
					Pos other = list.get(i);
					if(pos==other) continue;
					if(xx==other.x&&yy==other.y){
						other.x += size;
						calculate();
						break;
					}
				}
				pos.x = xx;
				pos.y = yy;
				width = Math.max(width, xx+size);
				height = Math.max(height, yy+size);
			}
			if(width!=dim.width||height!=dim.height){
				dim.width = width;
				dim.height = height;
				setSize(dim);
				calculate();
			}
			pack();
		}
		
		@Override
		public Dimension getPreferredSize(){
			return dim;
		}

		public void setDim(int w, int h){
			dim.width = w;
			dim.height = h;
		}
		
		public void addPos(Pos pos){
			list.add(pos);
		}

		private Pos getTarget(Point p){
			for(Pos pos:list){
				if(pos.contains(p)) return pos;
			}
			return null;
		}
		
		@Override
		public void mousePressed(MouseEvent me){
			moving = getTarget(me.getPoint());
		}
		
		@Override
		public void mouseReleased(MouseEvent me){
			try{
				Point p = me.getPoint();
				if(moving!=null){
					moving.setLocation(p.x, p.y);
					moving = null;
				}
				calculate();
				pack();
				repaint();
			}catch(Exception e){ System.out.print( e.toString()+"\n"+Arrays.toString(e.getStackTrace()).replace(",","\n"));}
		}
		
		@Override
		public void mouseDragged(MouseEvent me){
			if(moving!=null){
				int half = size/2;
				moving.setLocation(me.getX()-half, me.getY()-half);
				if(moving.x+size>dim.width){
					dim.width = moving.x+size;
				}
				else if(moving.x<0){
					moving.x = 0;
				}
				if(moving.y+size>dim.height){
					dim.height = moving.y+size;
				}
				else if(moving.y<0){
					moving.y = 0;
				}
				setSize(dim);
				pack();
				repaint();
			}
		}

		@Override 
		public void mouseClicked(MouseEvent me) {
			if(me.getButton()==MouseEvent.BUTTON1&&me.getClickCount()==2){
				Pos pos = getTarget(me.getPoint());
				ImagePlus posImp = IJ.openImage(pos.path);
				posImp.show();
			}
		}
		
		@Override public void mouseMoved(MouseEvent e) {}
		@Override public void mouseEntered(MouseEvent e) {}
		@Override public void mouseExited(MouseEvent e) {}
		
	}
	
	public Arranger(int n, Stitcher stitcher){
		super("Wagner Stitcher");
		this.stitcher = stitcher;
		setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("logo_icon.gif")));
		setLayout(new BorderLayout());
		canvas = new ArrangerCanvas();
		
		int side = (int) Math.ceil(Math.sqrt(n));	//start with a square grid
		for(int p=1;p<=n;p++){
			int xi = p-1;
			int yi = 0;
			while(xi>=side){
				xi -= side;
				yi++;
			}
			int xp = size*xi;
			int yp = size*yi;
			
			String imagePath = stitcher.stackPath+""+stitcher.wells.get(0).getTitle(p);
			System.out.println("Creating thumbnails - "+p+"/"+n);
			Image thumb = getThumbnail(imagePath, MAX);
			
			
			Pos pos = new Pos(xp, yp, p, imagePath, thumb);
			canvas.addPos( pos );
		}
		canvas.calculate();
		
		add(canvas, BorderLayout.CENTER);
		JPanel controlPan = new JPanel(new FlowLayout(FlowLayout.CENTER));
		
		/*JPanel something = new JPanel();
		something.add(new JLabel("?"));
		add(something, BorderLayout.WEST);*/
		
		SpinnerModel sm = new SpinnerNumberModel(overlapPercent, 0d, 100d, 1d);
		overlapSpinner = new JSpinner(sm);
		controlPan.add( new JLabel("Overlap:") );
		controlPan.add(overlapSpinner);
		controlPan.add( new JLabel("%") );
		controlPan.add(Box.createHorizontalStrut(10));
		
		JButton ok = new JButton("OK");
		ok.addActionListener(this);
		controlPan.add(ok);
		controlPan.add(Box.createHorizontalStrut(10));
		
		controlPan.add(new JLabel("Fit tiles:"));
		wrap = new JComboBox<String>(WRAP_ORDERS);
		wrap.setActionCommand("Wrap");
		wrap.addActionListener(this);
		controlPan.add(wrap);
		
		add(controlPan, BorderLayout.SOUTH);
		pack();
		setLocationRelativeTo(null);
		
		addComponentListener(new ComponentAdapter(){
			public void componentResized(ComponentEvent we){
				Rectangle paneRect = canvas.getBounds();	//set canvas preferred size to current bounds
				canvas.setDim(paneRect.width, paneRect.height);
			}
		});
		
		setVisible(true);
	}
	
	private Image getThumbnail(String imagePath, int method){
		ImagePlus thumbImp = IJ.openVirtual(imagePath);
		//ImagePlus thumbImp = IJ.openImage(imagePath);
		BufferedImage buf = null;
		if(thumbImp==null){
			System.out.println(imagePath+" not found");
			return null;
		}
		if(thumbImp.getNChannels()>1){
			thumbImp.setDisplayMode(IJ.COMPOSITE);
		}
		if(thumbImp.getNSlices()>1){
			if(method==MID){
				thumbImp.setPosition(1, thumbImp.getNSlices()/2, 1);
				buf = thumbImp.getProcessor().getBufferedImage();
			}
			else if(method==MAX){
				ZProjector zp = new ZProjector(thumbImp);
				zp.setMethod(ZProjector.MAX_METHOD);
				zp.doProjection();
				buf = zp.getProjection().getBufferedImage();
			}
		}
		else{
			buf = thumbImp.getBufferedImage();
		}
		
		float f = thumbImp.getHeight()/(float)thumbImp.getWidth();
		int w = size;
		int h = size;
		if(f>1) w *= f;
		else if(f<1) h *= f;
		//don't scale if f==1
		
		//thumbImp.show();
		thumbImp.close();
		
		ColorProcessor cp = new ColorProcessor(buf);
		return cp.makeThumbnail(w, h, 0d).getBufferedImage();
	}
	
	private void doWrap(String type){
		int w = canvas.getWidth()/size;
		int h = canvas.getHeight()/size;
		int f1x = (int) Math.floor(w/2);
		int f1y = (int) Math.floor(h/2);
		int xi = -1;
		int yi = 0;
		int d = 1;
		
		boolean centre1 = type.startsWith("1-centre")||type.equals("opera special");
		
		for(Pos pos:canvas.list){
			if(centre1&&pos.field==1){	//put field 1 in the centre
				pos.x = f1x * size;
				pos.y = f1y * size;
			}
			else{
				xi += d;
				if((type.equals("ordered")||type.equals("1-centre wrap"))&&xi>w-1){	//when side is reached, start next row
					xi = 0;
					yi++;
				}
				else if(type.equals("1-centre snake")&&(xi<0||xi>w-1)){	//when a side is reached, change direction and increment row
					d = -d;
					xi += d;
					yi++;
				}
				else if(type.equals("opera special")&&(xi<0||xi>w-1)){ //pirkon elmar use deliberately obstructive ordering
					if(yi==f1y-1){
						xi = w-1;
					}
					else{
						d = -d;
						xi += d;
					}
					yi++;
				}
				if(centre1&&xi==f1x&&yi==f1y){	//additional increment if the indices are the special field 1 position
					xi += d;
				}
				pos.x = xi * size;
				pos.y = yi * size;
			}
		}
		canvas.calculate();
		repaint();
	}
	
	public void actionPerformed(ActionEvent ae){
		String event = ae.getActionCommand();
		if(event.equals("OK")){		
			int w = canvas.getWidth()/size;
			int h = canvas.getHeight()/size;
			int[][] arrange = new int[h][w];
			for(Pos pos:canvas.list){
				int xi = (int) (pos.x/(float)size);
				int yi = (int) (pos.y/(float)size);
				arrange[yi][xi] = pos.field;
			}
			overlapPercent = (double) overlapSpinner.getValue();
			Prefs.set("Wagner.overlapPercent", overlapPercent);
			stitcher.stitch(arrange, overlapPercent);

			dispose();
			return;
		}
		else if(event.equals("Wrap")){
			doWrap( (String) wrap.getSelectedItem() );
		}
	}
	
}
