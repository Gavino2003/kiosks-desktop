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
                Boolean.TRUE.equals(c.getValue().active) ? "Ativo" : "Inativo"));

        tableStock.setRowFactory(tv -> new TableRow<ProductDto>() {
            @Override
            protected void updateItem(ProductDto ps, boolean empty) {
                super.updateItem(ps, empty);
                if (ps == null || isSelected()) {
                    setStyle("");
                } else if (!Boolean.TRUE.equals(ps.active)) {
                    setStyle("-fx-text-fill: #aaa;");
                } else if (ps.stockQuantity != null && ps.stockQuantity == 0) {
                    setStyle("-fx-background-color: #fde8e8;");
                } else if (ps.stockQuantity != null && ps.stockQuantity <= 3) {
                    setStyle("-fx-background-color: #fef3e2;");
                } else {
                    setStyle("");
                }
            }
        });

        javafx.util.StringConverter<CategoryDto> catConverter = new javafx.util.StringConverter<>() {
            public String      toString(CategoryDto c) { return c == null ? "Todas as categorias" : c.categoryName; }
            public CategoryDto fromString(String s)    { return null; }
        };
        cmbCategory.setConverter(catConverter);

        List<CategoryDto> cats = userStoreId != null
                ? api.getCategories(userStoreId)
                : List.of();
        List<CategoryDto> options = new ArrayList<>();
        options.add(null);
        options.addAll(cats);
        cmbCategory.setItems(FXCollections.observableArrayList(options));
        cmbCategory.getSelectionModel().selectFirst();

        txtSearch.textProperty().addListener((obs, o, n) -> handleFilter());

        loadStock(null);
    }

    private void loadStock(Long categoryId) {
        if (userStoreId == null) {
            lblInfo.setText("Utilizador sem loja atribuída.");
            return;
        }

        String search = txtSearch.getText().trim();
        List<ProductDto> all = api.getProducts(userStoreId, categoryId,
                search.isEmpty() ? null : search);

        tableStock.setItems(FXCollections.observableArrayList(all));
        long ativos = all.stream().filter(p -> Boolean.TRUE.equals(p.active)).count();
        lblInfo.setText(all.size() + " produto(s) — " + ativos + " ativo(s), "
                + (all.size() - ativos) + " inativo(s)");
    }

    @FXML
    private void handleFilter() {
        CategoryDto cat = cmbCategory.getValue();
        loadStock(cat != null ? cat.id : null);
    }

    @FXML
    private void handleRefresh() {
        cmbCategory.getSelectionModel().selectFirst();
        txtSearch.clear();
        loadStock(null);
    }
}
