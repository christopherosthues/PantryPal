package org.darchacheron.pantrypal

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform