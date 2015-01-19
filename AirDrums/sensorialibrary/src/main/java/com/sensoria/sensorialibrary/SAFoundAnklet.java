package com.sensoria.sensorialibrary;

/**
 * Created by Jacopo on 12/10/14.
 */
public class SAFoundAnklet {
    public String deviceCode;
    public String deviceMac;

    @Override
    public String toString() {
        return deviceCode + " (" + deviceMac + ")";
    }
}
