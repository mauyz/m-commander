/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package mg.eight.mplayer.model;

import java.io.Serializable;

/**
 * @author Mauyz
 */
public class Song implements Serializable {

    private final String id;
    private final String path;

    public Song(String id, String path) {
        this.id = id;
        this.path = path;
    }

    public String getId() {
        return id;
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        String ch = path.contains("\\")?"\\":"/";
        return path.substring(path.lastIndexOf(ch)+1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Song song = (Song) o;

        return id.equals(song.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}