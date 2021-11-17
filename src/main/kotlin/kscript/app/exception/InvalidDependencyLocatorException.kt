package kscript.app.exception

class InvalidDependencyLocatorException(locator: String) : RuntimeException("Invalid dependency locator: '$locator'")
