package com.sensoria.sensorialibrary;

/**
 * Created by Jacopo Mangiavacchi on 12/8/14.
 */
public interface SAAnkletInterface {

    public void didDiscoverDevice();
    public void didConnect();
    public void didError(String message);
    public void didUpdateData();
}
