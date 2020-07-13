package sgbootcamp.example.beacons;

public class BeaconModel {
    String namespaceID;
    String instanceID;
    String bateryId;
    String distanceID;
    String rssiId;
    String txId;

    public BeaconModel(String namespaceID, String instanceID, String bateryId, String distanceID, String rssiId, String txId) {
        this.namespaceID = namespaceID;
        this.instanceID = instanceID;
        this.bateryId = bateryId;
        this.distanceID = distanceID;
        this.rssiId = rssiId;
        this.txId = txId;
    }

    public String getNamespaceID() {
        return namespaceID;
    }

    public void setNamespaceID(String namespaceID) {
        this.namespaceID = namespaceID;
    }

    public String getInstanceID() {
        return instanceID;
    }

    public void setInstanceID(String instanceID) {
        this.instanceID = instanceID;
    }

    public String getBateryId() {
        return bateryId;
    }

    public void setBateryId(String bateryId) {
        this.bateryId = bateryId;
    }

    public String getDistanceID() {
        return distanceID;
    }

    public void setDistanceID(String distanceID) {
        this.distanceID = distanceID;
    }

    public String getRssiId() {
        return rssiId;
    }

    public void setRssiId(String rssiId) {
        this.rssiId = rssiId;
    }

    public String getTxId() {
        return txId;
    }

    public void setTxId(String txId) {
        this.txId = txId;
    }

    @Override
    public String toString() {
        return "BeaconModel{" +
                "namespaceID='" + namespaceID + '\'' +
                ", instanceID='" + instanceID + '\'' +
                ", bateryId='" + bateryId + '\'' +
                ", distanceID='" + distanceID + '\'' +
                ", rssiId='" + rssiId + '\'' +
                ", txId='" + txId + '\'' +
                '}';
    }
}
