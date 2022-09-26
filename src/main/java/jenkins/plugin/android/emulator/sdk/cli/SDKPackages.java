package jenkins.plugin.android.emulator.sdk.cli;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings(value = "EI_EXPOSE_REP")
public class SDKPackages {
    public static class SDKPackage implements Comparable<SDKPackage> {
        private String id;
        private Version version;
        private Version available;
        private String description;
        private String location;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Version getVersion() {
            return version;
        }

        public void setVersion(Version version) {
            this.version = version;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public Version getAvailable() {
            return available;
        }

        public void setAvailable(Version available) {
            this.available = available;
        }

        @Override
        public int compareTo(SDKPackage o) {
            if (o == null) {
                return 1;
            }
            int result = id.compareTo(o.getId());
            if (result == 0) {
                result = version.compareTo(o.getVersion());
            }
            return result;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, version);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass())
                return false;
            SDKPackage other = (SDKPackage) obj;
            return Objects.equals(id, other.id) && Objects.equals(version, other.version);
        }

        @Override
        public String toString() {
            return id;
        }
    }

    private List<SDKPackage> available = new LinkedList<>();
    private List<SDKPackage> updates = new LinkedList<>();
    private List<SDKPackage> installed = new LinkedList<>();

    public List<SDKPackage> getAvailable() {
        return available;
    }

    public void setAvailable(List<SDKPackage> available) {
        this.available = available;
    }

    public List<SDKPackage> getUpdates() {
        return updates;
    }

    public void setUpdates(List<SDKPackage> updates) {
        this.updates = updates;
    }

    public List<SDKPackage> getInstalled() {
        return installed;
    }

    public void setInstalled(List<SDKPackage> installed) {
        this.installed = installed;
    }
}
