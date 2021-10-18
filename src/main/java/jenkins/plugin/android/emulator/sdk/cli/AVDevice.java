package jenkins.plugin.android.emulator.sdk.cli;

import java.util.Objects;

public class AVDevice {

    private String name;
    private String path;
    private String sdCard;
    private String target;
    private String os;
    private String abi;
    private String error;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getSDCard() {
        return sdCard;
    }

    public void setSDCard(String sdcard) {
        this.sdCard = sdcard;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getAndroidOS() {
        return os;
    }

    public void setAndroidOS(String os) {
        this.os = os;
    }

    public String getABI() {
        return abi;
    }

    public void setABI(String abi) {
        this.abi = abi;
    }

    @Override
    public int hashCode() {
        return Objects.hash(error, name, path, sdCard, target, abi, os);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AVDevice other = (AVDevice) obj;
        return Objects.equals(error, other.error) && Objects.equals(name, other.name) //
                && Objects.equals(path, other.path) && Objects.equals(sdCard, other.sdCard) //
                && Objects.equals(target, other.target) && Objects.equals(abi, other.abi) //
                && Objects.equals(os, other.os);
    }

    public boolean hasError() {
        return error == null;
    }

}
