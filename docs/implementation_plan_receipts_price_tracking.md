# Implementation Plan: Price Tracking and Receipt Management

This document outlines the plan for implementing price tracking for inventory items and the ability to manage receipts within PantryPal.

## 1. Data Layer Changes

### Entities

#### ReceiptEntity
- **Table Name**: `receipts`
- **Fields**:
    - `id`: Uuid (Primary Key)
    - `serverId`: Uuid? (For sync)
    - `profileId`: Uuid (Foreign Key to Profile)
    - `storeName`: String?
    - `date`: LocalDate
    - `createdAt`: Instant
    - `lastModifiedAt`: Instant
- **Relationships**: One-to-Many with `ImageEntity` (via `receiptId`).

#### PricePointEntity
- **Table Name**: `price_points`
- **Fields**:
    - `id`: Uuid (Primary Key)
    - `serverId`: Uuid? (For sync)
    - `profileId`: Uuid (Foreign Key to Profile)
    - `inventoryItemId`: Uuid (Foreign Key to InventoryItem)
    - `receiptId`: Uuid? (Optional Foreign Key to Receipt)
    - `price`: Float (The purchase price)
    - `date`: LocalDate (Date of purchase/entry)
    - `createdAt`: Instant
    - `lastModifiedAt`: Instant
- **Goal**: Track the price of an inventory item over time.

#### ImageEntity (Update)
- Add `receiptId`: Uuid? as an optional foreign key to `ReceiptEntity`.
- Update `indices` and `foreignKeys` in `@Entity` annotation.

### DAOs
- **ReceiptDao**: CRUD operations for receipts.
- **PricePointDao**: 
    - Insert new price points.
    - Query price history for a specific `inventoryItemId`.
    - Query price history for a specific `profileId`.

## 2. Database Update
- Update `PantryPalDatabase.kt` to include `ReceiptEntity` and `PricePointEntity`.
- Add `receiptDao` and `pricePointDao` abstract properties.
- Increment database version if necessary (or handle migration).

## 3. Business Logic (Repository)

### ReceiptRepository
- Handle saving receipt images and metadata.
- Provide streams of receipts.

### InventoryRepository (Update)
- When adding a price to an inventory item, automatically create a `PricePointEntity`.
- Provide a way to fetch the price history for an item.

## 4. User Interface

### Navigation
- Add a "Receipts" item to the bottom navigation bar.

### Screens
#### Receipt List Screen
- Display a list of captured receipts with their dates and thumbnails.
- Floating Action Button (FAB) to capture a new receipt.

#### Receipt Detail Screen
- Show the full receipt image.
- List of "tagged" inventory items and their prices.
- Ability to add/remove inventory items from the receipt.

#### Inventory Detail Screen (Update)
- Display a "Price History" section.
- Implement a line chart showing price over time using a KMP-friendly library (e.g., a simple custom canvas implementation or a library like `Compose-Charts`).

#### Food/Inventory Entry (Update)
- When adding a new item, allow selecting a receipt to link it to and specify the price.

## 5. Synchronization (Phase 2)
- Update network services to sync `Receipts` and `PricePoints`.
