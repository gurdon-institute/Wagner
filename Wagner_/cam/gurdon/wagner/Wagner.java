package cam.gurdon.wagner;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Arrays;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;


/*	bash script to submit slurm jobs (submitWagner.sh)
#!/bin/bash
#SBATCH --job-name=Wagner
#SBATCH --partition=1604
#SBATCH --distribution=cyclic:block
#SBATCH --ntasks=12
#SBATCH --mail-type=END
#SBATCH --mail-user=

echo -e "JobID: $SLURM_JOB_ID\n====="
echo "Time: `date`"
echo "Running on master node: `hostname`"
echo "Current directory: `pwd`"
echo -e "\nExecuting command: $WAGNER_CMD\n====="

eval $WAGNER_CMD
*/	

/*	bash commands for script setup
export WAGNER_CMD="java -Xmx30000m -jar Wagner.jar -/mnt/DATA/home1/imaging/rsb48/data/ 12"
sbatch ./submitWagner.sh
cat slurm-jobno.out
*/

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

public class Wagner{
private static final String USAGE = "Wagner - by Richard Butler, Gurdon Institute Imaging Facility\n"
									+"constructs stacks from TIFFs with names matching r[0-9]{2}c[0-9]{2}f[0-9]{2}p[0-9]{2}-ch[0-9]sk[0-9]?[0-9]fk[0-9]fl[0-9].tiff\n"
		 							+"command line usage: Wagner directory_path(string, required) thread_count(int, optional)\n";

//~~~~~GUI mode things~~~~~
private JFrame gui;
private JLabel pathLabel;
private JSpinner threadSpin;
private static final Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
private static final Font FONT = new Font(Font.SANS_SERIF, Font.PLAIN, screen.height/100);
private static final String prefPath = System.getProperty("user.home")+File.separator+"Wagner.cfg";
private String path = System.getProperty("user.home");
private int threads = 2;
//~~~~~~~~~~

	public static void main(String[] args){
		try{
			String path = "";
			int nThreads = 0;
			if(args.length == 0){
				new Wagner().showGui();
				return;
			}
			else{
				path = args[0];
				nThreads = 1;
			}
			if(args.length > 1){
				try{
					nThreads = Integer.valueOf( args[1] );
				}catch(NumberFormatException nfe){
					System.out.println(args[1]+" is not an integer");
					System.out.print(USAGE);
					return;
				}
			}
			if(nThreads<1){
				System.out.print("Invalid thread count: "+nThreads);
				System.out.print(USAGE);
				return;
			}
	
			LocationManager manager = new LocationManager(path, nThreads);
			manager.execute();
		}catch(Exception e){ System.out.print( e.toString()+"\n"+Arrays.toString(e.getStackTrace()).replace(",","\n"));}
	}
	
	private class Log extends OutputStream{
		private JTextArea text;
		private StringBuilder sb;
		
		public Log(){
			text = new JTextArea();
			text.setFont(FONT);
			text.setEditable(false);
			text.setLineWrap(true);
			text.setText(USAGE+"\n\n");
					//+String.format("%.1f",Runtime.getRuntime().freeMemory()/1000000d)+" MB free JVM memory - increase using the -Xmx argument\n");
			sb = new StringBuilder();
		}
		
		public JTextArea getTextArea(){
			return text;
		}
		
		@Override
		public synchronized void write(int b) throws IOException {
			if (b == '\n') {
	            final String str = sb.toString() + "\n";
	            SwingUtilities.invokeLater(new Runnable() {
	                @Override
	                public void run() {
	                    text.append(str);
	                }
	            });
	            sb.setLength(0);
	            return;
	        }

	        sb.append((char) b);
		}
		
	}
	
	private void savePrefs(){
		try{
			PrintWriter writer = new PrintWriter(prefPath);
			writer.println(path);
			writer.println(threads);
			writer.close();
		}catch(Exception e){ System.out.print( e.toString()+"\n"+Arrays.toString(e.getStackTrace()).replace(",","\n"));}
	}
	
