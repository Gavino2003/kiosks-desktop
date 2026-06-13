package pt.ipvc.kiosks.desktop.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import pt.ipvc.kiosks.bll.services.ProductService;
import pt.ipvc.kiosks.dal.entities.*;
import pt.ipvc.kiosks.dal.repository.*;
import pt.ipvc.kiosks.desktop.util.SessionManager;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Controller
public class ProductsController implements Initializable {

    // ── tabela ──
    @FXML private TableView<Product>         tableProducts;
    @FXML private TableColumn<Product,String> colName;
    @FXML private TableColumn<Product,String> colPrice;
    @FXML private TableColumn<Product,String> colSku;
    @FXML private TableColumn<Product,String> colCategory;
    @FXML private TableColumn<Product,String> colStores;
    @FXML private TableColumn<Product,String> colActive;

    // ── filtros ──
    @FXML private ComboBox<Store>    cmbStore;
    @FXML private ComboBox<Category> cmbCategory;
    @FXML private TextField          txtSearch;

    // ── formulário ──
    @FXML private Label              lblFormTitle;
    @FXML private TextField          txtName;
    @FXML private TextField          txtDescription;
    @FXML private TextField          txtPrice;
    @FXML private TextField          txtSku;
    @FXML private TextField          txtImageUrl;
    @FXML private ComboBox<Category> cmbFormCategory;
    @FXML private VBox               vboxStores;      // checkboxes de lojas
    @FXML private Button             btnSave;
    @FXML private Button             btnDelete;
    @FXML private Button             btnCancelEdit;
    @FXML private Label              lblStatus;

    @Autowired private ProductService         productService;
    @Autowired private ProductRepository      productRepository;
    @Autowired private ProductStoreRepository productStoreRepository;
    @Autowired private StoreRepository        storeRepository;
    @Autowired private CategoryRepository     categoryRepository;

    private boolean  isAdmin;
    private List<Store> allStores = new ArrayList<>();
    private Product  editingProduct = null;

