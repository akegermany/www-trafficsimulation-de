package de.trafficsimulation.core;
import java.awt.Color;


public class Car implements Moveable,Constants{

    private double vel=10.0;
    private double pos;
    private int lane=0;
    private int laneLast = lane;
    private double realLane;
    public double tdelay=0.0; // delay between lane changes=lane changing time

    private Color  color=Color.red;
    private double length=5.;
    private int    nr;  // with nr possible to overwrite default color
                        // in SimCanvas, methods paint*
    public boolean isPerturbed=false;


    public MicroModel model;
    public LaneChange lanechange;
    
    // first state var x,v,lane; then models model,lanechange, then
    // proprties fixed for each car: color, (length,) index n

    public Car (double x, double v, int lane, 
                MicroModel model, LaneChange lanechange, 
                double length, Color color, int number){
	pos = x;
	vel = v;
	this.lane = lane;
	this.model = model;
	this.lanechange = lanechange;
        this.length = length;
        this.color = color;
	nr = number;
    }

    public Car (Car carToCopy){
	this.pos = carToCopy.pos;
	this.vel = carToCopy.vel;
	this.lane = carToCopy.lane;
	this.model = carToCopy.model;
	this.lanechange = carToCopy.lanechange;
        this.length = carToCopy.length;
        this.color = carToCopy.color;
	this.nr = carToCopy.nr;
    }

    public void setPerturbed(){isPerturbed=true;}
    public void setPosition(double x){pos=x;}
    public void setVelocity(double v){vel=v;}
    public void setLane(int lane){this.lane=lane;}
    public void setLaneLast(int lane){this.laneLast=lane;}
    public void setModel(MicroModel model){this.model=model;}
    public void setLaneChange(LaneChange lanechange){
         this.lanechange=lanechange;}
    public void setLength(double length){this.length=length;}
    public void setColor(Color color){this.color=color;}
    
    public double position(){return pos;}
    public double velocity(){return vel;}
    public int    lane(){return lane;}
    public double realLane() { return realLane;}
    public MicroModel model(){return model;}
    public double length(){return length;}
    public Color color(){return color;}
    public int NR(){return nr;}

    // update all lane related things!!
    public boolean timeToChange(double dt){
	tdelay=tdelay+dt;
	if (tdelay>=T_DELAY){
	   tdelay=tdelay-T_DELAY;
           laneLast = lane;
           realLane = laneLast;
	   return true;
	}
	else {
            realLane = laneLast + (lane - laneLast) * tdelay / T_DELAY;
            return false;
	}
    }


    public double dAcc(Moveable vwd, Moveable bwd){

	double b=bwd.acceleration(this);
	double a=bwd.acceleration(vwd);
	return (b-a);
    }
    public boolean change(Moveable fOld, Moveable fNew, Moveable bNew){
	//System.out.println("in Car.change: nr="+nr);
	return lanechange.changeOK(this, fOld, fNew, bNew);
    }

    public void translate(double dt){
	pos=pos+dt*vel;
    }
    public void accelerate(double dt, Moveable vwd){
	double acc=model.calcAcc(this, vwd);
	vel=vel+acc*dt;
	if (vel<0.0){
	    //double Out1=new Double(vwd.velocity());
	    vel=0.0;
	}
    }
    public double acceleration(Moveable vwd){
	double acc=model.calcAcc(this, vwd);
	return acc;
    }
}
