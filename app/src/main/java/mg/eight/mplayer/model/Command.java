/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mg.eight.mplayer.model;

import java.io.Serializable;

/**
 *
 * @author Mauyz
 */
public enum Command implements Serializable {
    PREVIOUS,
    TEST,
    STOP,
    PLAYPAUSE,
    NEXT,
    PLAY,
    VOLUME,
    MUTE,
    PLAYLIST,
    PAUSE,
    FILE,
    TIME,
    PLAYNEXT,
    LIST,
    SETLIST,
    DURATION,
    DEVICE,
    REMOVE,
    REFRESH,
    RANDOM,
    REPEAT,
    DOWNLOAD,
    NAME,
    BYTE,
    END,
    ERROR,
    CONTINUE,
    CANCEL,
    STATUS,
    CLOSE,
    SECRET("999");

    private String value;

    private Command(){
    }
    
    private Command(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Command{" + "value=" + value + '}';
    }
}
