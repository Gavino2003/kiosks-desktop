package pt.ipvc.kiosks.desktop.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import pt.ipvc.kiosks.desktop.client.CoreApiClient;
import pt.ipvc.kiosks.desktop.dto.CategoryDto;
import pt.ipvc.kiosks.desktop.dto.ProductDto;
import pt.ipvc.kiosks.desktop.util.SessionManager;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

@Controller
public class StockController implements Initializable {

    @FXML private TableView<ProductDto>          tableStock;
    @FXML private TableColumn<ProductDto,String> colName;
    @FXML private TableColumn<ProductDto,String> colSku;
    @FXML private TableColumn<ProductDto,String> colCategory;
    @FXML private TableColumn<ProductDto,String> colPrice;
    @FXML private TableColumn<ProductDto,String> colQty;
    @FXML private TableColumn<ProductDto,String> colStatus;

    @FXML private ComboBox<CategoryDto> cmbCategory;
    @FXML private TextField             txtSearch;
    @FXML private Label                 lblInfo;

    @Autowired private CoreApiClient api;

    private Long userStoreId;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        userStoreId = SessionManager.getInstance().getCurrentUser().storeId;

        colName.setCellValueFactory(c     -> new SimpleStringProperty(c.getValue().productName));
        colSku.setCellValueFactory(c      -> new SimpleStringProperty(
                c.getValue().sku != null ? c.getValue().sku : "—"));
        colCategory.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().categoryName != null ? c.getValue().categoryName : "—"));
        colPrice.setCellValueFactory(c    -> new SimpleStringProperty(
                c.getValue().price != null ? c.getValue().price.toPlainString() + " €" : ""));
        colQty.setCellValueFactory(c      -> new SimpleStringProperty(
                c.getValue().stockQuantity != null ? String.valueOf(c.getValue().stockQuantity) : "—"));
        colStatus.setCellValueFactory(c   -> new SimpleStringProperty(
                Boolean.TRUE.equals(c.getValue().active) ? "Active" : "Inactive"));

        tableStock.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(ProductDto product, boolean empty) {
                super.updateItem(product, empty);
                if (product == null || isSelected()) {
                    setStyle("");
                } else if (!Boolean.TRUE.equals(product.active)) {
                    setStyle("-fx-text-fill: #aaa;");
                } else if (product.stockQuantity != null && product.stockQuantity == 0) {
                    setStyle("-fx-background-color: #fde8e8;");
                } else if (product.stockQuantity != null && product.stockQuantity <= 3) {
                    setStyle("-fx-background-color: #fef3e2;");
                } else {
                    setStyle("");
                }
            }
        });

        cmbCategory.setConverter(new javafx.util.StringConverter<>() {
            public String      toString(CategoryDto c) { return c == null ? "All categories" : c.categoryName; }
            public CategoryDto fromString(String s)    { return null; }
        });

        List<CategoryDto> categories = userStoreId != null ? api.getCategories(userStoreId) : List.of();
        List<CategoryDto> options = new ArrayList<>();
        options.add(null);
        options.addAll(categories);
        cmbCategory.setItems(FXCollections.observableArrayList(options));
        cmbCategory.getSelectionModel().selectFirst();

        txtSearch.textProperty().addListener((obs, o, n) -> handleFilter());

        loadStock(null);
    }

    private void loadStock(Long categoryId) {
        if (userStoreId == null) {
            lblInfo.setText("No store assigned to this user.");
            return;
        }

        String search = txtSearch.getText().trim();
        List<ProductDto> products = api.getProducts(userStoreId, categoryId,
                search.isEmpty() ? null : search);

        tableStock.setItems(FXCollections.observableArrayList(products));

        long active = products.stream().filter(p -> Boolean.TRUE.equals(p.active)).count();
        lblInfo.setText(products.size() + " product(s) — " + active + " active, "
                + (products.size() - active) + " inactive");
    }

    @FXML
    private void handleFilter() {
        CategoryDto category = cmbCategory.getValue();
        loadStock(category != null ? category.id : null);
    }

    @FXML
    private void handleRefresh() {
        cmbCategory.getSelectionModel().selectFirst();
        txtSearch.clear();
        loadStock(null);
    }
}
