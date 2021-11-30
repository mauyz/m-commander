/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package mg.eight.mplayer.task;

import android.os.Environment;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import mg.eight.mplayer.model.Command;
import mg.eight.mplayer.model.Message;

/**
 *
 * @author Mauyz
 */

public class SocketDownloadManager implements Runnable{

    private final AppService service;
    private final Socket socket;
    private boolean canceled = false;


    public SocketDownloadManager(AppService service, Socket socket) {
        this.service = service;
        this.socket = socket;
    }

    @Override
    public void run() {
        File file = null;
        BufferedOutputStream bos = null;
        while(socket != null && !socket.isClosed()){
            try {
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                Object temp = in.readObject();
                if(temp != null && temp.getClass().equals(Message.class)){
                    Message<?> message = (Message<?>) temp;
                    switch (message.getCommand()) {
                        case NAME:{
                            String name = (String) message.getData();
                            file = new File(Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_DOWNLOADS)
                                    , name);
                            if (file.exists()) {
                                service.updateDownload(name, "File already saved", true);
                                sendMessage(new Message<>(Command.CANCEL, null));
                                file = null;
                                break;
                            }
                            try {
                                bos = new BufferedOutputStream(new FileOutputStream(file, true));
                                setCanceled(false);
                                service.updateDownload(name, null, false);
                                sendMessage(new Message<>(Command.CONTINUE, null));
                            } catch (IOException e) {
                                service.updateDownload(name, "Download error", true);
                                sendMessage(new Message<>(Command.CANCEL, null));
                                file = null;
                            }
                            break;
                        }
                        case BYTE:{
                            try {
                                bos.write((byte[]) message.getData());
                                bos.flush();
                                if (canceled) {
                                    sendMessage(new Message<>(Command.CANCEL, null));
                                    bos.close();
                                    file.delete();
                                    file = null;
                                } else sendMessage(new Message<>(Command.CONTINUE, null));
                            } catch (IOException e) {
                                service.updateDownload(file.getName(), "Download error", true);
                                sendMessage(new Message<>(Command.CANCEL, null));
                                file = null;
                            }
                            break;
                        }
                        case END:{
                            try {
                                bos.close();
                            } catch (IOException e) {
                            }
                            service.updateDownload(file.getName(), "Download completed"
                                    , true);
                            file = null;
                            break;
                        }
                        case ERROR:{
                            if (file != null) {
                                file.delete();
                                service.updateDownload(file.getName(), (String) message.getData(), true);
                            }
                            file = null;
                            break;
                        }
                    }
                }
                else break;
            } catch (IOException | ClassNotFoundException e) {
                if(file != null){
                    file.delete();
                    service.updateDownload(file.getName(), "Connection error" ,true);
                }
                break;
            }
        }
    }

    private void sendMessage(Message<?> msg) throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        out.writeObject(msg);
        out.flush();
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }
}
