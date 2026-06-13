package pt.ipvc.kiosks.desktop.dto;

import java.math.BigDecimal;
import java.util.List;

public class ProductDto {
    public Long       id;
    public String     productName;
    public String     description;
    public BigDecimal price;
    public String     sku;
    public String     imageUrl;
    public Boolean    active;
    public Long       categoryId;
    public String     categoryName;
    public Integer    stockQuantity;
    public List<Long> storeIds;

    @Override public String toString() { return productName != null ? productName : ""; }
}
