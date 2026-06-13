package pt.ipvc.kiosks.desktop.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import pt.ipvc.kiosks.desktop.client.CoreApiClient;
import pt.ipvc.kiosks.desktop.dto.CategoryDto;
import pt.ipvc.kiosks.desktop.dto.ProductDto;
import pt.ipvc.kiosks.desktop.dto.StoreDto;
import pt.ipvc.kiosks.desktop.util.SessionManager;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Controller
public class ProductsController implements Initializable {

    @FXML private TableView<ProductDto>          tableProducts;
    @FXML private TableColumn<ProductDto,String> colName;
    @FXML private TableColumn<ProductDto,String> colPrice;
    @FXML private TableColumn<ProductDto,String> colSku;
    @FXML private TableColumn<ProductDto,String> colCategory;
    @FXML private TableColumn<ProductDto,String> colStores;
    @FXML private TableColumn<ProductDto,String> colActive;

    @FXML private ComboBox<StoreDto>    cmbStore;
    @FXML private ComboBox<CategoryDto> cmbCategory;
    @FXML private TextField             txtSearch;

    @FXML private Label                 lblFormTitle;
    @FXML private TextField             txtName;
    @FXML private TextField             txtDescription;
    @FXML private TextField             txtPrice;
    @FXML private TextField             txtSku;
    @FXML private TextField             txtImageUrl;
    @FXML private ComboBox<CategoryDto> cmbFormCategory;
    @FXML private VBox                  vboxStores;
    @FXML private Button                btnSave;
    @FXML private Button                btnDelete;
    @FXML private Button                btnCancelEdit;
    @FXML private Label                 lblStatus;

    @Autowired private CoreApiClient api;

    private boolean       isAdmin;
    private List<StoreDto> allStores = new ArrayList<>();
    private ProductDto    editingProduct = null;

