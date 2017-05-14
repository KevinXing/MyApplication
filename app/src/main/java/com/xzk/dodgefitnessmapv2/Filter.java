package com.xzk.dodgefitnessmapv2;

import java.util.ArrayList;

/**
 * Created by uestc on 5/8/2017.
 */

public class Filter
{

    // kalman
    private double Q = 1e-3;
    private double R = 4e-1;
    private double x0 = 2;
    private double p0 = 1;

    public double kalman(double data) {
        double k = p0 / (p0 + R);
        x0 = x0 + k * (data - x0);
        p0 = p0 - k * p0 + Q;
        return x0;
    }

    // average
    ArrayList<Double> dataArray = new ArrayList<>();
    public double average(double data) {
        dataArray.add(data);
        if (dataArray.size() > 10) {
            dataArray.remove(0);
            double res = 0.1 * dataArray.get(0) + 0.1 * dataArray.get(1) + 0.1 * dataArray.get(2)
                    + 0.1 * dataArray.get(3) + 0.1 * dataArray.get(4) + 0.1 * dataArray.get(5)
                    + 0.1 * dataArray.get(6) + 0.1 * dataArray.get(7) + 0.1 * dataArray.get(8)
                    + 0.1 * dataArray.get(9);
            return res;
        } else {
            return data;
        }
    }

    public double filter(double data) {
        return average(kalman(data));
    }


}
