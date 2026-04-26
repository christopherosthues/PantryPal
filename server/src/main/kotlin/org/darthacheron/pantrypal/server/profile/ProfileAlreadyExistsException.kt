package org.darthacheron.pantrypal.server.profile

class ProfileAlreadyExistsException(message: String, val detail: String) : Exception(message)