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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.plugin.ZProjector;


public class Arranger extends JFrame implements ActionListener{
	private static final long serialVersionUID = 4736578849569548535L;

	private static final int size = 100;
	private static final Font LABELFONT = new Font(Font.SANS_SERIF, Font.PLAIN, 16);
	private static final int MID = -1, MAX = -2;
	
	private Stitcher stitcher;
	private ArrangerCanvas canvas;
	private JSpinner overlapSpinner;
	private double overlapPercent = Prefs.get("Wagner.overlapPercent", 10d);

	
	private class Pos extends Rectangle{
		private static final long serialVersionUID = 6623369613273386733L;
		
		int field;
		Image thumb;
		String path;
		
		private Pos(int x, int y, int f, String path){
			super(x, y, size, size);
			this.field = f;
			this.path = path;
		}
		public void setThumb(Image thumb) {
			this.thumb = thumb;
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
		
		@Override
		public void paintComponent(Graphics g1d){
			try{
				Graphics2D g = (Graphics2D) g1d;
				g.setFont(LABELFONT);
				FontMetrics fm = g.getFontMetrics();
				g.setColor(Color.GRAY);
				g.fillRect(0, 0, dim.width, dim.height);
				
				g.setColor(Color.BLACK);
				for(int x=0;x<dim.width;x+=size){
					g.drawLine(x, 0, x, dim.height);
				}
				for(int y=0;y<dim.height;y+=size){
					g.drawLine(0, y, dim.width, y);
				}
				
				int half = size/2;
				for(Pos pos : list){
					if(pos==moving) continue;	//draw moving last (on top)
					g.drawImage(pos.thumb, pos.x, pos.y, this);
					g.setColor(Color.GREEN);
					g.draw(pos);
					g.setColor(Color.MAGENTA);
					String labelStr = String.valueOf(pos.field);
					g.drawString(labelStr, pos.x+half-fm.stringWidth(labelStr)/2, pos.y+half);
				}
				if(moving!=null){
					g.setColor(Color.BLUE);
					g.draw(moving);
					g.setColor(Color.MAGENTA);
					String labelStr = ""+moving.field;
					g.drawString(labelStr, moving.x+half-fm.stringWidth(labelStr)/2, moving.y+half);
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
		
		int side = (int) Math.ceil(Math.sqrt(n));	//starting grid dimensions
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
			Image thumb = getThumbnail(imagePath, MAX);
			
			Pos pos = new Pos(xp, yp, p, imagePath);
			pos.setThumb(thumb);
			canvas.addPos( pos );
		}
		canvas.calculate();
		
		add(canvas, BorderLayout.CENTER);
		JPanel controlPan = new JPanel(new FlowLayout(FlowLayout.CENTER));
		
		SpinnerModel sm = new SpinnerNumberModel(overlapPercent, 0d, 100d, 1d);
		overlapSpinner = new JSpinner(sm);
		controlPan.add( new JLabel("Overlap:") );
		controlPan.add(overlapSpinner);
		controlPan.add( new JLabel("%") );
		controlPan.add(Box.createHorizontalStrut(10));
		JButton ok = new JButton("OK");
		ok.addActionListener(this);
		controlPan.add(ok);
		add(controlPan, BorderLayout.SOUTH);
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}
	
	//thumbnail of size*size from imagePath using middle slice or max intensity projection
	private Image getThumbnail(String imagePath, int method){
		ImagePlus thumbImp = IJ.openVirtual(imagePath);
		BufferedImage buf = null;
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
		thumbImp.close();
		return buf.getScaledInstance(size, size, BufferedImage.SCALE_FAST);
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
		}
	}
	
}
