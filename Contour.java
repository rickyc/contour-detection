import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.lang.*;
import java.util.*;
import javax.imageio.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.*;

/**
 * Ricky C.
 * Computer Vision: Contour Line Detection
 * March 2010
 */

public class Contour extends JFrame implements MouseListener {

    // RGB Constants
    public static final int RED = 0;
    public static final int GREEN = 1;
    public static final int BLUE = 2;
    public static final double PI = Math.PI;

    // (RFCT) Can be dynamically generated
    public static final double[] ANGLES = new double[]{-1*PI/2,-1*PI/4,0,PI/4,PI/2,3/4*PI,PI,5/4*PI};

    private Vertex[][] lookupTable;
    private BufferedImage[] savedImages = new BufferedImage[4];
    private PriorityQueue<Vertex> graph = new PriorityQueue<Vertex>();

    private Point startPos, endPos;
    private BufferedImage originalImage, imageOutput;
    private JOptionPane optionPane;
    private double gamma;

    public Contour() {
        setLayout(new FlowLayout());

        initMenuBar();
        addMouseListener(this);

        setTitle("Computer Vision - Contour Detection");
        setSize(1024,768);
        setVisible(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    public void loadImageData(String imageName) {
        // saves the four DIHat images
        for(int i=0;i<4;i++) {
            try {
                savedImages[i] = ImageIO.read(new File(imageName+"_"+i+".jpg"));
            } catch(IOException io) { io.printStackTrace(); }
        }

        lookupTable = new Vertex[savedImages[0].getWidth()][savedImages[0].getHeight()];
    }

    public BufferedImage cloneImage(BufferedImage source) {
        BufferedImage img = new BufferedImage(source.getWidth(),source.getHeight(),BufferedImage.TYPE_INT_RGB);
        Graphics g = img.getGraphics();
        g.drawImage(source,0,0,null);
        g.dispose();
        return img;
    }

    public void paint(Graphics g) {
        g.drawImage(imageOutput,0,20,this);
    }

    public void findContour() {
        Vertex sv = lookupTable[endPos.x][endPos.y];
        Vertex ev = lookupTable[startPos.x][startPos.y];

        while(sv != null && sv != ev) {
            System.out.println(sv.getPoint() + " " + ev.getPoint());
            Point p = sv.getPoint();
            Color c = new Color(255,0,0);
            imageOutput.setRGB(p.x,p.y,c.hashCode());
            sv = sv.getParent();
        }
        repaint();
    }

    public int[] getPixelRGB(BufferedImage img, Point p) {
        int[] rgb;

        switch(img.getType()) {
            case BufferedImage.TYPE_BYTE_GRAY:
                int gray = img.getRaster().getSample(p.x, p.y, 0);
                rgb = new int[]{ gray, gray, gray };
                break;

            case BufferedImage.TYPE_USHORT_GRAY:
                gray = img.getRaster().getSample(p.x, p.y, 0)/257;
                rgb = new int[]{ gray, gray, gray };
                break;

            default:
                int argb = img.getRGB(p.x, p.y);
                rgb = new int[]{
                    (argb >> 16) & 0xff,
                        (argb >>  8) & 0xff,
                        (argb      ) & 0xff
                };
                break;
        }

        return rgb;
    }

    // divide by square root instead of length
    public void dijkstra(Point source) {
        // (RFCT) implement a boundary to avoid unnecessary computation

        // set all cost to infinity
        for(int x=0;x<lookupTable.length;x++)
            for(int y=0;y<lookupTable[x].length;y++) {
                Vertex v = new Vertex(Double.POSITIVE_INFINITY);
                lookupTable[x][y] = v;
                graph.add(v);
            }

        swapPointWithVertex(source,optimal(source)); // set source cost

        while(!graph.isEmpty()) {
            Vertex u = graph.poll();
            if(u.getCost() == Double.POSITIVE_INFINITY)
                break;

            graph.remove(u);

            // grab each neighbor of u
            for(int i=0;i<ANGLES.length;i++) {
                Point np = nextPointWithPointAndAngle(u.getPoint(),ANGLES[i]); // grabs next point

                BufferedImage fImg = savedImages[0];
                if(!pointWithinRange(np,fImg))
                    continue;

                Vertex v = lookupTable[np.x][np.y];
                if(v != null && graph.contains(v)) { // still in the graph
                    BufferedImage img = savedImages[i%4];
                    int[] rgb = getPixelRGB(img,np);
                    double pixelCost = (double)1/(rgb[RED]+1); // energy cost

                    // angle cost added to calculations
                    int dist = Math.abs(v.getThetaIndex()-i);
                    dist = (dist >= 4) ? 8-dist : dist;
                    double angleCost = gamma*0.25*dist; // minimize gamma for smoother curves

                    double nCost = (u.getCost() + pixelCost)/2 + angleCost;
                    if(v.getCost() >= nCost) {
                        v.setCost(nCost);
                        v.setThetaIndex(i);
                        v.setParent(u);
                        swapPointWithVertex(np,v);
                    }
                }
            }
            // ---
        }
    }

    public void swapPointWithVertex(Point p, Vertex v) {
        Vertex old = lookupTable[p.x][p.y];
        graph.remove(old);
        graph.add(v);
        v.setPoint(p);
        lookupTable[p.x][p.y] = v;
    }

    public Vertex optimal(Point p) {
        Vertex best = new Vertex(Double.POSITIVE_INFINITY);

        for(int i=0;i<ANGLES.length;i++) {
            BufferedImage img = savedImages[i%4]; // images only range from 0-3
            int[] rgb = getPixelRGB(img,p); // current pixel + theta
            double pixelCost = (double)1/(rgb[RED]+1);

            // new cost is lower
            if(pixelCost < best.getCost())
                best = new Vertex(p,i,pixelCost);
        }

        return best;
    }

    public Point nextPointWithPointAndAngle(Point p, double theta) {
        int dx = (int)Math.round(Math.cos(theta));
        int dy = (int)Math.round(Math.sin(theta));
        return new Point(p.x+dx,p.y+dy);
    }

    public boolean pointWithinRange(Point p, BufferedImage img) {
        return !(p.x < 0 || p.y < 0 || p.x >= img.getWidth() || p.y >= img.getHeight());
    }

    public void clear() {
        Color bgcolor = new Color(0f,0f,0f,0f);
        Graphics g = getGraphics();
        g.setColor(bgcolor);
        g.clearRect(0,0,getWidth(),getHeight());
    }

    public void clearPoints() {
        startPos = endPos = null;
        imageOutput = cloneImage(originalImage);
        clear();
        repaint();
    }

    // (RFCT) duplicated code
    public void clearEndPoint() {
        endPos = null;
        imageOutput = cloneImage(originalImage);
        clear();
        repaint();
    }

    // mouse events for frame + selecting points
    // (RFCT) to consider out of bound exception
    public void mouseReleased(MouseEvent e) {
        Point p = e.getPoint();
        p.y -= 20;

        if(!pointWithinRange(p,imageOutput))
            return;

        if(startPos == null) {
            startPos = p;
            dijkstra(startPos);
        } else if(endPos == null) {
            endPos = p;
            findContour();
        }

        //	System.out.println(p.x + " " + (p.y-20));
    }

    public void mousePressed(MouseEvent e) { }
    public void mouseEntered(MouseEvent e) { }
    public void mouseExited(MouseEvent e) { }
    public void mouseClicked(MouseEvent e) { }

    // menubar
    public void initMenuBar() {
        System.setProperty("apple.laf.useScreenMenuBar", "true");

        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenu tools = new JMenu("Tools");
        bar.add(file);
        bar.add(tools);

        JMenuItem fileOpen = new JMenuItem("Open");
        JMenuItem fileSave = new JMenuItem("Save");
        JMenuItem fileQuit = new JMenuItem("Quit");

        JMenuItem toolsClear = new JMenuItem("Clear Points");
        JMenuItem toolsClearEndPos = new JMenuItem("Clear End Point");
        JMenuItem toolsGamma = new JMenuItem("Adjust Gamma");
        JMenuItem toolsInfo = new JMenuItem("Info");

        fileOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, java.awt.Event.META_MASK));
        fileSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, java.awt.Event.META_MASK));
        fileQuit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, java.awt.Event.META_MASK));
        toolsClear.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, java.awt.Event.META_MASK));
        toolsClearEndPos.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, java.awt.Event.META_MASK));
        toolsGamma.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, java.awt.Event.META_MASK));
        toolsInfo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, java.awt.Event.META_MASK));

        file.add(fileOpen);
        file.add(fileSave);
        file.add(fileQuit);
        tools.add(toolsClear);
        tools.add(toolsClearEndPos);
        tools.add(toolsGamma);
        tools.add(toolsInfo);

        fileOpen.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { launchFileChooser(); } } );
        fileSave.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { saveImage(); } } );
        fileQuit.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { System.exit(0); } } );
        toolsClear.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { clearPoints(); } } );
        toolsClearEndPos.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { clearEndPoint(); } } );
        toolsGamma.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { setGamma(); } } );
        toolsInfo.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { infoDialog(); } } );

        setJMenuBar(bar);
    }

    public void infoDialog() {
        String message = "Start Point: (" + startPos.x + "," + startPos.y + ")\n" +
            "End Point: (" + endPos.x + "," + endPos.y + ")\nGamma: " + gamma;
        JOptionPane.showMessageDialog(null, message, "Settings", JOptionPane.PLAIN_MESSAGE);
    }

    public void launchFileChooser() {
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("All Images","jpg","jpeg","gif","png");
        chooser.setFileFilter(filter);
        if(chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            System.out.println("You chose to open this file: " + chooser.getSelectedFile().getName());

            try {
                imageOutput = ImageIO.read(chooser.getSelectedFile());
                generateDHatImages(chooser.getSelectedFile());
            } catch(IOException ioe) { ioe.printStackTrace(); }
        }
    }

    // (RFCT) generate dhat images
    public void generateDHatImages(File f) {
        EdgeDetection di = new EdgeDetection();
        String imageName = f.getName().substring(0,f.getName().length()-4);

        try { // RFCT
            originalImage = ImageIO.read(f);
            imageOutput = cloneImage(originalImage);
        } catch(IOException io) { io.printStackTrace(); }

        di.calculateDIForImage(f,true);
        di.calculateDIHatForImage(imageName);
        loadImageData(imageName);
        clearPoints();
    }

    public void saveImage() {
        JFileChooser fc = new JFileChooser();

        if(fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File f = fc.getSelectedFile();
                f.createNewFile();
                ImageIO.write(imageOutput, getExtension(f), f);
            } catch(IOException ioe) { ioe.printStackTrace(); }
        }
    }

    public String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 &&  i < s.length() - 1)
            ext = s.substring(i+1).toLowerCase();

        return ext;
    }

    // gamma is used to fine tune the edge detection algorithm
    public void setGamma() {
        optionPane = new JOptionPane();
        JSlider slider = new JSlider();
        slider.setMaximum(100);
        slider.setMajorTickSpacing(10);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setValue((int)(gamma*100));

        slider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent ce) {
                JSlider theSlider = (JSlider)ce.getSource();
                if(!theSlider.getValueIsAdjusting())
            optionPane.setInputValue(new Integer(theSlider.getValue()));
            }
        } );

        optionPane.setMessage(new Object[] { "Select a value: ", slider });
        optionPane.setMessageType(JOptionPane.QUESTION_MESSAGE);
        optionPane.setOptionType(JOptionPane.OK_CANCEL_OPTION);
        JDialog dialog = optionPane.createDialog(this, "Adjust Gamma");
        dialog.setVisible(true);

        gamma = (Integer)(optionPane.getInputValue())*0.01;
        System.out.println("Gamma => " + gamma);
    }

    // main
    public static void main(String[] args) {
        Contour c = new Contour();
    }

    // (RFCT) Separate UI from algorithm.
}
