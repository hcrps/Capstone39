package com.mbientlab.metawear.app;

/**
 * Created by Person on 2018-02-07.
 */

public class Complex {
    private float re;   // the real part
    private float im;   // the imaginary part

    public Complex(){
        re = 0;
        im = 0;
    }

    // create a new object with the given real and imaginary parts
    public Complex(float real, float imaginary) {
        re = real;
        im = imaginary;
    }

    public void setRe(float real){
        re = real;
    }

    public void setIm(float imaginary){
        im = imaginary;
    }

    // return a new Complex object whose value is (this + b)
    public Complex plus(Complex b) {
        Complex a = this;             // invoking object
        float real = a.re + b.re;
        float imaginary = a.im + b.im;
        return new Complex(real, imaginary);
    }

    // return a new Complex object whose value is (this - b)
    public Complex minus(Complex b) {
        Complex a = this;
        float real = a.re - b.re;
        float imaginary = a.im - b.im;
        return new Complex(real, imaginary);
    }

    // return a new Complex object whose value is (this * b)
    public Complex times(Complex b) {
        Complex a = this;
        float real = a.re * b.re - a.im * b.im;
        float imaginary = a.re * b.im + a.im * b.re;
        return new Complex(real, imaginary);
    }

    // return the real or imaginary part
    public float re() { return re; }
    public float im() { return im; }
}

