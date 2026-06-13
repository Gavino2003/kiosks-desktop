package pt.ipvc.kiosks.desktop.dto;

public class StoreDto {
    public Long    id;
    public String  storeName;
    public String  storeType;
    public String  address;
    public String  city;
    public String  postalCode;
    public String  phone;
    public Boolean active;

    @Override public String toString() { return storeName != null ? storeName : ""; }
}
