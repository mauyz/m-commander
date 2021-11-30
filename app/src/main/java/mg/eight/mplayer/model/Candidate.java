package mg.eight.mplayer.model;

import java.util.ArrayList;

public class Candidate {

    private String name;
    private String firstName;
    private String address;
    private String phone;
    private ArrayList<Job> titles;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public ArrayList<Job> getTitles() {
        return titles;
    }

    public void setTitles(ArrayList<Job> titles) {
        this.titles = titles;
    }


}
