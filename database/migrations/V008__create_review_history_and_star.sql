-- //review-start，總共1次修改，第1次//
/*
 * V008 Review schema migration for SQL Server.
 * 可安全套用於 schema／資料表已存在的環境；既有 review 資料不會被刪除。
 */
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;
GO

IF SCHEMA_ID(N'review') IS NULL
    EXEC(N'CREATE SCHEMA review AUTHORIZATION dbo');
GO

IF OBJECT_ID(N'review.history', N'U') IS NULL
BEGIN
    CREATE TABLE review.history
    (
        history_id int IDENTITY(1,1)
            CONSTRAINT PK_review_history PRIMARY KEY,
        seller_id int NOT NULL,
        member_id int NOT NULL,
        order_id int NOT NULL,
        order_no nvarchar(30) NOT NULL,
        CONSTRAINT UQ_review_history_order UNIQUE (order_id)
    );
END;
GO

-- 舊版 history 若沒有 order_no，先以可追蹤的 legacy 值補資料，再改為 NOT NULL。
IF COL_LENGTH(N'review.history', N'order_no') IS NULL
BEGIN
    ALTER TABLE review.history ADD order_no nvarchar(30) NULL;
END;
GO

UPDATE review.history
SET order_no = CONCAT(N'LEGACY-', order_id)
WHERE order_no IS NULL;
ALTER TABLE review.history ALTER COLUMN order_no nvarchar(30) NOT NULL;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.key_constraints
    WHERE parent_object_id = OBJECT_ID(N'review.history')
      AND [type] = N'UQ'
      AND [name] = N'UQ_review_history_order
)
BEGIN
    ALTER TABLE review.history
        ADD CONSTRAINT UQ_review_history_order UNIQUE (order_id);
END;
GO

IF OBJECT_ID(N'review.star', N'U') IS NULL
BEGIN
    CREATE TABLE review.star
    (
        star_id int IDENTITY(1,1)
            CONSTRAINT PK_review_star PRIMARY KEY,
        history_id int NOT NULL,
        order_item_id int NOT NULL,
        product_id int NOT NULL,
        product_name nvarchar(100) NOT NULL,
        image_url nvarchar(500) NULL,
        base_price decimal(12,2) NOT NULL,
        img_one varbinary(max) NULL,
        img_two varbinary(max) NULL,
        img_three varbinary(max) NULL,
        feedback nvarchar(500) NULL,
        five_star int NULL,
        version bigint NOT NULL
            CONSTRAINT DF_review_star_version DEFAULT 0,
        star_upd_at datetime2(7) NOT NULL
            CONSTRAINT DF_review_star_updated_at DEFAULT SYSDATETIME(),
        review_priority AS
        (
            CASE
                WHEN feedback IS NOT NULL
                     AND LTRIM(RTRIM(feedback)) <> N''
                     AND
                     (
                         (img_one IS NOT NULL AND DATALENGTH(img_one) > 0)
                         OR (img_two IS NOT NULL AND DATALENGTH(img_two) > 0)
                         OR (img_three IS NOT NULL AND DATALENGTH(img_three) > 0)
                     )
                    THEN CONVERT(tinyint, 2)
                WHEN (feedback IS NOT NULL AND LTRIM(RTRIM(feedback)) <> N'')
                     OR (img_one IS NOT NULL AND DATALENGTH(img_one) > 0)
                     OR (img_two IS NOT NULL AND DATALENGTH(img_two) > 0)
                     OR (img_three IS NOT NULL AND DATALENGTH(img_three) > 0)
                    THEN CONVERT(tinyint, 1)
                ELSE CONVERT(tinyint, 0)
            END
        ) PERSISTED,
        CONSTRAINT CK_review_star_five_star
            CHECK (five_star IS NULL OR five_star BETWEEN 1 AND 5),
        CONSTRAINT CK_review_star_unreviewed_content
            CHECK
            (
                five_star IS NOT NULL
                OR
                (
                    feedback IS NULL
                    AND img_one IS NULL
                    AND img_two IS NULL
                    AND img_three IS NULL
                )
            ),
        CONSTRAINT FK_review_star_history
            FOREIGN KEY (history_id)
            REFERENCES review.history(history_id)
            ON DELETE CASCADE,
        CONSTRAINT UQ_review_star_history_order_item
            UNIQUE (history_id, order_item_id)
    );
END;
GO

-- 舊版索引包含 image_url；先移除，欄位調整完成後再建立。
IF EXISTS
(
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'review.star')
      AND [name] = N'IX_review_star_product_keyset'
)
    DROP INDEX IX_review_star_product_keyset ON review.star;
GO

IF COL_LENGTH(N'review.star', N'image_url') IS NOT NULL
    ALTER TABLE review.star ALTER COLUMN image_url nvarchar(500) NULL;
