package pt.ipvc.kiosks.desktop.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import pt.ipvc.kiosks.dal.entities.Category;
import pt.ipvc.kiosks.dal.entities.ProductStore;
import pt.ipvc.kiosks.dal.entities.Store;
import pt.ipvc.kiosks.dal.repository.CategoryRepository;
import pt.ipvc.kiosks.dal.repository.ProductStoreRepository;
import pt.ipvc.kiosks.desktop.util.SessionManager;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

@Controller
public class StockController implements Initializable {

    @FXML private TableView<ProductStore>         tableStock;
    @FXML private TableColumn<ProductStore,String> colName;
    @FXML private TableColumn<ProductStore,String> colSku;
    @FXML private TableColumn<ProductStore,String> colCategory;
    @FXML private TableColumn<ProductStore,String> colPrice;
    @FXML private TableColumn<ProductStore,String> colQty;
    @FXML private TableColumn<ProductStore,String> colStatus;
    @FXML private ComboBox<Category> cmbCategory;
    @FXML private TextField          txtSearch;
    @FXML private Label              lblInfo;

    @Autowired private ProductStoreRepository productStoreRepository;
    @Autowired private CategoryRepository     categoryRepository;

    private Store userStore;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        userStore = SessionManager.getInstance().getCurrentUser().getStore();

        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getProduct().getProductName()));
        colSku.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getProduct().getSku() != null ? c.getValue().getProduct().getSku() : "—"));
        colCategory.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getProduct().getCategory() != null
                        ? c.getValue().getProduct().getCategory().getCategoryName() : "—"));
        colPrice.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getProduct().getPrice().toPlainString() + " €"));
        colQty.setCellValueFactory(c -> new SimpleStringProperty(
                String.valueOf(c.getValue().getStockQuantity())));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getActive() ? "Ativo" : "Inativo"));

        // stock 0 → vermelho, stock ≤3 → laranja, inativo → cinzento; seleção override tudo
        tableStock.setRowFactory(tv -> new TableRow<>() {
            {
                selectedProperty().addListener((obs, o, selected) -> applyStyle(getItem(), selected));
            }
            @Override
            protected void updateItem(ProductStore ps, boolean empty) {
                super.updateItem(ps, empty);
                applyStyle(ps, isSelected());
            }
            private void applyStyle(ProductStore ps, boolean selected) {
                if (ps == null || selected) {
                    setStyle("");
                } else if (!ps.getActive()) {
                    setStyle("-fx-text-fill: #aaa;");
                } else if (ps.getStockQuantity() == 0) {
                    setStyle("-fx-background-color: #fde8e8;");
                } else if (ps.getStockQuantity() <= 3) {
                    setStyle("-fx-background-color: #fef3e2;");
                } else {
                    setStyle("");
                }
            }
        });

        javafx.util.StringConverter<Category> catConverter = new javafx.util.StringConverter<>() {
            public String   toString(Category c) { return c == null ? "Todas as categorias" : c.getCategoryName(); }
            public Category fromString(String s) { return null; }
        };
        cmbCategory.setConverter(catConverter);

        List<Category> cats = userStore != null
                ? categoryRepository.findByStoreIdStoreAndActiveTrue(userStore.getIdStore())
                : List.of();
        java.util.List<Category> options = new java.util.ArrayList<>();
        options.add(null);
        options.addAll(cats);
        cmbCategory.setItems(FXCollections.observableArrayList(options));
        cmbCategory.getSelectionModel().selectFirst();

        txtSearch.textProperty().addListener((obs, o, n) -> handleFilter());

        loadStock(null);
    }

    private void loadStock(Long categoryId) {
        if (userStore == null) {
            lblInfo.setText("Utilizador sem loja atribuída.");
            return;
        }
        List<ProductStore> all = productStoreRepository
                .findByStoreIdStoreOrderByProductProductName(userStore.getIdStore());

        // filtrar por categoria
        if (categoryId != null) {
            final Long fc = categoryId;
            all = all.stream()
                    .filter(ps -> ps.getProduct().getCategory() != null
                            && ps.getProduct().getCategory().getIdCategory().equals(fc))
                    .toList();
        }

        // filtrar por pesquisa
        String search = txtSearch.getText().trim().toLowerCase();
        if (!search.isEmpty()) {
            all = all.stream()
                    .filter(ps -> ps.getProduct().getProductName().toLowerCase().contains(search)
                            || (ps.getProduct().getSku() != null
                                && ps.getProduct().getSku().toLowerCase().contains(search)))
                    .toList();
        }

        tableStock.setItems(FXCollections.observableArrayList(all));
        long ativos = all.stream().filter(ProductStore::getActive).count();
        lblInfo.setText(all.size() + " produto(s) — " + ativos + " ativo(s), " + (all.size() - ativos) + " inativo(s)");
    }

    @FXML
    private void handleFilter() {
        Category cat = cmbCategory.getValue();
        loadStock(cat != null ? cat.getIdCategory() : null);
    }

    @FXML
    private void handleRefresh() {
        cmbCategory.getSelectionModel().selectFirst();
        txtSearch.clear();
        loadStock(null);
    }
}
