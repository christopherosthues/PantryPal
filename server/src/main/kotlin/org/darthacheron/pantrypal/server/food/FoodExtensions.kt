package org.darthacheron.pantrypal.server.food

import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
fun FoodDAO.toBasicDto() = this.toDto()
