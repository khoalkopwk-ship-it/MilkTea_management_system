-- =========================================================
-- 002-inventory.sql
-- Schema kho cho hệ thống MilkTea
-- SQL Server
-- =========================================================

-- 1. MATERIALS
CREATE TABLE materials (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name NVARCHAR(150) NOT NULL,
    type VARCHAR(20) NOT NULL,
    unit VARCHAR(30) NOT NULL,
    alert_threshold DECIMAL(18,3) NOT NULL DEFAULT 0,
    active BIT NOT NULL DEFAULT 1,

    CONSTRAINT chk_material_type
        CHECK (type IN ('THO', 'SOCHE')),

    CONSTRAINT chk_material_alert_threshold
        CHECK (alert_threshold >= 0)
);

-- 2. STOCKS
CREATE TABLE stocks (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    branch_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    location VARCHAR(20) NOT NULL,
    quantity DECIMAL(18,3) NOT NULL DEFAULT 0,

    CONSTRAINT fk_stock_branch
        FOREIGN KEY (branch_id)
        REFERENCES branches(id),

    CONSTRAINT fk_stock_material
        FOREIGN KEY (material_id)
        REFERENCES materials(id),

    CONSTRAINT chk_stock_location
        CHECK (location IN ('KHO', 'BEP')),

    CONSTRAINT chk_stock_quantity
        CHECK (quantity >= 0),

    CONSTRAINT uq_stock_branch_material_location
        UNIQUE (branch_id, material_id, location)
);

-- 3. STOCK MOVEMENTS
CREATE TABLE stock_movements (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    branch_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    location VARCHAR(20) NOT NULL,
    movement_type VARCHAR(50) NOT NULL,
    quantity DECIMAL(18,3) NOT NULL,
    reference_type VARCHAR(50) NULL,
    reference_id BIGINT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT fk_stock_movement_branch
        FOREIGN KEY (branch_id)
        REFERENCES branches(id),

    CONSTRAINT fk_stock_movement_material
        FOREIGN KEY (material_id)
        REFERENCES materials(id),

    CONSTRAINT chk_stock_movement_location
        CHECK (location IN ('KHO', 'BEP')),

    CONSTRAINT chk_stock_movement_type
        CHECK (
            movement_type IN (
                'IMPORT',
                'ISSUE_OUT',
                'ISSUE_IN',
                'PREPARATION_INPUT',
                'PREPARATION_OUTPUT',
                'CONSUMPTION'
            )
        )
);

-- 4. IMPORT RECEIPTS
CREATE TABLE import_receipts (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    branch_id BIGINT NOT NULL,
    receipt_code VARCHAR(50) NOT NULL UNIQUE,
    created_by BIGINT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    note NVARCHAR(500) NULL,

    CONSTRAINT fk_import_receipt_branch
        FOREIGN KEY (branch_id)
        REFERENCES branches(id)
);

-- 5. IMPORT RECEIPT ITEMS
CREATE TABLE import_receipt_items (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    import_receipt_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    quantity DECIMAL(18,3) NOT NULL,

    CONSTRAINT fk_import_item_receipt
        FOREIGN KEY (import_receipt_id)
        REFERENCES import_receipts(id),

    CONSTRAINT fk_import_item_material
        FOREIGN KEY (material_id)
        REFERENCES materials(id),

    CONSTRAINT chk_import_item_quantity
        CHECK (quantity > 0)
);

-- 6. STOCK ISSUES
CREATE TABLE stock_issues (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    branch_id BIGINT NOT NULL,
    issue_code VARCHAR(50) NOT NULL UNIQUE,
    created_by BIGINT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    note NVARCHAR(500) NULL,

    CONSTRAINT fk_stock_issue_branch
        FOREIGN KEY (branch_id)
        REFERENCES branches(id)
);

-- 7. STOCK ISSUE ITEMS
CREATE TABLE stock_issue_items (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    stock_issue_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    quantity DECIMAL(18,3) NOT NULL,

    CONSTRAINT fk_stock_issue_item_issue
        FOREIGN KEY (stock_issue_id)
        REFERENCES stock_issues(id),

    CONSTRAINT fk_stock_issue_item_material
        FOREIGN KEY (material_id)
        REFERENCES materials(id),

    CONSTRAINT chk_stock_issue_item_quantity
        CHECK (quantity > 0)
);

-- 8. PREPARATION BATCHES
CREATE TABLE preparation_batches (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    branch_id BIGINT NOT NULL,
    batch_code VARCHAR(50) NOT NULL UNIQUE,
    created_by BIGINT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    note NVARCHAR(500) NULL,

    CONSTRAINT fk_preparation_batch_branch
        FOREIGN KEY (branch_id)
        REFERENCES branches(id)
);

-- 9. PREPARATION BATCH ITEMS
CREATE TABLE preparation_batch_items (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    preparation_batch_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    item_role VARCHAR(20) NOT NULL,
    quantity DECIMAL(18,3) NOT NULL,

    CONSTRAINT fk_preparation_item_batch
        FOREIGN KEY (preparation_batch_id)
        REFERENCES preparation_batches(id),

    CONSTRAINT fk_preparation_item_material
        FOREIGN KEY (material_id)
        REFERENCES materials(id),

    CONSTRAINT chk_preparation_item_role
        CHECK (item_role IN ('INPUT', 'OUTPUT')),

    CONSTRAINT chk_preparation_item_quantity
        CHECK (quantity > 0)
);