package com.xzk.dodgefitnessmapv2;

public class Point {
    public double x;
    public double y;
    public int index;
    public static int Index = 0;

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
        //index = Index++;  //the index is the relative time.
    }


    //for time interval
    public void addIndex() {
        index++;
    }




    public static double distance(Point p1, Point p2) {
        return Math.sqrt(Math.pow((p1.x - p2.x), 2) + Math.pow((p1.y - p2.y), 2));
    }

    @Override
    public String toString() {
        String result = "The Point is " + "(" + x + ", " + y + ") ";
        return result;
    }
}