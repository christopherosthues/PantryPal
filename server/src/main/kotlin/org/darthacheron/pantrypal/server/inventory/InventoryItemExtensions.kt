package org.darthacheron.pantrypal.server.inventory

import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
fun InventoryItemDAO.toBasicDto() = this.toDto()
