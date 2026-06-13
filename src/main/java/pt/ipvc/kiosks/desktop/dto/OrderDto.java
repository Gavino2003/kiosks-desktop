package pt.ipvc.kiosks.desktop.dto;

import java.math.BigDecimal;
import java.util.List;

public class OrderDto {
    public Long       id;
    public String     reference;
    public String     status;
    public BigDecimal orderTotal;
    public String     createdAt;
    public Long       kioskId;
    public String     kioskName;
    public Long       storeId;
    public String     storeName;
    public List<LineDto> lines;

    public static class LineDto {
        public Long       productId;
        public String     productName;
        public Integer    quantity;
        public BigDecimal unitPrice;
    }
}
