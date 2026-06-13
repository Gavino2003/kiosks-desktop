package pt.ipvc.kiosks.desktop.dto;

public class CategoryDto {
    public Long    id;
    public String  categoryName;
    public Integer displayOrder;
    public Boolean active;
    public Long    storeId;

    @Override public String toString() { return categoryName != null ? categoryName : ""; }
}
