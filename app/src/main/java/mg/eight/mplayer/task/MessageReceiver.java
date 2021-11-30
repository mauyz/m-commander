/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package mg.eight.mplayer.task;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

import mg.eight.mplayer.model.Command;
import mg.eight.mplayer.model.Message;

/**
 *
 * @author Mauyz
 */

public class MessageReceiver implements Runnable{

    private final AppService service;
    private final Socket socket;

    public MessageReceiver(AppService service, Socket socket) {
        this.service = service;
        this.socket = socket;
    }

    @Override
    public void run() {
        while(socket != null && !socket.isClosed()){
            try {
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                Object temp = in.readObject();
                if(temp != null && temp.getClass().equals(Message.class)){
                    Message<?> message = (Message<?>) temp;
                    if(message.getCommand() == Command.SECRET){
                        if(!message.getData().toString().equals(Command.SECRET.toString()))
                            break;
                    }
                    service.handleMessage(message);
                }
                else break;
            } catch (IOException | ClassNotFoundException e) {
                break;
            }
        }
        service.startHandler();
    }
}
