package pt.ipvc.kiosks.desktop.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import pt.ipvc.kiosks.desktop.dto.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class CoreApiClient {

    private final RestTemplate rest = new RestTemplate();

    @Value("${core.api.url:http://localhost:8090}")
    private String baseUrl;

    // ── Auth ────────────────────────────────────────────────────────────────

    public UserDto login(String username, String password) {
        Map<String, String> body = Map.of("username", username, "password", password);
        try {
            return rest.postForObject(baseUrl + "/api/auth/login", body, UserDto.class);
        } catch (Exception e) {
            return null;
        }
    }

    public List<UserDto> getUsers() {
        return getList("/api/auth/users", new ParameterizedTypeReference<>() {});
    }

    public void createUser(String username, String password, String email,
                           String roleName, Long storeId) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("username", username);
        body.put("password", password);
        body.put("email", email);
        body.put("roleName", roleName);
        if (storeId != null) body.put("storeId", storeId);
        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, jsonHeaders());
        ResponseEntity<String> resp = rest.exchange(
                baseUrl + "/api/auth/users", HttpMethod.POST, req, String.class);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException(resp.getBody());
        }
    }

    public void updateUser(Long id, String email, String roleName, Long storeId) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("email", email);
        body.put("roleName", roleName);
        body.put("storeId", storeId);
        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, jsonHeaders());
        rest.exchange(baseUrl + "/api/auth/users/" + id, HttpMethod.PUT, req, String.class);
    }

    public void toggleUserActive(Long userId) {
        rest.patchForObject(baseUrl + "/api/auth/users/" + userId + "/active",
                null, Void.class);
    }

    // ── Stores ──────────────────────────────────────────────────────────────

    public List<StoreDto> getStores() {
        return getList("/api/stores", new ParameterizedTypeReference<>() {});
    }

    public List<StoreDto> getActiveStores() {
        return getList("/api/stores?active=true", new ParameterizedTypeReference<>() {});
    }

    public StoreDto getStore(Long id) {
        try {
            return rest.getForObject(baseUrl + "/api/stores/" + id, StoreDto.class);
        } catch (Exception e) { return null; }
    }

    public StoreDto createStore(String name, String type, String address,
                                 String city, String postalCode) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("storeName", name); body.put("storeType", type);
        body.put("address", address); body.put("city", city);
        body.put("postalCode", postalCode);
        return rest.postForObject(baseUrl + "/api/stores", body, StoreDto.class);
    }

    public StoreDto updateStore(Long id, String name, String type, String address,
                                 String city, String postalCode) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("storeName", name); body.put("storeType", type);
        body.put("address", address); body.put("city", city);
        body.put("postalCode", postalCode);
        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, jsonHeaders());
        return rest.exchange(baseUrl + "/api/stores/" + id, HttpMethod.PUT, req, StoreDto.class).getBody();
    }

    public void toggleStoreActive(Long id) {
        rest.patchForObject(baseUrl + "/api/stores/" + id + "/active", null, Void.class);
    }

    // ── Categories ──────────────────────────────────────────────────────────

    public List<CategoryDto> getCategories(Long storeId) {
        String url = "/api/categories" + (storeId != null ? "?storeId=" + storeId : "");
        return getList(url, new ParameterizedTypeReference<>() {});
    }

    public CategoryDto createCategory(String name, Long storeId) {
        Map<String, Object> body = Map.of("categoryName", name, "storeId", storeId);
        return rest.postForObject(baseUrl + "/api/categories", body, CategoryDto.class);
    }

    public void toggleCategoryActive(Long id) {
        rest.patchForObject(baseUrl + "/api/categories/" + id + "/active", null, Void.class);
    }

    // ── Kiosks ──────────────────────────────────────────────────────────────

    public List<KioskDto> getKiosks(Long storeId) {
        String url = "/api/kiosks" + (storeId != null ? "?storeId=" + storeId : "");
        return getList(url, new ParameterizedTypeReference<>() {});
    }

    public KioskDto createKiosk(String name, String serial, String model, Long storeId) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("kioskName", name); body.put("serialNumber", serial);
        body.put("model", model);    body.put("storeId", storeId);
        return rest.postForObject(baseUrl + "/api/kiosks", body, KioskDto.class);
    }

    public void updateKioskStatus(Long id, String status) {
        rest.patchForObject(baseUrl + "/api/kiosks/" + id + "/status?status=" + status,
                null, Void.class);
    }

    public KioskDto updateKiosk(Long id, String name, String serial, String model,
                                 Long storeId, String status) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("kioskName", name); body.put("serialNumber", serial);
        body.put("model", model); body.put("storeId", storeId); body.put("status", status);
        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, jsonHeaders());
        return rest.exchange(baseUrl + "/api/kiosks/" + id, HttpMethod.PUT, req, KioskDto.class).getBody();
    }

    // ── Products ─────────────────────────────────────────────────────────────

    public List<ProductDto> getProducts(Long storeId, Long categoryId, String q) {
        StringBuilder url = new StringBuilder("/api/products?");
        if (storeId    != null) url.append("storeId=").append(storeId).append("&");
        if (categoryId != null) url.append("categoryId=").append(categoryId).append("&");
        if (q != null && !q.isBlank()) url.append("q=").append(q);
        return getList(url.toString(), new ParameterizedTypeReference<>() {});
    }

    public ProductDto createProduct(String name, String desc, java.math.BigDecimal price,
                                     String sku, String imageUrl, Long categoryId, List<Long> storeIds) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("productName", name); body.put("description", desc);
        body.put("price", price); body.put("sku", sku); body.put("imageUrl", imageUrl);
        body.put("categoryId", categoryId); body.put("storeIds", storeIds);
        return rest.postForObject(baseUrl + "/api/products", body, ProductDto.class);
    }

    public ProductDto updateProduct(Long id, String name, String desc, java.math.BigDecimal price,
                                     String sku, String imageUrl, Long categoryId, List<Long> storeIds) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("productName", name); body.put("description", desc);
        body.put("price", price); body.put("sku", sku); body.put("imageUrl", imageUrl);
        body.put("categoryId", categoryId); body.put("storeIds", storeIds);
        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, jsonHeaders());
        return rest.exchange(baseUrl + "/api/products/" + id, HttpMethod.PUT, req, ProductDto.class).getBody();
    }

    public void deactivateProduct(Long id) {
        rest.delete(baseUrl + "/api/products/" + id + "/deactivate");
    }

    // ── Orders ───────────────────────────────────────────────────────────────

    public List<OrderDto> getOrders(String status, Long storeId, String ref) {
        StringBuilder url = new StringBuilder("/api/orders?");
        if (status  != null && !status.equals("TODOS")) url.append("status=").append(status).append("&");
        if (storeId != null) url.append("storeId=").append(storeId).append("&");
        if (ref     != null && !ref.isBlank()) url.append("ref=").append(ref);
        return getList(url.toString(), new ParameterizedTypeReference<>() {});
    }

    public void updateOrderStatus(Long id, String status) {
        rest.patchForObject(baseUrl + "/api/orders/" + id + "/status?status=" + status,
                null, Void.class);
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private <T> List<T> getList(String path, ParameterizedTypeReference<List<T>> type) {
        try {
            ResponseEntity<List<T>> resp = rest.exchange(
                    baseUrl + path, HttpMethod.GET, null, type);
            return resp.getBody() != null ? resp.getBody() : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }
}
