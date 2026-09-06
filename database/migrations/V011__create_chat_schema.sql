IF SCHEMA_ID(N'chat') IS NULL
BEGIN
    EXEC(N'CREATE SCHEMA chat');
END;
GO

IF OBJECT_ID(N'chat.Conversation', N'U') IS NULL
BEGIN
    CREATE TABLE chat.Conversation (
        conversation_id int IDENTITY(1,1) NOT NULL,
        buyer_id int NOT NULL,
        seller_id int NOT NULL,
        buyer_unread_count int NOT NULL
            CONSTRAINT df_chat_conversation_buyer_unread_count DEFAULT (0),
        seller_unread_count int NOT NULL
            CONSTRAINT df_chat_conversation_seller_unread_count DEFAULT (0),
        last_message_id int NULL,
        latest_message_at datetime2 NULL,
        created_at datetime2 NOT NULL
            CONSTRAINT df_chat_conversation_created_at DEFAULT (sysdatetime()),
        updated_at datetime2 NOT NULL
            CONSTRAINT df_chat_conversation_updated_at DEFAULT (sysdatetime()),
        CONSTRAINT pk_chat_conversation PRIMARY KEY (conversation_id),
        CONSTRAINT uq_chat_conversation_buyer_seller UNIQUE (buyer_id, seller_id),
        CONSTRAINT ck_chat_conversation_unread_count
            CHECK (buyer_unread_count >= 0 AND seller_unread_count >= 0),
        CONSTRAINT fk_chat_conversation_buyer
            FOREIGN KEY (buyer_id) REFERENCES member.Member(member_id),
        CONSTRAINT fk_chat_conversation_seller
            FOREIGN KEY (seller_id) REFERENCES seller.Seller(seller_id)
    );
END;
GO

IF OBJECT_ID(N'chat.Message', N'U') IS NULL
BEGIN
    CREATE TABLE chat.Message (
        message_id int IDENTITY(1,1) NOT NULL,
        conversation_id int NOT NULL,
        sender_member_id int NOT NULL,
        sender_role varchar(20) NOT NULL,
        message_type varchar(20) NOT NULL
            CONSTRAINT df_chat_message_message_type DEFAULT ('TEXT'),
        content nvarchar(1000) NULL,
        image_url nvarchar(500) NULL,
        image_public_id nvarchar(255) NULL,
        product_id int NULL,
        sku_id int NULL,
        order_id int NULL,
        created_at datetime2 NOT NULL
            CONSTRAINT df_chat_message_created_at DEFAULT (sysdatetime()),
        CONSTRAINT pk_chat_message PRIMARY KEY (message_id),
        CONSTRAINT ck_chat_message_sender_role CHECK (sender_role IN ('BUYER', 'SELLER')),
        CONSTRAINT ck_chat_message_message_type CHECK (message_type IN ('TEXT', 'IMAGE', 'PRODUCT', 'ORDER')),
        CONSTRAINT ck_chat_message_has_content CHECK (
            content IS NOT NULL
            OR image_url IS NOT NULL
            OR product_id IS NOT NULL
            OR order_id IS NOT NULL
        ),
        CONSTRAINT fk_chat_message_conversation
            FOREIGN KEY (conversation_id) REFERENCES chat.Conversation(conversation_id),
        CONSTRAINT fk_chat_message_sender
            FOREIGN KEY (sender_member_id) REFERENCES member.Member(member_id),
        CONSTRAINT fk_chat_message_product
            FOREIGN KEY (product_id) REFERENCES catalog.Product(product_id),
        CONSTRAINT fk_chat_message_sku
            FOREIGN KEY (sku_id) REFERENCES catalog.ProductSku(sku_id),
        CONSTRAINT fk_chat_message_order
            FOREIGN KEY (order_id) REFERENCES sales.Orders(order_id)
    );
END;
GO

IF OBJECT_ID(N'chat.QuickResponseTemplate', N'U') IS NULL
BEGIN
    CREATE TABLE chat.QuickResponseTemplate (
        template_id int IDENTITY(1,1) NOT NULL,
        seller_id int NOT NULL,
        title nvarchar(50) NOT NULL,
        content nvarchar(1000) NOT NULL,
        created_at datetime2 NOT NULL
            CONSTRAINT df_chat_quick_response_created_at DEFAULT (sysdatetime()),
        updated_at datetime2 NOT NULL
            CONSTRAINT df_chat_quick_response_updated_at DEFAULT (sysdatetime()),
        CONSTRAINT pk_chat_quick_response_template PRIMARY KEY (template_id),
        CONSTRAINT ck_chat_quick_response_title CHECK (len(ltrim(rtrim(title))) > 0),
        CONSTRAINT ck_chat_quick_response_content CHECK (len(ltrim(rtrim(content))) > 0),
        CONSTRAINT fk_chat_quick_response_seller
            FOREIGN KEY (seller_id) REFERENCES seller.Seller(seller_id)
    );
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.foreign_keys
    WHERE name = N'fk_chat_conversation_last_message'
      AND parent_object_id = OBJECT_ID(N'chat.Conversation')
)
BEGIN
    ALTER TABLE chat.Conversation WITH CHECK
        ADD CONSTRAINT fk_chat_conversation_last_message
        FOREIGN KEY (last_message_id) REFERENCES chat.Message(message_id);
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'ix_chat_conversation_buyer_updated'
      AND object_id = OBJECT_ID(N'chat.Conversation')
)
BEGIN
    CREATE INDEX ix_chat_conversation_buyer_updated
        ON chat.Conversation(buyer_id, updated_at DESC, conversation_id DESC);
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'ix_chat_conversation_seller_updated'
      AND object_id = OBJECT_ID(N'chat.Conversation')
)
BEGIN
    CREATE INDEX ix_chat_conversation_seller_updated
        ON chat.Conversation(seller_id, updated_at DESC, conversation_id DESC);
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'ix_chat_message_conversation_created'
      AND object_id = OBJECT_ID(N'chat.Message')
)
BEGIN
    CREATE INDEX ix_chat_message_conversation_created
        ON chat.Message(conversation_id, created_at ASC, message_id ASC);
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'ix_chat_message_sender'
      AND object_id = OBJECT_ID(N'chat.Message')
)
BEGIN
    CREATE INDEX ix_chat_message_sender
        ON chat.Message(sender_member_id);
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'ix_chat_quick_response_seller_updated'
      AND object_id = OBJECT_ID(N'chat.QuickResponseTemplate')
)
BEGIN
    CREATE INDEX ix_chat_quick_response_seller_updated
        ON chat.QuickResponseTemplate(seller_id, updated_at DESC, template_id DESC);
END;
GO
