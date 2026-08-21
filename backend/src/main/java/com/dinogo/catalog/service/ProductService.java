package com.dinogo.catalog.service;

import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.dinogo.catalog.dto.ProductCreateRequest;
import com.dinogo.catalog.dto.ProductDetailResponse;
import com.dinogo.catalog.dto.ProductImageCreateRequest;
import com.dinogo.catalog.dto.ProductImageResponse;
import com.dinogo.catalog.dto.ProductImageSortUpdateRequest;
import com.dinogo.catalog.dto.ProductResponse;
import com.dinogo.catalog.dto.ProductSkuCreateRequest;
import com.dinogo.catalog.dto.ProductSkuResponse;
import com.dinogo.catalog.dto.ProductSkuUpdateRequest;
import com.dinogo.catalog.dto.ProductUpdateRequest;
import com.dinogo.catalog.entity.Brand;
import com.dinogo.catalog.entity.Product;
import com.dinogo.catalog.entity.ProductImage;
import com.dinogo.catalog.entity.ProductSku;
import com.dinogo.catalog.entity.Subcategory;
import com.dinogo.catalog.repository.BrandRepository;
import com.dinogo.catalog.repository.ProductImageRepository;
import com.dinogo.catalog.repository.ProductRepository;
import com.dinogo.catalog.repository.ProductSkuRepository;
import com.dinogo.catalog.repository.SubcategoryRepository;
import com.dinogo.seller.entity.Seller;
import com.dinogo.seller.repository.SellerRepository;

