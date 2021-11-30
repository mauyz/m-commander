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
 * @param <T>
 */

public class Message <T> implements Serializable{

    static final long serialVersionUID = 7L;
    private Command command;
    private T data;

    public Message(Command command, T data) {
        this.command = command;
        this.data = data;
    }

    public Command getCommand() {
        return command;
    }

    public void setCommand(Command command) {
        this.command = command;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }


}
