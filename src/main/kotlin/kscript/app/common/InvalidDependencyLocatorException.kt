package kscript.app.common

class InvalidDependencyLocatorException(locator: String) : RuntimeException("Invalid dependency locator: '$locator'")
