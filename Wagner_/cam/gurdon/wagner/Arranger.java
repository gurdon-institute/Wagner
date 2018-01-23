package cam.gurdon.wagner;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
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
import ij.process.ImageProcessor;

public class Arranger extends JFrame implements ActionListener{
	private static final long serialVersionUID = 4736578849569548535L;

	private static final int SIZE = 50;
	private static final int HALF = 25;
	
	private Stitcher stitcher;
	private ArrangerCanvas canvas;
	private JSpinner overlapSpinner;
	private double overlapPercent = Prefs.get("Wagner.overlapPercent", 10d);
	
	private class Pos extends Rectangle{
		private static final long serialVersionUID = 6623369613273386733L;
		
		int field;
		Image thumb;
		
		private Pos(int x, int y, int f){
			super(x, y, SIZE, SIZE);
			this.field = f;
		}
		private Pos(int f){
			super(0, 0, SIZE, SIZE);
			this.field = f;
		}
		public void setThumb(Image thumb) {
			this.thumb = thumb;
		}
	}
	
	private class ArrangerCanvas extends JPanel implements MouseListener, MouseMotionListener{
		private static final long serialVersionUID = 1230765523317260099L;
		
		private Dimension dim = new Dimension(500, 500);
		private ArrayList<Pos> list;
		private Pos moving;
		
		private ArrangerCanvas(){
			super();
			list = new ArrayList<Pos>();
			addMouseListener(this);
			addMouseMotionListener(this);
		}
		
		public void paintComponent(Graphics g1d){
			try{
				Graphics2D g = (Graphics2D) g1d;
				g.setColor(Color.GRAY);
				g.fillRect(0, 0, dim.width, dim.height);
				
				g.setColor(Color.BLACK);
				for(int x=0;x<dim.width;x+=SIZE){
					g.drawLine(x, 0, x, dim.height);
				}
				for(int y=0;y<dim.height;y+=SIZE){
					g.drawLine(0, y, dim.width, y);
				}
				
				for(Pos pos : list){
					if(pos==moving) continue;	//draw moving last (on top)
					g.drawImage(pos.thumb, pos.x, pos.y, this);
					g.setColor(Color.GREEN);
					g.draw(pos);
					g.setColor(Color.BLACK);
					g.drawString(""+pos.field, pos.x+HALF, pos.y+HALF); //TODO: FontMetrics
				}
				if(moving!=null){
					g.setColor(Color.BLUE);
					g.draw(moving);
					g.setColor(Color.BLACK);
					g.drawString(""+moving.field, moving.x+HALF, moving.y+HALF); //TODO: FontMetrics
				}
			}catch(Exception e){ System.out.print( e.toString()+"\n"+Arrays.toString(e.getStackTrace()).replace(",","\n")+"\n");}
		}
		
		private void calculate(){ //recursive calculation of positions
			int width = SIZE;
			int height = SIZE;
			for(Pos pos:list){
				int x = pos.x;
				int y = pos.y;
				int xx = Math.round(x/SIZE)*SIZE;
				int yy = Math.round(y/SIZE)*SIZE;
				for(int i=0;i<list.size();i++){
					Pos other = list.get(i);
					if(pos==other) continue;
					if(xx==other.x&&yy==other.y){
						other.x += SIZE;
						calculate();
						break;
					}
				}
				pos.x = xx;
				pos.y = yy;
				width = Math.max(width, xx+SIZE);
				height = Math.max(height, yy+SIZE);
			}
			if(width!=dim.width||height!=dim.height){
				dim.width = width;
				dim.height = height;
				setSize(dim);
				calculate();
			}
			pack();
		}
		
		public Dimension getPreferredSize(){
			return dim;
		}

		public void addPos(Pos pos){
			list.add(pos);
		}
		public void addPos(int f){
			Pos pos = new Pos(f);
			list.add(pos);
		}

		private Pos getTarget(Point p){
			for(Pos pos:list){
				if(pos.contains(p)) return pos;
			}
			return null;
		}
		public void mousePressed(MouseEvent me){
			moving = getTarget(me.getPoint());
		}
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
		public void mouseDragged(MouseEvent me){
			if(moving!=null){
				moving.setLocation(me.getX()-HALF, me.getY()-HALF);
				if(moving.x+SIZE>dim.width){
					dim.width = moving.x+SIZE;
				}
				else if(moving.x<0){
					moving.x = 0;
				}
				if(moving.y+SIZE>dim.height){
					dim.height = moving.y+SIZE;
				}
				else if(moving.y<0){
					moving.y = 0;
				}
				setSize(dim);
				pack();
				repaint();
			}
		}

		@Override public void mouseMoved(MouseEvent e) {}
		@Override public void mouseClicked(MouseEvent e) {}
		@Override public void mouseEntered(MouseEvent e) {}
		@Override public void mouseExited(MouseEvent e) {}
		
	}
	
