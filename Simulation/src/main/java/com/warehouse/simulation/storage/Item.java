package com.warehouse.simulation.storage;

import java.awt.*;

import java.io.Serializable;

public class Item   implements Serializable {

    private String id;
    private String name;
    private double weight;
    // private static int num = 0;
    // private Point position; // (5; 10)
    private String storage_unit_id;

    public void setStorageUnitId(String id) {
        this.storage_unit_id = id;
    }

    // public Item (String name){
    //     this.name = name;
    //     this.id = name + "_" + num++;
    // } 
      public Item (String id,String name , double weight){

        this.id = id;
        this.name= name;
        this.weight=weight;
    }

    public String getId(){
        return id;
    }
    public String getName(){
        return name;
    }
      public double getWeight(){
        return weight;
    }
    // public Point getPosition(){
    //     return position;
    // }
 
    public String getStorageUnitId(){
        return storage_unit_id;
    }

    @Override
    public String toString(){
        return id+ ":" +name+ "("+weight+"kg)";
    }
}