    private static final StoreDto FILTER_ALL;
    static {
        FILTER_ALL = new StoreDto();
        FILTER_ALL.id = null;
        FILTER_ALL.storeName = "— Todas as lojas —";
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colName.setCellValueFactory(c     -> new SimpleStringProperty(c.getValue().productName));
        colPrice.setCellValueFactory(c    -> new SimpleStringProperty(
                c.getValue().price != null ? c.getValue().price + " €" : ""));
        colSku.setCellValueFactory(c      -> new SimpleStringProperty(
                c.getValue().sku != null ? c.getValue().sku : ""));
        colCategory.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().categoryName != null ? c.getValue().categoryName : ""));
        colStores.setCellValueFactory(c   -> new SimpleStringProperty(
                c.getValue().storeIds != null ? c.getValue().storeIds.size() + " loja(s)" : "—"));
        colActive.setCellValueFactory(c   -> new SimpleStringProperty(
                Boolean.TRUE.equals(c.getValue().active) ? "Ativo" : "Inativo"));

        var session = SessionManager.getInstance();
        var user    = session.getCurrentUser();
        isAdmin     = session.isAdmin();

        allStores = api.getActiveStores();

        javafx.util.StringConverter<StoreDto> storeConv = new javafx.util.StringConverter<>() {
            public String   toString(StoreDto s) { return s == null ? "" : s.storeName; }
            public StoreDto fromString(String s) { return null; }
        };
        javafx.util.StringConverter<CategoryDto> catConv = new javafx.util.StringConverter<>() {
            public String      toString(CategoryDto c) { return c == null ? "— Todas as categorias —" : c.categoryName; }
            public CategoryDto fromString(String s)    { return null; }
        };

        List<StoreDto> filterStores = new ArrayList<>();
        if (isAdmin) {
            filterStores.add(FILTER_ALL);
            filterStores.addAll(allStores);
        } else {
            allStores.stream()
                    .filter(s -> s.id.equals(user.storeId))
                    .findFirst()
                    .ifPresent(filterStores::add);
        }
        cmbStore.setConverter(storeConv);
        cmbStore.setItems(FXCollections.observableArrayList(filterStores));
        cmbStore.setDisable(!isAdmin);

        cmbCategory.setConverter(catConv);
        cmbFormCategory.setConverter(catConv);

        buildStoreCheckboxes(isAdmin ? allStores
                : filterStores.isEmpty() ? List.of() : List.of(filterStores.get(0)));

        cmbStore.getSelectionModel().selectedItemProperty().addListener((obs, o, store) -> {
            Long storeId = (store == null || store == FILTER_ALL) ? null : store.id;
            List<CategoryDto> cats = api.getCategories(storeId);

            if (storeId != null) {
                cmbFormCategory.setItems(FXCollections.observableArrayList(cats));
                if (!cats.isEmpty()) cmbFormCategory.getSelectionModel().selectFirst();
            }

            List<CategoryDto> opts = new ArrayList<>();
            opts.add(null);
            opts.addAll(cats);
            cmbCategory.setItems(FXCollections.observableArrayList(opts));
            cmbCategory.getSelectionModel().selectFirst();
            refreshTable();
        });

        if (!filterStores.isEmpty()) cmbStore.getSelectionModel().selectFirst();

        txtSearch.textProperty().addListener((obs, o, n) -> refreshTable());
        cmbCategory.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> refreshTable());

        tableProducts.getSelectionModel().selectedItemProperty().addListener((obs, o, p) -> {
            if (p != null) enterEditMode(p);
        });

        enterCreateMode();
    }

    private void buildStoreCheckboxes(List<StoreDto> stores) {
        vboxStores.getChildren().clear();
        for (StoreDto s : stores) {
            CheckBox cb = new CheckBox(s.storeName);
            cb.setUserData(s);
            vboxStores.getChildren().add(cb);
        }
        if (!isAdmin && !stores.isEmpty()) {
            CheckBox cb = (CheckBox) vboxStores.getChildren().get(0);
            cb.setSelected(true);
            cb.setDisable(true);
        }
    }

    private List<StoreDto> getSelectedStores() {
        return vboxStores.getChildren().stream()
                .filter(n -> n instanceof CheckBox && ((CheckBox) n).isSelected())
                .map(n -> (StoreDto) ((CheckBox) n).getUserData())
                .collect(Collectors.toList());
    }

    private void clearStoreCheckboxes() {
        vboxStores.getChildren().forEach(n -> {
            if (n instanceof CheckBox) {
                CheckBox cb = (CheckBox) n;
                if (!cb.isDisable()) cb.setSelected(false);
            }
        });
    }

    private Long selectedFilterStoreId() {
        StoreDto s = cmbStore.getValue();
        if (s == null || s == FILTER_ALL) return null;
        return s.id;
    }

    private void refreshTable() {
        String term    = txtSearch.getText().trim();
        Long   storeId = selectedFilterStoreId();
        CategoryDto cat = cmbCategory.getValue();
        Long catId = cat != null ? cat.id : null;
        List<ProductDto> results = api.getProducts(storeId, catId, term.isEmpty() ? null : term);
        tableProducts.setItems(FXCollections.observableArrayList(results));
    }

    private void enterCreateMode() {
        editingProduct = null;
        lblFormTitle.setText("Novo Produto");
        btnSave.setText("Criar Produto");
        btnCancelEdit.setVisible(false); btnCancelEdit.setManaged(false);
        btnDelete.setVisible(false);     btnDelete.setManaged(false);
        txtName.clear(); txtDescription.clear(); txtPrice.clear();
        txtSku.clear();  txtImageUrl.clear();
        clearStoreCheckboxes();
        lblStatus.setText("");
        lblStatus.getStyleClass().removeAll("error", "success");
        tableProducts.getSelectionModel().clearSelection();
    }

    private void enterEditMode(ProductDto product) {
        editingProduct = product;
        lblFormTitle.setText("Editar Produto");
        btnSave.setText("Guardar Alterações");
        btnCancelEdit.setVisible(true); btnCancelEdit.setManaged(true);
        btnDelete.setVisible(true);     btnDelete.setManaged(true);
        txtName.setText(product.productName);
        txtDescription.setText(product.description != null ? product.description : "");
        txtPrice.setText(product.price != null ? product.price.toPlainString() : "");
        txtSku.setText(product.sku          != null ? product.sku      : "");
        txtImageUrl.setText(product.imageUrl != null ? product.imageUrl : "");

        List<CategoryDto> cats = cmbFormCategory.getItems();
        if (cats != null) {
            cats.stream().filter(c -> c != null && c.id.equals(product.categoryId))
                    .findFirst().ifPresent(c -> cmbFormCategory.setValue(c));
        }

        List<Long> assocIds = product.storeIds != null ? product.storeIds : List.of();
        vboxStores.getChildren().forEach(n -> {
            if (n instanceof CheckBox) {
                CheckBox cb = (CheckBox) n;
                StoreDto s  = (StoreDto) cb.getUserData();
                cb.setSelected(assocIds.contains(s.id));
            }
        });

        lblStatus.setText("");
        lblStatus.getStyleClass().removeAll("error", "success");
    }

    private void showStatus(String msg, boolean error) {
        lblStatus.setText(msg);
        lblStatus.getStyleClass().removeAll("error", "success");
        lblStatus.getStyleClass().add(error ? "error" : "success");
    }

    @FXML private void handleSearch() { refreshTable(); }

    @FXML
    private void handleSave() {
        String      name     = txtName.getText().trim();
        String      desc     = txtDescription.getText().trim();
        String      priceStr = txtPrice.getText().trim();
        String      sku      = txtSku.getText().trim();
        String      imgUrl   = txtImageUrl.getText().trim();
        CategoryDto cat      = cmbFormCategory.getValue();
        List<StoreDto> stores = getSelectedStores();

        if (name.isEmpty() || priceStr.isEmpty() || cat == null || stores.isEmpty()) {
            showStatus("Nome, preço, categoria e pelo menos uma loja são obrigatórios.", true); return;
        }
        BigDecimal price;
        try { price = new BigDecimal(priceStr.replace(",", ".")); }
        catch (NumberFormatException e) { showStatus("Preço inválido.", true); return; }

        List<Long> storeIds = stores.stream().map(s -> s.id).toList();

        try {
            if (editingProduct == null) {
                api.createProduct(name, desc.isEmpty() ? null : desc, price,
                        sku.isEmpty() ? null : sku, imgUrl.isEmpty() ? null : imgUrl,
                        cat.id, storeIds);
                showStatus("Produto criado com sucesso.", false);
            } else {
                api.updateProduct(editingProduct.id, name, desc.isEmpty() ? null : desc, price,
                        sku.isEmpty() ? null : sku, imgUrl.isEmpty() ? null : imgUrl,
                        cat.id, storeIds);
                showStatus("Produto atualizado com sucesso.", false);
            }
            enterCreateMode();
            refreshTable();
        } catch (Exception e) {
            showStatus("Erro: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleDeactivate() {
        ProductDto selected = tableProducts.getSelectionModel().getSelectedItem();
        if (selected == null) { showStatus("Selecione um produto.", true); return; }
        try {
            api.deactivateProduct(selected.id);
            showStatus("Produto desativado.", false);
            refreshTable();
        } catch (Exception e) {
            showStatus("Erro: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleDelete() {
        if (editingProduct == null) { showStatus("Selecione um produto.", true); return; }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Desativar o produto \"" + editingProduct.productName + "\"?",
                ButtonType.YES, ButtonType.NO);
        alert.setTitle("Confirmar desativação");
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) handleDeactivate();
        });
    }

    @FXML private void handleCancelEdit() { enterCreateMode(); }
    @FXML private void handleRefresh()    { enterCreateMode(); refreshTable(); }
}
