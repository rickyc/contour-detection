import java.awt.Point;
import java.lang.Math;

/**
 * Ricky C.
 * Computer Vision: Contour Line Detection
 * March 2010
 */
public class Vertex implements Comparable<Vertex> {
	private Point p;
	private int thetaIndex;
	private double cost;
	private Vertex parent;

	// constructors
	public Vertex(double cost) {
		this.cost = cost;
	}

	public Vertex(Point p, int thetaIndex, double cost) {
		this.p = p;
		this.thetaIndex = thetaIndex;
		this.cost = cost;
	}

	public int compareTo(Vertex v) {
		return (cost == v.getCost()) ? 0 : (cost < v.getCost()) ? -1 : 1;
	}
	
	// setters
	public void setCost(double cost) { this.cost = cost; }
	public void setParent(Vertex parent) { this.parent = parent; }
	public void setPoint(Point p) { this.p = p; }
	public void setThetaIndex(int thetaIndex) { this.thetaIndex = thetaIndex; }

	// getters
	public double getCost() { return cost; }
	public Vertex getParent() { return parent; }
	public Point getPoint() { return p; }
	public int getThetaIndex() { return thetaIndex; }
}
