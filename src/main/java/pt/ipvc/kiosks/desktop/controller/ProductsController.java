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

    private boolean        isAdmin;
    private List<StoreDto> allStores     = new ArrayList<>();
    private ProductDto     editingProduct = null;

    private static final StoreDto ALL_STORES_OPTION;
    static {
        ALL_STORES_OPTION = new StoreDto();
        ALL_STORES_OPTION.id = null;
        ALL_STORES_OPTION.storeName = "— All stores —";
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
                c.getValue().storeIds != null ? c.getValue().storeIds.size() + " store(s)" : "—"));
        colActive.setCellValueFactory(c   -> new SimpleStringProperty(
                Boolean.TRUE.equals(c.getValue().active) ? "Active" : "Inactive"));

        var session = SessionManager.getInstance();
        var user    = session.getCurrentUser();
        isAdmin     = session.isAdmin();

        allStores = api.getActiveStores();

        javafx.util.StringConverter<StoreDto> storeConverter = new javafx.util.StringConverter<>() {
            public String   toString(StoreDto s) { return s == null ? "" : s.storeName; }
            public StoreDto fromString(String s) { return null; }
        };
        javafx.util.StringConverter<CategoryDto> categoryConverter = new javafx.util.StringConverter<>() {
            public String      toString(CategoryDto c) { return c == null ? "— All categories —" : c.categoryName; }
            public CategoryDto fromString(String s)    { return null; }
        };

        List<StoreDto> filterOptions = new ArrayList<>();
        if (isAdmin) {
            filterOptions.add(ALL_STORES_OPTION);
            filterOptions.addAll(allStores);
        } else {
            allStores.stream().filter(s -> s.id.equals(user.storeId)).findFirst()
                    .ifPresent(filterOptions::add);
        }

        cmbStore.setConverter(storeConverter);
        cmbStore.setItems(FXCollections.observableArrayList(filterOptions));
        cmbStore.setDisable(!isAdmin);

        cmbCategory.setConverter(categoryConverter);
        cmbFormCategory.setConverter(categoryConverter);

        buildStoreCheckboxes(isAdmin ? allStores
                : filterOptions.isEmpty() ? List.of() : List.of(filterOptions.get(0)));

        cmbStore.getSelectionModel().selectedItemProperty().addListener((obs, o, store) -> {
            Long storeId = (store == null || store == ALL_STORES_OPTION) ? null : store.id;
            List<CategoryDto> categories = api.getCategories(storeId);

            if (storeId != null) {
                cmbFormCategory.setItems(FXCollections.observableArrayList(categories));
                if (!categories.isEmpty()) cmbFormCategory.getSelectionModel().selectFirst();
            }

            List<CategoryDto> categoryOptions = new ArrayList<>();
            categoryOptions.add(null);
            categoryOptions.addAll(categories);
            cmbCategory.setItems(FXCollections.observableArrayList(categoryOptions));
            cmbCategory.getSelectionModel().selectFirst();
            refreshTable();
        });

        if (!filterOptions.isEmpty()) cmbStore.getSelectionModel().selectFirst();

        txtSearch.textProperty().addListener((obs, o, n) -> refreshTable());
        cmbCategory.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> refreshTable());

        tableProducts.getSelectionModel().selectedItemProperty().addListener((obs, o, product) -> {
            if (product != null) enterEditMode(product);
        });

        enterCreateMode();
    }

    private void buildStoreCheckboxes(List<StoreDto> stores) {
        vboxStores.getChildren().clear();
        for (StoreDto store : stores) {
            CheckBox cb = new CheckBox(store.storeName);
            cb.setUserData(store);
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
            if (n instanceof CheckBox cb && !cb.isDisable()) {
                cb.setSelected(false);
            }
        });
    }

    private Long selectedFilterStoreId() {
        StoreDto s = cmbStore.getValue();
        return (s == null || s == ALL_STORES_OPTION) ? null : s.id;
    }

    private void refreshTable() {
        String      term    = txtSearch.getText().trim();
        Long        storeId = selectedFilterStoreId();
        CategoryDto cat     = cmbCategory.getValue();
        List<ProductDto> results = api.getProducts(storeId, cat != null ? cat.id : null,
                term.isEmpty() ? null : term);
        tableProducts.setItems(FXCollections.observableArrayList(results));
    }

    private void enterCreateMode() {
        editingProduct = null;
        lblFormTitle.setText("New Product");
        btnSave.setText("Create Product");
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
        lblFormTitle.setText("Edit Product");
        btnSave.setText("Save Changes");
        btnCancelEdit.setVisible(true); btnCancelEdit.setManaged(true);
        btnDelete.setVisible(true);     btnDelete.setManaged(true);
        txtName.setText(product.productName);
        txtDescription.setText(product.description != null ? product.description : "");
        txtPrice.setText(product.price    != null ? product.price.toPlainString() : "");
        txtSku.setText(product.sku        != null ? product.sku      : "");
        txtImageUrl.setText(product.imageUrl != null ? product.imageUrl : "");

        List<CategoryDto> categories = cmbFormCategory.getItems();
        if (categories != null) {
            categories.stream().filter(c -> c != null && c.id.equals(product.categoryId))
                    .findFirst().ifPresent(c -> cmbFormCategory.setValue(c));
        }

        List<Long> assignedStoreIds = product.storeIds != null ? product.storeIds : List.of();
        vboxStores.getChildren().forEach(n -> {
            if (n instanceof CheckBox cb) {
                StoreDto store = (StoreDto) cb.getUserData();
                cb.setSelected(assignedStoreIds.contains(store.id));
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
        String      name        = txtName.getText().trim();
        String      description = txtDescription.getText().trim();
        String      priceStr    = txtPrice.getText().trim();
        String      sku         = txtSku.getText().trim();
        String      imageUrl    = txtImageUrl.getText().trim();
        CategoryDto category    = cmbFormCategory.getValue();
        List<StoreDto> stores   = getSelectedStores();

        if (name.isEmpty() || priceStr.isEmpty() || category == null || stores.isEmpty()) {
            showStatus("Name, price, category and at least one store are required.", true);
            return;
        }

        BigDecimal price;
        try {
            price = new BigDecimal(priceStr.replace(",", "."));
        } catch (NumberFormatException e) {
            showStatus("Invalid price.", true);
            return;
        }

        List<Long> storeIds = stores.stream().map(s -> s.id).toList();

        try {
            if (editingProduct == null) {
                api.createProduct(name, description.isEmpty() ? null : description, price,
                        sku.isEmpty() ? null : sku, imageUrl.isEmpty() ? null : imageUrl,
                        category.id, storeIds);
                showStatus("Product created successfully.", false);
            } else {
                api.updateProduct(editingProduct.id, name, description.isEmpty() ? null : description, price,
                        sku.isEmpty() ? null : sku, imageUrl.isEmpty() ? null : imageUrl,
                        category.id, storeIds);
                showStatus("Product updated successfully.", false);
            }
            enterCreateMode();
            refreshTable();
        } catch (Exception e) {
            showStatus("Error: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleDeactivate() {
        ProductDto selected = tableProducts.getSelectionModel().getSelectedItem();
        if (selected == null) { showStatus("Please select a product.", true); return; }
        try {
            api.deactivateProduct(selected.id);
            showStatus("Product deactivated.", false);
            refreshTable();
        } catch (Exception e) {
            showStatus("Error: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleDelete() {
        if (editingProduct == null) { showStatus("Please select a product.", true); return; }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Deactivate product \"" + editingProduct.productName + "\"?",
                ButtonType.YES, ButtonType.NO);
        alert.setTitle("Confirm deactivation");
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) handleDeactivate();
        });
    }

    @FXML private void handleCancelEdit() { enterCreateMode(); }
    @FXML private void handleRefresh()    { enterCreateMode(); refreshTable(); }
}