import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

        private final ProductRepository productRepository;
        private final SellerRepository sellerRepository;
        private final SubcategoryRepository subcategoryRepository;
        private final BrandRepository brandRepository;
        private final ProductSkuRepository productSkuRepository;
        private final ProductImageRepository productImageRepository;

        private ProductResponse toProductResponse(Product product) {

                BigDecimal minPrice = product.getSkus()
                                .stream()
                                .filter(sku -> sku.getStatus() == 1)
                                .filter(sku -> sku.getPrice() != null)
                                .map(ProductSku::getPrice)
                                .min(BigDecimal::compareTo)
                                .orElse(product.getBasePrice());

                BigDecimal maxPrice = product.getSkus()
                                .stream()
                                .filter(sku -> sku.getStatus() == 1)
                                .filter(sku -> sku.getPrice() != null)
                                .map(ProductSku::getPrice)
                                .max(BigDecimal::compareTo)
                                .orElse(product.getBasePrice());

                return new ProductResponse(
                                product.getProductId(),
                                product.getSeller().getSellerId(),
                                product.getSubcategory().getSubcategoryId(),
                                product.getBrand().getBrandId(),
                                product.getProductName(),
                                product.getDescription(),
                                product.getBasePrice(),
                                minPrice,
                                maxPrice,
                                product.getSkus()
                                                .stream()
                                                .filter(sku -> sku.getStatus() == 1)
                                                .mapToInt(ProductSku::getStock)
                                                .sum(),
                                product.getImages().isEmpty()
                                                ? null
                                                : product.getImages().get(0).getImageUrl(),
                                product.getStatus());
        }

        private ProductSkuResponse toProductSkuResponse(ProductSku sku) {

                return new ProductSkuResponse(
                                sku.getSkuId(),
                                sku.getProduct().getProductId(),
                                sku.getSpec1Name(),
                                sku.getSpec1Value(),
                                sku.getSpec2Name(),
                                sku.getSpec2Value(),
                                sku.getPrice(),
                                sku.getStock(),
                                sku.getStatus());
        }

        // 建立商品
        @Transactional
        public ProductResponse createProduct(ProductCreateRequest request) {

                // 目前先由前端傳 sellerId
                // 未來登入功能完成後，改成從登入身分取得
                Seller seller = sellerRepository
                                .findById(request.getSellerId())
                                .orElseThrow(() -> new RuntimeException("找不到賣家"));

                Subcategory subcategory = subcategoryRepository
                                .findById(request.getSubcategoryId())
                                .orElseThrow(() -> new RuntimeException("找不到子分類"));

                Brand brand = brandRepository
                                .findById(request.getBrandId())
                                .orElseThrow(() -> new RuntimeException("找不到品牌"));

                // =====================
                // 1. 建立 Product
                // =====================

                Product product = new Product();

                product.setSeller(seller);
                product.setSubcategory(subcategory);
                product.setBrand(brand);
                product.setProductName(request.getProductName());
                product.setDescription(request.getDescription());
                product.setBasePrice(request.getBasePrice());

                // 沒傳 status 就預設草稿
                product.setStatus(
                                request.getStatus() != null
                                                ? request.getStatus()
                                                : (byte) 0);

                Product savedProduct = productRepository.save(product);

                // =====================
                // 2. 建立 ProductSku
                // =====================

                if (request.getSkus() != null) {
                        for (ProductSkuCreateRequest skuCreateRequest : request.getSkus()) {

                                ProductSku sku = ProductSku.builder()
                                                .product(product)
                                                .spec1Name(skuCreateRequest.getSpec1Name())
                                                .spec1Value(skuCreateRequest.getSpec1Value())
                                                .spec2Name(skuCreateRequest.getSpec2Name())
                                                .spec2Value(skuCreateRequest.getSpec2Value())
                                                .price(skuCreateRequest.getPrice())
                                                .stock(skuCreateRequest.getStock())
                                                .status((byte) 1)
                                                .build();

                                ProductSku savedSku = productSkuRepository.save(sku);
                                savedProduct.getSkus().add(savedSku);
                        }
                }

                // =====================
                // 3. 建立 ProductImage
                // =====================
                if (request.getImages() != null) {
                        for (int i = 0; i < request.getImages().size(); i++) {

                                ProductImageCreateRequest imageRequest = request.getImages().get(i);

                                ProductImage image = ProductImage.builder()
                                                .product(product)
                                                .imageUrl(imageRequest.getImageUrl())
                                                .sortOrder(imageRequest.getSortOrder())
                                                .isMain(i == 0)
                                                .build();

                                ProductImage savedImg = productImageRepository.save(image);
                                savedProduct.getImages().add(savedImg);
                        }
                }

                savedProduct.getSkus().forEach(sku -> {
                        System.out.println(
                                        "skuId = " + sku.getSkuId()
                                                        + ", stock = " + sku.getStock()
                                                        + ", status = " + sku.getStatus());
                });

                System.out.println("圖片數量 = " + savedProduct.getImages().size());

                savedProduct.getImages().forEach(image -> {
                        System.out.println(
                                        "imageId = " + image.getImageId()
                                                        + ", imageUrl = " + image.getImageUrl()
                                                        + ", isMain = " + image.getIsMain());
                });

                return toProductResponse(savedProduct);
        }

        // 上架商品
        public ProductResponse publishProduct(Integer productId) {

                Integer sellerId = 1; // 暫時寫死，之後改成登入賣家

                Product product = productRepository
                                .findBySeller_SellerIdAndProductId(sellerId, productId)
                                .orElseThrow(() -> new IllegalArgumentException("找不到此賣家的商品"));

                // 草稿 0 或下架 2 才能上架
                if (product.getStatus() != 0 && product.getStatus() != 2) {
                        throw new IllegalArgumentException("目前狀態不可上架");
                }

                product.setStatus((byte) 1);

                Product savedProduct = productRepository.save(product);

                return toProductResponse(savedProduct);
        }

        // 下架商品
        public ProductResponse unpublishProduct(Integer productId) {

                Integer sellerId = 1; // 暫時寫死，之後改成登入賣家

                Product product = productRepository
                                .findBySeller_SellerIdAndProductId(sellerId, productId)
                                .orElseThrow(() -> new IllegalArgumentException("找不到此賣家的商品"));

                // 只有上架 1 才能下架
                if (product.getStatus() != 1) {
                        throw new IllegalArgumentException("只有上架中的商品可以下架");
                }

                product.setStatus((byte) 2);

                Product savedProduct = productRepository.save(product);

                return toProductResponse(savedProduct);
        }

        // 讀取商品列表
        public Page<ProductResponse> getProducts(
                        String keyword,
                        Integer categoryId,
                        Integer subcategoryId,
                        Integer brandId,
                        Integer page,
                        Integer size,
                        String sort) {

                Specification<Product> spec = (root, query, cb) -> {

                        List<Predicate> predicates = new ArrayList<>();

                        // 買家只能看到已上架商品
                        predicates.add(
                                        cb.equal(root.get("status"), (byte) 1));

                        if (keyword != null && !keyword.isBlank()) {
                                predicates.add(
                                                cb.like(
                                                                cb.lower(root.get("productName")),
                                                                "%" + keyword.toLowerCase() + "%"));
                        }
                        // 大分類
                        if (categoryId != null) {
                                predicates.add(
                                                cb.equal(
                                                                root.join("subcategory")
                                                                                .join("category")
                                                                                .get("categoryId"),
                                                                categoryId));
                        }

                        // 子分類
                        if (subcategoryId != null) {
                                predicates.add(
                                                cb.equal(
                                                                root.join("subcategory")
                                                                                .get("subcategoryId"),
                                                                subcategoryId));
                        }

                        // 品牌
                        if (brandId != null) {
                                predicates.add(
                                                cb.equal(
                                                                root.join("brand")
                                                                                .get("brandId"),
                                                                brandId));
                        }

                        return cb.and(
                                        predicates.toArray(new Predicate[0]));
                };

                Sort sorting = switch (sort == null ? "" : sort) {
                        case "priceAsc" ->
                                Sort.by(Sort.Direction.ASC, "minSkuPrice");

                        case "priceDesc" ->
                                Sort.by(Sort.Direction.DESC, "minSkuPrice");

                        case "salesDesc" ->
                                Sort.by(Sort.Direction.DESC, "soldCount");

                        case "newest" ->
                                Sort.by(Sort.Direction.DESC, "createdAt");

                        default ->
                                Sort.by(Sort.Direction.DESC, "productId");
                };

                Pageable pageable = PageRequest.of(page, size, sorting);

                Page<Product> products = productRepository.findAll(spec, pageable);

                return products.map(this::toProductResponse);
        }

        // 讀取商品詳情
        public ProductDetailResponse getProductDetail(Integer productId) {

                Product product = productRepository.findById(productId)
                                .orElseThrow(() -> new RuntimeException("找不到商品"));

                List<ProductImageResponse> images = product.getImages()
                                .stream()
                                .map(image -> ProductImageResponse.builder()
                                                .imageId(image.getImageId())
                                                .imageUrl(image.getImageUrl())
                                                .sortOrder(image.getSortOrder())
                                                .isMain(image.getIsMain())
                                                .build())
                                .toList();

                List<ProductSkuResponse> skus = product.getSkus()
                                .stream()
                                .filter(sku -> sku.getStatus() == 1)
                                .map(sku -> ProductSkuResponse.builder()
                                                .skuId(sku.getSkuId())
                                                .spec1Name(sku.getSpec1Name())
                                                .spec1Value(sku.getSpec1Value())
                                                .spec2Name(sku.getSpec2Name())
                                                .spec2Value(sku.getSpec2Value())
                                                .price(sku.getPrice())
                                                .stock(sku.getStock())
                                                .status(sku.getStatus())
                                                .build())
                                .toList();

                return ProductDetailResponse.builder()
                                .productId(product.getProductId())
                                .productName(product.getProductName())
                                .description(product.getDescription())
                                .basePrice(product.getBasePrice())
                                .status(product.getStatus())
                                .viewCount(product.getViewCount())
                                .soldCount(product.getSoldCount())

                                .brandId(product.getBrand().getBrandId())
                                .brandName(product.getBrand().getBrandName())

                                .subcategoryId(product.getSubcategory().getSubcategoryId())
                                .subcategoryName(product.getSubcategory().getSubcategoryName())

                                .categoryId(product.getSubcategory().getCategory().getCategoryId())
                                .categoryName(product.getSubcategory().getCategory().getCategoryName())

                                .images(images)
                                .skus(skus)

                                .build();
        }

        // 賣家讀取商品詳情
        // 包含已停用的 SKU，供編輯商品使用
        public ProductDetailResponse getSellerProductDetail(Integer productId) {

                Product product = productRepository.findById(productId)
                                .orElseThrow(() -> new RuntimeException("找不到商品"));

                List<ProductImageResponse> images = product.getImages()
                                .stream()
                                .map(image -> ProductImageResponse.builder()
                                                .imageId(image.getImageId())
                                                .imageUrl(image.getImageUrl())
                                                .sortOrder(image.getSortOrder())
                                                .isMain(image.getIsMain())
                                                .build())
                                .toList();

                // 賣家需要看到全部 SKU，包括 status = 0
                List<ProductSkuResponse> skus = product.getSkus()
                                .stream()
                                .map(sku -> ProductSkuResponse.builder()
                                                .skuId(sku.getSkuId())
                                                .spec1Name(sku.getSpec1Name())
                                                .spec1Value(sku.getSpec1Value())
                                                .spec2Name(sku.getSpec2Name())
                                                .spec2Value(sku.getSpec2Value())
                                                .price(sku.getPrice())
                                                .stock(sku.getStock())
                                                .status(sku.getStatus())
                                                .build())
                                .toList();

                return ProductDetailResponse.builder()
                                .productId(product.getProductId())
                                .productName(product.getProductName())
                                .description(product.getDescription())
                                .basePrice(product.getBasePrice())
                                .status(product.getStatus())
                                .viewCount(product.getViewCount())
                                .soldCount(product.getSoldCount())

                                .brandId(product.getBrand().getBrandId())
                                .brandName(product.getBrand().getBrandName())

                                .subcategoryId(product.getSubcategory().getSubcategoryId())
                                .subcategoryName(product.getSubcategory().getSubcategoryName())

                                .categoryId(product.getSubcategory().getCategory().getCategoryId())
                                .categoryName(product.getSubcategory().getCategory().getCategoryName())

                                .images(images)
                                .skus(skus)

                                .build();
        }

        // 修改商品
        public ProductResponse updateProduct(
                        Integer productId,
                        ProductUpdateRequest request) {

                Product product = productRepository.findById(productId)
                                .orElseThrow(() -> new RuntimeException("找不到商品：" + productId));

                // 修改子分類
                if (request.getSubcategoryId() != null) {
                        Subcategory subcategory = subcategoryRepository
                                        .findById(request.getSubcategoryId())
                                        .orElseThrow(() -> new RuntimeException(
                                                        "找不到子分類：" + request.getSubcategoryId()));

                        product.setSubcategory(subcategory);
                }

                // 修改品牌
                if (request.getBrandId() != null) {
                        Brand brand = brandRepository
                                        .findById(request.getBrandId())
                                        .orElseThrow(() -> new RuntimeException(
                                                        "找不到品牌：" + request.getBrandId()));

                        product.setBrand(brand);
                }

                // 修改商品名稱
                if (request.getProductName() != null) {
                        product.setProductName(request.getProductName());
                }

                // 修改商品描述
                if (request.getDescription() != null) {
                        product.setDescription(request.getDescription());
                }

                // 修改基本價格
                if (request.getBasePrice() != null) {
                        product.setBasePrice(request.getBasePrice());
                }

                Product updatedProduct = productRepository.save(product);

                return toProductResponse(updatedProduct);
        }

        // 修改商品Sku
        public ProductSkuResponse updateSku(
                        Integer productId,
                        Integer skuId,
                        ProductSkuUpdateRequest request) {

                ProductSku sku = productSkuRepository.findById(skuId)
                                .orElseThrow(() -> new RuntimeException("找不到 SKU：" + skuId));

                // 確認 SKU 真的是這個商品的
                if (!sku.getProduct().getProductId().equals(productId)) {
                        throw new RuntimeException("此 SKU 不屬於指定商品");
                }

                if (request.getSpec1Name() != null) {
                        sku.setSpec1Name(request.getSpec1Name());
                }

                if (request.getSpec1Value() != null) {
                        sku.setSpec1Value(request.getSpec1Value());
                }

                if (request.getSpec2Name() != null) {
                        sku.setSpec2Name(request.getSpec2Name());
                }

                if (request.getSpec2Value() != null) {
                        sku.setSpec2Value(request.getSpec2Value());
                }

                if (request.getPrice() != null) {
                        sku.setPrice(request.getPrice());
                }

                if (request.getStock() != null) {
                        sku.setStock(request.getStock());
                }

                if (request.getStatus() != null) {
                        sku.setStatus(request.getStatus());
                }

                ProductSku updatedSku = productSkuRepository.save(sku);

                return toProductSkuResponse(updatedSku);
        }

        // 批次新增商品 SKU
        @Transactional
        public List<ProductSkuResponse> createSkus(
                        Integer productId,
                        List<ProductSkuCreateRequest> requests) {

                Product product = productRepository.findById(productId)
                                .orElseThrow(() -> new RuntimeException("找不到商品：" + productId));

                if (requests == null || requests.isEmpty()) {
                        throw new RuntimeException("至少需要一筆 SKU");
                }

                List<ProductSku> existingSkus = productSkuRepository.findByProductProductId(productId);

                // 先檢查整批資料
                for (int i = 0; i < requests.size(); i++) {

                        ProductSkuCreateRequest request = requests.get(i);

                        // 檢查是否與資料庫既有 SKU 重複
                        boolean duplicateExisting = existingSkus.stream().anyMatch(sku -> Objects.equals(
                                        sku.getSpec1Value(),
                                        request.getSpec1Value())
                                        &&
                                        Objects.equals(
                                                        sku.getSpec2Value(),
                                                        request.getSpec2Value()));

                        if (duplicateExisting) {
                                throw new RuntimeException(
                                                "SKU 已存在："
                                                                + request.getSpec1Value()
                                                                + " / "
                                                                + request.getSpec2Value());
                        }

                        // 檢查這次送進來的 batch 本身是否重複
                        for (int j = i + 1; j < requests.size(); j++) {

                                ProductSkuCreateRequest other = requests.get(j);

                                boolean duplicateInBatch = Objects.equals(
                                                request.getSpec1Value(),
                                                other.getSpec1Value())
                                                &&
                                                Objects.equals(
                                                                request.getSpec2Value(),
                                                                other.getSpec2Value());

                                if (duplicateInBatch) {
                                        throw new RuntimeException(
                                                        "批次資料中有重複 SKU："
                                                                        + request.getSpec1Value()
                                                                        + " / "
                                                                        + request.getSpec2Value());
                                }
                        }
                }

                // 全部驗證成功後才開始新增
                List<ProductSkuResponse> result = new ArrayList<>();

                for (ProductSkuCreateRequest request : requests) {

                        ProductSku sku = new ProductSku();

                        sku.setProduct(product);

                        sku.setSpec1Name(request.getSpec1Name());
                        sku.setSpec1Value(request.getSpec1Value());

                        sku.setSpec2Name(request.getSpec2Name());
                        sku.setSpec2Value(request.getSpec2Value());

                        sku.setPrice(request.getPrice());
                        sku.setStock(request.getStock());
                        sku.setStatus((byte) 1);

                        ProductSku savedSku = productSkuRepository.save(sku);

                        result.add(toProductSkuResponse(savedSku));
                }

                return result;
        }

        public ProductSkuResponse disableSku(
                        Integer productId,
                        Integer skuId) {

                ProductSku sku = productSkuRepository.findById(skuId)
                                .orElseThrow(() -> new RuntimeException("找不到 SKU：" + skuId));

                if (!sku.getProduct().getProductId().equals(productId)) {
                        throw new RuntimeException("此 SKU 不屬於指定商品");
                }

                sku.setStatus((byte) 0);

                ProductSku updatedSku = productSkuRepository.save(sku);

                return toProductSkuResponse(updatedSku);
        }

        // 修改商品主圖
        @Transactional
        public ProductImageResponse updateMainImage(
                        Integer productId,
                        Integer imageId) {

                ProductImage targetImage = productImageRepository.findById(imageId)
                                .orElseThrow(() -> new RuntimeException("找不到圖片：" + imageId));

                // 確認圖片屬於指定商品
                if (!targetImage.getProduct()
                                .getProductId()
                                .equals(productId)) {

                        throw new RuntimeException(
                                        "此圖片不屬於商品：" + productId);
                }

                List<ProductImage> images = productImageRepository.findByProductProductId(productId);

                // 1. 先把目前所有主圖取消
                for (ProductImage image : images) {
                        if (Boolean.TRUE.equals(image.getIsMain())) {
                                image.setIsMain(false);
                        }
                }

                // 2. 強制先寫入 DB
                productImageRepository.saveAll(images);
                productImageRepository.flush();

                // 3. 再把指定圖片設為主圖
                targetImage.setIsMain(true);

                ProductImage savedImage = productImageRepository.saveAndFlush(targetImage);

                return ProductImageResponse.builder()
                                .imageId(savedImage.getImageId())
                                .imageUrl(savedImage.getImageUrl())
                                .sortOrder(savedImage.getSortOrder())
                                .isMain(savedImage.getIsMain())
                                .build();
        }

        // 修改商品圖片排序
        @Transactional
        public List<ProductImageResponse> updateImageSort(
                        Integer productId,
                        List<ProductImageSortUpdateRequest> requests) {

                if (requests == null || requests.isEmpty()) {
                        throw new RuntimeException("圖片排序資料不可為空");
                }

                List<ProductImage> images = new ArrayList<>();

                // ① 先把所有 request 對應的圖片查出來並驗證
                for (ProductImageSortUpdateRequest request : requests) {

                        ProductImage image = productImageRepository
                                        .findById(request.getImageId())
                                        .orElseThrow(() -> new RuntimeException(
                                                        "找不到圖片：" + request.getImageId()));

                        // 確認圖片屬於這個商品
                        if (!image.getProduct()
                                        .getProductId()
                                        .equals(productId)) {

                                throw new RuntimeException(
                                                "圖片不屬於商品：" + productId);
                        }

                        // sortOrder 從 1 開始
                        if (request.getSortOrder() == null ||
                                        request.getSortOrder() < 1) {

                                throw new RuntimeException(
                                                "圖片排序必須大於等於 1");
                        }

                        images.add(image);
                }

                // ② 第一階段：
                // 先全部改成暫時、不會與正式排序衝突的值
                for (int i = 0; i < images.size(); i++) {

                        ProductImage image = images.get(i);

                        // 使用大數字當暫存 sortOrder
                        image.setSortOrder(10000 + i);
                }

                productImageRepository.saveAll(images);

                // 強制先寫進 DB
                productImageRepository.flush();

                // ③ 第二階段：
                // 再設定真正的 sortOrder
                for (int i = 0; i < requests.size(); i++) {

                        ProductImageSortUpdateRequest request = requests.get(i);
                        ProductImage image = images.get(i);

                        image.setSortOrder(request.getSortOrder());
                }

                productImageRepository.saveAll(images);
                productImageRepository.flush();

                // ④ 回傳結果
                return images.stream()
                                .map(image -> ProductImageResponse.builder()
                                                .imageId(image.getImageId())
                                                .imageUrl(image.getImageUrl())
                                                .sortOrder(image.getSortOrder())
                                                .isMain(image.getIsMain())
                                                .build())
                                .toList();
        }

        @Transactional
        public void deleteImage(
                        Integer productId,
                        Integer imageId) {

                ProductImage image = productImageRepository.findById(imageId)
                                .orElseThrow(() -> new RuntimeException("找不到圖片：" + imageId));

                if (!image.getProduct()
                                .getProductId()
                                .equals(productId)) {

                        throw new RuntimeException(
                                        "圖片不屬於商品：" + productId);
                }

                // 主圖先禁止刪除，避免商品沒有主圖
                if (Boolean.TRUE.equals(image.getIsMain())) {
                        throw new RuntimeException(
                                        "主圖不可直接刪除，請先設定其他圖片為主圖");
                }

                productImageRepository.delete(image);
        }

        @Transactional
        public List<ProductImageResponse> uploadProductImages(
                        Integer productId,
                        MultipartFile[] files) {

                Product product = productRepository.findById(productId)
                                .orElseThrow(() -> new RuntimeException(
                                                "找不到商品：" + productId));

                if (files == null || files.length == 0) {
                        throw new RuntimeException("請至少上傳一張圖片");
                }

                // uploads/products/
                Path uploadPath = Paths.get(
                                "uploads",
                                "products").toAbsolutePath().normalize();

                try {
                        Files.createDirectories(uploadPath);
                } catch (IOException e) {
                        throw new RuntimeException(
                                        "建立圖片資料夾失敗",
                                        e);
                }

                // 找目前最大的 sortOrder
                List<ProductImage> existingImages = productImageRepository
                                .findByProductProductId(productId);

                int maxSortOrder = existingImages.stream()
                                .map(ProductImage::getSortOrder)
                                .filter(Objects::nonNull)
                                .max(Integer::compareTo)
                                .orElse(0);

                List<ProductImageResponse> responses = new ArrayList<>();

                for (MultipartFile file : files) {

                        if (file.isEmpty()) {
                                continue;
                        }

                        // 只接受圖片
                        String contentType = file.getContentType();

                        if (contentType == null ||
                                        !contentType.startsWith("image/")) {

                                throw new RuntimeException(
                                                "只能上傳圖片檔案");
                        }

                        // 取得副檔名
                        String originalFilename = file.getOriginalFilename();

                        String extension = "";

                        if (originalFilename != null &&
                                        originalFilename.contains(".")) {

                                extension = originalFilename.substring(
                                                originalFilename.lastIndexOf("."));
                        }

                        // UUID 避免檔名重複
                        String filename = UUID.randomUUID() + extension;

                        Path targetPath = uploadPath.resolve(filename);

                        try {
                                Files.copy(
                                                file.getInputStream(),
                                                targetPath,
                                                StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                                throw new RuntimeException(
                                                "圖片儲存失敗",
                                                e);
                        }

                        maxSortOrder++;

                        ProductImage image = ProductImage.builder()
                                        .product(product)
                                        .imageUrl(
                                                        "/uploads/products/" + filename)
                                        .sortOrder(maxSortOrder)
                                        .isMain(false)
                                        .build();

                        ProductImage saved = productImageRepository.save(image);

                        responses.add(
                                        ProductImageResponse.builder()
                                                        .imageId(saved.getImageId())
                                                        .imageUrl(saved.getImageUrl())
                                                        .sortOrder(saved.getSortOrder())
                                                        .isMain(saved.getIsMain())
                                                        .build());
                }

                return responses;
        }
}