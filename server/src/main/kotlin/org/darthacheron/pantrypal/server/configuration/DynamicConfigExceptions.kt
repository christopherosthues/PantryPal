package org.darthacheron.pantrypal.server.configuration

class RegistrationDisabledException(message: String = "Registration is currently disabled") : Exception(message)

class RemoteSyncDisabledException(message: String = "Remote synchronization is currently disabled") : Exception(message)

class ImageTooLargeException(maxSizeMB: Int) : Exception("Image size exceeds limit of $maxSizeMB MB")

class UnsupportedImageTypeException(message: String = "This image type is not supported") : Exception(message)
