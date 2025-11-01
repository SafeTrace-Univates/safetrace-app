package com.example.safetrace.model;

import java.util.Date;

public class Localizacao {
    private double latitude;
    private double longitude;
    private Date timestamp;
    private float precisao; // Precisão em metros
    
    public Localizacao() {
        this.timestamp = new Date();
    }
    
    public Localizacao(double latitude, double longitude, float precisao) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.precisao = precisao;
        this.timestamp = new Date();
    }
    
    public double getLatitude() {
        return latitude;
    }
    
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
    
    public double getLongitude() {
        return longitude;
    }
    
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    
    public Date getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
    
    public float getPrecisao() {
        return precisao;
    }
    
    public void setPrecisao(float precisao) {
        this.precisao = precisao;
    }
}