GO

IF COL_LENGTH(N'review.star', N'product_name') IS NOT NULL
    ALTER TABLE review.star ALTER COLUMN product_name nvarchar(100) NOT NULL;
GO

IF COL_LENGTH(N'review.star', N'base_price') IS NOT NULL
    ALTER TABLE review.star ALTER COLUMN base_price decimal(12,2) NOT NULL;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.default_constraints dc
    INNER JOIN sys.columns c
        ON c.object_id = dc.parent_object_id
       AND c.column_id = dc.parent_column_id
    WHERE dc.parent_object_id = OBJECT_ID(N'review.star')
      AND c.[name] = N'version'
)
BEGIN
    ALTER TABLE review.star
        ADD CONSTRAINT DF_review_star_version DEFAULT 0 FOR version;
END;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.default_constraints dc
    INNER JOIN sys.columns c
        ON c.object_id = dc.parent_object_id
       AND c.column_id = dc.parent_column_id
    WHERE dc.parent_object_id = OBJECT_ID(N'review.star')
      AND c.[name] = N'star_upd_at'
)
BEGIN
    ALTER TABLE review.star
        ADD CONSTRAINT DF_review_star_updated_at DEFAULT SYSDATETIME() FOR star_upd_at;
END;
GO

IF COL_LENGTH(N'review.star', N'review_priority') IS NULL
BEGIN
    EXEC(N'ALTER TABLE review.star
        ADD review_priority AS
        (
            CASE
                WHEN feedback IS NOT NULL
                     AND LTRIM(RTRIM(feedback)) <> N''''
                     AND
                     (
                         (img_one IS NOT NULL AND DATALENGTH(img_one) > 0)
                         OR (img_two IS NOT NULL AND DATALENGTH(img_two) > 0)
                         OR (img_three IS NOT NULL AND DATALENGTH(img_three) > 0)
                     )
                    THEN CONVERT(tinyint, 2)
                WHEN (feedback IS NOT NULL AND LTRIM(RTRIM(feedback)) <> N'''')
                     OR (img_one IS NOT NULL AND DATALENGTH(img_one) > 0)
                     OR (img_two IS NOT NULL AND DATALENGTH(img_two) > 0)
                     OR (img_three IS NOT NULL AND DATALENGTH(img_three) > 0)
                    THEN CONVERT(tinyint, 1)
                ELSE CONVERT(tinyint, 0)
            END
        ) PERSISTED');
END;
GO

IF NOT EXISTS
(
    SELECT 1 FROM sys.foreign_keys
    WHERE parent_object_id = OBJECT_ID(N'review.star')
      AND [name] = N'FK_review_star_history'
)
BEGIN
    ALTER TABLE review.star WITH CHECK
        ADD CONSTRAINT FK_review_star_history
        FOREIGN KEY (history_id)
        REFERENCES review.history(history_id)
        ON DELETE CASCADE;
END;
GO

IF NOT EXISTS
(
    SELECT 1 FROM sys.key_constraints
    WHERE parent_object_id = OBJECT_ID(N'review.star')
      AND [type] = N'UQ'
      AND [name] = N'UQ_review_star_history_order_item'
)
BEGIN
    ALTER TABLE review.star
        ADD CONSTRAINT UQ_review_star_history_order_item
        UNIQUE (history_id, order_item_id);
END;
GO

IF NOT EXISTS
(
    SELECT 1 FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID(N'review.star')
      AND [name] = N'CK_review_star_five_star'
)
BEGIN
    ALTER TABLE review.star WITH CHECK
        ADD CONSTRAINT CK_review_star_five_star
        CHECK (five_star IS NULL OR five_star BETWEEN 1 AND 5);
END;
GO

IF NOT EXISTS
(
    SELECT 1 FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID(N'review.star')
      AND [name] = N'CK_review_star_unreviewed_content'
)
BEGIN
    ALTER TABLE review.star WITH CHECK
        ADD CONSTRAINT CK_review_star_unreviewed_content
        CHECK
        (
            five_star IS NOT NULL
            OR
            (
                feedback IS NULL
                AND img_one IS NULL
                AND img_two IS NULL
                AND img_three IS NULL
            )
        );
END;
GO

IF NOT EXISTS
(
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'review.star')
      AND [name] = N'IX_review_star_product_keyset'
)
BEGIN
    CREATE INDEX IX_review_star_product_keyset
        ON review.star
        (
            product_id,
            review_priority DESC,
            star_upd_at DESC,
            star_id DESC
        )
        INCLUDE
        (
            history_id,
            order_item_id,
            product_name,
            image_url,
            base_price,
            five_star,
            feedback
        )
        WHERE five_star IS NOT NULL;
END;
GO
-- //review-end，總共1次修改，第1次//

