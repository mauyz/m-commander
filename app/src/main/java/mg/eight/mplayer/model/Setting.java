package mg.eight.mplayer.model;

public class Setting {

    private static final Setting instance = new Setting();
    private boolean mute;
    private boolean random;
    private String repeat = "No repeat";

    private Setting(){

    }

    public static Setting getInstance() {
        return instance;
    }

    public boolean isMute() {
        return mute;
    }

    public void setMute(boolean mute) {
        this.mute = mute;
    }

    public boolean isRandom() {
        return random;
    }

    public void setRandom(boolean random) {
        this.random = random;
    }

    public String getRepeat() {
        return repeat;
    }

    public void setRepeat(String repeat) {
        this.repeat = repeat;
    }
}