	public Arranger(int n, Stitcher stitcher){
		super();
		this.stitcher = stitcher;
		setLayout(new BorderLayout());
		canvas = new ArrangerCanvas();
		
		int side = (int) Math.ceil(Math.sqrt(n));
		for(int p=1;p<=n;p++){
			int xi = p-1;
			int yi = 0;
			while(xi>=side){
				xi -= side;
				yi++;
			}
			int xp = SIZE*xi;
			int yp = SIZE*yi;
			
			ImagePlus thumbImp = IJ.openVirtual(stitcher.stackPath+""+stitcher.wells.get(0).getTitle(p));
			thumbImp.setPosition(1, thumbImp.getNSlices()/2, 1);
			BufferedImage buf = thumbImp.getProcessor().getBufferedImage();
			Image thumb = buf.getScaledInstance(SIZE, SIZE, BufferedImage.SCALE_FAST);
			
			Pos pos = new Pos(xp, yp, p);
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
		controlPan.add(Box.createHorizontalStrut(20));
		JButton ok = new JButton("OK");
		ok.addActionListener(this);
		controlPan.add(ok);
		add(controlPan, BorderLayout.SOUTH);
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}
	
	public void actionPerformed(ActionEvent ae){
		String event = ae.getActionCommand();
		if(event.equals("OK")){
			int w = canvas.getWidth()/SIZE;
			int h = canvas.getHeight()/SIZE;
			int[][] arrange = new int[h][w];
			for(Pos pos:canvas.list){
				int xi = (int) (pos.x/(float)SIZE);
				int yi = (int) (pos.y/(float)SIZE);
				//System.out.println(pos.x+" ~ "+xi+" , "+pos.y+" ~ "+yi);
				arrange[yi][xi] = pos.field;
			}
			
			overlapPercent = (double) overlapSpinner.getValue();
			Prefs.set("Wagner.overlapPercent", overlapPercent);
			stitcher.stitch(arrange, overlapPercent);
			dispose();
		}
	}
	
}


/////////////////////////////////////// V1 ////////////////////////////////////////
/*import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;


public class Arranger extends JFrame{
	private static final long serialVersionUID = -8425580036453107021L;
	private static final int SIZE = 50;
	private static final int HALF = 25;
	
	int[][] locs;
	private ArrayList<LocPan> panels;
	private ArrangeArea area;
	private LocPan moving;
	
	private class LocPan extends JPanel{
		private static final long serialVersionUID = -4657274692979106676L;
		private int field;
		private LocPan(int f){
			super();
			setBorder(BorderFactory.createLineBorder(Color.GREEN, 2));
			field = f;
			add(new JLabel(""+f));
		}
		public Dimension getPreferredSize(){
			return new Dimension(SIZE,SIZE);
		}
		@Override
		public String toString(){
			return "Arranger.LocPan at X="+getX()+" Y="+getY()+" field="+field;
		}
	}
	
	private class DragGridLayout implements LayoutManager{
		private Dimension dim;
		private ArrayList<Component> list;
		
		private DragGridLayout(){
			list = new ArrayList<Component>();
		}

		private void calculate(){
			try{
				int maxX = SIZE;
				int maxY = SIZE;
				for(Component comp:list){
					int x = comp.getX();
					int y = comp.getY();
					int xx = Math.round(x/SIZE)*SIZE;
					int yy = Math.round(y/SIZE)*SIZE;
					for(Component other:list){
						if(comp==other) continue;
						//if(xx==other.getX()&&yy==other.getY()){
						
							xx+=SIZE;
						//}
					}
					maxX = Math.max(maxX, xx+SIZE);
					maxY = Math.max(maxY, yy+SIZE);
					comp.setLocation( xx, yy );
					System.out.println(xx+","+yy);
				}
				
				dim = new Dimension(maxX, maxY);
				area.setSize(maxX, maxY);
				System.out.println(dim.toString());
			}catch(Exception e){ System.out.print( e.toString()+"\n"+Arrays.toString(e.getStackTrace()).replace(",","\n")+"\n");}
		}
		
		@Override
		public void addLayoutComponent(String name, Component comp) {
			list.add(comp);
			calculate();
		}

		@Override
		public void removeLayoutComponent(Component comp) {
			list.remove(comp);
			calculate();
		}

		@Override
		public Dimension preferredLayoutSize(Container parent) {
			calculate();
			return dim;
		}

		@Override
		public Dimension minimumLayoutSize(Container parent) {
			return dim;
		}

		@Override
		public void layoutContainer(Container parent) {
			list = new ArrayList<Component>();
			for(Component comp: parent.getComponents()){
				if(comp instanceof LocPan) list.add(comp);
			}
			calculate();
		}
		
	}
	
	private class ArrangeArea extends JPanel implements MouseListener, MouseMotionListener{
		private static final long serialVersionUID = 6473650566715490063L;
		//private Dimension dim;
		
		public ArrangeArea(){
			//super(null);
			super( new DragGridLayout() );
			setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
			addMouseListener(this);
			addMouseMotionListener(this);
			//dim = new Dimension(0, 0);
		}
		
		@Override
		public Dimension getPreferredSize(){
			//setSize(dim);
			return dim;
		}
		@Override
		public Dimension getMinimumSize(){
			return dim;
		}
		@Override
		public Dimension getMaximumSize(){
			return dim;
		}
		
		@Override
		public Component add(Component comp){
			super.add(comp);
			if(dim.width>dim.height){
				dim.height += SIZE;
			}
			else{
				dim.width += SIZE;
			}
			validate();
			//System.out.println(dim.toString());
			return comp;
		}
		
		private LocPan getTarget(Point p){
			Component comp = area.getComponentAt(p.x, p.y);
			if(comp instanceof LocPan) return (LocPan) comp;
			return null;
		}
		public void mousePressed(MouseEvent me){
			moving = getTarget(me.getPoint());
		}
		public void mouseReleased(MouseEvent me){
			try{
				Point p = me.getPoint();
				if(moving!=null){
					moving.setLocation(p.x, p.y);
					moving = null;
					
					//packPanels();
					//repaint();
					pack();
				}
			}catch(Exception e){ System.out.print( e.toString()+"\n"+Arrays.toString(e.getStackTrace()).replace(",","\n"));}
		}
		public void mouseDragged(MouseEvent me){
			if(moving!=null){
				//setComponentZOrder(moving, 0);
				moving.setLocation(me.getX()-HALF, me.getY()-HALF);
				int w = area.getWidth();
				int h = area.getHeight();
				if(moving.getX()+50>w){
					w = moving.getX()+SIZE;
				}
				if(moving.getY()+SIZE>h){
					h = moving.getY()+SIZE;
				}
				dim = new Dimension(w, h);
				setSize(dim);
			}
		}

		@Override public void mouseMoved(MouseEvent e) {}
		@Override public void mouseClicked(MouseEvent e) {}
		@Override public void mouseEntered(MouseEvent e) {}
		@Override public void mouseExited(MouseEvent e) {}
		
		@Override
		public void paintComponent(Graphics g){
			super.paintComponent(g);
			int W = getWidth();
			int H = getHeight();
			g.setColor(Color.BLACK);
			for(int x=0;x<W;x+=SIZE){
				g.drawLine(x,0,x,H);
			}
			for(int y=0;y<H;y+=SIZE){
				g.drawLine(0,y,W,y);
			}
		}

		@Override
		public String toString(){
			return getPreferredSize().toString()+""+Arrays.toString(getComponents());
		}
		
	}
	
	public Arranger(int max1d, final Stitcher stitcher){
		super();
		setLayout(new BorderLayout());
		
		int max = (int)Math.ceil(Math.sqrt(max1d));
		//setLayout(new FlowLayout(FlowLayout.CENTER));
		setLayout(null);
		area = new ArrangeArea();
		//main.setLayout(new GridLayout(max,max));
		//main.setLayout(null);
		//main.setSize(500,500);
		add(area);
		
		panels = new ArrayList<LocPan>();
		locs = new int[max][max];
		for(int i=0;i<max1d;i++){
			panels.add( new LocPan(i+1) );
			area.add(panels.get(i));
		}
		//packPanels();
		
		add(area, BorderLayout.CENTER);
		
		JButton ok = new JButton("OK");
		ok.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent ae){
				int[][] arrange = getArrangement();
				stitcher.stitch(arrange);
			}
		});
		add(ok, BorderLayout.SOUTH);
		//packPanels();
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
		
		//System.out.println( area.toString() );
	}
	
	public void packPanels(){
		int maxX = SIZE;
		int maxY = SIZE;
		for(LocPan pan:panels){
			int x = pan.getX();
			int y = pan.getY();
			int xx = Math.round(x/SIZE)*SIZE;
			int yy = Math.round(y/SIZE)*SIZE;
			for(LocPan other:panels){
				if(pan==other) continue;
				if(xx==other.getX()&&yy==other.getY()){
					xx+=SIZE;
				}
			}
			maxX = Math.max(maxX, xx+SIZE);
			maxY = Math.max(maxY, yy+SIZE);
			pan.setLocation( xx, yy );
		}
		
		//if(getWidth()<maxX||getHeight()<maxY){
		//	setSize(area.getSize());
		//}
		//setSize(maxX+100, maxY+100);
		//setSize(area.getPreferredSize());
		//repaint();
		//pack();
		area.setSize(maxX, maxY);
		setSize(maxX+100,maxY+100);
	}
	
	//public Dimension getPreferredSize(){
	//	return area.getPreferredSize();
	//}
	
	private int[][] getArrangement(){
		for(LocPan pan:panels){
			locs[(int)(pan.getX()/SIZE)][(int)(pan.getY()/SIZE)] = pan.field;
		}
		return locs;
	}
}*/
