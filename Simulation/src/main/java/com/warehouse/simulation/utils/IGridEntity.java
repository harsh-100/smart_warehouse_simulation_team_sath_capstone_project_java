package com.warehouse.simulation.utils;

import java.awt.Point;


//this Point class comprises two fields : X and Y, 3 constructors 
//and the most important methods for us: Point getLocation()
//documentation: https://docs.oracle.com/javase/8/docs/api/java/awt/Point.html
//i suggest to use interface IGridEntity so we can use methods for every class in our main simulation class
//and i also suggest to set only static object in the grid (charging stations and storage racks)
public interface IGridEntity {
	Point getLocation();
	String getID();
}
