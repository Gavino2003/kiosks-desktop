package pt.ipvc.kiosks.desktop.dto;

public class KioskDto {
    public Long   id;
    public String kioskName;
    public String serialNumber;
    public String model;
    public String status;
    public Long   storeId;
    public String storeName;

    @Override public String toString() { return kioskName != null ? kioskName : ""; }
}
