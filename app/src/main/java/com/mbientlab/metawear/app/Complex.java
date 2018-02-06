package com.mbientlab.metawear.app;

/**
 * Created by Person on 2018-02-05.
 */

public class Complex {
    private double re;   // the real part
    private double im;   // the imaginary part

    public Complex(){
        re = 0.0;
        im = 0.0;
    };

    // create a new object with the given real and imaginary parts
    public Complex(double real, double imag) {
        re = real;
        im = imag;
    }

    public void setRe(double real){
        re = real;
    }

    public void setIm(double imaginary){
        im = imaginary;
    }

    // return a new Complex object whose value is (this + b)
    public Complex plus(Complex b) {
        Complex a = this;             // invoking object
        double real = a.re + b.re;
        double imag = a.im + b.im;
        return new Complex(real, imag);
    }

    // return a new Complex object whose value is (this - b)
    public Complex minus(Complex b) {
        Complex a = this;
        double real = a.re - b.re;
        double imag = a.im - b.im;
        return new Complex(real, imag);
    }

    // return a new Complex object whose value is (this * b)
    public Complex times(Complex b) {
        Complex a = this;
        double real = a.re * b.re - a.im * b.im;
        double imag = a.re * b.im + a.im * b.re;
        return new Complex(real, imag);
    }

    // return the real or imaginary part
    public double re() { return re; }
    public double im() { return im; }
}
