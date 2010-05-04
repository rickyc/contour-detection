import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.Math;
import java.util.ArrayList;
import javax.imageio.ImageIO;

// ---------------------------------------------------------------------------
// THIS CODE IS NECESSARY IN ORDER TO RUN THE CONTOUR / EDGE DETECTION HOWEVER
// IT NEEDS MAJOR REFACTORING AS IT WAS A QUICK PORT FROM THE ORIGINAL C CODE
// THIS ALGORITHM WAS WRITTEN IN. 
// ---------------------------------------------------------------------------
// TEST CODE PORT OVER TO REAL APP
// THIS CODE NEEDS TO BE REFACTORED
// THIS IS A PORT OF MY C CODE
// ---------------------------------------------------------------------------

public class EdgeDetection {

	// (RFCT) code repeated from Contour file
	public static final double PI = Math.PI;
	public static final int RED = 0;

	// NEW
	public static final int UPPER = 0;
	public static final int CENTER = 1;
	public static final int LOWER = 2;
	public static final int[] scales = new int[]{3};

	// test code
	public void debugger() {
		calculateDIForImage(new File("circle"),true);
		calculateDIHatForImage("circle");
	}

	// rfct to path
	public void calculateDIHatForImage(String imageName) {
		for (int i=0;i<scales.length;i++) {
			int scale = scales[i];
			for(int j=0;j<4;j++) { // doing some hard coding
				double theta = (-1.0/2.0)*PI + ((1.0/4.0)*PI)*j;
				int k = 4+j; // opposite angle, 4 should be angles.legnth/2

				try {
					BufferedImage firstImage = ImageIO.read(new File(imageName+"_"+i+"_"+j+".jpg"));
					BufferedImage secondImage = ImageIO.read(new File(imageName+"_"+i+"_"+k+".jpg"));

					BufferedImage newImage = new BufferedImage(firstImage.getWidth(),firstImage.getHeight(),BufferedImage.TYPE_INT_RGB);

					for (int x=0;x<firstImage.getWidth();x++)
						for (int y=0;y<firstImage.getHeight();y++) {
							Point p = new Point(x,y);
							int c = (getPixelRGB(firstImage,p)[0] - getPixelRGB(secondImage,p)[0])/2 + 128;
							newImage.setRGB(x,y,(new Color(c,c,c)).hashCode());
						}

					// save images after you are done
					File file = new File(imageName+"_"+j+".jpg");
					//File file = new File(imageName+"_"+i+"_"+j+"_di.jpg");
					file.createNewFile();
					ImageIO.write(newImage, "jpg", file);

				} catch(IOException io) { io.printStackTrace(); }
			}
		}	
	}

	// rfct to path
	public void calculateDIForImage(File f, boolean diAvgAlgo) {
		BufferedImage image = new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB);
		try {
			image = ImageIO.read(f);
		} catch(IOException io) { io.printStackTrace(); }

		for(int i=0;i<scales.length;i++) {
			int scale = scales[i];
			for(int j=0;j<8;j++) {
				BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(),BufferedImage.TYPE_INT_RGB);
				double theta = (-1.0/2.0)*PI + ((1.0/4.0)*PI)*j;
				
				System.out.println(theta);
				for(int x=scale;x<image.getWidth()-scale;x++) {
					for(int y=scale;y<image.getHeight()-scale;y++) {
						ArrayList<Point> q = diAvgAlgo ? getPointsWithPositionDIHat(new Point(x,y),theta,scale)
							: getPointsWithPosition(new Point(x,y),theta,scale);
						int sum = 0;
						for(Point p : q)
							sum += getPixelRGB(image,p)[RED];
						int avg = sum/q.size();
						newImage.setRGB(x,y,(new Color(avg,avg,avg).hashCode()));
					}
				}

				// save images after you are done
				try {
					String imageName = f.getName(); // RFCT
					imageName = imageName.substring(0,imageName.length()-4);

					File file = new File(imageName+"_"+i+"_"+j+".jpg");
					file.createNewFile();
					ImageIO.write(newImage, "jpg", file);
				} catch(IOException io) { io.printStackTrace(); }
			}
		}
	}

	// RFCT
	public ArrayList<Point> getPointsWithPositionDIHat(Point p, double theta, int scale) {
		Point u = nextPointWithPointAndAngle(p,theta,UPPER);
		Point c = nextPointWithPointAndAngle(p,theta);
		Point l = nextPointWithPointAndAngle(p,theta,LOWER);
		
		ArrayList<Point> points = new ArrayList<Point>();
		points.addAll(getPointsWithPosition(u,theta,scale));
		points.addAll(getPointsWithPosition(c,theta,scale));
		points.addAll(getPointsWithPosition(l,theta,scale));

		// append extra point at center
		points.add(getPointByAngleAndScale(p,theta,scale+1));
		
		return points;
	}

	public ArrayList<Point> getPointsWithPosition(Point p, double theta, int scale) {
		ArrayList<Point> points = new ArrayList<Point>();
		
		// lookup table would be more efficient here, if scale is 5, then take the calculation
		// from scale of 3 and append 2 pixels to it, etc. (RFCT)
		for(int i=0;i<scale;i++) {
			points.add(p);
			p = nextPointWithPointAndAngle(p,theta);
		}

		return points;
	}

	public Point nextPointWithPointAndAngle(Point p, double theta) {
		return nextPointWithPointAndAngle(p,theta,CENTER);
	}

	// RFCT?
	public Point getPointByAngleAndScale(Point p, double theta, int scale) {
		int dx = (int)Math.round(Math.cos(theta));
		int dy = (int)Math.round(Math.sin(theta));

		return new Point(p.x+(dx*3),p.y+(dy*3));
	}

	public Point nextPointWithPointAndAngle(Point p, double theta, int direction) {
		int dx = (int)Math.round(Math.cos(theta));
		int dy = (int)Math.round(Math.sin(theta));

		switch(direction) {
			case UPPER:		return new Point(p.x+dy,p.y-dx);
			case CENTER:	return new Point(p.x+dx,p.y+dy);
			case LOWER:		return new Point(p.x-dy,p.y+dx);
		}
		return p; // should never reach
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

	/*
	public static void main(String[] args) {
		EdgeDetection ed = new EdgeDetection();
		ed.debugger();
	}*/
}