    private static final Store FILTER_ALL = new Store("— Todas as lojas —", null, null, null, null);

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getProductName()));
        colPrice.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPrice() + " €"));
        colSku.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getSku() != null ? c.getValue().getSku() : ""));
        colCategory.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCategory() != null ? c.getValue().getCategory().getCategoryName() : ""));
        colStores.setCellValueFactory(c -> {
            List<ProductStore> assoc = productService.getStoreAssociations(c.getValue().getIdProduct());
            String names = assoc.stream().map(ps -> ps.getStore().getStoreName()).collect(Collectors.joining(", "));
            return new SimpleStringProperty(names.isEmpty() ? "—" : names);
        });
        colActive.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getActive() ? "Ativo" : "Inativo"));

        var currentUser = SessionManager.getInstance().getCurrentUser();
        var userStore   = currentUser.getStore();
        isAdmin = currentUser.getRole() != null && "ADMIN".equals(currentUser.getRole().getRoleName());

        // lojas reais (para checkboxes e para filtro se não-ADMIN)
        allStores = storeRepository.findByActiveTrue();

        // converters
        javafx.util.StringConverter<Store> storeConv = new javafx.util.StringConverter<>() {
            public String toString(Store s)    { return s == null ? "" : s.getStoreName(); }
            public Store  fromString(String s) { return null; }
        };
        javafx.util.StringConverter<Category> catConv = new javafx.util.StringConverter<>() {
            public String   toString(Category c) { return c == null ? "— Todas as categorias —" : c.getCategoryName(); }
            public Category fromString(String s) { return null; }
        };

        // ── filtro de loja ──
        List<Store> filterStores = new ArrayList<>();
        if (isAdmin) {
            filterStores.add(FILTER_ALL);
            filterStores.addAll(allStores);
        } else {
            filterStores = userStore != null ? List.of(userStore) : List.of();
        }
        cmbStore.setConverter(storeConv);
        cmbStore.setItems(FXCollections.observableArrayList(filterStores));
        cmbStore.setDisable(!isAdmin);

        // ── filtro de categoria ──
        cmbCategory.setConverter(catConv);

        // ── categoria no formulário ──
        cmbFormCategory.setConverter(catConv);

        // ── checkboxes de lojas no formulário ──
        buildStoreCheckboxes(isAdmin ? allStores : (userStore != null ? List.of(userStore) : List.of()));

        // quando muda filtro de loja → recarrega categorias de filtro
        cmbStore.getSelectionModel().selectedItemProperty().addListener((obs, o, store) -> {
            List<Category> cats;
            if (store == FILTER_ALL || store == null) {
                cats = categoryRepository.findByActiveTrueOrderByCategoryName();
            } else {
                cats = categoryRepository.findByStoreIdStoreAndActiveTrue(store.getIdStore());
                // formulário de categoria também restringe à loja seleccionada
                cmbFormCategory.setItems(FXCollections.observableArrayList(cats));
                if (!cats.isEmpty()) cmbFormCategory.getSelectionModel().selectFirst();
            }
            List<Category> opts = new ArrayList<>();
            opts.add(null);
            opts.addAll(cats);
            cmbCategory.setItems(FXCollections.observableArrayList(opts));
            cmbCategory.getSelectionModel().selectFirst();
            refreshTable();
        });

        if (!filterStores.isEmpty()) cmbStore.getSelectionModel().selectFirst();

        // pesquisa automática
        txtSearch.textProperty().addListener((obs, o, n) -> refreshTable());
        cmbCategory.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> refreshTable());

        tableProducts.getSelectionModel().selectedItemProperty().addListener((obs, o, p) -> {
            if (p != null) enterEditMode(p);
        });

        enterCreateMode();
    }

    private void buildStoreCheckboxes(List<Store> stores) {
        vboxStores.getChildren().clear();
        for (Store s : stores) {
            CheckBox cb = new CheckBox(s.getStoreName());
            cb.setUserData(s);
            vboxStores.getChildren().add(cb);
        }
        // não-ADMIN: pré-selecionar e bloquear a sua única loja
        if (!isAdmin && !stores.isEmpty()) {
            CheckBox cb = (CheckBox) vboxStores.getChildren().get(0);
            cb.setSelected(true);
            cb.setDisable(true);
        }
    }

    private List<Store> getSelectedStores() {
        return vboxStores.getChildren().stream()
                .filter(n -> n instanceof CheckBox && ((CheckBox) n).isSelected())
                .map(n -> (Store) ((CheckBox) n).getUserData())
                .collect(Collectors.toList());
    }

    private void clearStoreCheckboxes() {
        vboxStores.getChildren().forEach(n -> {
            if (n instanceof CheckBox cb) {
                if (!cb.isDisable()) cb.setSelected(false);
            }
        });
    }

    private Long selectedFilterStoreId() {
        Store s = cmbStore.getValue();
        if (s == null || s == FILTER_ALL) return null;
        return s.getIdStore();
    }

    private void refreshTable() {
        String   term    = txtSearch.getText().trim();
        Long     storeId = selectedFilterStoreId();
        Category cat     = cmbCategory.getValue();
        Long     catId   = cat != null ? cat.getIdCategory() : null;

        List<Product> results = productService.searchProductsByStore(term.isEmpty() ? null : term, storeId);

        if (catId != null) {
            final Long fc = catId;
            results = results.stream()
                    .filter(p -> p.getCategory() != null && p.getCategory().getIdCategory().equals(fc))
                    .toList();
        }
        tableProducts.setItems(FXCollections.observableArrayList(results));
    }

    private void enterCreateMode() {
        editingProduct = null;
        lblFormTitle.setText("Novo Produto");
        btnSave.setText("Criar Produto");
        btnCancelEdit.setVisible(false); btnCancelEdit.setManaged(false);
        btnDelete.setVisible(false);     btnDelete.setManaged(false);
        txtName.clear(); txtDescription.clear(); txtPrice.clear(); txtSku.clear(); txtImageUrl.clear();
        clearStoreCheckboxes();
        lblStatus.setText("");
        lblStatus.getStyleClass().removeAll("error", "success");
        tableProducts.getSelectionModel().clearSelection();
    }

    private void enterEditMode(Product product) {
        editingProduct = product;
        lblFormTitle.setText("Editar Produto");
        btnSave.setText("Guardar Alterações");
        btnCancelEdit.setVisible(true); btnCancelEdit.setManaged(true);
        btnDelete.setVisible(true);     btnDelete.setManaged(true);
        txtName.setText(product.getProductName());
        txtDescription.setText(product.getDescription() != null ? product.getDescription() : "");
        txtPrice.setText(product.getPrice().toPlainString());
        txtSku.setText(product.getSku() != null ? product.getSku() : "");
        txtImageUrl.setText(product.getImageUrl() != null ? product.getImageUrl() : "");
        if (product.getCategory() != null) cmbFormCategory.setValue(product.getCategory());

        // marcar checkboxes das lojas onde o produto já está
        List<Long> assocIds = productService.getStoreAssociations(product.getIdProduct())
                .stream().map(ps -> ps.getStore().getIdStore()).toList();
        vboxStores.getChildren().forEach(n -> {
            if (n instanceof CheckBox cb) {
                Store s = (Store) cb.getUserData();
                cb.setSelected(assocIds.contains(s.getIdStore()));
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
        String    name     = txtName.getText().trim();
        String    desc     = txtDescription.getText().trim();
        String    priceStr = txtPrice.getText().trim();
        String    sku      = txtSku.getText().trim();
        String    imgUrl   = txtImageUrl.getText().trim();
        Category  cat      = cmbFormCategory.getValue();
        List<Store> stores = getSelectedStores();

        if (name.isEmpty() || priceStr.isEmpty() || cat == null || stores.isEmpty()) {
            showStatus("Nome, preço, categoria e pelo menos uma loja são obrigatórios.", true);
            return;
        }
        BigDecimal price;
        try { price = new BigDecimal(priceStr.replace(",", ".")); }
        catch (NumberFormatException e) { showStatus("Preço inválido.", true); return; }

        try {
            if (editingProduct == null) {
                List<Long> storeIds = stores.stream().map(Store::getIdStore).toList();
                productService.createProduct(name, desc.isEmpty() ? null : desc, price,
                        sku.isEmpty() ? null : sku, cat.getIdCategory(), storeIds);
                showStatus("Produto criado com sucesso.", false);
            } else {
                editingProduct.setProductName(name);
                editingProduct.setDescription(desc.isEmpty() ? null : desc);
                editingProduct.setPrice(price);
                editingProduct.setSku(sku.isEmpty() ? null : sku);
                editingProduct.setImageUrl(imgUrl.isEmpty() ? null : imgUrl);
                editingProduct.setCategory(cat);
                productRepository.save(editingProduct);
                productService.updateStoreAssociations(editingProduct, stores);
                showStatus("Produto atualizado com sucesso.", false);
            }
            enterCreateMode();
            refreshTable();
        } catch (IllegalArgumentException e) {
            showStatus("Erro: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleDeactivate() {
        Product selected = tableProducts.getSelectionModel().getSelectedItem();
        if (selected == null) { showStatus("Selecione um produto.", true); return; }
        selected.setActive(!selected.getActive());
        productRepository.save(selected);
        showStatus(selected.getActive() ? "Produto ativado." : "Produto desativado.", false);
        refreshTable();
        tableProducts.getItems().stream()
                .filter(p -> p.getIdProduct().equals(selected.getIdProduct()))
                .findFirst().ifPresent(p -> tableProducts.getSelectionModel().select(p));
    }

    @FXML
    private void handleDelete() {
        if (editingProduct == null) { showStatus("Selecione um produto.", true); return; }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Apagar o produto \"" + editingProduct.getProductName() + "\" definitivamente?",
                ButtonType.YES, ButtonType.NO);
        alert.setTitle("Confirmar eliminação");
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    productRepository.delete(editingProduct);
                    enterCreateMode(); refreshTable();
                } catch (Exception e) {
                    showStatus("Erro ao apagar: " + e.getMessage(), true);
                }
            }
        });
    }

    @FXML private void handleCancelEdit() { enterCreateMode(); }

    @FXML
    private void handleRefresh() { enterCreateMode(); refreshTable(); }
}