	private void loadPrefs(){
		try{
			File cfg = new File(prefPath);
			if(cfg.exists()){
				//List<String> lines = Files.readAllLines(Paths.get(prefPath));	//java 8 required, not supported on iProducts
				//path = lines.get(0);
				//threads = Integer.valueOf(lines.get(1));
				FileInputStream fis = new FileInputStream(cfg);
				StringBuilder sb = new StringBuilder();
				int i;
				while((i = fis.read()) != -1){
					sb.append((char)i);
				}
				String config = sb.toString();
				String[] lines = config.split("\\s*\\r?\\n\\s*"); //split on newlines and trim extra white space
				path = lines[0];
				threads = Integer.valueOf(lines[1]);
				fis.close();
			}
			else{
				savePrefs();	//save defaults
			}
		}catch(Exception e){ System.out.print( e.toString()+"\n"+Arrays.toString(e.getStackTrace()).replace(",","\n"));}
	}
	
	private void showGui(){
		
		loadPrefs();
		
		ActionListener listen = new ActionListener(){
			public void actionPerformed(ActionEvent ae){
				try{
					String event = ae.getActionCommand();
					if(event.equals("Run")){
						threads = (int)threadSpin.getValue();
						SwingWorker<Void, String> worker = new SwingWorker<Void, String>(){
							public Void doInBackground(){
								LocationManager manager = new LocationManager(path, threads);
								manager.execute();
								savePrefs();
								return null;
							}
						};
						worker.execute();
					}
					else if(event.equals("Cancel")){
						gui.dispose();
					}
					else if(event.equals("Path...")){
						JFileChooser chooser = new JFileChooser();
						chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					    FileNameExtensionFilter filter = new FileNameExtensionFilter("Images", "tif", "tiff");
					    chooser.setFileFilter(filter);
					    chooser.setSelectedFile(new File(path));
					    if(chooser.showOpenDialog(gui) == JFileChooser.APPROVE_OPTION) {
					    	try{
						    	path = chooser.getSelectedFile().getAbsolutePath();
						    	pathLabel.setText(path);
					    	}catch(Exception e){ System.out.print( e.toString()+"\n"+Arrays.toString(e.getStackTrace()).replace(",","\n"));}
					    }
					}
				}catch(Exception e){ System.out.print( e.toString()+"\n"+Arrays.toString(e.getStackTrace()).replace(",","\n"));}
			}
		};
		
		Log log = new Log();
		PrintStream outStream = new PrintStream(log);
		System.setOut(outStream);
		System.setErr(outStream);
		
		gui = new JFrame("Wagner");
		gui.setLayout(new BoxLayout(gui.getContentPane(), BoxLayout.Y_AXIS));
		gui.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("logo_icon.gif")));
		gui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JScrollPane scrollPane = new JScrollPane(log.getTextArea()){
			private static final long serialVersionUID = 2410141039613207390L;
			public Dimension getPreferredSize(){
				return new Dimension(screen.width/3, screen.height/4);
			}
		};
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		gui.add(scrollPane);
		
		JPanel controlPan = new JPanel();
		pathLabel = new JLabel(path);
		pathLabel.setFont(FONT);
		controlPan.add(pathLabel);
		JButton pathButton = new JButton("Path...");
		pathButton.setFont(FONT);
		pathButton.addActionListener(listen);
		controlPan.add(pathButton);
		controlPan.add(Box.createHorizontalStrut(20));
		JLabel threadLabel = new JLabel("Threads:");
		threadLabel.setFont(FONT);
		controlPan.add(threadLabel);
		SpinnerModel model = new SpinnerNumberModel(threads, 1, 32, 1);
		threadSpin = new JSpinner(model);
		threadSpin.setFont(FONT);
		controlPan.add(threadSpin);
		gui.add(controlPan);
		
		JPanel buttonPan = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));
		JButton run = new JButton("Run");
		run.setFont(FONT);
		run.addActionListener(listen);
		buttonPan.add(run);
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(listen);
		cancel.setFont(FONT);
		buttonPan.add(cancel);
		gui.add(buttonPan);
		
		gui.pack();
		gui.setLocationRelativeTo(null);
		gui.setVisible(true);
		
	}
	
}